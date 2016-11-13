import ScriptGenerator.scriptBuffer
import com.intellij.ide.util.newProjectWizard.FrameworksTree
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testGuiFramework.framework.GuiTestUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import components.GuiRecorderComponent
import org.fest.swing.core.BasicRobot
import org.fest.swing.core.GenericTypeMatcher
import org.fest.swing.exception.ComponentLookupException
import ui.GuiScriptEditorFrame
import ui.KeyUtil
import java.awt.Component
import java.awt.Container
import javax.swing.*

/**
 * @author Sergey Karashevich
 */
object ScriptGenerator {

    val scriptBuffer = StringBuilder()

    var openComboBox = false;

    fun getScriptBuffer() = scriptBuffer.toString()

    fun clearScriptBuffer() = scriptBuffer.setLength(0)

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

    fun processTyping(keyChar: Char) {
        Typer.type(keyChar)
    }

    fun flushTyping() {
        Typer.flushBuffer()
    }

    fun clickCmp(cmp: Component, itemName: String?, clickCount: Int) {
        Typer.flushBuffer()
        checkContext(cmp)
        when (cmp) {
            is JButton -> Writer.writeln(Templates.findAndClickButton(cmp.text))
            is com.intellij.ui.components.labels.ActionLink -> Writer.writeln(Templates.clickActionLink(cmp.text))
            is JTextField -> {
                if (clickCount == 1) {
                    val label = getLabel((currentContextComponent as Container?)!!, cmp)
                    if (label == null)
                        Writer.writeln(Templates.findJTextField())
                    else
                        Writer.writeln(Templates.findJTextFieldByLabel(label.text))
                } else if (clickCount == 2) {
                    val label = getLabel((currentContextComponent as Container?)!!, cmp)
                    if (label == null)
                        Writer.writeln(Templates.findJTextFieldAndDoubleClick())
                    else
                        Writer.writeln(Templates.findJTextFieldByLabelAndDoubleClick(label.text))
                }
            }
            is JBList<*> -> {
                if (isPopupList(cmp))
                    Writer.writeln(Templates.clickPopupItem(itemName!!))
                else if (isFrameworksTree(cmp))
                else
                    Writer.writeln(Templates.clickListItem(itemName!!))
            }
            is JList<*> -> {
                if (cmp.javaClass.name.contains("BasicComboPopup")){
                    if (openComboBox) {
                        Writer.writeln(Templates.selectComboBox(itemName!!))
                        openComboBox = false
                    } else {
                        throw Exception("Unable to find combo box for this BasicComboPopup")
                    }
                } else {
                    throw UnsupportedOperationException("not implemented")
                }

            }
            is FrameworksTree -> Writer.writeln(Templates.clickFrameworksTree(itemName!!))
            is JBCheckBox -> Writer.writeln(Templates.clickJBCheckBox(cmp.text))
            is JCheckBox -> Writer.writeln(Templates.clickJCheckBox(cmp.text))
            is JComboBox<*> -> {
                openComboBox = true
                val label = getBoundedLabelForComboBox(cmp)
                Writer.write(Templates.onJComboBox(label.text))
            }
        }
    }

