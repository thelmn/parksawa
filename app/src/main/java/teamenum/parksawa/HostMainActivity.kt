package teamenum.parksawa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.school.dialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_host_main.*
import teamenum.parksawa.data.AuthState

class HostMainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    companion object {
        private const val REQUEST_HOST_SIGN_IN = 824
    }

    private lateinit var error: MaterialDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_main)
        isCreated = true

        error = MaterialDialog.Builder(this)
                .title("An error occurred!")
                .titleColorRes(R.color.primary_text)
                .contentColorRes(R.color.secondary_text)
                .negativeText("Cancel")
                .build()

        if (auth.currentUser == null) goToSignIn()
        else testIsHost()
    }

    private fun testIsHost(make: Boolean = true) {
        isLoading = true
        val showError = fun () {
            error.setContent("Could not connect.")
            error.setOnDismissListener { finish() }
            error.show()
        }
        val user = auth.currentUser
        user?.getIdToken(true)
                ?.addOnCompleteListener { task ->
                    val token = task.result
                    if (task.isSuccessful && token != null) {
                        Log.d("HostMainActivity", "onCreate: token: ${token.claims["parkingHost"]}")
                        val isHost = token.claims[getString(R.string.host_claim)] as? Boolean ?: false
                        when {
                            isHost -> initUI()
                            make -> makeHost()
                            else -> showError()
                        }
                    } else showError()
                }
    }

    private fun initUI(){
        isLoading = false
    }

    private fun makeHost() {
        val user = auth.currentUser
        val email = user?.email
        if (email != null && !email.isEmpty()) {
            functions.getHttpsCallable("setAsHost")
                    .call()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            user.getIdToken(true)
                            val result = task.result?.data
                            Log.d("HostMainActivity", "makeHost: $result")
                        } else {
                            Log.d("HostMainActivity", "makeHost: ${task.exception}")
                            goToSignIn()
                        }
                    }
        } else goToSignIn()
    }

    private fun goToSignIn() {
        isLoading = true
        Prefs.authState = AuthState.HOST_SIGN_IN
        startActivityForResult(Intent(this, WelcomeActivity::class.java), REQUEST_HOST_SIGN_IN)
    }

    private fun onSignIn() {
        val user = auth.currentUser
        if (user != null) {
            initUI()
            testIsHost(false)
        }
    }

    private var isCreated = false
    private var isLoading = false
        set(value) {
            field = value
            if (isCreated) {
                cover.visibility = if (value) View.VISIBLE else View.GONE
                progressBar.visibility = if (value) View.VISIBLE else View.GONE
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_HOST_SIGN_IN -> {
                if (resultCode == Activity.RESULT_OK) {
                    onSignIn()
                } else {
                    error.setContent("Could not sign in.")
                    error.show()
                }
            }
        }
    }
}
