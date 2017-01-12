package org.fest.swing.core

import com.intellij.util.ui.EdtInvocationManager
import org.fest.swing.awt.AWT
import org.fest.swing.hierarchy.ExistingHierarchy
import org.fest.swing.timing.Pause
import org.fest.util.Preconditions
import java.awt.Component
import java.awt.MouseInfo
import java.awt.Point

/**
 * @author Sergey Karashevich
 */
class SmartWaitRobot() : BasicRobot(null, ExistingHierarchy()) {

    val waitConst = 30L

    override fun waitForIdle() {
        Pause.pause(waitConst)
        EdtInvocationManager.getInstance().invokeAndWait({  })
    }

    //smooth mouse move
    override fun moveMouse(x: Int, y: Int) {
        val n = 20
        val t = 80
        val start = MouseInfo.getPointerInfo().location
        val dx = (x - start.x) / n.toDouble()
        val dy = (y - start.y) / n.toDouble()
        val dt = t / n.toDouble()
        for (step in 1..n) {
            try {
                Pause.pause(1L)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            super.moveMouse(
                    (start.x + dx * ((Math.log(1.0 * step / n) - Math.log(1.0 / n)) * n / (0 - Math.log(1.0 / n)))).toInt(),
                    (start.y + dy * ((Math.log(1.0 * step / n) - Math.log(1.0 / n)) * n / (0 - Math.log(1.0 / n)))).toInt())
        }
        super.moveMouse(x, y)
    }


    //smooth mouse move to component
    override fun moveMouse(c: Component, x: Int, y: Int) {
        val p = Preconditions.checkNotNull(AWT.translate(c, x, y)) as Point
        moveMouse(p.x, p.y);
    }

    //smooth mouse move for find and click actions
    override fun click(c: Component, where: Point, button: MouseButton, times: Int) {
        moveMouse(c, where.x, where.y)
        super.click(c, where, button, times)
    }

    override fun click(where: Point, button: MouseButton, times: Int) {
        moveMouse(where.x, where.y)
        super.click(where, button, times)
    }

}