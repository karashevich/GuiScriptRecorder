import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import java.awt.AWTEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

/**
 * @author Sergey Karashevich
 */

object GlobalActionRecorder{

    private var isActive = false

    private val globalActionListener = object : AnActionListener {
        override fun beforeActionPerformed(p0: AnAction?, p1: DataContext?, p2: AnActionEvent?) {
            println("IDEA is going to perform action ${p0!!.templatePresentation.text}")
        }

        override fun beforeEditorTyping(p0: Char, p1: DataContext?) {
            println("IDEA typing detected: ${p0!!}")
        }

        override fun afterActionPerformed(p0: AnAction?, p1: DataContext?, p2: AnActionEvent?) {
            println("IDEA action performed${p0!!.templatePresentation.text}")
        }
    }

    private val globalAwtPostprocessor = object : IdeEventQueue.EventDispatcher {

        override fun dispatch(awtEvent: AWTEvent?): Boolean {
            when (awtEvent) {
                is MouseEvent -> println("Mouse event: ${awtEvent.toString()}")
                is KeyEvent -> println("Key event: ${awtEvent.toString()}")
            }
            return false
        }
    }

    fun activate() {
        if (isActive) return
        ActionManager.getInstance().addAnActionListener(globalActionListener)
        IdeEventQueue.getInstance().addPostprocessor(globalAwtPostprocessor, null) //todo: add disposal dependency on component
        isActive = true
    }

    fun deactivate() {
        if (isActive) {
            ActionManager.getInstance().removeAnActionListener(globalActionListener)
            IdeEventQueue.getInstance().removePostprocessor(globalAwtPostprocessor)
        }
        isActive = false
    }

}