package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import ui.GuiScriptEditorFrame

/**
 * @author Sergey Karashevich
 */
class ShowLightEditorWindowAction(): AnAction() {

    var myGuiScriptEditorFrame: GuiScriptEditorFrame? = null

    override fun actionPerformed(p0: AnActionEvent?) {
        myGuiScriptEditorFrame = GuiScriptEditorFrame()
    }
}