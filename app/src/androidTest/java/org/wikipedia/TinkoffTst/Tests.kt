package org.wikipedia.TinkoffTst


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
import org.wikipedia.TinkoffTst.screens.AboutScreen
import org.wikipedia.main.MainActivity
import org.wikipedia.TinkoffTst.screens.BottomSheet
import org.wikipedia.TinkoffTst.screens.CreateAccountScreen
import org.wikipedia.TinkoffTst.screens.NavigationBar
import org.wikipedia.TinkoffTst.screens.SettingFeedScreen
import org.wikipedia.TinkoffTst.screens.SettingsScreen
import org.wikipedia.settings.Prefs
import java.lang.Thread.sleep


/*
Проверка перехода в браузер
+ 1. Открываем приложение
+ 2. Нажимаем на кнопку "Еще"
+ 3. Нажимаем кнопку "Пожертвовать"
+ 4. Проверяем, что случился переход на окно выбора браузера для открытия
-----------------------------------------------------------------------------
Проверка настройки ленты по умолчанию
+ 1. Открываем приложение
+ 2. Нажимаем на кнопку "Еще"
+ 3. Нажимаем кнопку "Настройки"
+ 4. Нажимаем на кнопку "Настройки ленты"
+ 5. Проверяем, что каждый чек-бокс в состоянии checked
Не забываем про правило один кейс — одна проверка :wink:
-----------------------------------------------------------------------------
Проверка блоков на экране "О приложении"
+ 1. Открываем приложение
+ 2. Нажимаем "Еще"
+ 3. Нажимаем настройки
+ 4. Нажимаем на "О приложении "Википедия""
+ 5. Проверяем, что на экране есть блок "Авторы", "Переводчики" и "Лицензия"
-----------------------------------------------------------------------------
Проверка, что пароль скрывается, если нажать на глазик два раза
+ 1. Открываем приложение
+ 2. Нажимаем на кнопку «Еще»
+ 3. Нажимаем "Создать учетную запись"
+ 4. Заполняем поле пароля любимыми данными
+ 5. Нажимаем иконку "глазик"
+ 6. Проверяем что отображается введенный пароль
+ 7. Нажимаем иконку "глазик"
+ 8. Проверяем, что отображаются точки (пароль скрыт)
 */

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
            typePassword("pass12345")
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
            typeUsername("Smth.username")
            typePassword("pass12")
            typeRepeatPassword("pass12")
            typeEmail("smth.email@gmail.com")
            clickNextButton()
//            checkErrorMessageColor()
        }
    }


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



