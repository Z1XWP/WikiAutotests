package org.wikipedia.tinkofftst.matchers

import android.view.View
import androidx.annotation.AttrRes
import androidx.test.espresso.matcher.BoundedMatcher
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.wikipedia.util.ResourceUtil.getThemedColor


object TextColorMatchers {
    fun withTextColor(@AttrRes expectedColor: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, TextInputLayout>(TextInputLayout::class.java) {
            override fun matchesSafely(item: TextInputLayout): Boolean {
                val convertedColor = getThemedColor(item.context, expectedColor)
                return convertedColor == item.errorCurrentTextColors
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
                description.appendValue(expectedColor)
            }
        }
    }

}