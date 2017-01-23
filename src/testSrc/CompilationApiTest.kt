import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import compile.KotlinCompileUtil
import compile.LocalCompiler
import org.junit.Test
import java.io.File
import java.net.URL
import java.nio.file.Paths

/**
 * @author Sergey Karashevich
 */


class CompilationApiTest(): LightPlatformCodeInsightFixtureTestCase() {

    override fun getTestDataPath() : String {
        val resourceURL : URL = this.javaClass.getResource("${this.javaClass.canonicalName}.class")
        return "${File(resourceURL.path).parentFile.parentFile.parentFile.path}${File.separator}resources${File.separator}test"
    }

    @Test
    fun testCompilationApi() {
        compile(getApi0_5())
    }


    @Test
    fun testCompilationWithErrorApi() {
        var wasCaught = false
        try {
            compile(getWrongApi0_5())
        } catch(ae: AssertionError) {
            assert(ae.message!!.contains("missing '}"))
            wasCaught = true
        }
        assert(wasCaught)
    }

    private fun compile(codeString: String) {
        val result = LocalCompiler().compile(ScriptGenerator.ScriptWrapper.wrapScript(codeString), KotlinCompileUtil.getAllUrls().map { Paths.get(it.toURI()).toFile().path })
        assert (result)
    }

    private fun getTestKtx(name: String) = FileUtil.loadFile(File(testDataPath + File.separator + name))

    fun getApi0_5() = getTestKtx("ScriptApi0_5.ktx")
    fun getWrongApi0_5() = getTestKtx("ScriptWrongApi0_5.ktx")


}