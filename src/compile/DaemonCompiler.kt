
package compile

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import compile.KotlinCompileUtil.forced_urls
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
import java.util.*
import java.util.function.Consumer

/**
 * @author Sergey Karashevich
 */

object DaemonCompiler {

    private var myNotifier: Consumer<String>? = null

    fun setNotifier(extNotifier: Consumer<String>) {
        myNotifier = extNotifier
    }

    val compilerId by lazy(LazyThreadSafetyMode.NONE) {
        CompilerId.makeCompilerId(getKotlinLibUrls().map{ it -> it.toFile() } as List<File>)
    }

    //load via reflection from PerformActionScript.kt
    @Suppress("unused")
    fun compileAndEval(code: String, classpathFromClassloader: List<File>, templateClassName: String, evaluatorClassLoader: ClassLoader?) {
        status("<long>Connecting to kotlin compile daemon")
        withDaemon { daemon ->
            withDisposable { disposable ->
                val repl = KotlinRemoteReplCompiler(disposable, daemon, null, CompileService.TargetPlatform.JVM,
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

        val startTime = Date().time
        status("<long>Compilation started...")

        val codeLine = ReplCodeLine(0, code)
//        val myEmptyList = emptyList() as kotlin.collections.Iterable<org.jetbrains.kotlin.cli.common.repl.ReplCodeLine>
        val compileResult = repl.compile(codeLine, emptyList())
        status("Compilation completed (${(Date().time - startTime)}ms)")
        val res1c = compileResult as? ReplCompileResult.CompiledClasses
        if (res1c == null ) status("Compile error: see details in idea.log")
        assert(res1c != null) //Unexpected compile result

        status("Evaluation started")
        val evalResult = localEvaluator.eval(codeLine, emptyList(), res1c!!.classes, res1c.hasResult, res1c.classpathAddendum)
        val checkEvalResult = evalResult as? ReplEvalResult.UnitResult
    }


    fun withDaemon(body: (CompileService) -> Unit) {
        withFlagFile("currentTest", ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(verbose = true, reportPerf = true)
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false)
            val daemon: CompileService? = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assert(daemon != null) //failed to connect daemon
            if (daemon == null) status("Failed connect to kotlin compile daemon")
            body(daemon!!)
        }
    }

    fun status(str: String) = myNotifier!!.accept(str)
}

fun getKotlinLibUrls(): List<URL> {
    val kotlinPluginClassLoader = org.jetbrains.kotlin.daemon.client.KotlinRemoteReplCompiler::class.java.classLoader
    // should be added by previous call of kotlinPluginClassLoader KotlinComileUtil.getKotlinCompilerUrl()
    val url1 = kotlinPluginClassLoader.forced_urls().filter { it.toString().contains("kotlin-compiler.jar") }.single()
    val url2: URL = kotlinPluginClassLoader.forced_urls().filter { it.toString().contains("kotlin-daemon-client.jar") }.single()
    return listOf(url1, url2)
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

