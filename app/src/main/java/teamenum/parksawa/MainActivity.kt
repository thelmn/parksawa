package teamenum.parksawa

import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.mapbox.mapboxsdk.Mapbox
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import teamenum.parksawa.adapters.ParkingLocationsAdapter
import teamenum.parksawa.adapters.TagsAdapter
import teamenum.parksawa.data.Parking

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    TagsAdapter.OnTagsListener, ParkingLocationsAdapter.OnLocationsListener {

    private var fullView = false
    private lateinit var adapter: ParkingLocationsAdapter

    private val parkingSamples = arrayListOf(
            Parking(1, "Place 1"),
            Parking(2, "Place 2"),
            Parking(3, "Place 3"),
            Parking(4, "Place 4"),
            Parking(5, "Place 5"),
            Parking(6, "Place 6"),
            Parking(7, "Place 7"),
            Parking(8, "Place 8"),
            Parking(9, "Place 9"),
            Parking(10, "Place 10")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this,
                "pk.eyJ1IjoidGhlbG1uIiwiYSI6ImNqb3ptbjJhODJ2bHozcHJueThjOWFpdzAifQ.irTW--pySvKW_DCbGFYHgw")
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)

        nav_view.setNavigationItemSelectedListener(this)

        recyclerTags.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerTags.adapter = TagsAdapter(arrayListOf("RECENT", "NEAR ME"), this, this)

        adapter = ParkingLocationsAdapter(parkingSamples, this, this)
        adapter.viewBeneath = mapViewMain

        recyclerPlaces.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerPlaces.adapter = adapter

        mapViewMain.onCreate(savedInstanceState)
    }

    fun onSearchLocationClick(view: View) {
        Snackbar.make(content, "Search", Snackbar.LENGTH_SHORT).show()
    }

    override fun onTagClick(item: String, position: Int) {
        // todo
        Snackbar.make(content, "item", Snackbar.LENGTH_SHORT).show()
    }

    override fun onTopViewClick() {
//        Snackbar.make(content, "top view", Snackbar.LENGTH_SHORT).show()
        fullView = !fullView
        adapter.fullView = fullView
        Log.d("MainActivity", "onTopViewClick: fullview: $fullView")
        adapter.topViewHeight = if (fullView) {
            content.height + 100
        } else {
            content.height - 240
        }
        buttonSearchByPin.visibility = if (fullView) View.VISIBLE else View.GONE
        recyclerPlaces.isLayoutFrozen = fullView
    }

    override fun onLocationClick(item: String, position: Int) {
        Snackbar.make(content, item, Snackbar.LENGTH_SHORT).show()
    }

    fun onSearchByPinClick(view: View) {
        onTopViewClick()
    }

    override fun onStart() {
        super.onStart()
        mapViewMain.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapViewMain.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapViewMain.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapViewMain.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapViewMain.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewMain.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (outState != null) mapViewMain.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_logout -> {
                Log.d("MainActivity", "onOptionsItemSelected: sign out clicked")
                true
            }
            R.id.action_settings -> {
                Log.d("MainActivity", "onOptionsItemSelected: settings clicked")
                true
            }
            else -> false
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
