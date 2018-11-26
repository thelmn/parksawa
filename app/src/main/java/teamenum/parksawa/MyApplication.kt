package teamenum.parksawa

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        inst = this
    }

    companion object {
        lateinit var inst: MyApplication
        private set
    }
}