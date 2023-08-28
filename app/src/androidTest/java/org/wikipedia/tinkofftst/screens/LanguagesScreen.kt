package org.wikipedia.tinkofftst.screens

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R


class LanguagesScreen {
    private val addLanguageButton by lazy { onView(withText(R.string.wikipedia_languages_add_language_text)) }
    private val selectLanguageTextView by lazy { onView((withText(SELECT_LANG))) }

    fun clickAddLanguageButton() {
        addLanguageButton.perform(click())
    }

    fun clickToSelectLanguage() {
        selectLanguageTextView.perform(click(),
            //swipeLeft()
        )
    }



    companion object {
        private const val SELECT_LANG = "Russian"

        inline operator fun invoke(crossinline block: LanguagesScreen.() -> Unit) {
            LanguagesScreen().block()
        }
    }

}