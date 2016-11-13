import actions.StartPauseRecAction
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.diagnostic.Logger
import components.GuiRecorderComponent
import java.awt.AWTEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

/**
 * @author Sergey Karashevich
 */

object GlobalActionRecorder {

    private val LOG by lazy { Logger.getInstance("#${GlobalActionRecorder::class.qualifiedName}") }

    private var isActive = false

    private val globalActionListener = object : AnActionListener {
        override fun beforeActionPerformed(p0: AnAction?, p1: DataContext?, p2: AnActionEvent?) {
            EventProcessor.processActionEvent(p2)
            LOG.info("IDEA is going to perform action ${p0!!.templatePresentation.text}")
        }

        override fun beforeEditorTyping(p0: Char, p1: DataContext?) {
            LOG.info("IDEA typing detected: ${p0!!}")
        }

        override fun afterActionPerformed(p0: AnAction?, p1: DataContext?, p2: AnActionEvent?) {
            LOG.info("IDEA action performed${p0!!.templatePresentation.text}")
        }
    }

    private val globalAwtProcessor = object : IdeEventQueue.EventDispatcher {

        override fun dispatch(awtEvent: AWTEvent): Boolean {
            when (awtEvent) {
                is MouseEvent -> EventProcessor.processMouseEvent(awtEvent)
                is KeyEvent -> EventProcessor.processKeyBoardEvent(awtEvent)
            }
            return false
        }
    }

    fun activate() {
        if (isActive) return
        LOG.info("Global action recorder is active")
        ActionManager.getInstance().addAnActionListener(globalActionListener)
        IdeEventQueue.getInstance().addDispatcher(globalAwtProcessor, GuiRecorderComponent) //todo: add disposal dependency on component
        isActive = true
    }

    fun deactivate() {
        if (isActive) {
            LOG.info("Global action recorder is non active")
            ActionManager.getInstance().removeAnActionListener(globalActionListener)
            IdeEventQueue.getInstance().removeDispatcher(globalAwtProcessor)
            StartPauseRecAction().setSelected(null, false)
        }
        isActive = false
    }

}