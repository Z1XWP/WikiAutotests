package org.wikipedia.tinkofftst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.wikipedia.R


class SearchScreen {
    private val searchBarView by lazy { onView(withId(R.id.search_container)) }
    private val langButton by lazy { onView(withId(R.id.search_lang_button)) }
    private val selectedLanguageButton by lazy { onView(withText(SELECTED_LANGUAGE)) }
    private val searchBarViewText by lazy { onView((withId(R.id.search_cab_view))) }


    fun clickSearchBar() {
        searchBarView.perform(click())
    }

    fun clickLangButton() {
        langButton.perform(doubleClick())
    }

    fun clickToSelectedLanguageButton() {
        selectedLanguageButton.perform(click())
    }

    fun typeInSearchBar(request: String = DEFAULT_REQUEST) {
        searchBarViewText.perform(replaceText(request))
    }


    companion object {
        private const val DEFAULT_REQUEST = "Тинькофф премьер лига"
        private const val SELECTED_LANGUAGE = "русский"

        inline operator fun invoke(crossinline block: SearchScreen.() -> Unit) {
            SearchScreen().block()
        }
    }

}
