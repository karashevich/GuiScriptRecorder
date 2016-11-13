package actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import components.GuiRecorderComponent
import ui.Notifier

/**
 * @author Sergey Karashevich
 */
class SyncEditorAction : ToggleAction(null, "Synchronize Editor with Generated GUI Script", AllIcons.Actions.Refresh) {

    override fun isSelected(e: AnActionEvent?): Boolean = if (GuiRecorderComponent.getFrame() != null) GuiRecorderComponent.getFrame()!!.isSyncToEditor() else false

    override fun setSelected(e: AnActionEvent?, toSync: Boolean) {
        val frame = GuiRecorderComponent.getFrame() ?: return
        frame.setSyncToEditor(toSync)
        if (toSync)
            Notifier.updateStatus("Synchronization is on")
        else
            Notifier.updateStatus("Synchronization is off")
    }

}