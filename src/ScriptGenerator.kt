import ScriptGenerator.scriptBuffer
import com.intellij.ide.util.newProjectWizard.FrameworksTree
import com.intellij.testGuiFramework.framework.GuiTestUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import org.fest.swing.core.BasicRobot
import ui.KeyUtil
import java.awt.Component
import java.awt.Container
import javax.swing.*

/**
 * @author Sergey Karashevich
 */
object ScriptGenerator {

    val scriptBuffer = StringBuilder()

    fun getScriptBuffer() = scriptBuffer.toString()

    fun getWrappedScriptBuffer(): String {
        if (scriptBuffer.length > 0)
            scriptBuffer.appendln("}")  //close context
        return wrapScriptWithFunDef(scriptBuffer.toString())
    }

    fun wrapScriptWithFunDef(body: String): String {
        val import = "import com.intellij.testGuiFramework.framework.* \n" +
                "import com.intellij.testGuiFramework.fixtures.* \n " +
                "import com.intellij.testGuiFramework.fixtures.newProjectWizard.NewProjectWizardFixture \n" +
                "import org.fest.swing.core.Robot \n" +
                "import java.awt.Component \n" +
                "import com.intellij.openapi.application.ApplicationManager \n" +
                "import org.fest.swing.fixture.*;"

        val injection = "fun clickListItem(itemName: String, robot: Robot, component: Component) {(this as NewProjectWizardFixture).selectProjectType(itemName) }"
        val testFun = "fun testGitImport(){"
        val postfix = "} \n setUp() \n" +
                "testGitImport()"
        return "$import \n " +
//                "$injection \n " +
                "$testFun \n" +
                "$body \n " +
                "$postfix"
    }

    var currentContextComponent: Component? = null
    private var currentContext = Contexts()

//    fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent?) {
//        get action type for script: click, enter text, mark checkbox
//        val component = e!!.getDataContext().getData("Component") as Component
//        checkContext(component)
//        clickCmp(component, e)
//    }

    fun processTyping(keyChar: Char){
        Typer.type(keyChar)
    }

    fun flushTyping() {
        Typer.flushBuffer()
    }

    fun clickCmp(cmp: Component, itemName: String?, clickCount: Int) {
        Typer.flushBuffer()
        checkContext(cmp)
        when (cmp) {
            is JButton -> Writer.write(Templates.findAndClickButton(cmp.text))
            is com.intellij.ui.components.labels.ActionLink -> Writer.write(Templates.clickActionLink(cmp.text))
            is JTextField -> {
                if (clickCount == 1) {
                    val label = getLabel((currentContextComponent as Container?)!!, cmp)
                    if (label == null)
                        Writer.write(Templates.findJTextField())
                    else
                        Writer.write(Templates.findJTextFieldByLabel(label.text))
                } else if (clickCount == 2) {
                    val label = getLabel((currentContextComponent as Container?)!!, cmp)
                    if (label == null)
                        Writer.write(Templates.findJTextFieldAndDoubleClick())
                    else
                        Writer.write(Templates.findJTextFieldByLabelAndDoubleClick(label.text))
                }
            }
            is JBList<*> -> {
                if (isPopupList(cmp))
                    Writer.write(Templates.clickPopupItem(itemName!!))
                else if (isFrameworksTree(cmp))
                else
                    Writer.write(Templates.clickListItem(itemName!!))
            }
            is FrameworksTree -> Writer.write(Templates.clickFrameworksTree(itemName!!))
            is JBCheckBox -> Writer.write(Templates.clickJBCheckBox(cmp.text))
            is JCheckBox -> Writer.write(Templates.clickJCheckBox(cmp.text))
        }
    }

