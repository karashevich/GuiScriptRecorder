package guiTestSrc

import RecorderTestUtil
import actions.ShowGuiEditorWindowAction
import com.intellij.testGuiFramework.impl.GuiTestCase
import com.intellij.util.ui.EdtInvocationManager
import org.junit.Test

/**
 * @author Sergey Karashevich
 */
class GradleProjectTest: GuiTestCase() {

    override fun setUp() {
        super.setUp()
        EdtInvocationManager.getInstance().invokeAndWait{ ShowGuiEditorWindowAction().actionPerformed(null) }
    }

    @Test
    fun testGradleProject(){
        val code = RecorderTestUtil.getCodeSnippet("gradleProject.ktx")
        RecorderTestUtil.insertCode(code)
        RecorderTestUtil.runCode()
    }

}
