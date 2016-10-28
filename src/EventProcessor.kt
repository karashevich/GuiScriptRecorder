import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBList
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.util.containers.HashMap
import java.awt.Component
import java.awt.Container
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON1
import java.awt.event.MouseEvent.MOUSE_PRESSED
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * @author Sergey Karashevich
 */
object EventProcessor{

    val LOG = Logger.getInstance(EventProcessor::class.java)

    fun processMouseEvent(me: MouseEvent) {

//        if (!(me.clickCount == 1 && me.id == MOUSE_CLICKED && me.button == BUTTON1)) return
        if (!(me.clickCount == 1 && me.id == MOUSE_PRESSED && me.button == BUTTON1)) return

            //increase highlighting component range
        var component: Component? = me.component

        //Component component = FocusManager.getCurrentManager().getFocusedWindow().getComponentAt(me.getLocationOnScreen());

        val mousePoint = me.point

        if (component is JFrame) {
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
            //user click
                    var itemName: String? = null
                    val dataMap = HashMap<String, Any>()
                    dataMap.put("Component", component)
                    if (component is JBList<*>)
                        itemName = getCellText((component as JBList<*>?)!!, mousePoint)
                LOG.info("Delegate click from component:${component}")
                ScriptGenerator.clickCmp(component, itemName)
        }
    }


    fun getCellText(jbList: JBList<*>, pointOnList: Point): String? {
        val index = jbList.locationToIndex(pointOnList)
        val cellBounds = jbList.getCellBounds(index, index)
        if (cellBounds.contains(pointOnList)) {
            val elementAt = jbList.model.getElementAt(index)
            if (elementAt is PopupFactoryImpl.ActionItem) {
                return elementAt.text
            } else {
                return elementAt.toString()
            }
        }
        return null
    }
}