package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import ui.LightEditor
import ui.LightEditorWindow

/**
 * @author Sergey Karashevich
 */
class ShowLightEditorWindowAction(): AnAction() {

    var myLightEditorWindow: LightEditorWindow? = null

    override fun actionPerformed(p0: AnActionEvent?) {
        val myEditor = LightEditor()
        myLightEditorWindow = LightEditorWindow(myEditor)
    }
}