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

    override fun isSelected(actionEvent: AnActionEvent?): Boolean = GlobalActionRecorder.isActive()

    override fun setSelected(actionEvent: AnActionEvent?, toStart: Boolean) {
        if (toStart) {
            val presentation = if (actionEvent != null) actionEvent.presentation else templatePresentation
            presentation.description = "Stop GUI Script Recording"
            Notifier.updateStatus("Recording started")
            GlobalActionRecorder.activate()
        } else {
            val presentation = if (actionEvent != null) actionEvent.presentation else templatePresentation
            presentation.description = "Start GUI Script Recording"
            Notifier.updateStatus("Recording paused")
            GlobalActionRecorder.deactivate()
        }
    }

}