package actions

import GlobalActionRecorder
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import ui.Notifier

/**
 * @author Sergey Karashevich
 */

class StartPauseRecAction : ToggleAction(null, "Start/Stop GUI Script Recording", AllIcons.Ide.Macro.Recording_1){

    companion object{
        var state = State.PAUSED
    }

    override fun isSelected(actionEvent: AnActionEvent?): Boolean = state == State.STARTED

    override fun setSelected(actionEvent: AnActionEvent?, toStart: Boolean) {
        if (toStart) {
            state = State.STARTED
            val presentation = if (actionEvent != null) actionEvent.presentation else templatePresentation
            presentation.description = "Stop GUI Script Recording"
            presentation.icon = AllIcons.Ide.Macro.Recording_stop
            Notifier.updateStatus("Recording started")
            GlobalActionRecorder.activate()
        } else {
            state = State.PAUSED
            val presentation = if (actionEvent != null) actionEvent.presentation else templatePresentation
            presentation.description = "Start GUI Script Recording"
            presentation.icon = AllIcons.Ide.Macro.Recording_1
            Notifier.updateStatus("Recording paused")
            GlobalActionRecorder.deactivate()
        }
    }

    enum class State {STARTED, PAUSED, STOPPED}

}