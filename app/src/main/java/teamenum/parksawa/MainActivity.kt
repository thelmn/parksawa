package teamenum.parksawa

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import teamenum.parksawa.adapters.ParkingLocationsAdapter
import teamenum.parksawa.data.AuthState
import teamenum.parksawa.data.ListItem
import teamenum.parksawa.data.Parking

class MainActivity : AppCompatActivity(),
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener,
        GoogleMap.OnCameraIdleListener,
        OnMapReadyCallback,
        NavigationView.OnNavigationItemSelectedListener,
        ParkingLocationsAdapter.OnLocationsListener, FirebaseAuth.AuthStateListener {

    companion object {
        private const val REQUEST_PLACES_SEARCH = 200
        private const val REQUEST_USER_SIGN_IN = 300

        private const val TAG_MAP_FRAGMENT = "teamenum.parksawa.MainActivity.TAG_MAP_FRAGMENT"
        private const val KEY_IS_SEARCH = "teamenum.parksawa.MainActivity.KEY_IS_SEARCH"
    }

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var fullView = false
    private lateinit var adapter: ParkingLocationsAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    private var pinnedMarker: Marker? = null
    private var isSearch = true
    private val itemSearchHere = object : ListItem { override val VIEW_TYPE = ParkingLocationsAdapter.TYPE_SEARCH_HERE }
    private val itemChangeSearch = object : ListItem { override val VIEW_TYPE = ParkingLocationsAdapter.TYPE_CHANGE_SEARCH }
    private val parkingSamples = arrayListOf(
            Parking(1, "Place 1") as ListItem,
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

    private lateinit var googleMap: GoogleMap
    private val nairobi = LatLng(-1.2833300, 36.8166700)
    private val southEastKenyaBound = LatLng(-5.4,42.0)
    private val northWestKenyaBound = LatLng(4.9, 33.5)
    private val zoomLevel = 13f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)

        navView.setNavigationItemSelectedListener(this)
        onSignIn()

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.peekHeight = dpToPx(200)
        frameTopBackground.alpha = 0f

        frameTopBackground.alpha = 0f
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val bottomSheetListener = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                frameTopBackground.alpha = slideOffset
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when(newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        frameTopBackground.alpha = 1f
                        adapter.setSearchOrChange(null)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        frameTopBackground.alpha = 0f
                        setSearchOrChange()
                    }
                }
            }
        }
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetListener)
        ViewCompat.setNestedScrollingEnabled(recyclerPlaces, false)

        adapter = ParkingLocationsAdapter(parkingSamples, this, this)
        setSearchOrChange()

        recyclerPlaces.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerPlaces.adapter = adapter

        Handler().postDelayed({
            if (!isFinishing) {
                var mapFragment = supportFragmentManager.findFragmentByTag(TAG_MAP_FRAGMENT) as? SupportMapFragment?
                if (mapFragment == null) {
                    val mapOptions = GoogleMapOptions()
                            .camera(CameraPosition.fromLatLngZoom(nairobi, zoomLevel))
                    mapFragment = SupportMapFragment.newInstance(mapOptions) as SupportMapFragment
                }
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.mapView, mapFragment, TAG_MAP_FRAGMENT)
                        .commit()
                mapFragment.getMapAsync(this@MainActivity)
            }
        }, 100)
    }

    fun onSearchLocationClick(view: View) {
        Snackbar.make(content, "Search", Snackbar.LENGTH_SHORT).show()
        try {
            val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .setBoundsBias(LatLngBounds(southEastKenyaBound, northWestKenyaBound))
                    .build(this)
            startActivityForResult(intent, REQUEST_PLACES_SEARCH)
        } catch (e: GooglePlayServicesRepairableException) {
            Snackbar.make(content, "Repairable Exception", Snackbar.LENGTH_SHORT).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            Snackbar.make(content, "Play Services Not Available", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onLocationClick(item: String, position: Int) {
        Snackbar.make(content, item, Snackbar.LENGTH_SHORT).show()
    }

    override fun onSearchOrChange() {
        if (isSearch) pinToMap(googleMap.cameraPosition.target)
        else unPinMarker()
    }

    private fun setSearchOrChange() {
        adapter.setSearchOrChange(if (isSearch) itemSearchHere else itemChangeSearch)
    }

    override fun onCameraMoveStarted(p0: Int) {
        // disable select current location button
    }

    override fun onCameraMove() {
        //
    }

    override fun onCameraMoveCanceled() {
        //
    }

    override fun onCameraIdle() {
        //
        val target = googleMap.cameraPosition.target
        Log.d("MainActivity", "onCameraIdle: $target")
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap ?: return
        Log.d("MainActivity", "onMapReady: got map")
        with(googleMap) {
            setOnCameraMoveStartedListener(this@MainActivity)
            setOnCameraMoveListener(this@MainActivity)
            setOnCameraMoveCanceledListener(this@MainActivity)
            setOnCameraIdleListener(this@MainActivity)

            uiSettings.isMyLocationButtonEnabled = true

//            moveCamera(CameraUpdateFactory.newLatLngZoom(nairobi, zoomLevel))
        }
    }

    private fun unPinMarker() {
        isSearch = true
        setSearchOrChange()
        pinnedMarker?.remove()
        pinnedMarker = null
        placeMarker.visibility = View.VISIBLE
    }

    private fun pinToMap(point: LatLng) {
        isSearch = false
        setSearchOrChange()
        placeMarker.visibility = View.INVISIBLE
        if (pinnedMarker != null) {
            pinnedMarker?.remove()
            pinnedMarker = null
        }
        pinnedMarker = googleMap.addMarker(MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_pinned)))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(point))
        // show loading and perform db search at point
    }

    private fun onSignIn() {
        val user = auth.currentUser
        val isLoggedIn = user != null
        val v = navView.getHeaderView(0)

        with(v.usernameView) {
            visibility = if (isLoggedIn) View.VISIBLE else View.INVISIBLE
            text = user?.displayName
        }
        with(v.phoneView) {
            visibility = if (isLoggedIn) View.VISIBLE else View.INVISIBLE
            text = user?.phoneNumber
        }

        navView.menu.findItem(R.id.nav_sign_in).isVisible = !isLoggedIn
        navView.menu.findItem(R.id.nav_sign_out).isVisible = isLoggedIn

        user?.getIdToken(false)?.addOnSuccessListener { result ->
            val isHost = result.claims[getString(R.string.host_claim)] as? Boolean ?: false
            Log.d("MainActivity", "onCreate: $isHost")
            if (isHost) {
                navView.menu.findItem(R.id.nav_be_host).title = "Manage my Parking Spaces"
            }
        }
    }

    private fun signOut() {
        auth.signOut()
        Prefs.authState = AuthState.INIT
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        auth.addAuthStateListener(this)
    }

    override fun onPause() {
        super.onPause()
        auth.removeAuthStateListener(this)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        onSignIn()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        outState?.putBoolean(KEY_IS_SEARCH, isSearch)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        isSearch = savedInstanceState?.getBoolean(KEY_IS_SEARCH) ?: true
        setSearchOrChange()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_PLACES_SEARCH -> {
                when (resultCode) {
                    AppCompatActivity.RESULT_OK -> {
                        val place = PlaceAutocomplete.getPlace(this, data)
                        Log.d("MainActivity", "onActivityResult: Place: ${place.latLng}")
                        pinToMap(place.latLng)
                    }
                    PlaceAutocomplete.RESULT_ERROR -> {
                        val status = PlaceAutocomplete.getStatus(this, data)
                        Log.e("MainActivity", "onActivityResult: ${status.statusMessage}")
                    }
                    AppCompatActivity.RESULT_CANCELED -> Snackbar.make(content, "Cancelled Search.", Snackbar.LENGTH_SHORT)
                }
            }
            REQUEST_USER_SIGN_IN -> {
                if (resultCode == Activity.RESULT_OK) {
                    // refresh nav bar
                    Snackbar.make(content, "Signed In", Snackbar.LENGTH_SHORT).show()
                }
            }
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
            R.id.nav_home -> {

            }
            R.id.nav_be_host -> {
                startActivity(Intent(this, HostMainActivity::class.java))
                finish()
            }
            R.id.nav_sign_in -> {
                startActivityForResult(Intent(this, WelcomeActivity::class.java), REQUEST_USER_SIGN_IN)
            }
            R.id.nav_sign_out -> {
                signOut()
            }
            R.id.nav_settings -> {

            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    class SelfRemovingGlobalLayoutListener(val view: View, val func: ()->Unit) : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            func()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            } else {
                view.viewTreeObserver.removeGlobalOnLayoutListener(this)
            }
        }
    }
}
