package com.shalzz.attendance.ui.login

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManager.KEY_ERROR_MESSAGE
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.bugsnag.android.Bugsnag
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.shalzz.attendance.R
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.sync.AccountAuthenticatorActivity
import com.shalzz.attendance.sync.MyAccountManager
import com.shalzz.attendance.utils.Miscellaneous
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class AuthenticatorActivity: AccountAuthenticatorActivity(), LoginMvpView {

    @BindView(R.id.etUserId)
    lateinit var textInputUserId: TextInputLayout

    @BindView(R.id.toolbar)
    lateinit var mToolbar: Toolbar

    @Inject
    @field:Named("app")
    lateinit var mTracker: FirebaseAnalytics

    @Inject
    lateinit var mLoginPresenter: LoginPresenter

    @Inject
    lateinit var mPreferencesHelper: PreferencesHelper

    var etUserId: EditText? = null

    var progressDialog: MaterialDialog? = null

    private lateinit var mAccountManager: AccountManager
    private var mAuthTokenType: String? = null

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        super.onCreate(savedInstanceState)
        activityComponent().inject(this)
        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)
        Bugsnag.setContext("LoginActivity")
        mLoginPresenter.attachView(this)

        // set toolbar as actionbar
        setSupportActionBar(mToolbar)

        // Static background with ScrollView
        window.setBackgroundDrawableResource(R.drawable.background)

        mAccountManager = AccountManager.get(baseContext)
        mAuthTokenType = intent.getStringExtra(ARG_AUTH_TYPE)
        if (mAuthTokenType == null)
            mAuthTokenType = MyAccountManager.AUTHTOKEN_TYPE_FULL_ACCESS

        etUserId = textInputUserId.editText

        // Shows the CaptchaDialog when user presses 'Done' on keyboard.
        if (etUserId != null) {
            etUserId!!.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doLogin()
                    return@setOnEditorActionListener true
                }
                false
            }
        }

    }

    /**
     * Checks if the form is valid
     * @return true or false
     */
    private val isValid: Boolean
        get() {
            val sapid = etUserId!!.text.toString()

            if (sapid.isEmpty() || sapid.length != 10) {
                textInputUserId.requestFocus()
                textInputUserId.error = getString(R.string.form_userid_error)
                Miscellaneous.showKeyboard(this, etUserId)
                return false
            }
            return true
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == Activity.RESULT_OK) {
            finishLogin(data!!)
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun finishLogin(intent: Intent) {
        Timber.d( "> finishLogin")

        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val accountPassword = intent.getStringExtra(PARAM_USER_PASS)
        val account = Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Timber.d("> finishLogin > addAccountExplicitly")
            val authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)
            val authtokenType = mAuthTokenType

            // Creating the account on the device and setting the auth regId we got
            // (Not setting the auth regId will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, null)
            mAccountManager.setAuthToken(account, authtokenType, authtoken)
        } else {
            Timber.d("> finishLogin > setPassword")
            mAccountManager.setPassword(account, accountPassword)
        }

        setAccountAuthenticatorResult(intent.extras)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    @OnClick(R.id.bLogin)
    fun doLogin() {
        if (!isValid)
            return

        val userId = etUserId!!.text.toString()
        val bundle = Bundle()
        bundle.putString(Miscellaneous.Analytics.Param.USER_ID, userId)
        mTracker.logEvent(Miscellaneous.Analytics.Event.LOGIN_INITIATED, bundle)

        Miscellaneous.closeKeyboard(this, etUserId)
        mLoginPresenter.login(userId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoginPresenter.detachView()
    }

    /***** MVP View methods implementation  */

    override fun showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = MaterialDialog.Builder(this)
                    .content("Logging in...")
                    .cancelable(false)
                    .autoDismiss(false)
                    .progress(true, 0)
                    .build()
        }
        progressDialog!!.show()
    }

    override fun dismissProgressDialog() {
        if (progressDialog != null)
            progressDialog!!.dismiss()
    }

    override fun successfulLogin(authtoken: String) {
        val data = Bundle()
//        data.putString(AccountManager.KEY_ACCOUNT_NAME, userName)
//        data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
//        data.putString(AccountManager.KEY_AUTHTOKEN, authtoken)
//        data.putString(PARAM_USER_PASS, userPass)
        val res = Intent()
        res.putExtras(data)
        if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
            Toast.makeText(baseContext, intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show()
        } else {
            finishLogin(intent)
        }
    }

    override fun showError(message: String?) {
        dismissProgressDialog()

        Miscellaneous.showSnackBar(mToolbar,
                if (message == null) message
                else getString(R.string.unexpected_error)
        )
    }

    companion object {
        var ARG_ACCOUNT_TYPE: String = "ACCOUNT_TYPE"
        var ARG_AUTH_TYPE: String = "AUTH_TYPE"
        var ARG_IS_ADDING_NEW_ACCOUNT: String = "IS_ADDING_ACCOUNT"
        val PARAM_USER_PASS = "USER_PASS"
        private val REQ_SIGNUP = 1
    }
}