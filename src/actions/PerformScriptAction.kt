package actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import logic.KotlinKompilerUtil

/**
 * @author Sergey Karashevich
 */

class PerformScriptAction: AnAction(){

    override fun actionPerformed(p0: AnActionEvent?) {
        val demoCode = "println(\"Demo\")"

        KotlinKompilerUtil.kompileAndEval(demoCode)
    }

}
