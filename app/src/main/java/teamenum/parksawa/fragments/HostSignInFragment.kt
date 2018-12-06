package teamenum.parksawa.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ion.validator.Form
import com.ion.validator.Validate
import com.ion.validator.validator.EmailValidator
import com.ion.validator.validator.NotEmptyValidator
import com.ion.validator.validator.RegExpValidator
import kotlinx.android.synthetic.main.fragment_host_sign_in.*
import teamenum.parksawa.R
import java.util.regex.Pattern

class HostSignInFragment : Fragment() {
    private var listener: OnHostSignInListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_host_sign_in, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        signInButton.setOnClickListener { onRegisterClick() }
        signUpButton.setOnClickListener { onRegisterClick(true) }
    }

    private fun onRegisterClick(isNewAccount: Boolean = false) {
        val notEmptyValidator = NotEmptyValidator(activity)
        val passwordLengthValidator = RegExpValidator(activity, R.string.password_too_short)
        passwordLengthValidator.setPattern(Pattern.compile(getString(R.string.password_length_regex)))
        val emailValidator = EmailValidator(activity, R.string.not_valid_email)

        val emailValidate = Validate(emailEdit)
        emailValidate.addValidator(notEmptyValidator, emailValidator)
        val passwordValidate = Validate(passwordEdit)
        passwordValidate.addValidator(notEmptyValidator, passwordLengthValidator)

        val form = Form()
        form.addValidates(emailValidate, passwordValidate)
        if (form.validate()) listener?.onHostSignIn(emailEdit.text.toString(), passwordEdit.text.toString(), isNewAccount)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnHostSignInListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnHostSignInListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnHostSignInListener {
        fun onHostSignIn(email: String, password: String, isNewAccount: Boolean)
    }
}