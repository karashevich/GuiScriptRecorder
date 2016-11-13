package actions

import ScriptGenerator
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import components.GuiRecorderComponent
import ui.Notifier

/**
 * @author Sergey Karashevich
 */
class UpdateEditorAction : AnAction(null, "Update GUI Script Editor", AllIcons.Actions.NextOccurence) {

    override fun actionPerformed(actionEvent: AnActionEvent?) {
        val editor = GuiRecorderComponent.getFrame()!!.getEditor()
        ApplicationManager.getApplication().runWriteAction { editor.document.setText(getGuiScriptBuffer()) }
        Notifier.updateStatus("GUI script updated")
    }

    fun getGuiScriptBuffer() = ScriptGenerator.getScriptBuffer()

}