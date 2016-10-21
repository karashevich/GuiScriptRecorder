package test

import com.intellij.testFramework.LightPlatformTestCase

/**
 * @author Sergey Karashevich
 */

class DefaultScript : LightPlatformTestCase() {

    fun testSomething() = print("Tested something")
}