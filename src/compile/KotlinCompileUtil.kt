package compile;

/**
 * @author Sergey Karashevich
 */
import ScriptGenerator
import actions.PerformScriptAction
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
    fun getAllUrls(): List<URL> =
            (ServiceManager::class.java.classLoader.forcedUrls() + ServiceManager::class.java.classLoader.forcedBaseUrls() + PerformScriptAction::class.java.classLoader.forcedUrls())

    fun URL.getParentURL() = File(this.file)!!.parentFile.toURI().toURL()

    fun ClassLoader.forcedUrls() = ((this.javaClass.getMethod("getUrls").invoke(this) as? List<*>)!!.filter { it is URL }) as List<URL>

    fun ClassLoader.forcedBaseUrls() = ((this.javaClass.getMethod("getBaseUrls").invoke(this) as? List<*>)!!.filter { it is URL && it.protocol == "file" && !it.file.endsWith("jar!") }) as List<URL>

}