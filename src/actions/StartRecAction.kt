package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * @author Sergey Karashevich
 */

class StartRecAction: AnAction(){

    override fun actionPerformed(p0: AnActionEvent?) {
        GlobalActionRecorder.activate()
    }

}