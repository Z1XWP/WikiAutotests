package org.wikipedia.tinkofftst.screens


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isFocused
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import org.wikipedia.R
import org.wikipedia.tinkofftst.matchers.PasswordTransformationMethodMatchers.hasNotPasswordTransformation
import org.wikipedia.tinkofftst.matchers.PasswordTransformationMethodMatchers.hasPasswordTransformation
import org.wikipedia.tinkofftst.matchers.TextColorMatchers.withTextColor



class CreateAccountScreen {
    private val usernameEditText by lazy { onView(withHint(R.string.create_account_username_hint)) }
    private val passwordEditText by lazy { onView(withHint(R.string.account_creation_password_hint)) }
    private val repeatPasswordEditText by lazy { onView(withHint(R.string.create_account_password_repeat_hint)) }
    private val emailEditText by lazy { onView(withHint(R.string.create_account_email_hint)) }
    private val nextButton by lazy { onView(withId(R.id.create_account_submit_button)) }
    private val errorMessageTextView by lazy { onView(withId(R.id.create_account_password_input)) }
    private val obfuscatePasswordButton by lazy {
        onView(
            allOf(
                isDescendantOfA(hasSibling(isFocused())),
                withId(PASSWORD_EYE_BUTTON_ID)
            )
        )
    }

    fun typeUsername(username: String = DEFAULT_USERNAME) {
        usernameEditText.perform(
            typeText(username)
        )
    }

    fun typePassword(password: String = INVALID_PASSWORD) {
        passwordEditText.perform(
            typeText(password)
        )
    }

    fun typeRepeatPassword(password: String = INVALID_PASSWORD) {
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

    fun clickObfuscatePasswordButton() {
        obfuscatePasswordButton.perform(click())
    }

    fun checkPasswordIsObfuscated() {
        passwordEditText.check(matches(hasPasswordTransformation()))
    }

    fun checkPasswordIsNotObfuscated() {
        passwordEditText.check(matches(hasNotPasswordTransformation()))
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

    companion object {
        private const val DEFAULT_USERNAME = "Smth.username"
        private const val INVALID_PASSWORD = "pass12"
        private const val DEFAULT_EMAIL = "smth.email@gmail.com"
        private const val PASSWORD_EYE_BUTTON_ID = 2131297700
        inline operator fun invoke(crossinline block: CreateAccountScreen.() -> Unit) {
            CreateAccountScreen().block()
        }
    }

}
