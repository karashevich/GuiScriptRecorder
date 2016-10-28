
package compile

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import junit.framework.Assert.assertNotNull
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
import java.io.File
import java.net.URL

/**
 * @author Sergey Karashevich
 */


object DaemonCompiler {

    val compilerId by lazy(LazyThreadSafetyMode.NONE) {
        CompilerId.makeCompilerId(getKotlinLibUrls().map{ it -> it.toFile() } as List<File>)
    }

    //load via reflection from PerformActionScript.kt
    @Suppress("unused")
    fun compileAndEval(code: String, classpathFromClassloader: List<File>,  templateClassName: String, evaluatorClassLoader: ClassLoader?) {
        withDaemon { daemon ->
            withDisposable { disposable ->
                val repl = KotlinRemoteReplCompiler(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
                        classpathFromClassloader,
                        templateClassName,
                        System.err)

                val localEvaluator = GenericReplCompiledEvaluator(emptyList(),
                        evaluatorClassLoader) //PluginClassLoader::class.java.classLoader

                doReplWithLocalEval(repl, localEvaluator, code)
            }
        }

    }


    private fun doReplWithLocalEval(repl: KotlinRemoteReplCompiler, localEvaluator: GenericReplCompiledEvaluator, code: String) {

        val codeLine = ReplCodeLine(0, code)
        val compileResult = repl.compile(codeLine, emptyList())
        val res1c = compileResult as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $compileResult", res1c)

        val evalResult = localEvaluator.eval(codeLine, emptyList(), res1c!!.classes, res1c.hasResult, res1c.newClasspath)
        val checkEvalResult = evalResult as? ReplEvalResult.UnitResult
        TestCase.assertNotNull("Unexpected eval result: $evalResult", checkEvalResult)
    }


    fun withDaemon(body: (CompileService) -> Unit) {
        withFlagFile("currentTest", ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = (getRunFilesPath().path))
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false)
            val daemon: CompileService? = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)

            body(daemon!!)
        }
    }
}


fun getKotlinLibUrls(): List<URL> {
    val url1 = DaemonCompiler::class.java.classLoader.getResource("libxx/kotlin-compiler.jar")
    val url2 = DaemonCompiler::class.java.classLoader.getResource("libxx/kotlin-daemon-client.jar")
    return listOf(url1, url2)
}

fun getRunFilesPath() = DaemonCompiler::class.java.classLoader.getResource("compile/DaemonCompiler.class").toFile()!!.parentFile


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
