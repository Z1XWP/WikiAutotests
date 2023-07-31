package org.wikipedia.tinkofftst


import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.junit4.DisplayName
import junit.framework.TestCase.assertEquals
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.wikipedia.tinkofftst.screens.SearchScreen
import org.wikipedia.tinkofftst.screens.AboutScreen
import org.wikipedia.main.MainActivity
import org.wikipedia.tinkofftst.screens.BottomSheet
import org.wikipedia.tinkofftst.screens.CreateAccountScreen
import org.wikipedia.tinkofftst.screens.NavigationBar
import org.wikipedia.tinkofftst.screens.SettingFeedScreen
import org.wikipedia.tinkofftst.screens.SettingsScreen
import org.wikipedia.settings.Prefs

@Epic("Tinkoff")


class Tests {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    @AllureId("1")
    @DisplayName("Проверка перехода в браузер")
    fun testCheckRedirectToBrowser() {

        NavigationBar {
            clickMoreButton()
        }

        BottomSheet {
            clickDonateButton()
        }

        checkBrowserOpened()
    }

    @Test
    @AllureId("2")
    @DisplayName("Проверка настройки ленты по умолчанию")
    fun testDefaultFeedSettings() {

        NavigationBar {
            clickMoreButton()
        }

        BottomSheet {
            clickSettingsButton()
        }

        SettingsScreen {
            clickExploreFeedButton()
        }

        SettingFeedScreen {
            checkAllSwitches()
        }
    }

    @Test
    @AllureId("3")
    @DisplayName("Проверка блоков на экране \"О приложении\"")
    fun testCheckBlocksOnAboutAppScreen() {

        NavigationBar {
            clickMoreButton()
        }

        BottomSheet {
            clickSettingsButton()
        }

        SettingsScreen {
            scrollRecyclerView()
            clickAboutWikiButton()
        }

        AboutScreen {
            checkContributorsIsDisplayed()
            checkTranslatorsIsDisplayed()
            checkLicenseIsDisplayed()
        }

    }

    @Test
    @AllureId("4")
    @DisplayName("Проверка, что пароль скрывается, если нажать на глазик два раза")
    fun testPasswordIsObfuscated() {

        NavigationBar {
            clickMoreButton()
        }

        BottomSheet {
            clickLoginButton()
        }

        CreateAccountScreen {
            typePassword()
            clickObfuscatePasswordButton()
            checkPasswordIsNotObfuscated()
            clickObfuscatePasswordButton()
            checkPasswordIsObfuscated()
        }
    }

    @Test
    @AllureId("5")
    @DisplayName("Проверка валидации поля установки пароля")
    fun testValidationPasswordField() {

        NavigationBar {
            clickMoreButton()
        }

        BottomSheet {
            clickLoginButton()
        }

        CreateAccountScreen {
            typeUsername()
            typePassword()
            typeRepeatPassword()
            typeEmail()
            clickNextButton()
            checkErrorMessageColor()
        }
    }

/*    @Test
    @AllureId("6")
    @DisplayName("Проверка добавления статьи в избранное")
    fun testCheckAddArticleToFavorites() {

        SearchScreen{
            typeInSearchBar()
        }
    }
*/
    private fun checkBrowserOpened() {
        step("Проверяем, что браузер открылся") {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val currentPackage = device.currentPackageName
            assertEquals("Browser is not opened", CHROME_PACKAGE, currentPackage)
        }
    }


    companion object {

        private const val CHROME_PACKAGE = "com.android.chrome"

        @JvmStatic
        @BeforeClass
        fun initialOnboardingSkip() {
            Prefs.isInitialOnboardingEnabled = false
        }
    }


}



