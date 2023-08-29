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
    private val searchBarViewText by lazy { onView((withHint(R.string.search_hint))) }


    fun clickSearchBar() {
        searchBarView.perform(click())
    }


    fun typeInSearchBar(request: String = DEFAULT_REQUEST) {
        searchBarViewText.perform(replaceText(request))
    }


    companion object {
        private const val DEFAULT_REQUEST = "Тинькофф премьер лига"

        inline operator fun invoke(crossinline block: SearchScreen.() -> Unit) {
            SearchScreen().block()
        }
    }

}
