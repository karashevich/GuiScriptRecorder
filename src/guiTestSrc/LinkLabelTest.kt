package guiTestSrc

import RecorderTestUtil
import actions.ShowGuiEditorWindowAction
import com.intellij.testGuiFramework.impl.GuiTestCase
import com.intellij.util.ui.EdtInvocationManager
import org.junit.Test

/**
 * @author Sergey Karashevich
 */
class LinkLabelTest: GuiTestCase() {

    override fun setUp() {
        super.setUp()
        EdtInvocationManager.getInstance().invokeAndWait{ ShowGuiEditorWindowAction().actionPerformed(null) }
    }

    @Test
    fun testLinkLabel(){
        val code = RecorderTestUtil.getCodeSnippet("linkLabel.ktx")
        RecorderTestUtil.insertCode(code)
        RecorderTestUtil.runCode()
    }

}
