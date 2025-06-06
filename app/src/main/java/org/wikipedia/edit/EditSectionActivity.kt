package org.wikipedia.edit

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.auth.AccountUtil.isLoggedIn
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.ActivityEditSectionBinding
import org.wikipedia.databinding.DialogWithCheckboxBinding
import org.wikipedia.databinding.ItemEditActionbarButtonBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwParseResponse
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.edit.preview.EditPreviewFragment
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.edit.summaries.EditSummaryFragment
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.search.SearchActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.ThemeChooserDialog
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.EditNoticesDialog
import org.wikipedia.views.ViewUtil
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class EditSectionActivity : BaseActivity(), ThemeChooserDialog.Callback {
    private lateinit var binding: ActivityEditSectionBinding
    private lateinit var textWatcher: TextWatcher
    private lateinit var captchaHandler: CaptchaHandler
    private lateinit var editPreviewFragment: EditPreviewFragment
    private lateinit var editSummaryFragment: EditSummaryFragment
    private lateinit var syntaxHighlighter: SyntaxHighlighter
    lateinit var pageTitle: PageTitle
        private set

    private var sectionID = -1
    private var sectionAnchor: String? = null
    private var textToHighlight: String? = null
    private var sectionWikitext: String? = null
    private val editNotices = mutableListOf<String>()

    private var sectionTextModified = false
    private var sectionTextFirstLoad = true
    private var editingAllowed = false

    // Current revision of the article, to be passed back to the server to detect possible edit conflicts.
    private var currentRevision: Long = 0
    private var actionMode: ActionMode? = null
    private val disposables = CompositeDisposable()

    private val requestLinkFromSearch = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            it.data?.getParcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE)?.let { title ->
                binding.editKeyboardOverlay.insertLink(title, pageTitle.wikiSite.languageCode)
            }
        }
    }

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            updateEditLicenseText()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        }
    }

    private val requestInsertMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == InsertMediaActivity.RESULT_INSERT_MEDIA_SUCCESS) {
            it.data?.let { intent ->
                binding.editSectionText.inputConnection?.commitText("${intent.getStringExtra(InsertMediaActivity.RESULT_WIKITEXT)}", 1)
            }
        }
    }

    private val editTokenThenSave: Unit
        get() {
            cancelCalls()
            binding.editSectionCaptchaContainer.visibility = View.GONE
            captchaHandler.hideCaptcha()
            editSummaryFragment.saveSummary()
            disposables.add(CsrfTokenClient.getToken(pageTitle.wikiSite)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ doSave(it) }) { showError(it) })
        }

    private val movementMethod = LinkMovementMethodExt { urlStr ->
        UriUtil.visitInExternalBrowser(this, Uri.parse(UriUtil.resolveProtocolRelativeUrl(pageTitle.wikiSite, urlStr)))
    }

    private val syntaxButtonCallback = object : WikiTextKeyboardView.Callback {
        override fun onPreviewLink(title: String) {
            val dialog = LinkPreviewDialog.newInstance(HistoryEntry(PageTitle(title, pageTitle.wikiSite), HistoryEntry.SOURCE_INTERNAL_LINK), null)
            ExclusiveBottomSheetPresenter.show(supportFragmentManager, dialog)
            binding.root.post {
                dialog.dialog?.setOnDismissListener {
                    if (!isDestroyed) {
                        binding.root.postDelayed({
                            DeviceUtil.showSoftKeyboard(binding.editSectionText)
                        }, 200)
                    }
                }
            }
        }

        override fun onRequestInsertMedia() {
            requestInsertMedia.launch(InsertMediaActivity.newIntent(this@EditSectionActivity, pageTitle.wikiSite, pageTitle.displayText))
        }

        override fun onRequestInsertLink() {
            requestLinkFromSearch.launch(SearchActivity.newIntent(this@EditSectionActivity, Constants.InvokeSource.EDIT_ACTIVITY, null, true))
        }

        override fun onRequestHeading() {
            if (binding.editKeyboardOverlayHeadings.isVisible) {
                hideAllSyntaxModals()
                return
            }
            hideAllSyntaxModals()
            binding.editKeyboardOverlayHeadings.isVisible = true
            binding.editKeyboardOverlay.onAfterHeadingsShown()
        }

        override fun onRequestFormatting() {
            if (binding.editKeyboardOverlayFormattingContainer.isVisible) {
                hideAllSyntaxModals()
                return
            }
            hideAllSyntaxModals()
            binding.editKeyboardOverlayFormattingContainer.isVisible = true
            binding.editKeyboardOverlay.onAfterFormattingShown()
        }

        override fun onSyntaxOverlayCollapse() {
            hideAllSyntaxModals()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))

        pageTitle = intent.getParcelableExtra(Constants.ARG_TITLE)!!
        sectionID = intent.getIntExtra(EXTRA_SECTION_ID, -1)
        sectionAnchor = intent.getStringExtra(EXTRA_SECTION_ANCHOR)
        textToHighlight = intent.getStringExtra(EXTRA_HIGHLIGHT_TEXT)
        supportActionBar?.title = ""
        syntaxHighlighter = SyntaxHighlighter(this, binding.editSectionText, binding.editSectionScroll)
        binding.editSectionScroll.isSmoothScrollingEnabled = false
        captchaHandler = CaptchaHandler(this, pageTitle.wikiSite, binding.captchaContainer.root,
                binding.editSectionText, "", null)
        editPreviewFragment = supportFragmentManager.findFragmentById(R.id.edit_section_preview_fragment) as EditPreviewFragment
        editSummaryFragment = supportFragmentManager.findFragmentById(R.id.edit_section_summary_fragment) as EditSummaryFragment
        editSummaryFragment.title = pageTitle

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            EditAttemptStepEvent.logInit(pageTitle)
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_KEY_TEMPORARY_WIKITEXT_STORED)) {
                sectionWikitext = Prefs.temporaryWikitext
            }
            editingAllowed = savedInstanceState.getBoolean(EXTRA_KEY_EDITING_ALLOWED, false)
            sectionTextModified = savedInstanceState.getBoolean(EXTRA_KEY_SECTION_TEXT_MODIFIED, false)
        }
        L10nUtil.setConditionalTextDirection(binding.editSectionText, pageTitle.wikiSite.languageCode)
        fetchSectionText()

        binding.viewEditSectionError.retryClickListener = View.OnClickListener {
            binding.viewEditSectionError.visibility = View.GONE
            captchaHandler.requestNewCaptcha()
            fetchSectionText()
        }
        binding.viewEditSectionError.backClickListener = View.OnClickListener {
            onBackPressed()
        }

        textWatcher = binding.editSectionText.doAfterTextChanged {
            if (sectionTextFirstLoad) {
                sectionTextFirstLoad = false
                return@doAfterTextChanged
            }
            if (!sectionTextModified) {
                sectionTextModified = true
                // update the actionbar menu, which will enable the Next button.
                invalidateOptionsMenu()
            }
        }
        binding.editKeyboardOverlay.editText = binding.editSectionText
        binding.editKeyboardOverlay.callback = syntaxButtonCallback
        binding.editKeyboardOverlayFormatting.editText = binding.editSectionText
        binding.editKeyboardOverlayFormatting.callback = syntaxButtonCallback
        binding.editKeyboardOverlayHeadings.editText = binding.editSectionText
        binding.editKeyboardOverlayHeadings.callback = syntaxButtonCallback

        binding.editSectionText.setOnClickListener { finishActionMode() }
        onEditingPrefsChanged()

        binding.editSectionContainer.viewTreeObserver.addOnGlobalLayoutListener {
            binding.editSectionContainer.post {
                if (!isDestroyed) {
                    if (isHardKeyboardAttached() || window.decorView.height - binding.editSectionContainer.height > DimenUtil.roundedDpToPx(150f)) {
                        binding.editKeyboardOverlayContainer.isVisible = true
                    } else {
                        hideAllSyntaxModals()
                        binding.editKeyboardOverlayContainer.isVisible = false
                    }
                }
            }
        }

        // set focus to the EditText, but keep the keyboard hidden until the user changes the cursor location:
        binding.editSectionText.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        hideAllSyntaxModals()
    }

    public override fun onStart() {
        super.onStart()
        updateEditLicenseText()
    }

    public override fun onDestroy() {
        captchaHandler.dispose()
        cancelCalls()
        binding.editSectionText.removeTextChangedListener(textWatcher)
        syntaxHighlighter.cleanup()
        super.onDestroy()
    }

    private fun isHardKeyboardAttached(): Boolean {
        return (resources.configuration.hardKeyboardHidden == Configuration.KEYBOARDHIDDEN_NO &&
                resources.configuration.keyboard != Configuration.KEYBOARD_UNDEFINED &&
                resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS)
    }

    private fun updateEditLicenseText() {
        val editLicenseText = ActivityCompat.requireViewById<TextView>(this, R.id.licenseText)
        editLicenseText.text = StringUtil.fromHtml(getString(if (isLoggedIn) R.string.edit_save_action_license_logged_in else R.string.edit_save_action_license_anon,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_4_url)))
        editLicenseText.movementMethod = LinkMovementMethodExt { url: String ->
            if (url == "https://#login") {
                val loginIntent = LoginActivity.newIntent(this@EditSectionActivity, LoginActivity.SOURCE_EDIT)
                requestLogin.launch(loginIntent)
            } else {
                UriUtil.handleExternalLink(this@EditSectionActivity, url.toUri())
            }
        }
    }

    private fun cancelCalls() {
        disposables.clear()
    }

    private fun doSave(token: String) {
        val sectionAnchor = StringUtil.addUnderscores(StringUtil.removeHTMLTags(sectionAnchor.orEmpty()))
        val isMinorEdit = if (editSummaryFragment.isMinorEdit) true else null
        val watchThisPage = if (editSummaryFragment.watchThisPage) "watch" else "unwatch"
        var summaryText = if (sectionAnchor.isEmpty() || sectionAnchor == pageTitle.prefixedText) {
            if (pageTitle.wikiSite.languageCode == "en") "/* top */" else ""
        } else "/* ${StringUtil.removeUnderscores(sectionAnchor)} */ "
         summaryText += editSummaryFragment.summary
        // Summaries are plaintext, so remove any HTML that's made its way into the summary
        summaryText = StringUtil.removeHTMLTags(summaryText)
        if (!isFinishing) {
            showProgressBar(true)
        }
        disposables.add(ServiceFactory.get(pageTitle.wikiSite).postEditSubmit(pageTitle.prefixedText,
                if (sectionID >= 0) sectionID.toString() else null, null, summaryText, if (isLoggedIn) "user" else null,
                binding.editSectionText.text.toString(), null, currentRevision, token,
                if (captchaHandler.isActive) captchaHandler.captchaId() else "null",
                if (captchaHandler.isActive) captchaHandler.captchaWord() else "null", isMinorEdit, watchThisPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    result.edit?.run {
                        when {
                            editSucceeded -> waitForUpdatedRevision(newRevId)
                            hasCaptchaResponse -> onEditSuccess(CaptchaResult(captchaId))
                            hasSpamBlacklistResponse -> onEditFailure(MwException(MwServiceError(code, spamblacklist)))
                            hasEditErrorCode -> onEditFailure(MwException(MwServiceError(code, info)))
                            else -> onEditFailure(IOException("Received unrecognized edit response"))
                        }
                    } ?: run {
                        onEditFailure(IOException("An unknown error occurred."))
                    }
                }) { onEditFailure(it) }
        )

        BreadCrumbLogEvent.logInputField(this, editSummaryFragment.summaryText)
    }

    @Suppress("SameParameterValue")
    private fun waitForUpdatedRevision(newRevision: Long) {
        AnonymousNotificationHelper.onEditSubmitted()
        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite)
            .getSummaryResponse(pageTitle.prefixedText, null, OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(), null, null, null)
            .delay(2, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .map { response ->
                if (response.body()!!.revision < newRevision) {
                    throw IllegalStateException()
                }
                response.body()!!.revision
            }
            .retry(10) { it is IllegalStateException }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                onEditSuccess(EditSuccessResult(it))
            }, {
                onEditSuccess(EditSuccessResult(newRevision))
            })
        )
    }

    private fun onEditSuccess(result: EditResult) {
        if (result is EditSuccessResult) {
            EditAttemptStepEvent.logSaveSuccess(pageTitle)
            // TODO: remove the artificial delay and use the new revision
            // ID returned to request the updated version of the page once
            // revision support for mobile-sections is added to RESTBase
            // See https://github.com/wikimedia/restbase/pull/729
            Handler(mainLooper).postDelayed(TimeUnit.SECONDS.toMillis(2)) {
                showProgressBar(false)

                // Build intent that includes the section we were editing, so we can scroll to it later
                val data = Intent()
                data.putExtra(EXTRA_SECTION_ID, sectionID)
                setResult(EditHandler.RESULT_REFRESH_PAGE, data)
                DeviceUtil.hideSoftKeyboard(this@EditSectionActivity)
                finish()
            }
            return
        }
        showProgressBar(false)
        if (result is CaptchaResult) {
            binding.editSectionCaptchaContainer.visibility = View.VISIBLE
            captchaHandler.handleCaptcha(null, result)
        } else {
            EditAttemptStepEvent.logSaveFailure(pageTitle)
            // Expand to do everything.
            onEditFailure(Throwable())
        }
    }

    private fun onEditFailure(caught: Throwable) {
        showProgressBar(false)
        if (caught is MwException) {
            handleEditingException(caught)
        } else {
            showRetryDialog(caught)
        }
        L.e(caught)
    }

    private fun showRetryDialog(t: Throwable) {
        MaterialAlertDialogBuilder(this@EditSectionActivity)
                .setTitle(R.string.dialog_message_edit_failed)
                .setMessage(t.localizedMessage)
                .setPositiveButton(R.string.dialog_message_edit_failed_retry) { dialog, _ ->
                    editTokenThenSave
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_message_edit_failed_cancel) { dialog, _ -> dialog.dismiss() }.show()
    }

    /**
     * Processes API error codes encountered during editing, and handles them as appropriate.
     * @param caught The MwException to handle.
     */
    private fun handleEditingException(caught: MwException) {
        val code = caught.title

        // In the case of certain AbuseFilter responses, they are sent as a code, instead of a
        // fully parsed response. We need to make one more API call to get the parsed message:
        if (code.startsWith("abusefilter-") && caught.message.contains("abusefilter-") && caught.message.length < 100) {
            disposables.add(ServiceFactory.get(pageTitle.wikiSite).parsePage("MediaWiki:" + StringUtil.sanitizeAbuseFilterCode(caught.message))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response: MwParseResponse -> showError(MwException(MwServiceError(code, response.text))) }) { showError(it) })
        } else if ("editconflict" == code) {
            MaterialAlertDialogBuilder(this@EditSectionActivity)
                    .setTitle(R.string.edit_conflict_title)
                    .setMessage(R.string.edit_conflict_message)
                    .setPositiveButton(R.string.edit_conflict_dialog_ok_button_text, null)
                    .show()
            resetToStart()
        } else {
            showError(caught)
        }
    }

    /**
     * Executes a click of the actionbar button, and performs the appropriate action
     * based on the current state of the button.
     */
    fun clickNextButton() {
        when {
            editSummaryFragment.isActive -> {
                editTokenThenSave
                EditAttemptStepEvent.logSaveAttempt(pageTitle)
                supportActionBar?.title = getString(R.string.preview_edit_summarize_edit_title)
            }
            editPreviewFragment.isActive -> {
                editSummaryFragment.show()
                supportActionBar?.title = getString(R.string.preview_edit_summarize_edit_title)
            }
            else -> {
                // we must be showing the editing window, so show the Preview.
                DeviceUtil.hideSoftKeyboard(this)
                editPreviewFragment.showPreview(pageTitle, binding.editSectionText.text.toString())
                EditAttemptStepEvent.logSaveIntent(pageTitle)
                supportActionBar?.title = getString(R.string.preview_edit_title)
                setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_section -> {
                clickNextButton()
                true
            }
            R.id.menu_edit_theme -> {
                binding.editSectionText.enqueueNoScrollingLayoutChange()
                ExclusiveBottomSheetPresenter.show(supportFragmentManager, ThemeChooserDialog.newInstance(Constants.InvokeSource.EDIT_ACTIVITY, true))
                true
            }
            R.id.menu_find_in_editor -> {
                showFindInEditor()
                true
            }
            R.id.menu_edit_notices -> {
                showEditNotices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_section, menu)
        val item = menu.findItem(R.id.menu_save_section)

        supportActionBar?.elevation = if (editPreviewFragment.isActive) 0f else DimenUtil.dpToPx(4f)
        menu.findItem(R.id.menu_edit_notices).isVisible = editNotices.isNotEmpty() && !editPreviewFragment.isActive
        menu.findItem(R.id.menu_edit_theme).isVisible = !editPreviewFragment.isActive
        menu.findItem(R.id.menu_find_in_editor).isVisible = !editPreviewFragment.isActive
        item.title = getString(if (editSummaryFragment.isActive) R.string.edit_done else R.string.edit_next)
        if (editingAllowed && binding.viewProgressBar.isGone) {
            item.isEnabled = sectionTextModified
        } else {
            item.isEnabled = false
        }
        val summaryFilledOrNotActive = if (editSummaryFragment.isActive) editSummaryFragment.summary.isNotEmpty() else true
        applyActionBarButtonStyle(item, item.isEnabled && summaryFilledOrNotActive)
        return true
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (mode.tag == null) {
            // since we disabled the close button in the AndroidManifest.xml, we need to manually setup a close button when in an action mode if long pressed on texts.
            ViewUtil.setCloseButtonInActionMode(this@EditSectionActivity, mode)
        }
    }

    private fun applyActionBarButtonStyle(menuItem: MenuItem, emphasize: Boolean) {
        val actionBarButtonBinding = ItemEditActionbarButtonBinding.inflate(layoutInflater)
        menuItem.actionView = actionBarButtonBinding.root
        actionBarButtonBinding.editActionbarButtonText.text = menuItem.title
        actionBarButtonBinding.editActionbarButtonText.setTextColor(
            ResourceUtil.getThemedColor(this,
                if (emphasize) R.attr.progressive_color else R.attr.placeholder_color))
        actionBarButtonBinding.root.tag = menuItem
        actionBarButtonBinding.root.isEnabled = menuItem.isEnabled
        actionBarButtonBinding.root.setOnClickListener { onOptionsItemSelected(it.tag as MenuItem) }
    }

    fun showError(caught: Throwable?) {
        DeviceUtil.hideSoftKeyboard(this)
        binding.viewEditSectionError.setError(caught)
        binding.viewEditSectionError.visibility = View.VISIBLE
    }

    private fun showFindInEditor() {
        startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                actionMode = mode
                val menuItem = menu.add(R.string.edit_section_find_in_page)
                menuItem.actionProvider = FindInEditorActionProvider(binding.editSectionScroll,
                        binding.editSectionText, syntaxHighlighter, actionMode!!)
                menuItem.expandActionView()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.tag = "actionModeFindInEditor"
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                syntaxHighlighter.clearSearchQueryInfo()
                binding.editSectionText.setSelection(binding.editSectionText.selectionStart,
                        binding.editSectionText.selectionStart)
            }
        })
    }

    private fun finishActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_KEY_TEMPORARY_WIKITEXT_STORED, true)
        outState.putBoolean(EXTRA_KEY_SECTION_TEXT_MODIFIED, sectionTextModified)
        outState.putBoolean(EXTRA_KEY_EDITING_ALLOWED, editingAllowed)
        Prefs.temporaryWikitext = sectionWikitext.orEmpty()
    }

    private fun updateTextSize() {
        binding.editSectionText.textSize = WikipediaApp.instance.getFontSize(window, editing = true)
    }

    private fun resetToStart() {
        if (captchaHandler.isActive) {
            captchaHandler.cancelCaptcha()
            binding.editSectionCaptchaContainer.visibility = View.GONE
        }
        if (editSummaryFragment.isActive) {
            editSummaryFragment.hide()
        }
        if (editPreviewFragment.isActive) {
            editPreviewFragment.hide(binding.editSectionContainer)
        }
    }

    private fun fetchSectionText() {
        if (sectionWikitext == null) {
            showProgressBar(true)
            disposables.add(ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfo(pageTitle.prefixedText, if (sectionID >= 0) sectionID else null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnTerminate { showProgressBar(false) }
                    .subscribe({ response ->
                        val firstPage = response.query?.firstPage()!!
                        val rev = firstPage.revisions[0]

                        pageTitle = PageTitle(firstPage.title, pageTitle.wikiSite).apply {
                            this.displayText = pageTitle.displayText
                        }
                        sectionWikitext = rev.contentMain
                        currentRevision = rev.revId

                        val editError = response.query?.firstPage()!!.getErrorForAction("edit")
                        if (editError.isEmpty()) {
                            editingAllowed = true
                        } else {
                            val error = editError[0]
                            FeedbackUtil.showError(this, MwException(error), pageTitle.wikiSite)
                        }
                        displaySectionText()
                        maybeShowEditSourceDialog()

                        editNotices.clear()
                        // Populate edit notices, but filter out anonymous edit warnings, since
                        // we show that type of warning ourselves when previewing.
                        editNotices.addAll(firstPage.getEditNotices()
                            .filterKeys { key -> (key.startsWith("editnotice") && !key.endsWith("-notext")) }
                            .values.filter { str -> StringUtil.fromHtml(str).trim().isNotEmpty() })
                        invalidateOptionsMenu()
                        if (Prefs.autoShowEditNotices) {
                            showEditNotices()
                        } else {
                            maybeShowEditNoticesTooltip()
                        }
                    }) {
                        showError(it)
                        L.e(it)
                    })
        } else {
            displaySectionText()
        }
    }

    private fun maybeShowEditNoticesTooltip() {
        if (!Prefs.autoShowEditNotices && !Prefs.isEditNoticesTooltipShown) {
            Prefs.isEditNoticesTooltipShown = true
            binding.root.postDelayed({
                val anchorView = findViewById<View>(R.id.menu_edit_notices)
                if (!isDestroyed && anchorView != null) {
                    FeedbackUtil.showTooltip(this, anchorView, getString(R.string.edit_notices_tooltip), aboveOrBelow = false, autoDismiss = false)
                }
            }, 100)
        }
    }

    private fun showEditNotices() {
        if (editNotices.isEmpty()) {
            return
        }
        EditNoticesDialog(pageTitle.wikiSite, editNotices, this).show()
    }

    private fun maybeShowEditSourceDialog() {
        if (!Prefs.showEditTalkPageSourcePrompt || (pageTitle.namespace() !== Namespace.TALK && pageTitle.namespace() !== Namespace.USER_TALK)) {
            return
        }
        val binding = DialogWithCheckboxBinding.inflate(layoutInflater)
        binding.dialogMessage.text = StringUtil.fromHtml(getString(R.string.talk_edit_disclaimer))
        binding.dialogMessage.movementMethod = movementMethod
        MaterialAlertDialogBuilder(this@EditSectionActivity)
            .setView(binding.root)
            .setPositiveButton(R.string.onboarding_got_it) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                Prefs.showEditTalkPageSourcePrompt = !binding.dialogCheckbox.isChecked
            }
            .show()
    }

    private fun displaySectionText() {
        binding.editSectionText.setText(sectionWikitext)
        showProgressBar(false)
        binding.editSectionContainer.isVisible = true
        scrollToHighlight(textToHighlight)
        binding.editSectionText.isEnabled = editingAllowed
        binding.editKeyboardOverlay.isVisible = editingAllowed
        hideAllSyntaxModals()
    }

    private fun scrollToHighlight(highlightText: String?) {
        if (highlightText == null || !TextUtils.isGraphic(highlightText)) {
            return
        }
        binding.editSectionText.highlightText(highlightText)
    }

    private fun hideAllSyntaxModals() {
        binding.editKeyboardOverlayHeadings.isVisible = false
        binding.editKeyboardOverlayFormattingContainer.isVisible = false
        binding.editKeyboardOverlay.onAfterOverlaysHidden()
    }

    fun showProgressBar(enable: Boolean) {
        binding.viewProgressBar.isVisible = enable
        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (binding.viewProgressBar.isVisible) {
            // If it is visible, it means we should wait until all the requests are done.
            return
        }
        showProgressBar(false)
        if (captchaHandler.isActive) {
            captchaHandler.cancelCaptcha()
            binding.editSectionCaptchaContainer.visibility = View.GONE
        }
        binding.viewEditSectionError.isVisible = false
        if (editSummaryFragment.handleBackPressed()) {
            supportActionBar?.title = getString(R.string.preview_edit_title)
            return
        }
        if (editPreviewFragment.isActive) {
            editPreviewFragment.hide(binding.editSectionContainer)
            supportActionBar?.title = null
            return
        }
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))
        DeviceUtil.hideSoftKeyboard(this)
        if (sectionTextModified) {
            val alert = MaterialAlertDialogBuilder(this)
            alert.setMessage(getString(R.string.edit_abandon_confirm))
            alert.setPositiveButton(getString(R.string.edit_abandon_confirm_yes)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            alert.setNegativeButton(getString(R.string.edit_abandon_confirm_no)) { dialog, _ -> dialog.dismiss() }
            alert.create().show()
        } else {
            finish()
        }
    }

    companion object {
        private const val EXTRA_KEY_SECTION_TEXT_MODIFIED = "sectionTextModified"
        private const val EXTRA_KEY_TEMPORARY_WIKITEXT_STORED = "hasTemporaryWikitextStored"
        private const val EXTRA_KEY_EDITING_ALLOWED = "editingAllowed"
        const val EXTRA_SECTION_ID = "org.wikipedia.edit_section.sectionid"
        const val EXTRA_SECTION_ANCHOR = "org.wikipedia.edit_section.anchor"
        const val EXTRA_HIGHLIGHT_TEXT = "org.wikipedia.edit_section.highlight"

        fun newIntent(context: Context, sectionId: Int, sectionAnchor: String?, title: PageTitle, highlightText: String? = null): Intent {
            return Intent(context, EditSectionActivity::class.java)
                .putExtra(EXTRA_SECTION_ID, sectionId)
                .putExtra(EXTRA_SECTION_ANCHOR, sectionAnchor)
                .putExtra(Constants.ARG_TITLE, title)
                .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
        }
    }

    override fun onToggleDimImages() { }

    override fun onToggleReadingFocusMode() { }

    override fun onCancelThemeChooser() { }

    override fun onEditingPrefsChanged() {
        binding.editSectionText.enqueueNoScrollingLayoutChange()
        updateTextSize()
        syntaxHighlighter.enabled = Prefs.editSyntaxHighlightEnabled
        binding.editSectionText.enableTypingSuggestions(Prefs.editTypingSuggestionsEnabled)
        binding.editSectionText.typeface = if (Prefs.editMonoSpaceFontEnabled) Typeface.MONOSPACE else Typeface.DEFAULT
        binding.editSectionText.showLineNumbers = Prefs.editLineNumbersEnabled
        binding.editSectionText.invalidate()
    }
}
