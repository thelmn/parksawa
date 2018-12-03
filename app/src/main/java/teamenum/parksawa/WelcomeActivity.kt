package teamenum.parksawa

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.ion.validator.Form
import com.ion.validator.Validate
import com.ion.validator.validator.NotEmptyValidator
import com.ion.validator.validator.PhoneValidator
import com.school.dialogs.MaterialDialog
import kotlinx.android.synthetic.main.fragment_enter_code.*
import kotlinx.android.synthetic.main.fragment_signup.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import teamenum.parksawa.data.AuthState
import teamenum.parksawa.widgets.TextDrawable
import java.util.concurrent.TimeUnit

class WelcomeActivity : AppCompatActivity(),
        WelcomeFragment.OnWelcomeFragmentListener,
        EnterCodeFragment.OnEnterCodeFragmentListener,
        EnterUsernameFragment.OnEnterUsernameFragmentListener {

    private val welcomeFragment = WelcomeFragment()
    private val enterCodeFragment = EnterCodeFragment()
    private val enterUsernameFragment = EnterUsernameFragment()

    private lateinit var auth: FirebaseAuth

    companion object {
        const val FLAG_VERIFYING_PHONE = "teamenum.parksawa.WelcomeActivity.FLAG_VERIFYING_PHONE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        auth = FirebaseAuth.getInstance()

        loader =  MaterialDialog.Builder(this)
                .title("Please wait...")
                .titleColorRes(R.color.primary_text)
                .contentColorRes(R.color.secondary_text)
                .canceledOnTouchOutside(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .build()
        error =  MaterialDialog.Builder(this)
                .title("An error occurred!")
                .titleColorRes(R.color.primary_text)
                .contentColorRes(R.color.secondary_text)
                .negativeText("Cancel")
                .dismissListener { if(isError) isError = false }
                .build()

        val userPhone = Prefs.userPhone
        if (Prefs.authState == AuthState.CONFIRMING_PHONE) {
            if (userPhone != null) verifyPhone(userPhone)
            else Prefs.authState = AuthState.INIT
        }

        switchFragment()
    }

    private fun switchFragment() {
        val fragment = when(Prefs.authState) {
            AuthState.INIT ->  welcomeFragment
            AuthState.ENTER_CODE ->  enterCodeFragment
            AuthState.ENTER_USERNAME ->  enterUsernameFragment
            AuthState.SIGN_IN -> {
                //do other stuff
                welcomeFragment
            }
            else -> null
        }
        fragment ?: return
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_enter, R.anim.slide_exit)
                .replace(R.id.formFrame, fragment)
                .commit()
    }

    override fun onPhoneEnter(phone: String) {
        MaterialDialog.Builder(this)
                .title("Phone number confirmation.")
                .content("A confirmation message will be sent to this number. Normal charges may apply.")
                .titleColorRes(R.color.primary_text)
                .contentColorRes(R.color.secondary_text)
                .positiveText("Ok, Confirm")
                .negativeText("Cancel")
                .onPositive { _, _ -> verifyPhone(phone)}
                .show()
    }

    override fun onCodeEnter(code: String) {
        isLoading = true
        val verificationId = Prefs.verificationId
        verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    override fun onUsernameEnter(username: String) {
        isLoading = true
        val user = auth.currentUser
        val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(username.capitalize())
                .build()
        user?.updateProfile(profileUpdate)
                ?.addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        Prefs.authState = AuthState.DONE
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        error.setContent("Unable to update name")
                        isError = true
                    }
                }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task -> run {
                    if (task.isSuccessful) {
                        Prefs.authState = AuthState.SIGNED_IN
                        val user = task.result?.user
                        if (user == null) {
                            isVerifyingPhone = false
                            Prefs.authState = AuthState.INIT
                            error.setContent("Could not get the user.")
                            isError = true
                            switchFragment()
                        } else {
                            isVerifyingPhone = false
                            val name = user.displayName
                            if (name == null || name.isEmpty()) {
                                Prefs.authState = AuthState.ENTER_USERNAME
                                switchFragment()
                            } else {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    } else {
                        isVerifyingPhone = false
                        Prefs.authState = AuthState.INIT
                        error.setContent("Could not sign in.")
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            Prefs.authState = AuthState.ENTER_CODE
                            error.setContent("Could not sign in. Invalid code entered")
                        }
                        isError = true
                        switchFragment()
                    }
                } }
    }

    private var resendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isVerifyingPhone = false
        set(value) {
            field = value
            isLoading = value
        }
    private var isLoading = false
        set(value) {
            field = value
            if (value) loader.show() else loader.dismiss()
        }
    private lateinit var loader: MaterialDialog

    private var isError = false
        set(value) {
            field = value
            if (value) error.show() else error.dismiss()
        }
    private lateinit var error: MaterialDialog

    private fun verifyPhone(phone: String) {
        isVerifyingPhone = true
        Prefs.authState = AuthState.CONFIRMING_PHONE
        Prefs.userPhone = phone
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phone,
                60,
                TimeUnit.SECONDS,
                this,
                phoneVerificationCallbacks
        )
    }

    private val phoneVerificationCallbacks = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Prefs.authState = AuthState.SIGN_IN
            signInWithCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException?) {
            isVerifyingPhone = false
            Prefs.authState = AuthState.INIT
            if (e is FirebaseAuthInvalidCredentialsException) {
                // invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // SMS quota reached
            }
        }

        override fun onCodeSent(verificationId: String?, resendingToken: PhoneAuthProvider.ForceResendingToken?) {
            isVerifyingPhone = false
            Prefs.authState = AuthState.ENTER_CODE
            Prefs.verificationId = verificationId
            this@WelcomeActivity.resendingToken = resendingToken
            switchFragment()
            super.onCodeSent(verificationId, resendingToken)
        }

        override fun onCodeAutoRetrievalTimeOut(p0: String?) {
            super.onCodeAutoRetrievalTimeOut(p0)
            enterCodeFragment.setResendEnabled(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isLoading) isLoading = false
        if (isError) isError = false
    }
}

