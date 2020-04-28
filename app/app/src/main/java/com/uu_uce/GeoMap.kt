package com.uu_uce

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.*
import com.uu_uce.shapefiles.HeightLineReader
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.PolygonReader
import com.uu_uce.views.DragStatus
import kotlinx.android.synthetic.main.activity_geo_map.*
import org.jetbrains.annotations.TestOnly
import java.io.File

const val debug = false

class GeoMap : AppCompatActivity() {
    private lateinit var pinViewModel: PinViewModel
    private val permissionsNeeded = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private var screenDim = Point(0,0)
    private var statusBarHeight = 0
    private var resourceId = 0
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("LocationServices", false)
        Logger.setTagEnabled("Pin", false)
        Logger.setTagEnabled("DrawOverlay", false)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        }
        else{
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)

        //start()
        // This is needed on older phones, even though maps are in internal memory
        if(checkPermissions(this, permissionsNeeded).count() > 0){
            getPermissions(this, permissionsNeeded, EXTERNAL_FILES_REQUEST)
        }
        else{
            start()
        }
    }

    private fun start(){
        setContentView(R.layout.activity_geo_map)

        // TODO: Remove when database is fully implemented
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("com.uu_uce.USER_POINTS", 0)
            apply()
        }

        // Start database and get pins from database
        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)
        this.customMap.setPinViewModel(pinViewModel)
        this.customMap.setLifeCycleOwner(this)
        this.customMap.setPins(pinViewModel.allPinData)

        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        val longest = maxOf(screenDim.x, screenDim.y)
        val size = (longest*menu.buttonPercent).toInt()

        // Initialize menu
        val btn1 = allpins_button
        btn1.setOnClickListener{customMap.startAllPins()}

        val btn2 = fieldbook_button
        btn2.setOnClickListener{customMap.startFieldBook()}

        dragBar.clickAction      = {menu.dragButtonTap()}
        dragBar.dragAction       = { dx, dy -> menu.drag(dx,dy)}
        dragBar.dragEndAction    = { dx, dy -> menu.snap(dx, dy)}

        val mydir = File(filesDir,"mydir")
        try {
            val heightlines = File(mydir, "heightlines")
            customMap.addLayer(LayerType.Water, HeightLineReader(heightlines), toggle_layer_layout, size, true)
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $heightlines")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }
        try {
            customMap.addLayer(LayerType.Water, PolygonReader(mydir),  toggle_layer_layout, size, false)
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $mydir")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }

        customMap.initializeCamera()

        customMap.tryStartLocServices(this)

        menu.post{
            initMenu()
        }

        // Set center on location button functionality
        center_button.setOnClickListener{
            if(customMap.locationAvailable){
                customMap.zoomToDevice()
                customMap.setCenterPos()
            }
            else{
                Toast.makeText(this, "Location not avaiable", Toast.LENGTH_LONG).show()
                getPermissions(this, LocationServices.permissionsNeeded, LOCATION_REQUEST)
            }
        }

        started = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Move the menu down when the map is tapped
        if(menu.dragStatus != DragStatus.Down &&
            ev.action == MotionEvent.ACTION_DOWN &&
            !(ev.x > menu.x && ev.x < menu.x + menu.width && ev.y-statusBarHeight > menu.y && ev.y-statusBarHeight < menu.y + menu.height)){
            menu.down()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if(menu.dragStatus != DragStatus.Down){
            menu.down()
            return
        }

        customMap.activePopup?.dismiss()
    }

    override fun onResume() {
        if(started){
            super.onResume()
            customMap.setPins(pinViewModel.allPinData)
            customMap.redrawMap()
        }
        super.onResume()
    }

    private fun initMenu(){
        menu.setScreenHeight(screenDim.y - statusBarHeight, dragBar.height, toggle_layer_scroll.height, lower_menu_layout.height)
    }

    // Respond to permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            EXTERNAL_FILES_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"GeoMap", "Permissions granted")
                    start()
                }
                else{
                    Logger.log(LogType.Info,"GeoMap", "Permissions were not granted, asking again")
                    getPermissions(this, permissionsNeeded, EXTERNAL_FILES_REQUEST)
                }
            }
            LOCATION_REQUEST -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Logger.log(LogType.Info,"GeoMap", "Permissions granted")
                    customMap.locationAvailable = true
                    customMap.startLocServices()
                }
                else{
                    Logger.log(LogType.Info,"GeoMap", "Permissions were not granted")
                    customMap.locationAvailable = false
                }
            }
        }
    }

    @TestOnly
    fun setPinData(newPinData : List<PinData>){
        pinViewModel.setPins(newPinData)
    }

    @TestOnly
    fun getPinLocation() : Pair<Float, Float>{
        return customMap.getPinLocation()
    }
}