    private fun isPopupList(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("listpopup")
    private fun isFrameworksTree(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("AddSupportForFrameworksPanel".toLowerCase())

    private fun getLabel(container: Container, jTextField: JTextField): JLabel? {
        val robot = BasicRobot.robotWithCurrentAwtHierarchyWithoutScreenLock()
        return GuiTestUtil.findBoundedLabel(container, jTextField, robot)
    }

    fun checkContext(cmp: Component) {
        cmp as JComponent
        if (isPopupList(cmp)) return //dont' change context for a popup menu
        if (currentContextComponent != null && !cmp.rootPane.equals(currentContextComponent)) {
            Writer.write(currentContext.closeContext())
        }
        if (currentContextComponent == null || !cmp.rootPane.equals(currentContextComponent)) {
            currentContextComponent = cmp.rootPane
            when (cmp.rootPane.parent) {
                is JDialog -> {
                    if ((cmp.rootPane.parent as JDialog).title.equals(com.intellij.ide.IdeBundle.message("title.new.project")))
                        Writer.write(currentContext.projectWizardContextStart())
                    else
                        Writer.write(currentContext.dialogContextStart((cmp.rootPane.parent as JDialog).title))
                }
                is com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame -> Writer.write(currentContext.welcomeFrameStart())
                is JFrame -> println("is JFrame")
            }

        }
    }

}

private object Writer{
    fun write(str: String) {
        println(str)
        scriptBuffer.appendln(str)
    }
}

private object Typer{
    val strBuffer = StringBuilder()
    val rawBuffer = StringBuilder()

    fun type(keyChar: Char) {
        strBuffer.append(KeyUtil.patch(keyChar))
        rawBuffer.append("${if(rawBuffer.length > 0) ", " else ""}\"${keyChar.toInt()}\"")
    }

    fun flushBuffer() {
        if (strBuffer.length == 0) return
        Writer.write("//typed:[${strBuffer.length},\"${strBuffer.toString()}\", raw=[${rawBuffer.toString()}]]")
        Writer.write(Templates.typeText(strBuffer.toString()))
        strBuffer.setLength(0)
        rawBuffer.setLength(0)
    }
}

//TEMPLATES
private object Templates {
    fun findDialog(name: String, title: String?) = "val ${name} = DialogFixture.find(robot(), \"${title}\")"
    fun withDialog(name: String) = "with (${name}){"
    fun findProjectWizard(name: String) = "val ${name} = findNewProjectWizard()"
    fun withProjectWizard(name: String) = "with (${name}){"
    fun findWelcomeFrame(name: String) = "val ${name} = findWelcomeFrame()"
    fun withWelcomeFrame(name: String) = "with (${name}){"

    fun findAndClickButton(text: String) = "GuiTestUtil.findAndClickButton(this, \"${text}\")"
    fun clickActionLink(text: String) = "ActionLinkFixture.findActionLinkByName(\"${text}\", robot(), this.target()).click()"

    fun clickPopupItem(itemName: String) = "GuiTestUtil.clickPopupMenuItem(\"${itemName}\", true, this.target(), robot())"
    fun clickListItem(name: String) = "JListFixture(robot(), robot().finder().findByType(this.target(), com.intellij.ui.components.JBList::class.java, true)).clickItem(\"$name\")"

    fun findJTextField() = "val textField = myRobot.finder().findByType(JTextField::class.java)"
    fun findJTextFieldByLabel(labelText: String) = "val textField = findTextField(\"${labelText}\").click()"
    fun findJTextFieldAndDoubleClick() = "val textField = myRobot.finder().findByType(JTextField::class.java).doubleClick()"
    fun findJTextFieldByLabelAndDoubleClick(labelText: String) = "val textField = findTextField(\"${labelText}\").doubleClick()"

    fun typeText(text: String) = "GuiTestUtil.typeText(\"$text\", robot(), 10)"
    fun clickFrameworksTree(itemName: String) = "selectFramework(\"$itemName\")"
    fun clickJBCheckBox(text: String) = "JBCheckBoxFixture.findByText(\"$text\", this.target(), robot(), true).click()"
    fun clickJCheckBox(text: String) = "CheckBoxFixture.findByText(\"$text\", this.target(), robot(), true).click()"
}


private class Contexts() {

    enum class Type {DIALOG, WELCOME_FRAME, PROJECT_WIZARD, IDE_FRAME}

    var dialogCount = 0
    var currentContextType: Type? = null

    fun dialogContextStart(title: String): String {
        currentContextType = Type.DIALOG
        val name = "dialog${dialogCount++}"
        val findDialog = Templates.findDialog(name, title)
        val withDialog = Templates.withDialog(name)
        return findDialog + "\n" + withDialog
    }

    fun projectWizardContextStart(): String {
        currentContextType = Type.PROJECT_WIZARD
        val name = "projectWizard"
        val findProjectWizard = Templates.findProjectWizard(name)
        val withProjectWizard = Templates.withProjectWizard(name)
        return findProjectWizard + "\n" + withProjectWizard
    }

    fun welcomeFrameStart(): String {
        currentContextType = Type.WELCOME_FRAME
        val name = "welcomeFrame"
        val findWelcomeFrame = Templates.findWelcomeFrame(name)
        val withWelcomeFrame = Templates.withWelcomeFrame(name)
        return findWelcomeFrame + "\n" + withWelcomeFrame
    }

    fun closeContext() = "}"
}

