package teamenum.parksawa

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.ion.validator.Form
import com.ion.validator.Validate
import com.ion.validator.validator.NotEmptyValidator
import com.school.dialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_create_space.*
import kotlinx.android.synthetic.main.view_parking.view.*
import pub.devrel.easypermissions.EasyPermissions
import teamenum.parksawa.data.Parking
import teamenum.parksawa.data.addSpace
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class CreateSpaceActivity : AppCompatActivity(),
        EasyPermissions.PermissionCallbacks {
    companion object {
        const val REQUEST_PLACE_PICKER = 100
        const val REQUEST_CAMERA = 200
        const val REQUEST_PERMISSION_STORAGE = 300

        const val RESULT_SPACE_ID = "teamenum.parksawa.CreateSpaceActivity.RESULT_SPACE_ID"
    }

    private var created = false
    private val placeHint = "Hint: Zoom the map to street level to provide drivers with an accurate location of your packing."

    private var requirePhoto = true
    private var selectedPhotoPath: String? = null
    private var selectedPlace: Place? = null

    private val db = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var user: FirebaseUser

    private lateinit var error: MaterialDialog
    private lateinit var loading: MaterialDialog

    //    val nairobi = LatLng(-1.2833300, 36.8166700)
    private val bounds = LatLngBounds(LatLng(-1.4, 36.6), LatLng(-1.2, 37.0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_space)
        created = true

        val u = FirebaseAuth.getInstance().currentUser
        if (u == null) {
            finish()
            return
        }
        user = u
        error = MaterialDialog.Builder(this)
                .title("An error occurred!")
                .titleColorRes(R.color.primary_text)
                .contentColorRes(R.color.secondary_text)
                .negativeText("Cancel")
                .build()
        loading = MaterialDialog.Builder(this)
                .title("Please wait")
                .titleColorRes(R.color.primary_text)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .canceledOnTouchOutside(false)
                .build()

        pickLocation.setOnClickListener { openPlacePicker() }
        perHourOption.setOnCheckedChangeListener { _, checked -> perHourPrice.isEnabled = checked }
        perDayOption.setOnCheckedChangeListener { _, checked -> perDayPrice.isEnabled = checked }
        overnightOption.setOnCheckedChangeListener { _, checked -> overnightPrice.isEnabled = checked }
        addPhoto.setOnClickListener { openCamera() }
        register.setOnClickListener { registerPlace() }
    }

    private fun registerPlace() {
        val notEmptyValidator = NotEmptyValidator(this)

        val nameValidate = Validate(spaceNameEdit)
        val perHourValidate = Validate(perHourPrice)
        val perDayValidate = Validate(perDayPrice)
        val overnightValidate = Validate(overnightPrice)
        val slotsValidate = Validate(slots)

        nameValidate.addValidator(notEmptyValidator)
        if (perHourOption.isChecked) perHourValidate.addValidator(notEmptyValidator)
        if (perDayOption.isChecked) perDayValidate.addValidator(notEmptyValidator)
        if (overnightOption.isChecked) overnightValidate.addValidator(notEmptyValidator)
        slotsValidate.addValidator(notEmptyValidator)

        val form = Form()
        form.addValidates(nameValidate, perHourValidate, perDayValidate, overnightValidate, slotsValidate)

        if (selectedPlace == null) {
            showMessage("Please select the location of your parking.")
            return
        }

        if (form.validate()) {
            // do upload
            loading.show()
            val pricing = arrayListOf(
                    if (perHourOption.isChecked) perHourPrice.text.toString().toInt() else -1,
                    if (perDayOption.isChecked) perDayPrice.text.toString().toInt() else -1,
                    if (overnightOption.isChecked) overnightPrice.text.toString().toInt() else -1
            )

            val selectedPhotoName = try { File(selectedPhotoPath).name } catch (e: Exception) {""}
            val parking = Parking(
                    name = spaceNameEdit.text.toString(),
                    owner = user.uid,
                    latitude = selectedPlace!!.latLng.latitude,
                    longitude = selectedPlace!!.latLng.longitude,
                    slotCount = slots.text.toString().toInt(),
                    reservable = reservableHint.isChecked,
                    pricing = pricing,
                    images = arrayListOf(selectedPhotoName ?: "")
            )
            val continued = addSpace(db.reference,
                    storage.reference,
                    parking,
                    selectedPhotoPath,
                    { e ->
                        loading.dismiss()
                        Log.e("CreateSpaceActivity", "registerPlace: ", e)
                        error.setContent("Could not create parking. Error: ${e.localizedMessage}")
                        error.show()
                    },
                    { spaceId, imageUploaded ->
                        loading.dismiss()
                        val imageStatus = if (imageUploaded) "1" else "NO"
                        MaterialDialog.Builder(this)
                                .title("Success")
                                .content("Parking space created. $imageStatus image uploaded.")
                                .titleColorRes(R.color.primary_text)
                                .contentColorRes(R.color.secondary_text)
                                .negativeText("Cancel")
                                .dismissListener {
                                    intent.putExtra(RESULT_SPACE_ID, spaceId)
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                                .show()
                    })
            if (!continued) {
                loading.dismiss()
                showMessage("Something went wrong. Try again")
            }
        }
    }

    private fun setSelectedPlace(place: Place) {
        selectedPlace = place
        pickLocation.text = if (selectedPlace == null) "Pick on Map" else "Change Location"
        locationHint.text = if (selectedPlace == null) placeHint else "Near ${place.address}"
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val defaultDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        var storageDir = File("$defaultDir/ParkSawa")
        if (!storageDir.exists()) {
            if (!storageDir.mkdir()) storageDir = defaultDir
        }
        val suffix = Random().nextInt(1000)
        return File.createTempFile(
                "JPEG_${timestamp}_",
                "$suffix.jpg",
                storageDir
        ).apply {
            selectedPhotoPath = absolutePath
        }
    }

    private fun openCamera() {
        val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!EasyPermissions.hasPermissions(this, *perms)) {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.storage_perm_rationale),
                    REQUEST_PERMISSION_STORAGE,
                    *perms)
            return
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .also { intent -> intent.resolveActivity(packageManager)
                            ?.also {
                                val photoFile = try {
                                    createImageFile()
                                } catch (e: IOException) {
                                    null
                                }
                                photoFile?.also {file ->
                                    val photoURI = FileProvider.getUriForFile(
                                            this,
                                            "teamenum.parksawa.fileprovider",
                                            file
                                    )
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                    startActivityForResult(intent, REQUEST_CAMERA)
                                    return
                                }
                            }
                    }
        }
        showMessage("Unable to start camera. Use gallery")
    }

    private fun openPlacePicker() {
        val intent = PlacePicker.IntentBuilder()
                .setLatLngBounds(bounds)
                .build(this)
        startActivityForResult(intent, REQUEST_PLACE_PICKER)
    }

    private fun showMessage(message: String) {
        Snackbar.make(content, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun addToGallery() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { intent ->
            selectedPhotoPath?.also { path ->
                val file = File(path)
                intent.data = Uri.fromFile(file)
                sendBroadcast(intent)
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        requirePhoto = false
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (perms.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            openCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_PLACE_PICKER -> {
                if (resultCode == Activity.RESULT_OK) {
                    val place = PlacePicker.getPlace(this, data)
                    setSelectedPlace(place)
                }
            }
            REQUEST_CAMERA -> {
                if (resultCode == Activity.RESULT_OK) {
                    selectedPhotoPath?.also { path -> largePic(photoView, path) }
                    addToGallery()
                    return
                }
                showMessage("Could not get the image.")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPause() {
        created = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        created = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
