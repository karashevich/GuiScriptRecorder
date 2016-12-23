package actions

import GlobalActionRecorder
import ScriptGenerator
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import ui.Notifier

/**
 * @author Sergey Karashevich
 */
class StopRecAction: AnAction(null, "Stop Recording and Clear Buffer", AllIcons.Actions.Suspend){

    override fun actionPerformed(p0: AnActionEvent?) {
        GlobalActionRecorder.deactivate()
        Notifier.updateStatus("Recording stopped")
        ScriptGenerator.clearScriptBuffer()
    }

}