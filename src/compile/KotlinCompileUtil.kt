package compile

import GlobalActionRecorder
import ScriptGenerator
import ScriptGenerator.wrapScriptWithFunDef
import actions.PerformScriptAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testGuiFramework.script.GuiTestCases
import com.intellij.util.lang.UrlClassLoader
import java.net.URL
import java.util.*
import java.util.function.Consumer

/**
 * @author Sergey Karashevich
 */
object KotlinCompileUtil {

    fun compiledAndEvalWithNotifier(codeString: String, notifier: Consumer<String>){

        withDiffContextClassLoader {
            withIsolatedClassLoader { isolatedKotlinLibClassLoader ->
                val daemonCompilerCls = isolatedKotlinLibClassLoader.loadClass(DaemonCompiler::class.qualifiedName)
                val loadClassInstance = daemonCompilerCls.getField("INSTANCE").get(null)

                val setNotifierMethod = daemonCompilerCls.getMethod("setNotifier", java.util.function.Consumer::class.java)
                setNotifierMethod.invoke(loadClassInstance, notifier)

                val method = daemonCompilerCls.getMethod("compileAndEval", String::class.java, List::class.java, String::class.java, ClassLoader::class.java)
                method.invoke(loadClassInstance, codeString, getAllUrls().map { it.toFile() }, GuiTestCases::class.qualifiedName, PerformScriptAction::class.java.classLoader)
            }
        }
    }

    fun compileAndEval(codeString: String) {
        withDiffContextClassLoader {
            withIsolatedClassLoader { isolatedKotlinLibClassLoader ->
                val daemonCompilerCls = isolatedKotlinLibClassLoader.loadClass(DaemonCompiler::class.qualifiedName)
                val loadClassInstance = daemonCompilerCls.getField("INSTANCE").get(null)
                val method = daemonCompilerCls.getMethod("compileAndEval", String::class.java, List::class.java, String::class.java, ClassLoader::class.java)
                method.invoke(loadClassInstance, codeString, getAllUrls().map { it.toFile() }, GuiTestCases::class.qualifiedName, PerformScriptAction::class.java.classLoader)
            }
        }
    }

    fun compileAndEvalCurrentTest() = compileAndEval(getCurrentTestText())

    fun compileAndEvalScriptBuffer() = compileAndEval(ScriptGenerator.getWrappedScriptBuffer())

    fun compileAndEvalCode(code: String) = compileAndEval(wrapScriptWithFunDef(code))

    fun compileAndEvalCodeWithNotifier(code: String, notifier: Consumer<String>) = compiledAndEvalWithNotifier(wrapScriptWithFunDef(code), notifier)

    private fun getCurrentTestText() = StreamUtil.readText(GlobalActionRecorder.javaClass.getResourceAsStream("CurrentTest.ktt"), CharsetToolkit.UTF8)

    fun getKotlinLibUrls(): List<URL> {
        val classLoader = DaemonCompiler::class.java.classLoader
        val urls = ArrayList<URL>()

        urls.add(classLoader.getResource("compile/DaemonCompiler.class").getParentURL().getParentURL())
        urls.add(classLoader.getResource("libxx/kotlin-compiler.jar"))
        urls.add(classLoader.getResource("libxx/kotlin-daemon-client.jar"))
        urls.add(classLoader.getResource("libxx/kotlin-runtime.jar"))
        urls.add(classLoader.getResource("libxx/kotlin-reflect.jar"))
        urls.add(classLoader.getResource("libxx/kotlin-script-runtime.jar"))
        urls += getUrlFromUrlClassloader("junit")

        return urls
    }

    fun withDiffContextClassLoader(body: () -> Unit) {
        val threadClassLoader = Thread.currentThread().contextClassLoader
        body()
        Thread.currentThread().contextClassLoader = threadClassLoader
    }

    fun withIsolatedClassLoader(body: (ClassLoader) -> Unit) {
        val isolatedKotlinLibClassLoader = UrlClassLoader.build()
                .urls(getKotlinLibUrls()).get()

        Thread.currentThread().contextClassLoader = isolatedKotlinLibClassLoader
        body(isolatedKotlinLibClassLoader)
    }

    fun getUrlFromUrlClassloader(partOfLibName: String): List<URL> {
        val probablyIdeaClassLoader = ServiceManager::class.java.classLoader //we presume that ServiceManager class is always loaded by IDEA UrlClassLoader
        return probablyIdeaClassLoader.forced_urls().filter{ it.path.contains(partOfLibName) }
    }

    fun getAllUrls(): List<URL> = (ServiceManager::class.java.classLoader.forced_urls() + PerformScriptAction::class.java.classLoader.forced_urls())

    fun URL.getParentURL() = this.toFile()!!.parentFile.toURI().toURL()

    @Suppress("UNCHECKED_CAST")
    fun ClassLoader.forced_urls() = ((this.javaClass.getMethod("getUrls").invoke(this) as? List<*>)!!.filter{it is URL }) as List<URL>
}

