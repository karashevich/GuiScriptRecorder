package compile

/**
 * @author Sergey Karashevich
 */
import ScriptGenerator
import actions.PerformScriptAction
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.net.URL
import java.nio.file.Paths

/**
 * @author Sergey Karashevich
 */
object KotlinCompileUtil {

    private val LOG by lazy { Logger.getInstance("#${KotlinCompileUtil::class.qualifiedName}") }

    fun compile(codeString: String) {
        LocalCompiler().compileOnPooledThread(ScriptGenerator.ScriptWrapper.wrapScript(codeString), getAllUrls().map { Paths.get(it.toURI()).toFile().path})
    }

    fun compileAndRun(codeString: String) {
        LocalCompiler().compileAndRunOnPooledThread(ScriptGenerator.ScriptWrapper.wrapScript(codeString), getAllUrls().map { Paths.get(it.toURI()).toFile().path})
    }

    fun getAllUrls(): List<URL> {
        if (ServiceManager::class.java.classLoader.javaClass.name.contains("Launcher\$AppClassLoader")) {
            //lets substitute jars with a common lib dir to avoid Windows long path error
            val urls = ServiceManager::class.java.classLoader.forcedUrls()
            val libUrl = urls.filter { url -> (url.file.endsWith("idea.jar") && File(url.path).parentFile.name == "lib" ) }.firstOrNull()!!.getParentURL()
            urls.filter { url -> !url.file.startsWith(libUrl.file) }.plus(libUrl).toSet()

            //add git4idea urls to allow git configuration from local runner
            if (System.getenv("TEAMCITY_VERSION") != null) urls.plus(getGit4IdeaUrls())

            if (!ApplicationManager.getApplication().isUnitTestMode)
                urls.plus(ServiceManager::class.java.classLoader.forcedBaseUrls())
            return urls.toList()
        }

        var list: List<URL> = (ServiceManager::class.java.classLoader.forcedUrls()
                + PerformScriptAction::class.java.classLoader.forcedUrls())
        if (!ApplicationManager.getApplication().isUnitTestMode)
            list += ServiceManager::class.java.classLoader.forcedBaseUrls()
        return list
    }

    fun getGit4IdeaUrls(): List<URL> {
        val git4IdeaPluginClassLoader = PluginManager.getPlugins().filter { pluginDescriptor -> pluginDescriptor.name.toLowerCase() == "git integration" }.firstOrNull()!!.pluginClassLoader
        val urls = git4IdeaPluginClassLoader.forcedUrls()
        val libUrl =  urls.filter { url -> (url.file.endsWith("git4idea.jar") && File(url.path).parentFile.name == "lib" ) }.firstOrNull()!!.getParentURL()
        urls.filter { url -> !url.file.startsWith(libUrl.file) }.plus(libUrl).toSet()
        return urls
    }


    fun URL.getParentURL() = File(this.file).parentFile.toURI().toURL()

    fun ClassLoader.forcedUrls(): List<URL> {

        val METHOD_DEFAULT_NAME: String = "getUrls"
        val METHOD_ALTERNATIVE_NAME: String = "getURLs"

        var methodName: String = METHOD_DEFAULT_NAME

        if (this.javaClass.methods.any { mtd ->  mtd.name == METHOD_ALTERNATIVE_NAME}) methodName = METHOD_ALTERNATIVE_NAME
        val method = this.javaClass.getMethod(methodName)
        method.isAccessible
        val methodResult = method.invoke(this)
        val myList: List<*> = (methodResult as? Array<*>)?.asList() ?: methodResult as List<*>
        return myList.filterIsInstance(URL::class.java)
    }

    fun ClassLoader.forcedBaseUrls(): List<URL> {
        try {
            return ((this.javaClass.getMethod("getBaseUrls").invoke(this) as? List<*>)!!.filter { it is URL && it.protocol == "file" && !it.file.endsWith("jar!") }) as List<URL>

        } catch (e: NoSuchMethodException) {
            return emptyList()
        }


    }

}