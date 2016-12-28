package compile

import GlobalActionRecorder
import ScriptGenerator
import ScriptGenerator.wrapScriptWithFunDef
import actions.PerformScriptAction
import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testGuiFramework.script.GuiTestCases
import com.intellij.util.io.URLUtil
import com.intellij.util.lang.UrlClassLoader
import java.io.File
import java.net.URL
import java.util.*
import java.util.function.Consumer

/**
 * @author Sergey Karashevich
 */
object KotlinCompileUtil {

    private val LOG by lazy { Logger.getInstance("#${KotlinCompileUtil::class.qualifiedName}") }

    fun compiledAndEvalWithNotifier(codeString: String, notifier: Consumer<String>){

        withDiffContextClassLoader {
            withIsolatedClassLoader { isolatedKotlinLibClassLoader ->
                LOG.info(isolatedKotlinLibClassLoader.forced_urls().joinToString { it.toString() })
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

        val requirementKotlinJars = arrayListOf("kotlin-daemon-client.jar",
                "kotlin-runtime.jar",
                "kotlin-reflect.jar",
                "kotlin-script-runtime.jar")

        val kotlinPluginClassLoader = org.jetbrains.kotlin.daemon.client.KotlinRemoteReplCompiler::class.java.classLoader
        val kotlinJarsUrlList = kotlinPluginClassLoader.forced_urls().filter { requirementKotlinJars.contains(URLUtil.urlToFile(it).name) }

        //add GuiScriptRecorder.jar or classes
        val pluginJarUrl = (classLoader as PluginClassLoader).urls.filter { it.toString().endsWith("GuiScriptRecorder.jar") }.firstOrNull()

        //add all jars to url list
        urls.add(pluginJarUrl ?: classLoader.getResource("compile/DaemonCompiler.class").getParentURL().getParentURL())
        urls.addAll(kotlinJarsUrlList)
        urls.add(getKotlinCompilerUrl(kotlinPluginClassLoader))

        return urls
    }

    fun getKotlinCompilerUrl(kotlinPluginClassLoader: ClassLoader?): URL {

        //find kotlin-compiler.jar and add it to url list
        val urlBase = kotlinPluginClassLoader!!.forced_urls().first()
        val providedFile = URLUtil.urlToFile(urlBase).parentFile.parentFile //jump up to Kotlin dir
        var kotlinCompilerJarFile: File? = null
        FileUtil.processFilesRecursively(providedFile, { file -> if (file.name == "kotlin-compiler.jar") kotlinCompilerJarFile = file; true })
        return kotlinCompilerJarFile!!.toURI().toURL()
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


    fun getAllUrls(): List<URL> = (ServiceManager::class.java.classLoader.forced_urls() + PerformScriptAction::class.java.classLoader.forced_urls())

    fun URL.getParentURL() = this.toFile()!!.parentFile.toURI().toURL()

    @Suppress("UNCHECKED_CAST")
    fun ClassLoader.forced_urls() = ((this.javaClass.getMethod("getUrls").invoke(this) as? List<*>)!!.filter{it is URL }) as List<URL>
}

