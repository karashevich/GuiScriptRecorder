import ScriptGenerator.makeIndent
import ScriptGenerator.scriptBuffer
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
import com.intellij.ui.CheckboxTree
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.ui.tree.TreeUtil
import components.GuiRecorderComponent
import org.fest.swing.core.BasicRobot
import org.fest.swing.core.GenericTypeMatcher
import org.fest.swing.exception.ComponentLookupException
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

    object ScriptWrapper {

        val TEST_METHOD_NAME = "testMe"

        private fun classWrap(function: () -> (String)): String = "class CurrentTest: GuiTestCase() {\n${function.invoke()}\n}"
        private fun funWrap(function: () -> String): String = "fun $TEST_METHOD_NAME(){\n${function.invoke()}\n}"

        private fun importsWrap(vararg imports: String, function: () -> String): String {
            val sb = StringBuilder()
            imports.forEach { sb.append("$it\n") }
            sb.append(function.invoke())
            return sb.toString()
        }

        fun wrapScript(code: String): String =
                importsWrap(
                        "import com.intellij.testGuiFramework.* ",
                        "import com.intellij.testGuiFramework.fixtures.*",
                        "import com.intellij.testGuiFramework.framework.*",
                        "import com.intellij.testGuiFramework.impl.*",
                        "import org.fest.swing.core.Robot",
                        "import java.awt.Component",
                        "import com.intellij.openapi.application.ApplicationManager",
                        "import org.fest.swing.fixture.*")
                {
                    classWrap {
                        funWrap {
                            code
                        }
                    }
                }
    }

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
//            Writer.writeln(Templates.shortcut(keyStrokeStr))
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
        Writer.writeln(Templates.shortcut(keyStrokeStr))

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
        //check if context has been changed; don't check context for combobox popups
        awareListsAndPopups(cmp) { currentContext.check(cmp) }
        when (cmp) {
            is JButton -> Writer.writeln(Templates.findAndClickButton(cmp.text))
            is ActionButton -> Writer.writeln(Templates.findAndClickActionButton(ActionManager.getInstance().getId(cmp.action)))
            is com.intellij.ui.components.labels.ActionLink -> Writer.writeln(Templates.clickActionLink(cmp.text))
            is JTextField -> {
                val parentContainer = cmp.rootPane.parent
                if (clickCount == 1) {
                    val label = getLabel(parentContainer, cmp)
                    if (label == null)
                        Writer.writeln(Templates.findJTextField())
                    else
                        Writer.writeln(Templates.findJTextFieldByLabel(label.text))
                } else if (clickCount == 2) {
                    val label = getLabel(parentContainer, cmp)
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
                        Writer.write(Templates.selectComboBox(itemName!!) + "\n")
                        openComboBox = false
                    } else {
                        throw Exception("Unable to find combo box for this BasicComboPopup")
                    }
                } else {
                    throw UnsupportedOperationException("not implemented")
                }

            }
            is CheckboxTree -> Writer.writeln(Templates.clickFrameworksTree(itemName!!))
            is SimpleTree -> Writer.writeln(Templates.selectSimpleTreeItem(Util.convertSimpleTreeItemToPath(cmp as SimpleTree, itemName!!)))
            is JBCheckBox -> Writer.writeln(Templates.clickJBCheckBox(cmp.text))
            is JCheckBox -> Writer.writeln(Templates.clickJCheckBox(cmp.text))
            is JComboBox<*> -> {
                openComboBox = true
                val label = getBoundedLabelForComboBox(cmp)
                Writer.write(makeIndent() + Templates.onJComboBox(label.text))
            }
            is JRadioButton -> {
                Writer.writeln(Templates.clickRadioButton(cmp.text))
            }
            is EditorComponentImpl -> Writer.writeln(currentContext.editorActivate())
            is JTree -> {
                Writer.writeln(Templates.selectTreePath(itemName!!))
//                Writer.writeln(Templates.selectTreePath(cmp.javaClass.name, itemName))
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
        while (pivotComponent.parent != null) {
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

    fun awareListsAndPopups(cmp: Component, body: () -> Unit) {
        cmp as JComponent
        if (cmp is JList<*> && openComboBox) return //don't change context for comboBox list
        if (isPopupList(cmp)) return //dont' change context for a popup menu
        body()
    }

    fun makeIndent() = currentContext.getIndent()

    fun clearContext() {
        currentContext.clear()
    }


}

private fun Any.inToolWindow(): Boolean {
    if (this is Component) return true
    else return false
}

object Writer {

    fun writeln(str: String) {
        write("${makeIndent()}" + str + "\n")
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