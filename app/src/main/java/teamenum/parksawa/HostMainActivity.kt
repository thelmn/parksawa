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
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import com.school.dialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_host_main.*
import teamenum.parksawa.adapters.HostMainAdapter
import teamenum.parksawa.data.AuthState
import teamenum.parksawa.data.Parking
import teamenum.parksawa.data.Title

class HostMainActivity : AppCompatActivity(), HostMainAdapter.OnMainHostListener {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    companion object {
        private const val REQUEST_HOST_SIGN_IN = 824
        private const val REQUEST_CREATE_SPACE = 567
        private const val REQUEST_CREATE_ATTENDANT = 278
    }

    private lateinit var adapter: HostMainAdapter
    private lateinit var error: MaterialDialog

    private val sampleItems = arrayListOf(
            HostMainAdapter.AddButton(HostMainAdapter.TAG_ADD_SPACE),
            HostMainAdapter.AddButton(HostMainAdapter.TAG_ADD_ATTENDANT),
            Title("Spaces"),
            Title("Attendants")
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

        adapter = HostMainAdapter(sampleItems, this, this)
        recyclerHostMain.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerHostMain.adapter = adapter

        if (auth.currentUser == null) goToSignIn()
        else testIsHost()
    }

    private fun syncSpaces() {
        val user = auth.currentUser
        user ?: return
        Snackbar.make(content, "Loading...", Snackbar.LENGTH_LONG).show()
        db.reference.child("users/${user.uid}/spaces")
                .addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HostMainActivity", "syncSpaces: $error")
                        Snackbar.make(content, "Could not fetch your Parking Spaces.", Snackbar.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(snapshotKeys: DataSnapshot) {
                        if (snapshotKeys.exists()) {
                            val mapType = object : GenericTypeIndicator<Map<String, Boolean>>(){}
                            snapshotKeys.getValue(mapType)?.also { keys ->
                                for ((spaceId, _) in keys) {
                                    db.reference.child("spaces/$spaceId")
                                            .addValueEventListener(object : ValueEventListener {
                                                override fun onCancelled(error: DatabaseError) {
                                                    Log.e("HostMainActivity", "syncSpaces: $spaceId: $error")
                                                }

                                                override fun onDataChange(snapshotSpaces: DataSnapshot) {
                                                    snapshotSpaces.getValue(Parking::class.java)?.also { space ->
                                                        space.id = spaceId
                                                        adapter.insert(3, space)
                                                    }
                                                }
                                            })
                                }
                            }
                        }
                    }
                })
    }

    private fun syncSpace(spaceId: String) {
        db.getReference("spaces/$spaceId")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HostMainActivity", "syncSpace: $spaceId: $error")
                        Snackbar.make(content, "Unable to update parking spaces", Snackbar.LENGTH_LONG).show()
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.d("HostMainActivity", "syncSpace: snap: ${snapshot.value as HashMap<*, *>}")
                            snapshot.getValue(Parking::class.java)?.also { parking ->
                                parking.id = spaceId
                                adapter.insert(3, parking)
                            }
                        }
                    }
                })
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
        syncSpaces()
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
        when (tag) {
            HostMainAdapter.TAG_ADD_SPACE ->
                Intent(this, CreateSpaceActivity::class.java)
                        .also { intent -> startActivityForResult(intent, REQUEST_CREATE_SPACE) }
            HostMainAdapter.TAG_ADD_ATTENDANT ->
                Intent(this, CreateAttendantActivity::class.java)
                        .also { intent -> startActivityForResult(intent, REQUEST_CREATE_ATTENDANT) }
        }
    }

    override fun onSpaceClick(id: String) {
        Snackbar.make(content, "Space $id", Snackbar.LENGTH_SHORT).show()
    }

    override fun onAttendantClick(id: String) {
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
            REQUEST_CREATE_SPACE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.getStringExtra(CreateSpaceActivity.RESULT_SPACE_ID)
                            ?.also { spaceId -> syncSpace(spaceId) }
                }
            }
            REQUEST_CREATE_ATTENDANT -> {}
        }
    }
}
