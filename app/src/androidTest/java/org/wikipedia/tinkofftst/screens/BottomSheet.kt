package org.wikipedia.tinkofftst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.qameta.allure.kotlin.Allure.step
import org.wikipedia.R

class BottomSheet {
    private val donateButton by lazy { onView(withId(R.id.main_drawer_donate_container)) }
    private val settingsButton by lazy { onView(withId(R.id.main_drawer_settings_container)) }
    private val loginButton by lazy { onView(withId(R.id.main_drawer_login_button)) }

    fun clickDonateButton() {
        step("Нажимаем на кнопку \"Пожертвовать\"") {
            donateButton.perform(click())
        }
    }

    fun clickSettingsButton() {
        step("Нажимаем на кнопку \"Настройки\"") {
            settingsButton.perform(click())
        }
    }

    fun clickLoginButton() {
        step("Нажимаем на кнопку \"Создать учётную запись\"") {
            loginButton.perform(click())
        }
    }

    companion object {
        inline operator fun invoke(crossinline block: BottomSheet.() -> Unit) {
            BottomSheet().block()
        }
    }

}