package org.wikipedia.TinkoffTst.screens

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import io.qameta.allure.kotlin.Allure.step
import org.hamcrest.Matchers.allOf
import org.wikipedia.R

class SettingFeedScreen {


    private fun isSwitchCheckedByTitle(@StringRes title: Int) {
        onView(allOf(
                withId(R.id.feed_content_type_checkbox),
                hasSibling(hasDescendant(withText(title)))

            )
        ).check(matches(isChecked()))
    }

    fun checkAllSwitches() {
        step("Проверяем, что все переключатели активны") {
            switchTitles.forEach { title ->
                isSwitchCheckedByTitle(title)
            }
        }
    }

    companion object {
        private val switchTitles = listOf(
                R.string.view_featured_article_card_title,
                R.string.view_top_read_card_title,
                R.string.view_featured_image_card_title,
                R.string.view_because_you_read_card_title,
                R.string.view_card_news_title,
                R.string.on_this_day_card_title,
                R.string.view_random_card_title,
                R.string.view_main_page_card_title,
        )
        inline operator fun invoke(crossinline block: SettingFeedScreen.() -> Unit) {
            SettingFeedScreen().block()
        }
    }


}