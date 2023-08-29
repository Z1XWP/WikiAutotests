package org.wikipedia.tinkofftst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.wikipedia.R

class MainScreen {
    private val searchBarView by lazy { onView(withId(R.id.search_container)) }
    private val searchResultView by lazy { onView(withText(DEFAULT_SEARCH_RESULT)) }

    fun clickSearchBar() {
        searchBarView.perform(ViewActions.click())
    }

    fun clickSearchResult() {
        searchResultView.perform()
    }

    companion object {
        private const val DEFAULT_SEARCH_RESULT = "Российская премьер-лига"

        inline operator fun invoke(crossinline block: MainScreen.() -> Unit) {
            MainScreen().block()
        }
    }
}