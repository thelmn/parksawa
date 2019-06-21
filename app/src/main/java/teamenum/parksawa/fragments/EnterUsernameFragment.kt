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
import kotlinx.android.synthetic.main.fragment_signup.*
import teamenum.parksawa.R

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