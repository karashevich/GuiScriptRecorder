package org.fest.swing.core

import com.intellij.util.ui.EdtInvocationManager
import org.fest.swing.hierarchy.ExistingHierarchy
import org.fest.swing.timing.Pause

/**
 * @author Sergey Karashevich
 */
class SmartWaitRobot() : BasicRobot(null, ExistingHierarchy()) {

    val waitConst = 30L

    override fun waitForIdle() {
        Pause.pause(waitConst)
        EdtInvocationManager.getInstance().invokeAndWait({  })
    }

}