package org.wikipedia.TinkoffTst.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.wikipedia.R

class CreateAccountScreen {
    private val usernameEditText by lazy { onView(withHint("Username")) }
    private val passwordEditText by lazy { onView(withHint("Password")) }
    private val repeatPasswordEditText by lazy { onView(withHint("Repeat password")) }
    private val emailEditText by lazy { onView(withHint("Email (Optional)")) }
    private val nextButton by lazy { onView(withId(R.id.create_account_submit_button)) }

    fun typeUsername(username: String) {
        usernameEditText.perform(
            click(),
            replaceText(username)
            )
    }

    fun typePassword(password: String) {
        passwordEditText.perform(
            click(),
            replaceText(password)
        )
    }

    fun typeRepeatPassword(repeatPassword: String) {
        repeatPasswordEditText.perform(
            click(),
            replaceText(repeatPassword)
        )
    }

    fun typeEmail(email: String) {
        emailEditText.perform(
            click(),
            replaceText(email)
        )
    }

    fun clickNextButton() {
        nextButton.perform(click())
    }

    companion object{
        inline operator fun invoke(crossinline block: CreateAccountScreen.() -> Unit) {
            CreateAccountScreen().block()
        }
    }

}