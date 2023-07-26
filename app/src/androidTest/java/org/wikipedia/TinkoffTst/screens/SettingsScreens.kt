package org.wikipedia.TinkoffTst.screens

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.qameta.allure.kotlin.Allure.step
import org.wikipedia.R

class SettingsScreen {
    private val exploreFeedButton by lazy { onView(withText(R.string.preference_title_customize_explore_feed)) }
    private val aboutWikiButton by lazy { onView(withText(R.string.about_description)) }
    private val recyclerView by lazy { onView(withId(R.id.recycler_view)) }

    fun clickExploreFeedButton() {
        step("Нажимаем кнопку \"Настройки ленты\"") {
            exploreFeedButton.perform(click())
        }
    }

    fun scrollRecyclerView() {
        step("Скроллим вниз до кнопки \"О приложении \"Википедия\"\"") {
            recyclerView.perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText(R.string.about_description))
                    )
            )
        }
    }

    fun clickAboutWikiButton() {
        step("Нажимаем кнопку \"О приложении \"Википедия\"\"") {

            aboutWikiButton.perform(click())
        }
    }

    companion object {
        inline operator fun invoke(crossinline block: SettingsScreen.() -> Unit) {
            SettingsScreen().block()
        }

    }
}

