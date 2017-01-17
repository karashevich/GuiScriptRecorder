package compile

/**
 * @author Sergey Karashevich
 */
import ScriptGenerator
import actions.PerformScriptAction
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
        var list: List<URL> = (ServiceManager::class.java.classLoader.forcedUrls()
                + PerformScriptAction::class.java.classLoader.forcedUrls())
        if (!ApplicationManager.getApplication().isUnitTestMode)
            list += ServiceManager::class.java.classLoader.forcedBaseUrls()
        return list
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

    fun ClassLoader.forcedBaseUrls() = ((this.javaClass.getMethod("getBaseUrls").invoke(this) as? List<*>)!!.filter { it is URL && it.protocol == "file" && !it.file.endsWith("jar!") }) as List<URL>

}