    private fun isPopupList(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("listpopup")
    private fun isFrameworksTree(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("AddSupportForFrameworksPanel".toLowerCase())

    private fun getLabel(container: Container, jTextField: JTextField): JLabel? {
        val robot = BasicRobot.robotWithCurrentAwtHierarchyWithoutScreenLock()
        return GuiTestUtil.findBoundedLabel(container, jTextField, robot)
    }


    //We are looking for a closest bounded label in a 2 levels of hierarchy for JComboBox component
    private fun getBoundedLabelForComboBox(cb: JComboBox<*>): JLabel {
        val robot = BasicRobot.robotWithCurrentAwtHierarchyWithoutScreenLock()

        val findBoundedLabel: (Component) -> JLabel? = { component ->
            try {
                robot.finder().find(component.parent as Container, object : GenericTypeMatcher<JLabel>(JLabel::class.java) {
                    override fun isMatching(label: JLabel): Boolean {
                        return label.labelFor != null && label.labelFor == component
                    }
                })
            } catch(e: ComponentLookupException) {
                null
            }
        }

        val bounded1 = findBoundedLabel(cb)
        if (bounded1 !== null) return bounded1

        val bounded2 = findBoundedLabel(cb.parent)
        if (bounded2 !== null) return bounded2

        val bounded3 = findBoundedLabel(cb.parent!!.parent)
        if (bounded3 !== null) return bounded3

        throw ComponentLookupException("Unable to find bounded label in 2 levels from JComboBox")

    }

    fun checkContext(cmp: Component) {
        cmp as JComponent
        if (openComboBox) return //don't change context for comboBox list
        if (isPopupList(cmp)) return //dont' change context for a popup menu
        val parent = cmp.rootPane.parent
        if (currentContextComponent != null && cmp.rootPane != currentContextComponent) {
            if (parent is JFrame && parent.title == GuiScriptEditorFrame.GUI_SCRIPT_FRAME_TITLE) return //do nothing if switch to GUI Script Editor
            Writer.writeln(currentContext.closeContext())
        }
        if (currentContextComponent == null || cmp.rootPane != currentContextComponent) {
            currentContextComponent = cmp.rootPane
            when (parent) {
                is JDialog -> {
                    if (parent.title == com.intellij.ide.IdeBundle.message("title.new.project")) {
                        Writer.writeln(currentContext.projectWizardContextStart())
                        makeIndent()
                        return
                    } else {
                        Writer.writeln(currentContext.dialogContextStart(parent.title))
                        makeIndent()
                        return
                    }
                }
                is com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame -> {
                    Writer.writeln(currentContext.welcomeFrameStart())
                    makeIndent()
                    return
                }
                is JFrame -> {
                    println("is JFrame")
                    return
                }
            }
        } else {
            makeIndent()
        }
    }

    private fun makeIndent() {
        Writer.write("  ")
    }

}

private object Writer {

    fun writeln(str: String) {
        write(str + "\n")
    }

    fun write(str: String) {
        writeToConsole(str)
        if (GuiRecorderComponent.getFrame() != null && GuiRecorderComponent.getFrame()!!.isSyncToEditor())
            writeToEditor(str)
        else
            writeToBuffer(str)
    }

    fun writeToConsole(str: String){
        print(str)
    }

    fun writeToBuffer(str: String){
        scriptBuffer.append(str)
    }

    fun writeToEditor(str: String){
        if (GuiRecorderComponent.getFrame() != null && GuiRecorderComponent.getFrame()!!.getEditor() != null) {
            val editor = GuiRecorderComponent.getFrame()!!.getEditor()
            val document = editor.document
//            ApplicationManager.getApplication().runWriteAction { document.insertString(document.textLength, str) }
            WriteCommandAction.runWriteCommandAction(null, {document.insertString(document.textLength, str)})
        }
    }
}

private object Typer {
    val strBuffer = StringBuilder()
    val rawBuffer = StringBuilder()

    fun type(keyChar: Char) {
        strBuffer.append(KeyUtil.patch(keyChar))
        rawBuffer.append("${if (rawBuffer.length > 0) ", " else ""}\"${keyChar.toInt()}\"")
    }

    fun flushBuffer() {
        if (strBuffer.length == 0) return
        Writer.writeln("//typed:[${strBuffer.length},\"${strBuffer.toString()}\", raw=[${rawBuffer.toString()}]]")
        Writer.writeln(Templates.typeText(strBuffer.toString()))
        strBuffer.setLength(0)
        rawBuffer.setLength(0)
    }
}

//TEMPLATES
private object Templates {
    fun findDialog(name: String, title: String?) = "val ${name} = DialogFixture.find(robot(), \"${title}\")"
    fun withDialog(name: String) = "with (${name}){"
    fun findProjectWizard(name: String) = "${name} = findNewProjectWizard()"
    fun withProjectWizard(name: String) = "with (${name}){"
    fun findWelcomeFrame(name: String) = "${name} = findWelcomeFrame()"
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

    fun onJComboBox(text: String) = "GuiTestUtil.findComboBox(robot(), this.target(), \"$text\")"
    fun selectComboBox(itemName: String) = ".selectItem(\"$itemName\")"
}


private class Contexts() {

    enum class Type {DIALOG, WELCOME_FRAME, PROJECT_WIZARD, IDE_FRAME }

    private var projectWizardFind = false
    private var welcomeFrameFind = false

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
        val findProjectWizard = (if(projectWizardFind) "" else "var ") + Templates.findProjectWizard(name)
        val withProjectWizard = Templates.withProjectWizard(name)
        projectWizardFind = true

        return findProjectWizard + "\n" + withProjectWizard
    }

    fun welcomeFrameStart(): String {
        currentContextType = Type.WELCOME_FRAME
        val name = "welcomeFrame"
        val findWelcomeFrame = (if(welcomeFrameFind) "" else "var ") + Templates.findWelcomeFrame(name)
        val withWelcomeFrame = Templates.withWelcomeFrame(name)
        welcomeFrameFind = true

        return findWelcomeFrame + "\n" + withWelcomeFrame
    }

    fun closeContext() = "}"
}

