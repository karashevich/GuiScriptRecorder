package templates

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testGuiFramework.fixtures.newProjectWizard.NewProjectWizardFixture
import org.fest.swing.core.Robot
import java.awt.Component


//todo: remove from release version
object Injections {
    fun clickListItem(itemName: String, robot: Robot, component: Component) {
        (this as NewProjectWizardFixture).selectProjectType(itemName)
    }

    fun runOnPooled(){
        ApplicationManager.getApplication().executeOnPooledThread {  }
    }
}

fun installInjection(injectionName: String) {

}