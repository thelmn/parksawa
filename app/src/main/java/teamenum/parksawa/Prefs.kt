package teamenum.parksawa

import android.content.Context
import android.content.SharedPreferences
import teamenum.parksawa.data.AuthState

object Prefs {
    private const val APP_SETTINGS = "APP_SETTINGS"
    private const val AUTH_TOKEN = "AUTH_TOKEN"
    private const val AUTH_STATE = "AUTH_STATE"
    private const val JOIN_PHONE_EMAIL_PREFERENCE = "JOIN_PHONE_EMAIL_PREFERENCE"
    private const val USER_CONFIRMED = "USER_CONFIRMED"
    private const val USER_VERIFICATION_ID = "USER_VERIFICATION_ID"
    private const val USER_ID = "USER_ID"
    private const val USER_NAME = "USER_NAME"
    private const val USER_PHONE = "USER_PHONE"
    
    private lateinit var prefs: SharedPreferences

    enum class Type {STRING, BOOLEAN, INT, FLOAT, LONG}
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE)
    }
    
    var authToken: String?
        get() = prefs.getString(AUTH_TOKEN, null)
        set(value) = setValue(AUTH_TOKEN, value)

    var userPhone: String?
        get() = prefs.getString(USER_PHONE, null)
        set(value) = setValue(USER_PHONE, value)

    var authState: Int
        get() = prefs.getInt(AUTH_STATE, AuthState.INIT)
        set(value) = setValue(AUTH_STATE, value)

    var verificationId: String?
        get() = prefs.getString(USER_VERIFICATION_ID, null)
        set(value) = setValue(USER_VERIFICATION_ID, value)

    fun setValue(flag: String, value: Any?) {
        val editor = prefs.edit()
        when(value) {
            is Boolean -> editor.putBoolean(flag, value)
            is String? -> editor.putString(flag, value)
            is Int -> editor.putInt(flag, value)
            is Float -> editor.putFloat(flag, value)
            is Long -> editor.putLong(flag, value)
        }
        editor.apply()
    }
}