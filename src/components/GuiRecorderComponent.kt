package components

import actions.StartStopRecAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.ui.playback.commands.ActionCommand
import java.awt.event.InputEvent

/**
 * @author Sergey Karashevich
 */
class GuiRecorderComponent: ApplicationComponent{


    override fun getComponentName() = "GuiRecorderComponent"

    override fun disposeComponent() {

    }

    override fun initComponent() {
        val recAction = StartStopRecAction()
        ActionManager.getInstance().tryToExecute(recAction, ActionCommand.getInputEvent(ActionManager.getInstance().getId(recAction)), null, ActionPlaces.UNKNOWN, false)
    }

}