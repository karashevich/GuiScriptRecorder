import ScriptGenerator.scriptBuffer
import com.intellij.ide.util.newProjectWizard.FrameworksTree
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.WindowManager
import com.intellij.testGuiFramework.fixtures.SettingsTreeFixture
import com.intellij.testGuiFramework.framework.GuiTestUtil
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.ui.tree.TreeUtil
import components.GuiRecorderComponent
import org.fest.swing.core.BasicRobot
import org.fest.swing.core.GenericTypeMatcher
import org.fest.swing.exception.ComponentLookupException
import ui.GuiScriptEditorFrame
import ui.KeyUtil
import java.awt.Component
import java.awt.Container
import java.awt.Menu
import java.awt.MenuItem
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.KeyStroke.getKeyStrokeForEvent
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

/**
 * @author Sergey Karashevich
 */
object ScriptGenerator {

    val scriptBuffer = StringBuilder("")

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

    //(keyEvent.id == KeyEvent.KEY_PRESSED) for all events here
    fun processKeyPressing(keyEvent: KeyEvent) {
        //retrieve shortcut here
//        val keyStroke = getKeyStrokeForEvent(keyEvent)
//        val actionIds = KeymapManager.getInstance().activeKeymap.getActionIds(keyStroke)
//        if (!actionIds.isEmpty()) {
//            val firstActionId = actionIds[0]
//            if (IgnoredActions.ignore(firstActionId)) return
//            val keyStrokeStr = KeyStrokeAdapter.toString(keyStroke)
//            if (IgnoredActions.ignore(keyStrokeStr)) return
//            Writer.writeln(Templates.invokeActionComment(firstActionId))
//            makeIndent()
//            Writer.writeln(Templates.invokeAction(keyStrokeStr))
//        }
    }

    fun processKeyActionEvent(anAction: AnAction, anActionEvent: AnActionEvent) {
        //retrieve shortcut here
        val keyEvent = anActionEvent.inputEvent as KeyEvent
        val keyStroke = getKeyStrokeForEvent(keyEvent)
        val actionId = anActionEvent.actionManager.getId(anAction)
        if (IgnoredActions.ignore(actionId)) return
        val keyStrokeStr = KeyStrokeAdapter.toString(keyStroke)
        if (IgnoredActions.ignore(keyStrokeStr)) return
        ScriptGenerator.flushTyping()

        Writer.writeln(Templates.invokeActionComment(actionId))
        makeIndent()
        Writer.writeln(Templates.invokeAction(keyStrokeStr))

    }

    fun processMainMenuActionEvent(anActionTobePerformed: AnAction, anActionEvent: AnActionEvent) {
        val pathWithSemicolon = Util.getPathFromMainMenu(anActionTobePerformed, anActionEvent)
        if (pathWithSemicolon != null) Writer.writeln(Templates.invokeMainMenuAction(pathWithSemicolon.split(";").toTypedArray()))
    }


    fun flushTyping() {
        Typer.flushBuffer()
    }

