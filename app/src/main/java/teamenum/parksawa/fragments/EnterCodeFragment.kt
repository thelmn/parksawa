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
import kotlinx.android.synthetic.main.fragment_enter_code.*
import teamenum.parksawa.R

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