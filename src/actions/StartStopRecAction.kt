package actions

import GlobalActionRecorder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

/**
 * @author Sergey Karashevich
 */

class StartStopRecAction : ToggleAction(){

    companion object{
        var state = State.STOPPED
    }

    override fun isSelected(p0: AnActionEvent?): Boolean = state == State.STARTED

    override fun setSelected(p0: AnActionEvent?, toStart: Boolean) {
        if (toStart) {
            state = State.STARTED
            GlobalActionRecorder.activate()
        } else {
            state = State.STOPPED
            GlobalActionRecorder.deactivate()
        }
    }

    enum class State {STARTED, STOPPED}

}