    fun clickCmp(cmp: Component, itemName: String?, clickCount: Int) {
        Typer.flushBuffer()
        checkContext(cmp)
        when (cmp) {
            is JButton -> Writer.writeln(Templates.findAndClickButton(cmp.text))
            is ActionButton -> Writer.writeln(Templates.findAndClickActionButton(ActionManager.getInstance().getId(cmp.action)))
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
                if (cmp.javaClass.name.contains("BasicComboPopup")) {
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
            is SimpleTree -> Writer.writeln(Templates.selectSimpleTreeItem(Util.convertSimpleTreeItemToPath(cmp as SimpleTree, itemName!!)))
            is JBCheckBox -> Writer.writeln(Templates.clickJBCheckBox(cmp.text))
            is JCheckBox -> Writer.writeln(Templates.clickJCheckBox(cmp.text))
            is JComboBox<*> -> {
                openComboBox = true
                val label = getBoundedLabelForComboBox(cmp)
                Writer.write(Templates.onJComboBox(label.text))
            }
            is JRadioButton -> {
                Writer.writeln(Templates.clickRadioButton(cmp.text))
            }
            is EditorComponentImpl -> Writer.writeln(currentContext.editorActivate())
            is JTree -> {
                Writer.writeln(Templates.selectTreePath(itemName!!))
                Writer.writeln(Templates.selectTreePath(cmp.javaClass.name, itemName))
            }
            else -> if (cmp.inToolWindow()) {
                    when (cmp.javaClass.toString()) {
                         "class com.intellij.ide.projectView.impl.ProjectViewPane$1" -> Writer.writeln(currentContext.toolWindowActivate("Project"))
                    }
                }
        }
    }

    private fun isPopupList(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("listpopup")
    private fun isFrameworksTree(cmp: Component) = cmp.javaClass.name.toLowerCase().contains("AddSupportForFrameworksPanel".toLowerCase())

    private fun getLabel(container: Container, jTextField: JTextField): JLabel? {
        val robot = BasicRobot.robotWithCurrentAwtHierarchyWithoutScreenLock()
        return GuiTestUtil.findBoundedLabel(container, jTextField, robot)
    }

    private fun Component.inToolWindow(): Boolean {
        var pivotComponent = this
        while(pivotComponent.parent != null) {
            if (pivotComponent is SimpleToolWindowPanel) return true
            else pivotComponent = pivotComponent.parent
        }
        return false
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
            if (currentContext.currentContextType != Contexts.Type.IDE_FRAME)
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
                    Writer.writeln(currentContext.ideFrameStart())
                    makeIndent()
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

    fun clearContext() {
        currentContextComponent = null
    }


}

private fun Any.inToolWindow(): Boolean {
    if (this is Component) return true
    else return false
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

    fun writeToConsole(str: String) {
        print(str)
    }

    fun writeToBuffer(str: String) {
        scriptBuffer.append(str)
    }

    fun writeToEditor(str: String) {
        if (GuiRecorderComponent.getFrame() != null && GuiRecorderComponent.getFrame()!!.getEditor() != null) {
            val editor = GuiRecorderComponent.getFrame()!!.getEditor()
            val document = editor.document
//            ApplicationManager.getApplication().runWriteAction { document.insertString(document.textLength, str) }
            WriteCommandAction.runWriteCommandAction(null, { document.insertString(document.textLength, str) })
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
    fun findDialog(name: String, title: String?) = "val ${name} = JDialogFixture.find(robot(), \"${title}\")"
    fun withDialog(name: String) = "with (${name}){"
    fun findProjectWizard(name: String) = "${name} = findNewProjectWizard()"
    fun withProjectWizard(name: String) = "with (${name}){"
    fun findWelcomeFrame(name: String) = "${name} = findWelcomeFrame()"
    fun withWelcomeFrame(name: String) = "with (${name}){"
    fun findIdeFrame(name: String) = "${name} = findIdeFrame()"
    fun withIdeFrame(name: String) = "with (${name}){"

    fun findAndClickButton(text: String) = "GuiTestUtil.findAndClickButton(this, \"${text}\")"
    fun findAndClickActionButton(actionId: String) = "ActionButtonFixture.findByActionId(\"${actionId}\", robot(), this.target()).click()"

    fun clickActionLink(text: String) = "ActionLinkFixture.findActionLinkByName(\"${text}\", robot(), this.target()).click()"
    fun clickPopupItem(itemName: String) = "GuiTestUtil.clickPopupMenuItem(\"${itemName}\", true, this.target(), robot())"

    fun clickListItem(name: String) = "JListFixture(robot(), robot().finder().findByType(this.target(), com.intellij.ui.components.JBList::class.java, true)).clickItem(\"$name\")"
    fun findJTextField() = "val textField = myRobot.finder().findByType(JTextField::class.java)"
    fun findJTextFieldByLabel(labelText: String) = "val textField = findTextField(\"${labelText}\").click()"
    fun findJTextFieldAndDoubleClick() = "val textField = myRobot.finder().findByType(JTextField::class.java).doubleClick()"

    fun findJTextFieldByLabelAndDoubleClick(labelText: String) = "val textField = findTextField(\"${labelText}\").doubleClick()"
    fun typeText(text: String) = "GuiTestUtil.typeText(\"$text\", robot(), 10)"
    fun clickFrameworksTree(itemName: String) = "selectFramework(\"$itemName\")"
    fun selectSimpleTreeItem(path: String) = "SettingsTreeFixture.find(robot()).select(\"$path\")"
    fun clickJBCheckBox(text: String) = "JBCheckBoxFixture.findByText(\"$text\", this.target(), robot(), true).click()"

    fun clickJCheckBox(text: String) = "CheckBoxFixture.findByText(\"$text\", this.target(), robot(), true).click()"
    fun onJComboBox(text: String) = "GuiTestUtil.findComboBox(robot(), this.target(), \"$text\")"
    fun selectComboBox(itemName: String) = ".selectItem(\"$itemName\")"

    fun clickRadioButton(text: String) = "GuiTestUtil.findRadioButton(robot(), this.target(), \"$text\").select()"
    fun invokeActionComment(actionId: String) = "//invoke an action \"$actionId\" via keystroke string "

    fun invokeAction(keyStrokeStr: String) = "GuiTestUtil.invokeActionViaShortcut(robot(), \"$keyStrokeStr\")"
    fun invokeMainMenuAction(menuPath: Array<String>) = "${Contexts.IDE_FRAME_VAL}.invokeMenuPath(${menuPath.joinToString { "\"$it\"" } })"
    fun selectTreePath(path: String) = "GuiTestUtil.findJTreeFixture(robot(), this.target()).clickPath(\"$path\")"
    fun selectTreePath(treeClass: String, path: String) = "//GuiTestUtil.findJTreeFixtureByClassName(robot(), this.target(), \"$treeClass\").clickPath(\"$path\")"
}


class Contexts() {

    enum class Type {DIALOG, WELCOME_FRAME, PROJECT_WIZARD, IDE_FRAME}
    enum class SubType {EDITOR, TOOL_WINDOW}

    private var projectWizardFound = false
    private var welcomeFrameFound = false
    private var ideFrameFound = false
    private var myToolWindowId: String? = null

    companion object {
        val PROJECT_WIZARD_VAL = "projectWizard"
        val WELCOME_FRAME_VAL = "welcomeFrame"
        val IDE_FRAME_VAL = "ideFrame"
    }

    var dialogCount = 0
    var currentContextType: Type? = null
    var currentSubContextType: SubType? = null

    fun dialogContextStart(title: String): String {
        currentContextType = Type.DIALOG
        val name = "dialog${dialogCount++}"
        val findDialog = Templates.findDialog(name, title)
        val withDialog = Templates.withDialog(name)
        return findDialog + "\n" + withDialog
    }

    fun projectWizardContextStart(): String {
        currentContextType = Type.PROJECT_WIZARD
        val findProjectWizard = (if (projectWizardFound) "" else "var ") + Templates.findProjectWizard(PROJECT_WIZARD_VAL)
        val withProjectWizard = Templates.withProjectWizard(PROJECT_WIZARD_VAL)
        projectWizardFound = true

        return findProjectWizard + "\n" + withProjectWizard
    }

    fun welcomeFrameStart(): String {
        currentContextType = Type.WELCOME_FRAME
        val findWelcomeFrame = (if (welcomeFrameFound) "" else "var ") + Templates.findWelcomeFrame(WELCOME_FRAME_VAL)
        val withWelcomeFrame = Templates.withWelcomeFrame(WELCOME_FRAME_VAL)
        welcomeFrameFound = true

        return findWelcomeFrame + "\n" + withWelcomeFrame
    }

    fun ideFrameStart(): String {
        currentContextType = Type.IDE_FRAME
        val findIdeFrame = (if (ideFrameFound) "" else "val ") + Templates.findIdeFrame(IDE_FRAME_VAL)
        val withIdeFrame = Templates.withIdeFrame(IDE_FRAME_VAL)
        ideFrameFound = true

        return findIdeFrame + "\n" + withIdeFrame
    }

    fun editorActivate(): String {
        currentSubContextType = SubType.EDITOR
        return "EditorFixture(robot(), $IDE_FRAME_VAL).requestFocus()"
    }

    fun toolWindowActivate(toolWindowId: String? = null): String {
        currentSubContextType = SubType.TOOL_WINDOW
        myToolWindowId = toolWindowId
        return "ToolWindowFixture(\"$toolWindowId\", $IDE_FRAME_VAL.getProject(), robot()).activate()"
    }

    fun closeContext() = "}"
}

object IgnoredActions {

    val ignoreActions = listOf("EditorBackSpace")
    val ignoreShortcuts = listOf("space")

    fun ignore(actionOrShortCut: String): Boolean = (ignoreActions.contains(actionOrShortCut) || ignoreShortcuts.contains(actionOrShortCut))
}

object Util {
    fun getPathFromMainMenu(anActionTobePerformed: AnAction, anActionEvent: AnActionEvent): String? {
//        WindowManager.getInstance().findVisibleFrame().menuBar.getMenu(0).label
        val menuBar = WindowManager.getInstance().findVisibleFrame().menuBar ?: return null
        //in fact it should be only one String in "map"
        return (0..(menuBar.menuCount - 1)).mapNotNull { traverseMenu(menuBar.getMenu(it), anActionTobePerformed.templatePresentation.text!!) }.lastOrNull()
    }

    fun traverseMenu(menuItem: MenuItem, itemName: String): String? {
        if (menuItem is Menu) {
            if (menuItem.itemCount == 0) {
                if (menuItem.label == itemName) return itemName
                else return null
            } else {
                (0..(menuItem.itemCount - 1))
                        .mapNotNull { traverseMenu(menuItem.getItem(it), itemName) }
                        .forEach { return "${menuItem.label};$it" }
                return null
            }
        } else {
            if (menuItem.label == itemName) return itemName
            else return null
        }
    }

    fun convertSimpleTreeItemToPath(tree: SimpleTree, itemName: String): String {
        val searchableNodeRef = Ref.create<TreeNode>()
        val searchableNode: TreeNode?
        TreeUtil.traverse(tree.getModel().getRoot() as TreeNode) { node ->
            val valueFromNode = SettingsTreeFixture.getValueFromNode(tree, node)
            if (valueFromNode != null && valueFromNode == itemName) {
                assert(node is TreeNode)
                searchableNodeRef.set(node as TreeNode)
            }
            true
        }
        searchableNode = searchableNodeRef.get()
        val path = TreeUtil.getPathFromRoot(searchableNode!!)
        return path.toString().removePrefix("[").removeSuffix("]").split(",").filter(String::isNotEmpty).map(String::trim).joinToString("/")
    }

    fun getJTreePath(cmp: JTree, path: TreePath): String {
        var treePath = path
        val result = StringBuilder()
        val bcr = org.fest.swing.driver.BasicJTreeCellReader()
        while (treePath.pathCount != 1 || (cmp.isRootVisible && treePath.pathCount == 1)) {
            val valueAt = bcr.valueAt(cmp, treePath.lastPathComponent)
            result.insert(0, "$valueAt${if (!result.isEmpty()) "/" else ""}")
            if (treePath.pathCount == 1) break
            else treePath = treePath.parentPath
        }
        return result.toString()
    }

}