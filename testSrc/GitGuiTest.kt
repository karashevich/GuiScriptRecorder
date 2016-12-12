import com.intellij.dvcs.ui.CloneDvcsDialog
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.testGuiFramework.fixtures.IdeaDialogFixture
import com.intellij.testGuiFramework.fixtures.MessagesFixture
import com.intellij.testGuiFramework.fixtures.ToolWindowFixture
import com.intellij.testGuiFramework.fixtures.WelcomeFrameFixture
import com.intellij.testGuiFramework.framework.GuiTestUtil
import com.intellij.testGuiFramework.impl.GuiTestCase
import com.intellij.ui.EditorComboBox
import org.fest.swing.edt.GuiActionRunner
import org.fest.swing.edt.GuiTask
import org.fest.swing.fixture.DialogFixture
import org.junit.Ignore
import org.junit.Test


//Test from IntelliJ IDEA guiTestFramework
class GitGuiTest : GuiTestCase() {

  @Test @Ignore
  fun testGitImport(){
    val vcsName = "Git"
    val gitApp = "path_to_git_repo"
    val projectPath = getMasterProjectDirPath(gitApp)

    val welcomeFrame = WelcomeFrameFixture.find(myRobot)
    welcomeFrame.checkoutFrom()
//    JBListPopupFixture.findListPopup(myRobot).invokeAction(vcsName)

    val cloneVcsDialog = DialogFixture(myRobot, IdeaDialogFixture.find(myRobot, CloneDvcsDialog::class.java).dialog) //don't miss robot as the first argument or you'll stuck with a deadlock
    with(cloneVcsDialog) {
      val labelText = DvcsBundle.message("clone.repository.url", vcsName)
      val editorComboBox = myRobot.finder().findByLabel(this.target(), labelText, EditorComboBox::class.java)
      GuiActionRunner.execute(object : GuiTask() {
        @Throws(Throwable::class)
        override fun executeInEDT() {
          editorComboBox.text = projectPath.absolutePath
        }
      })
      GuiTestUtil.findAndClickButton(this, DvcsBundle.getString("clone.button"))
    }
    MessagesFixture.findByTitle(myRobot, welcomeFrame.target(), VcsBundle.message("checkout.title")).clickYes()
    val dialog1 = com.intellij.testGuiFramework.fixtures.DialogFixture.find(myRobot, "Import Project")
    with (dialog1) {
      GuiTestUtil.findAndClickButton(this, "Next")
      val textField = GuiTestUtil.findTextField(myRobot, "Project name:").click()
      GuiTestUtil.findAndClickButton(this, "Next")
      GuiTestUtil.findAndClickButton(this, "Next")
      GuiTestUtil.findAndClickButton(this, "Next") //libraries
      GuiTestUtil.findAndClickButton(this, "Next") //module dependencies
      GuiTestUtil.findAndClickButton(this, "Next") //select sdk
      GuiTestUtil.findAndClickButton(this, "Finish")
    }
    val ideFrame = findIdeFrame()
    ideFrame.waitForBackgroundTasksToFinish()

    val projectView = ideFrame.projectView
    val testJavaPath = "src/First.java"
    val editor = ideFrame.editor
    editor.open(testJavaPath)

    ToolWindowFixture.showToolwindowStripes(myRobot)

    //prevent from ProjectLeak (if the project is closed during the indexing
    DumbService.getInstance(ideFrame.project).waitForSmartMode()

  }
}

