package org.wikipedia.tinkofftst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import org.wikipedia.R


class SearchScreen {
    private val searchViewText by lazy { onView(withHint(R.string.search_hint)) }

    fun typeInSearchBar(request: String = DEFAULT_REQUEST){
        searchViewText.perform(typeText(request))
    }



    companion object {
        private const val DEFAULT_REQUEST = "Тинькофф премьер лига"

        inline operator fun invoke(crossinline block: SearchScreen.() -> Unit) {
            SearchScreen().block()
        }
    }

}
