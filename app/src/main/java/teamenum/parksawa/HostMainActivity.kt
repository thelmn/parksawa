package teamenum.parksawa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.school.dialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_host_main.*
import teamenum.parksawa.adapters.HostMainAdapter
import teamenum.parksawa.data.Attendant
import teamenum.parksawa.data.AuthState
import teamenum.parksawa.data.Parking
import teamenum.parksawa.data.Title

class HostMainActivity : AppCompatActivity(), HostMainAdapter.OnMainHostListener {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    companion object {
        private const val REQUEST_HOST_SIGN_IN = 824
    }

    private lateinit var error: MaterialDialog

    private val sampleItems = arrayListOf(
            HostMainAdapter.AddButton(HostMainAdapter.TAG_ADD_SPACE),
            HostMainAdapter.AddButton(HostMainAdapter.TAG_ADD_ATTENDANT),
            Title("Spaces"),
            Parking(123, "Space 1"),
            Parking(124, "Space 2"),
            Title("Attendants"),
            Attendant(123, "User1", "example@mail.com"),
            Attendant(124, "User2", "example@mail.com")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_main)
        isCreated = true
        setSupportActionBar(toolbar)

        error = MaterialDialog.Builder(this)
                .title("An error occurred!")
                .titleColorRes(R.color.primary_text)
                .contentColorRes(R.color.secondary_text)
                .negativeText("Cancel")
                .build()

        val adapter = HostMainAdapter(sampleItems, this, this)
        recyclerHostMain.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerHostMain.adapter = adapter

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
                ?.addOnSuccessListener { token ->
                    Log.d("HostMainActivity", "onCreate: token: ${token.claims["parkingHost"]}")
                    val isHost = token.claims[getString(R.string.host_claim)] as? Boolean ?: false
                    when {
                        isHost -> initUI()
                        make -> makeHost()
                        else -> showError()
                    }
                }
                ?.addOnFailureListener { e ->
                   if (e is FirebaseNetworkException) showMessage("Network Error! Cant Connect to server")
                    else showError()
                }
    }

    private fun showMessage(message: String) {
        Snackbar.make(content, message, Snackbar.LENGTH_SHORT).show()
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

    override fun onButtonClick(tag: String) {
        val intent = when (tag) {
            HostMainAdapter.TAG_ADD_SPACE -> Intent(this, CreateSpaceActivity::class.java)
            HostMainAdapter.TAG_ADD_ATTENDANT -> Intent(this, CreateAttendantActivity::class.java)
            else -> null
        }
        intent?.also {
            startActivity(it)
        }
    }

    override fun onSpaceClick(id: Long) {
        Snackbar.make(content, "Space $id", Snackbar.LENGTH_SHORT).show()
    }

    override fun onAttendantClick(id: Long) {
        Snackbar.make(content, "Attendant $id", Snackbar.LENGTH_SHORT).show()
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
