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
//        Logger.setFactory(LoggerFactory::class.java)
//        Logger.setFactory(LoggerFactory::class.java)
//        LOG.info("Gui script recorder logger factory has been set")
    }

}