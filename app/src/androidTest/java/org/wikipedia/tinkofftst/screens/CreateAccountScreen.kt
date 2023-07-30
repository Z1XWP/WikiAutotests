package org.wikipedia.tinkofftst.screens

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Checks
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.material.textfield.TextInputLayout
import junit.framework.TestCase.assertTrue
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil.getThemedColor


class CreateAccountScreen {
    private val usernameEditText by lazy { onView(withHint(R.string.create_account_username_hint)) }
    private val passwordEditText by lazy { onView(withHint(R.string.account_creation_password_hint)) }
    private val repeatPasswordEditText by lazy { onView(withHint(R.string.create_account_password_repeat_hint)) }
    private val emailEditText by lazy { onView(withHint(R.string.create_account_email_hint)) }
    private val nextButton by lazy { onView(withId(R.id.create_account_submit_button)) }
    private val errorMessageTextView by lazy { onView(withId(R.id.create_account_password_input)) }

    fun typeUsername(username: String = DEFAULT_USERNAME) {
        usernameEditText.perform(
            typeText(username)
        )
    }

    fun typePassword(password: String = DEFAULT_PASSWORD) {
        passwordEditText.perform(
            typeText(password)
        )
    }

    fun typeRepeatPassword(password: String = DEFAULT_PASSWORD) {
        repeatPasswordEditText.perform(
            typeText(password)
        )
    }

    fun typeEmail(email: String = DEFAULT_EMAIL) {
        emailEditText.perform(
            typeText(email)
        )
    }

    fun clickNextButton() {
        nextButton.perform(click())
    }

    fun checkPasswordIsObfuscated() {
        passwordEditText.check { view, _ ->
            assertTrue(
                "PasswordTransformationMethod should be set",
                (view as EditText).transformationMethod is PasswordTransformationMethod
            )
        }
    }

    fun checkErrorMessageColor() {
        errorMessageTextView.check(
            matches(
                allOf(
                    isDisplayed(),
                    withTextColor(R.attr.destructive_color)
                )
            )
        )
    }

    private fun withTextColor(expectedColor: Int): Matcher<View?> {
        Checks.checkNotNull(expectedColor)
        return object : BoundedMatcher<View?, TextInputLayout>(TextInputLayout::class.java) {
            override fun matchesSafely(item: TextInputLayout?): Boolean {
                return expectedColor == item?.errorCurrentTextColors
            }

            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
                description.appendValue(expectedColor)
            }
        }
    }


    companion object {
        private const val DEFAULT_USERNAME = "Smth.username"
        private const val DEFAULT_PASSWORD = "pass12"
        private const val DEFAULT_EMAIL = "smth.email@gmail.com"
        inline operator fun invoke(crossinline block: CreateAccountScreen.() -> Unit) {
            CreateAccountScreen().block()
        }
    }

}
