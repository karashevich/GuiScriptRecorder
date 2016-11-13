package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import components.GuiRecorderComponent
import ui.GuiScriptEditorFrame

/**
 * @author Sergey Karashevich
 */
class ShowGuiEditorWindowAction(): AnAction() {

    override fun actionPerformed(p0: AnActionEvent?) {
        val frame = GuiRecorderComponent.getFrame()
        if (frame == null) {
            GuiScriptEditorFrame()
        } else {
            frame.toFront()
        }
    }
}