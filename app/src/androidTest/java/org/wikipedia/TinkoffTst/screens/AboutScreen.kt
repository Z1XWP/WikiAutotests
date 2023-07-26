package org.wikipedia.TinkoffTst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.qameta.allure.kotlin.Allure.step
import org.wikipedia.R

class AboutScreen {
    private val contributorsTextView by lazy { onView(withText(R.string.about_contributors_heading)) }
    private val translatorsTextView by lazy { onView(withText(R.string.about_translators_heading)) }
    private val licenseTextView by lazy { onView(withText(R.string.about_app_license_heading)) }

    fun checkContributorsIsDisplayed() {
        step("Проверяем, что поле \"Авторы\" отобразилось") {
            contributorsTextView.check(matches(isDisplayed()))
        }
    }

    fun checkTranslatorsIsDisplayed() {
        step("Проверяем, что поле \"Переводчики\" отобразилось"){
            translatorsTextView.check(matches(isDisplayed()))
        }
    }

    fun checkLicenseIsDisplayed() {
        step("Проверяем, что поле \"Лицензия\" отобразилось"){
            licenseTextView.check(matches(isDisplayed()))
        }
    }


    companion object {
        inline operator fun invoke(crossinline block: AboutScreen.() -> Unit) {
            AboutScreen().block()
        }
    }
}