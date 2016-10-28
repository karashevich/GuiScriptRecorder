package com.intellij.testGuiFramework.script

import com.intellij.testGuiFramework.framework.GuiTestBase
import org.fest.swing.core.FastRobot

/**
 * @author Sergey Karashevich
 */
open class GuiTestCases(): GuiTestBase() {

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

//    setUpDefaultProjectCreationLocationPath()
        setRobot(FastRobot())

//    setIdeSettings()
//    setUpSdks()
//    setPathToGit()
    }


}

