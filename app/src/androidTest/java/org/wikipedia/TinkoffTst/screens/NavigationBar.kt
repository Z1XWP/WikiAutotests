package org.wikipedia.TinkoffTst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.qameta.allure.kotlin.Allure.step
import org.wikipedia.R

class NavigationBar {
    private val moreButton by lazy { onView(withId(R.id.nav_more_container)) }

    fun clickMoreButton() {
        step("Нажимаем кнопку \"Ещё\"") {
            moreButton.perform(click())
        }
    }

    companion object {
        inline operator fun invoke(crossinline block: NavigationBar.() -> Unit) {
            NavigationBar().block()
        }
    }
}