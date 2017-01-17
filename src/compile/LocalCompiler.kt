package compile

import ScriptGenerator
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.lang.UrlClassLoader
import ui.Notifier
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import java.util.stream.Collectors

/**
 * @author Sergey Karashevich
 */
class LocalCompiler {

    private val LOG by lazy { Logger.getInstance("#${LocalCompiler::class.qualifiedName}") }

    val TEST_CLASS_NAME = "CurrentTest"

    val helloKtText = "fun main(args: Array<String>) { \n println(\"Hello, World!\") \n }"
    val kotlinCompilerJarUrl = "http://central.maven.org/maven2/org/jetbrains/kotlin/kotlin-compiler/1.0.6/kotlin-compiler-1.0.6.jar"
    val kotlinCompilerJarName = "kotlin-compiler-1.0.6.jar"

    val KOTLINC_PLUGIN_DIR = "kotlinc"

    val tempDir by lazy { FileUtil.createTempDirectory("kotlin-compiler-tmp", null, true) }
    val scriptKt by lazy { createTempFile(helloKtText) }
    var tempFile: File? = null

    private fun createTempFile(code: String): File {
        val tempFile = FileUtil.createTempFile(TEST_CLASS_NAME, ".kt", true)
        FileUtil.writeToFile(tempFile, code, false)
        this.tempFile = tempFile
        return tempFile
    }

    fun compileAndRunOnPooledThread(code: String, classpath: List<String>) {
        ApplicationManager.getApplication().executeOnPooledThread({
            try {
                compile(code, classpath)
                run(classpath)
            } catch (ce: CompilationException) {
                LOG.error(ce.message)
            }
        })
    }

    private fun run(classpath: List<String>) {
        if (tempFile == null) throw Exception("Unable to find tempFile")
//        findClass
        if (!tempDir.listFiles().any { file -> (file.name.contains(TEST_CLASS_NAME) && file.extension == "class")}) throw Exception("Unable to locate compiled class files")
        val pluginClassLoader = this.javaClass.classLoader as PluginClassLoader
        pluginClassLoader.addLibDirectories(listOf(tempDir.path))

        //create a copy of a plugin classloader
        val urlClassLoader = UrlClassLoader.build().parent(ApplicationManager::class.java.classLoader).urls(classpath.map { it -> File(it).toURI().toURL() }.plus(tempDir.toURI().toURL())).get()
        val currentTest = urlClassLoader.loadClass(TEST_CLASS_NAME) ?: throw Exception("Unable to load by pluginClassLoader $TEST_CLASS_NAME.class file")
        val testCase = currentTest.newInstance()
        val setUpMethod = currentTest.getMethod("setUp")
        val testMethod = currentTest.getMethod(ScriptGenerator.ScriptWrapper.TEST_METHOD_NAME)
        Notifier.updateStatus("<long>Script running...")
        setUpMethod.invoke(testCase)
        testMethod.invoke(testCase)
        Notifier.updateStatus("Script stopped")
    }

    fun compileOnPooledThread(code: String, classpath: List<String>) {
        ApplicationManager.getApplication().executeOnPooledThread(Callable<Boolean> {
            compile(code, classpath)
        })
    }

    fun compile(code: String, classpath: List<String>): Boolean{
        val tempFile = createTempFile(code)
        return compile(tempFile, classpath.joinToString(separator = if(SystemInfo.isWindows) ";" else ":"))
    }

