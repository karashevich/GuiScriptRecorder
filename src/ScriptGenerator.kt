import com.intellij.testGuiFramework.framework.GuiTestUtil
import com.intellij.ui.components.JBList
import org.fest.swing.core.BasicRobot
import ui.KeyUtil
import java.awt.Component
import java.awt.Container
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * @author Sergey Karashevich
 */
object ScriptGenerator {

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
                else
                    Writer.write(Templates.clickListItem(itemName!!))
            }
        }
    }

    private fun isPopupList(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("listpopup")
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
        Writer.write("typed:[${strBuffer.length},\"${strBuffer.toString()}\", raw=\"${rawBuffer.toString()}\"]")
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

    fun clickPopupItem(itemName: String) = "GuiTestUtil.clickPopupMenuItem(\"${itemName}\", this.target(), robot())"
    fun clickListItem(name: String) = "clickListItem(\"${name}\", robot(), this.target())"

    fun findJTextField() = "val textField = myRobot.finder().findByType(JTextField::class.java)"
    fun findJTextFieldByLabel(labelText: String) = "val textField = findTextField(\"${labelText}\").click()"
    fun findJTextFieldAndDoubleClick() = "val textField = myRobot.finder().findByType(JTextField::class.java).doubleClick()"
    fun findJTextFieldByLabelAndDoubleClick(labelText: String) = "val textField = findTextField(\"${labelText}\").doubleClick()"
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
        var withWelcomeFrame = Templates.withWelcomeFrame(name)
        return findWelcomeFrame + "\n" + withWelcomeFrame
    }

    fun closeContext() = "}"
}