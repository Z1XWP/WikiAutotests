package org.wikipedia.TinkoffTst.screens

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Checks
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import junit.framework.TestCase.assertTrue
import org.hamcrest.Description
import org.hamcrest.Matchers.allOf
import org.wikipedia.R

class CreateAccountScreen {
    private val usernameEditText by lazy { onView(withHint(R.string.create_account_username_hint)) }
    private val passwordEditText by lazy { onView(withHint(R.string.account_creation_password_hint)) }
    private val repeatPasswordEditText by lazy { onView(withHint(R.string.create_account_password_repeat_hint)) }
    private val emailEditText by lazy { onView(withHint(R.string.create_account_email_hint)) }
    private val nextButton by lazy { onView(withId(R.id.create_account_submit_button)) }
    private val errorMessageTextView by lazy { onView(withId(R.id.create_account_password_input)) }

    fun typeUsername(username: String) {
        usernameEditText.perform(
            typeText(username)
            )
    }

    fun typePassword(password: String) {
        passwordEditText.perform(
            typeText(password)
        )
    }

    fun typeRepeatPassword(repeatPassword: String) {
        repeatPasswordEditText.perform(
            typeText(repeatPassword)
        )
    }

    fun typeEmail(email: String) {
        emailEditText.perform(
            typeText(email)
        )
    }

    fun clickNextButton() {
        nextButton.perform(click())
    }

    fun checkPasswordIsObfuscated(){
            passwordEditText.check { view, _ ->
                assertTrue(
                    "PasswordTransformationMethod should be set",
                    (view as EditText).transformationMethod is PasswordTransformationMethod
                )
            }
    }

    fun checkErrorMessageColor(){
        errorMessageTextView.check(matches(allOf(isDisplayed(), withTextColor(R.color.red700))))
    }

    private fun withTextColor(expectedColor: Int): BoundedMatcher<View?, TextView> {
        Checks.checkNotNull(expectedColor)
        return object : BoundedMatcher<View?, TextView>(TextView::class.java) {
            override fun matchesSafely(textView: TextView): Boolean {
                return expectedColor == textView.currentTextColor
            }
            override fun describeTo(description: Description) {
                description.appendText("with text color: ")
                description.appendValue(expectedColor)
            }
        }
    }

    companion object{
        inline operator fun invoke(crossinline block: CreateAccountScreen.() -> Unit) {
            CreateAccountScreen().block()
        }
    }

}