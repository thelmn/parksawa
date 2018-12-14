package teamenum.parksawa.data

import android.net.Uri
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.io.File
import java.lang.Exception

fun addSpace(db: DatabaseReference,
             storage: StorageReference,
             parking: Parking,
             filePath: String?,
             fail: (Exception)->Unit,
             success: (String, Boolean)->Unit): Boolean {
    if (parking.owner.isEmpty()) return false

    // insert space at /spaces/$spaceId
    val ref = db.child("spaces")
    val spaceId = ref.push().key
    spaceId ?: return false
    db.child("spaces/$spaceId")
            .setValue(parking)
            .addOnSuccessListener { _ ->
                // add space to user's spaces index at /users/$uid/spaces/$spaceId = true
                db.child("users/${parking.owner}/spaces/$spaceId")
                        .setValue(true)
                        .addOnSuccessListener { _ ->
                            // upload image if any
                            if (!(filePath == null || filePath.isEmpty())) {
                                val file = File(filePath)
                                if (file.canRead()) {
                                    storage.child("spaces/${parking.owner}/${file.name}")
                                            .putFile(Uri.fromFile(file))
                                            .addOnSuccessListener { success(spaceId, true) }
                                            .addOnFailureListener { success(spaceId, false) }
                                } else success(spaceId, false)
                            } else success(spaceId, false)
                        }
                        .addOnFailureListener(fail)
            }
            .addOnFailureListener(fail)
    return true
}