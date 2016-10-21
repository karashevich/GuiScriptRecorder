package components

import com.intellij.openapi.components.ApplicationComponent

/**
 * @author Sergey Karashevich
 */
class GuiRecorderComponent: ApplicationComponent{

    override fun getComponentName() = "GuiRecorderComponent"

    override fun disposeComponent() {

    }

    override fun initComponent() {
    }

}