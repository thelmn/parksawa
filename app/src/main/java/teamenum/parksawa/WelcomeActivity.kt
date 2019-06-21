package teamenum.parksawa

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.school.dialogs.MaterialDialog
import teamenum.parksawa.data.AuthState
import teamenum.parksawa.fragments.EnterCodeFragment
import teamenum.parksawa.fragments.EnterUsernameFragment
import teamenum.parksawa.fragments.HostSignInFragment
import teamenum.parksawa.fragments.WelcomeFragment
import java.util.concurrent.TimeUnit

class WelcomeActivity : AppCompatActivity(),
        WelcomeFragment.OnWelcomeFragmentListener,
        EnterCodeFragment.OnEnterCodeFragmentListener,
        EnterUsernameFragment.OnEnterUsernameFragmentListener,
        HostSignInFragment.OnHostSignInListener, FirebaseAuth.AuthStateListener {

    private val welcomeFragment by lazy { WelcomeFragment() }
    private val enterCodeFragment by lazy { EnterCodeFragment() }
    private val enterUsernameFragment by lazy { EnterUsernameFragment() }
    private val hostSignInFragment by lazy { HostSignInFragment() }

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
            AuthState.INIT, AuthState.SIGN_IN -> welcomeFragment
            AuthState.ENTER_CODE -> enterCodeFragment
            AuthState.ENTER_USERNAME ->  enterUsernameFragment
            AuthState.HOST_SIGN_IN -> hostSignInFragment
            else -> return
        }

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

    override fun onHostSignIn(email: String, password: String, isNewAccount: Boolean) {
        isLoading = true
        var signIn: (() -> Unit)? = null
        var signUp: (() -> Unit)? = null
        val success = fun(user: FirebaseUser?) {
            Prefs.authState = AuthState.SIGNED_IN
            isLoading = false
            if (user == null) {
                error.setContent("Could not get the user.")
                isError = true
            } else {
                if (!user.isEmailVerified) {
                    MaterialDialog.Builder(this)
                            .title("Email Verification.")
                            .content("An verification email will be sent to your address. Please confirm.")
                            .titleColorRes(R.color.primary_text)
                            .contentColorRes(R.color.secondary_text)
                            .positiveText("Ok, Got it")
                            .show()
                }
                val name = user.displayName
                if (name == null || name.isEmpty()) {
                    Prefs.authState = AuthState.ENTER_USERNAME
                    switchFragment()
                } else {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
        val fail = fun(exception: Exception?) {
            Log.d("WelcomeActivity", "onHostSignIn: new: $isNewAccount, $exception")
            when (exception) {
                is FirebaseAuthUserCollisionException ->
                    MaterialDialog.Builder(this)
                        .title("Email already exists!")
                        .content("Sign in with $email?")
                        .titleColorRes(R.color.primary_text)
                        .contentColorRes(R.color.secondary_text)
                        .negativeText("Cancel")
                        .positiveText("Yes, Sign in")
                        .onPositive { dialog, _ ->
                            dialog.dismiss()
                            signIn?.invoke()
                        }
                        .show()
                is FirebaseAuthInvalidUserException ->
                    MaterialDialog.Builder(this)
                        .title("Could not get the user!")
                        .content("Create a new account for $email?")
                        .titleColorRes(R.color.primary_text)
                        .contentColorRes(R.color.secondary_text)
                        .negativeText("Cancel")
                        .positiveText("Yes, Create")
                        .onPositive { dialog, _ ->
                            dialog.dismiss()
                            signUp?.invoke()
                        }
                        .show()
                else -> {
                    isLoading = false
                    error.setContent("Could not get the user.")
                    isError = true
                }
            }
        }
        signIn = fun() {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) success(task.result?.user)
                        else fail(task.exception)
                    }
        }
        signUp = fun() {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) success(task.result?.user)
                        else fail(task.exception)
                    }
        }
        if (isNewAccount) signUp() else signIn()
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

    private fun sendVerificationEmail() {
        val user = auth.currentUser
        if (user != null) {
            val email = user.email
            val name = user.displayName
            if (!(email == null || email.isEmpty() ||
                            name == null || name.isEmpty() ||
                            user.isEmailVerified ||
                            Prefs.isVerificationEmailSent
                            )) {
                user.sendEmailVerification().addOnSuccessListener { Prefs.isVerificationEmailSent = true }
            }
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        sendVerificationEmail()
    }

    override fun onResume() {
        super.onResume()
        auth.addAuthStateListener(this)
    }

    override fun onPause() {
        super.onPause()
        auth.removeAuthStateListener(this)
        if (isLoading) isLoading = false
        if (isError) isError = false
    }
}

