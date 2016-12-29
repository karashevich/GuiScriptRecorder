package compile

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.maven.K2JVMCompileMojo
import java.io.File

/**
 * @author Sergey Karashevich
 */
class LocalCompiler {

    val helloKtText = "fun main(args: Array<String>) { \n println(\"Hello, World!\") \n }"

    inner class MyCompiler : K2JVMCompileMojo() {
    }

    val myCompiler by lazy {
        MyCompiler().execute()
    }

    fun createTempFile(code: String) : File {
        val tempFile = FileUtil.createTempFile("CurrentTest", ".kt", true)
        FileUtil.writeToFile(tempFile, code, false)
        return tempFile
    }

    fun compile(fileKt: File, classpath: String){

    }
}
