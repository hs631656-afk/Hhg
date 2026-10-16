package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.LocalIntentEngine
import com.example.model.JarvisAction
import com.example.model.NavType
import com.example.model.ScrollDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS Core", appName)
    }

    @Test
    fun `test local intent engine hindi open app`() {
        val result = LocalIntentEngine.parse("YouTube kholo")
        assertTrue(result.action is JarvisAction.OpenApp)
        assertEquals("youtube", (result.action as JarvisAction.OpenApp).appQuery)
    }

    @Test
    fun `test local intent engine navigation`() {
        val result = LocalIntentEngine.parse("Home screen jao")
        assertTrue(result.action is JarvisAction.SystemNav)
        assertEquals(NavType.HOME, (result.action as JarvisAction.SystemNav).navType)
    }

    @Test
    fun `test local intent engine scroll down`() {
        val result = LocalIntentEngine.parse("Niche scroll karo")
        assertTrue(result.action is JarvisAction.ScreenScroll)
        assertEquals(ScrollDirection.DOWN, (result.action as JarvisAction.ScreenScroll).direction)
    }

    @Test
    fun `test local intent engine sensitive call confirmation`() {
        val result = LocalIntentEngine.parse("Call Papa")
        assertTrue(result.isSensitive)
        assertTrue(result.action is JarvisAction.MakeCall)
    }
}
