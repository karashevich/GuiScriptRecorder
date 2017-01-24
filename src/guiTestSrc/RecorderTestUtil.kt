import actions.PerformScriptAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.EdtInvocationManager
import components.GuiRecorderComponent
import components.GuiRecorderComponent.States
import org.fest.swing.timing.Condition
import org.fest.swing.timing.Pause
import org.fest.swing.timing.Timeout
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * @author Sergey Karashevich
 */
object RecorderTestUtil {

    private fun getTestDataPath() : String {
        val resourceURL : URL = this.javaClass.getResource("${this.javaClass.canonicalName}.class")
        val guiTestDir = File(resourceURL.path).parentFile
        val guiTestDirName = guiTestDir.name
        val buildDir = guiTestDir.parentFile.parentFile
        return "${buildDir.path}${File.separator}resources${File.separator}$guiTestDirName"
    }

    fun insertCode(code: String) = EdtInvocationManager.getInstance().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction {
            GuiRecorderComponent.getEditor().document.setText(code)
        }
    }

    fun runCode(timeout: Long = 300, timeUnit: TimeUnit = TimeUnit.SECONDS) {
        EdtInvocationManager.getInstance().invokeAndWait { PerformScriptAction().actionPerformed(null) }
        GuiRecorderComponent.setState(States.TEST_INIT)
        Pause.pause(object: Condition("Waiting when script is performed for $timeout ${timeUnit.toString()}") {
            override fun test() = (
                    GuiRecorderComponent.getState() == States.IDLE
                    || GuiRecorderComponent.getState() == States.RUNNING_ERROR) }, Timeout.timeout(timeout, timeUnit))
    }

    fun getCodeSnippet(name: String): String {
        val str = FileUtil.loadFile(File(getTestDataPath() + File.separator + name))
        if (SystemInfo.isWindows) return StringUtil.convertLineSeparators(str)
        else return str
    }

}