    private fun compile(fileKt: File? = null, classpath: String? = null): Boolean {

        val helloKt = fileKt ?: scriptKt
        val kotlinCompilerJar = getKotlinCompilerJar()
        val libDirLocation = getApplicationLibDir().parentFile

        Notifier.updateStatus("<long>Compiling...")
        val compilationProcessBuilder = if(classpath == null)
            ProcessBuilder("java", "-jar",
                kotlinCompilerJar.path,
                "-kotlin-home", libDirLocation.path,
                "-d", tempDir.path,
                helloKt.path) else
            ProcessBuilder("java", "-jar",
                    kotlinCompilerJar.path,
                    "-kotlin-home", libDirLocation.path,
                    "-d", tempDir.path,
                    "-cp", classpath,
                    helloKt.path)
        val process = compilationProcessBuilder.start()
        val wait = process.waitFor(120, TimeUnit.MINUTES)
        assert(wait)
        if (process.exitValue() == 1) {
            LOG.error(BufferedReader(InputStreamReader(process.errorStream)).lines().collect(Collectors.joining("\n")))
            Notifier.updateStatus("Compilation error (see idea.log)")
            throw CompilationException("Compilation error (see idea.log)")
        } else {
            Notifier.updateStatus("Compilation is done")
        }
        return wait
    }

    private fun getKotlinCompilerJar(): File {
        val kotlinCompilerDir = getPluginKotlincDir()
        if (!isKotlinCompilerDir(kotlinCompilerDir)) downloadKotlinCompilerJar(kotlinCompilerDir.path)
        val kotlinCompilerJar = kotlinCompilerDir.listFiles().filter { file -> file.name.contains("kotlin-compiler") }.firstOrNull()
                ?: throw FileNotFoundException("Unable to find kotlin-compiler*.jar in ${kotlinCompilerDir.path} directory")

        return kotlinCompilerJar
    }

    private fun getApplicationLibDir(): File {
        return File(PathManager.getLibPath())
    }

    private fun getPluginKotlincDir(): File {

        if (ApplicationManager.getApplication().isUnitTestMode) return File(PathManager.getTempPath())

        val pluginId = (this.javaClass.classLoader as PluginClassLoader).pluginId
        val pluginPath = PluginManager.getPlugin(pluginId)!!.path
        val kotlinCompilerDirPath = pluginPath.path + File.separator + KOTLINC_PLUGIN_DIR
        val kotlinCompilerDir = File(kotlinCompilerDirPath)

//        ensure that dir for kotlinc is created
        FileUtil.ensureExists(kotlinCompilerDir)
        return kotlinCompilerDir
    }

    private fun isKotlinCompilerDir(dir: File): Boolean {
        return dir.listFiles().any { file -> file.name.contains("kotlin-compiler") }
    }

    private fun unJarKotlinCompiler(jarFile: File, destDir: File) {
        ProcessBuilder("jar", "")
    }


    private fun unJarKotlinCompiler2(jarFile: File, destDir: File) {
        val jar = JarFile(jarFile.path)
        val enumEntries = jar.entries()
        //TODO: ProgressManager here
        while (enumEntries.hasMoreElements()) {
            val f = File(destDir.path + File.separator + enumEntries.nextElement().name)
            if (enumEntries.nextElement().isDirectory) { // if its a directory, create it
                f.mkdir()
                continue
            }
            val inStream = jar.getInputStream(enumEntries.nextElement()) // get the input stream
            val fos = java.io.FileOutputStream(f)
            while (inStream.available() > 0) {  // write contents of 'is' to 'fos'
                fos.write(inStream.read())
            }
            fos.close()
            inStream.close()
        }
    }

    fun downloadKotlinCompilerJar(destDirPath: String?): File {

        Notifier.updateStatus("<long>Downloading kotlin-compiler.jar...")
        val downloader = DownloadableFileService.getInstance()
        val description = downloader.createFileDescription(kotlinCompilerJarUrl, kotlinCompilerJarName)
        ApplicationManager.getApplication().invokeAndWait({
            downloader.createDownloader(Arrays.asList(description), kotlinCompilerJarName)
                    .downloadFilesWithProgress(destDirPath, null, null)
        })
        Notifier.updateStatus("kotlin-compiler.jar downloaded successfully")
        return File(destDirPath + File.separator + kotlinCompilerJarName)
    }

    class CompilationException(s: String) : Exception()
}

