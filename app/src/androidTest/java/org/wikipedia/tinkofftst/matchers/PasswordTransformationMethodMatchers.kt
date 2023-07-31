package org.wikipedia.tinkofftst.matchers

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not

object PasswordTransformationMethodMatchers {
    fun hasPasswordTransformation(): Matcher<View> {
        return object : BoundedMatcher<View, EditText>(EditText::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("has password transformation")
            }

            override fun matchesSafely(editText: EditText): Boolean {
                return editText.transformationMethod is PasswordTransformationMethod
            }
        }
    }

    fun hasNotPasswordTransformation(): Matcher<View> = not(hasPasswordTransformation())

}