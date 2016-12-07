import com.intellij.framework.PresentableVersion
import com.intellij.ide.util.frameworkSupport.FrameworkVersion
import com.intellij.ide.util.newProjectWizard.FrameworksTree
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.ProjectTemplate
import com.intellij.ui.components.JBList
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.containers.HashMap
import ui.GuiScriptEditorFrame
import java.awt.Component
import java.awt.Container
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON1
import java.awt.event.MouseEvent.MOUSE_PRESSED
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities

/**
 * @author Sergey Karashevich
 */
object EventDispatcher {

    val LOG = Logger.getInstance(EventDispatcher::class.java)

    fun processMouseEvent(me: MouseEvent) {

//        if (!(me.clickCount == 1 && me.id == MOUSE_CLICKED && me.button == BUTTON1)) return
        if (!(me.id == MOUSE_PRESSED && me.button == BUTTON1)) return

        var component: Component? = me.component
        val mousePoint = me.point
        val clickCount = me.clickCount

        if (component is JFrame)
            if (component.title == GuiScriptEditorFrame.GUI_SCRIPT_FRAME_TITLE) return // ignore mouse events from GUI Script Editor Frame

        if (component is RootPaneContainer) {

            val layeredPane = component.layeredPane
            val pt = SwingUtilities.convertPoint(component, mousePoint, layeredPane)
            component = layeredPane.findComponentAt(pt)
        } else if (component is Container) {
            component = component.findComponentAt(mousePoint)
        }

        if (component == null) {
            component = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
        }
        if (component != null) {
            var itemName: String? = null
            val dataMap = HashMap<String, Any>()
            dataMap.put("Component", component)
            val convertedPoint = Point(
                    me.locationOnScreen.x - component.locationOnScreen.x,
                    me.locationOnScreen.y - component.locationOnScreen.y)
            when(component) {
                is JList<*> -> itemName = getCellText((component as JList<*>?)!!, convertedPoint)
                is JBList<*> -> itemName = getCellText((component as JBList<*>?)!!, convertedPoint)
                is FrameworksTree -> itemName = component.getClosestPathForLocation(convertedPoint.x, convertedPoint.y).lastPathComponent.toString()
                is SimpleTree -> itemName = component.getDeepestRendererComponentAt(convertedPoint.x, convertedPoint.y).toString()
            }
            LOG.info("Delegate click from component:${component}")
            ScriptGenerator.clickCmp(component, itemName, clickCount)
        }
    }

    fun getCellText(jbList: JBList<*>, pointOnList: Point): String? {
        val index = jbList.locationToIndex(pointOnList)
        val cellBounds = jbList.getCellBounds(index, index)
        if (cellBounds.contains(pointOnList)) {
            val elementAt = jbList.model.getElementAt(index)
            when(elementAt){
                is PopupFactoryImpl.ActionItem -> return elementAt.text
                is ProjectTemplate -> return elementAt.name
                else -> return elementAt.toString()
            }
        }
        return null
    }

    fun getCellText(jList: JList<*>, pointOnList: Point): String? {
        val index = jList.locationToIndex(pointOnList)
        val cellBounds = jList.getCellBounds(index, index)
        if (cellBounds.contains(pointOnList)) {
            val elementAt = jList.model.getElementAt(index)
            when(elementAt){
                is PopupFactoryImpl.ActionItem -> return elementAt.text
                is ProjectTemplate -> return elementAt.name
                is PresentableVersion -> return elementAt.presentableName
                is FrameworkVersion -> return elementAt.versionName
                else -> return elementAt.toString()
            }
        }
        return null
    }

    fun processKeyBoardEvent(keyEvent: KeyEvent) {
        if (keyEvent.component is JFrame  && (keyEvent.component as JFrame).title == GuiScriptEditorFrame.GUI_SCRIPT_FRAME_TITLE) return
        if (keyEvent.id == KeyEvent.KEY_TYPED)
            ScriptGenerator.processTyping(keyEvent.keyChar)
//        if (keyEvent.id == KeyEvent.KEY_PRESSED)
//            ScriptGenerator.processKeyPressing(keyEvent)
    }

    fun  processActionEvent(anActionTobePerformed: AnAction, anActionEvent: AnActionEvent?) {
        if (anActionEvent!!.inputEvent is KeyEvent) ScriptGenerator.processKeyActionEvent(anActionTobePerformed, anActionEvent)
        if (anActionEvent.place == ActionPlaces.MAIN_MENU) ScriptGenerator.processMainMenuActionEvent(anActionTobePerformed, anActionEvent)
    }

}