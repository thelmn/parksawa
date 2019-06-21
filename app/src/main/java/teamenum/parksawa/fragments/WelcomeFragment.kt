package teamenum.parksawa.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ion.validator.Form
import com.ion.validator.Validate
import com.ion.validator.validator.NotEmptyValidator
import com.ion.validator.validator.PhoneValidator
import kotlinx.android.synthetic.main.fragment_welcome.*
import teamenum.parksawa.R
import teamenum.parksawa.widgets.TextDrawable

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