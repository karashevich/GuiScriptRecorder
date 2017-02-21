import com.intellij.testGuiFramework.impl.GuiTestCase
import org.junit.Test

/**
 * @author Sergey Karashevich
 */

class InstallPluginTest: GuiTestCase() {

    @Test
    fun testInstallPlugin() {
        welcomeFrame {
            actionLink("Configure").click()
            popupClick("Plugins")
            dialog("Plugins") {
                button("Install JetBrains plugin...").click()
                dialog("Browse JetBrains Plugins ") {
                    textfield("").click()
                    typeText("Ide features trainer")
                    pluginTable().selectPlugin("IDE Features Trainer")
                    button("Install").click()
                    button("Restart IntelliJ IDEA").click()
                    dialog("This should not be shown") {
                        button("Shutdown").click()
                    }
                }
            }
        }
    }
}