class WelcomeFragment : Fragment() {

    private var listener: OnWelcomeFragmentListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val countryCodeKe = getString(R.string.country_code_ke)
        phoneEdit.setCompoundDrawablesWithIntrinsicBounds(TextDrawable(countryCodeKe, phoneEdit.textSize), null, null, null)
        phoneEdit.compoundDrawablePadding = countryCodeKe.length * 22

        buttonNext.setOnClickListener {
            val nonEmptyValidator = NotEmptyValidator(context, R.string.please_enter_phone)
            val phoneValidator = PhoneValidator(context, R.string.unacceptable_phone)
            val validate = Validate(phoneEdit)
            validate.addValidator(nonEmptyValidator, phoneValidator)
            val form = Form()
            form.addValidates(validate)
            if (form.validate()) { listener?.onPhoneEnter(countryCodeKe + phoneEdit.text.toString()) }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnWelcomeFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnWelcomeFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnWelcomeFragmentListener {
        fun onPhoneEnter(phone: String)
    }
}

class EnterUsernameFragment : Fragment() {

    private var listener: OnEnterUsernameFragmentListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        buttonSignUp.setOnClickListener {
            val notEmptyValidator = NotEmptyValidator(context, R.string.enter_username)
            val validate = Validate(usernameEdit)
            validate.addValidator(notEmptyValidator)
            val form = Form()
            form.addValidates(validate)
            if (form.validate()) listener?.onUsernameEnter(usernameEdit.text.toString())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnEnterUsernameFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnEnterUsernameFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnEnterUsernameFragmentListener {
        fun onUsernameEnter(username: String)
    }
}

class EnterCodeFragment : Fragment() {
    private var isCreated = false
    private var listener: OnEnterCodeFragmentListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_enter_code, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        isCreated = true

        buttonVerify.setOnClickListener {
            val notEmptyValidator = NotEmptyValidator(context, R.string.enter_code)
            val validate = Validate(codeEdit)
            validate.addValidator(notEmptyValidator)
            val form = Form()
            form.addValidates(validate)
            if (form.validate()) listener?.onCodeEnter(codeEdit.text.toString())
        }
    }

    fun setResendEnabled(enable: Boolean) {
        if (isCreated) {
            buttonResend.isEnabled = enable
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnEnterCodeFragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnEnterCodeFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnEnterCodeFragmentListener {
        fun onCodeEnter(code: String)
    }
}


