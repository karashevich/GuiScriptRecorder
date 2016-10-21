package logic

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.repl.GenericReplCompiledEvaluator
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.client.KotlinRemoteReplCompiler
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.utils.PathUtil
import test.DefaultScript
import java.io.File
import java.net.URL
import java.util.concurrent.Future

/**
 * @author Sergey Karashevich
 */


object KotlinKompilerUtil {

    val compilerClassPath = listOf(
            File(getCompilerLib(), "kotlin-compiler.jar"))
    val daemonClientClassPath = listOf(File(getCompilerLib(), "kotlin-daemon-client.jar"),
            File(getCompilerLib(), "kotlin-compiler.jar"))
    val compilerId by lazy(LazyThreadSafetyMode.NONE) {
        CompilerId.makeCompilerId(compilerClassPath)
    }

    fun kompileAndEval(code: String) {
        withDaemon { daemon ->
            withDisposable { disposable ->
                val repl = KotlinRemoteReplCompiler(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
                        classpathFromClassloader(),
                        ScriptWithNoParam::class.qualifiedName!!,
                        System.err)

                val localEvaluator = GenericReplCompiledEvaluator(emptyList(),
                        PluginClassLoader::class.java.classLoader)

                doReplWithLocalEval(repl, localEvaluator, code)
            }
        }

    }

//    fun kompileAndEval(code: String) {
//        withDaemon { daemon ->
//            withDisposable { disposable ->
//                val repl = KotlinRemoteReplCompiler(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
//                        classpathFromClassloader(),
//                        ScriptWithNoParam::class.qualifiedName!!,
//                        System.err)
//
//                val localEvaluator = GenericReplCompiledEvaluator(emptyList(),
//                        PluginClassLoader::class.java.classLoader)
//
//                doReplWithLocalEval(repl, localEvaluator, code)
//            }
//        }
//
//    }

    fun classpathFromPluginClassloader(): List<File> {

        //here could be two different PluginClassLoaders from PluginClassLoader itself and from UrlClassLoader
        //(when we call it TestKotlinScriptDummyDependenciesResolver::class.java.classLoader)
        //to avoid ClassCastException let's walk around it with reflection chain

        val originalClassLoader = (TestKotlinScriptDummyDependenciesResolver::class.java.classLoader.javaClass).classLoader
        val probablyPluginClassloader = TestKotlinScriptDummyDependenciesResolver::class.java.classLoader
        val probablyPluginClassloaderClass = probablyPluginClassloader.javaClass
        assert(probablyPluginClassloaderClass.name.equals("com.intellij.ide.plugins.cl.PluginClassLoader"))
        val getUrlsMethod = probablyPluginClassloaderClass.getMethod("getUrls")
        assertNotNull(getUrlsMethod)

        val urls = getUrlsMethod.invoke(probablyPluginClassloader) as List<URL>
        val result = urls?.mapNotNull(URL::toFile)
        return result
    }

//    fun classpathFromUrlClassloader(): List<File> {
//
//        val originalUrlClassLoader = (GuiTestCase::class.java).classLoader
//
//        val getUrlsMethod = originalUrlClassLoader.javaClass.getMethod("getUrls")
//        assertNotNull(getUrlsMethod)
//
//        val urls = getUrlsMethod.invoke(originalUrlClassLoader) as List<URL>
//        val result = urls?.mapNotNull(URL::toFile)
//        return result
//    }

    fun classpathFromClassloader(): List<File> {
//        return classpathFromUrlClassloader() + classpathFromPluginClassloader()
        return classpathFromPluginClassloader()

    }


    private fun doReplWithLocalEval(repl: KotlinRemoteReplCompiler, localEvaluator: GenericReplCompiledEvaluator, code: String) {
//        val checkResult = repl.check(ReplCodeLine(0, code), emptyList())
//        TestCase.assertTrue("Unexpected check results: $checkResult", checkResult is ReplCheckResult.Incomplete)

        val codeLine = ReplCodeLine(0, code)
        val compileResult = repl.compile(codeLine, emptyList())
        val res1c = compileResult as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $compileResult", res1c)

        val evalResult = localEvaluator.eval(codeLine, emptyList(), res1c!!.classes, res1c.hasResult, res1c.newClasspath)
        val checkEvalResult = evalResult as? ReplEvalResult.UnitResult
        TestCase.assertNotNull("Unexpected eval result: $evalResult", checkEvalResult)
    }


    fun withDaemon(body: (CompileService) -> Unit) {
//        FileUtil.createTempDirectory("currentTestDir", "currentTest")
        withFlagFile("currentTest", ".alive") { flagFile ->
            val scriptFile = PathUtil.getResourcePathForClass(DefaultScript::class.java)
            val daemonOptions = DaemonOptions(runFilesPath = (scriptFile.absolutePath + "/test")) //todo: replace with shared dir
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false)
            val daemon: CompileService? = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)

            body(daemon!!)
        }
    }
}

internal fun getCompilerLib(): File {
    val file = PathUtil.getKotlinPathsForCompiler().getLibPath().getAbsoluteFile()
    assertTrue("Lib directory doesn't exist. Run 'ant dist'", file.isDirectory())
    return file
}

internal inline fun withDisposable(body: (Disposable) -> Unit) {
    val disposable = Disposer.newDisposable()
    try {
        body(disposable)
    } finally {
        Disposer.dispose(disposable)
    }
}

internal fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        } catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

internal inline fun withFlagFile(prefix: String, suffix: String? = null, body: (File) -> Unit) {
    val file = createTempFile(prefix, suffix)
    try {
        body(file)
    } finally {
        file.delete()
    }
}

open class TestKotlinScriptDummyDependenciesResolver : ScriptDependenciesResolver {

    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?>
    {
        return object : KotlinScriptExternalDependencies {
            override val classpath: Iterable<File> = KotlinKompilerUtil.classpathFromClassloader()
            override val imports: Iterable<String> = listOf()
        }.asFuture()
    }
}

@ScriptTemplateDefinition(resolver = TestKotlinScriptDummyDependenciesResolver::class)
abstract class ScriptWithNoParam()

