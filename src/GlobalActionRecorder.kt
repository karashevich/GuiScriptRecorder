import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.diagnostic.Logger
import util.ClassLoaderUtil
import java.awt.AWTEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

/**
 * @author Sergey Karashevich
 */

object GlobalActionRecorder {

    private val LOG by lazy { Logger.getInstance("#${GlobalActionRecorder::class.qualifiedName}") }

    private var active = false

    fun isActive() = active


    private val globalActionListener = object : AnActionListener {
        override fun beforeActionPerformed(anActionToBePerformed: AnAction?, p1: DataContext?, anActionEvent: AnActionEvent?) {
            EventDispatcher.processActionEvent(anActionToBePerformed!!, anActionEvent)
            LOG.info("IDEA is going to perform action ${anActionToBePerformed.templatePresentation.text}")
        }

        override fun beforeEditorTyping(p0: Char, p1: DataContext?) {
            LOG.info("IDEA typing detected: ${p0!!}")
        }

        override fun afterActionPerformed(p0: AnAction?, p1: DataContext?, p2: AnActionEvent?) {
            LOG.info("IDEA action performed ${p0!!.templatePresentation.text}")
        }
    }

    private val globalAwtDispatcher = object : IdeEventQueue.EventDispatcher {

        override fun dispatch(awtEvent: AWTEvent): Boolean = myAwtProcessor(awtEvent)
    }

    private val myAwtProcessor: (AWTEvent) -> Boolean = {
        when (it) {
            is MouseEvent -> EventDispatcher.processMouseEvent(it)
            is KeyEvent -> EventDispatcher.processKeyBoardEvent(it)
        }
        false
    }

    fun activate() {
        if (active) return
        LOG.info("Global action recorder is active")
        ActionManager.getInstance().addAnActionListener(globalActionListener)
//        IdeEventQueue.getInstance().addDispatcher(myAwtProcessor, { }) //todo: add disposal dependency on component
        ClassLoaderUtil.addDispatcherToIdeEventQueue(globalAwtDispatcher)
        active = true
    }

    fun deactivate() {
        if (active) {
            LOG.info("Global action recorder is non active")
            ActionManager.getInstance().removeAnActionListener(globalActionListener)
            IdeEventQueue.getInstance().removeDispatcher(globalAwtDispatcher)

        }
        active = false
        ScriptGenerator.clearContext()
    }

}