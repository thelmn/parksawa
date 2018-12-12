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
import android.view.MenuItem
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.ion.validator.Form
import com.ion.validator.Validate
import com.ion.validator.validator.NotEmptyValidator
import kotlinx.android.synthetic.main.activity_create_space.*
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateSpaceActivity : AppCompatActivity(),
        EasyPermissions.PermissionCallbacks {
    companion object {
        const val REQUEST_PLACE_PICKER = 100
        const val REQUEST_CAMERA = 200
        const val REQUEST_PERMISSION_STORAGE = 300
    }

    private var created = false
    private val placeHint = "Hint: Zoom the map to street level to provide drivers with an accurate location of your packing."

    private var requirePhoto = true
    private var selectedPhotoPath: String? = null
    private var selectedPlace: Place? = null

//    val nairobi = LatLng(-1.2833300, 36.8166700)
    private val bounds = LatLngBounds(LatLng(-1.4, 36.6), LatLng(-1.2, 37.0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_space)
        created = true


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

        nameValidate.addValidator(notEmptyValidator)
        if (perHourOption.isChecked) perHourValidate.addValidator(notEmptyValidator)
        if (perDayOption.isChecked) perDayValidate.addValidator(notEmptyValidator)
        if (overnightOption.isChecked) overnightValidate.addValidator(notEmptyValidator)

        val form = Form()
        form.addValidates(nameValidate, perHourValidate, perDayValidate, overnightValidate)

        if (selectedPlace == null) {
            showMessage("Please select the location of your parking.")
            return
        }

        if (form.validate()) {
            // do upload
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
        return File.createTempFile(
                "JPEG_${timestamp}_",
                ".jpg",
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
