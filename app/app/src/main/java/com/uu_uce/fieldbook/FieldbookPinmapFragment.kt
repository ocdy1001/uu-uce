package com.uu_uce.fieldbook

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.uu_uce.R
import com.uu_uce.defaultPinSize
import com.uu_uce.misc.ListenableBoolean
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.needsReload
import com.uu_uce.services.LOCATION_REQUEST
import com.uu_uce.shapefiles.*
import kotlinx.android.synthetic.main.activity_geo_map.*
import java.io.File

class FieldbookPinmapFragment : Fragment() {

    companion object {
        fun newInstance() =
            FieldbookPinmapFragment()
    }

    private lateinit var viewModel  : FieldbookViewModel
    private lateinit var frActivity : Activity
    private lateinit var frContext  : Context
    private lateinit var window     : Window

    private var statusBarHeight = 0
    private var resourceId = 0
    private var started = false

    private lateinit var sharedPref : SharedPreferences

    // TODO: Remove temporary hardcoded map information
    private val mapsName = "maps.zip"
    private lateinit var maps : List<String>

    private var polyStyles: List<PolyStyle> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[FieldbookViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        frActivity = requireActivity()
        frContext  = requireContext()
        window     = frActivity.window

        maps = listOf(frActivity.getExternalFilesDir(null)?.path + File.separator + mapsName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fieldbook_fragment_pinmap, container, false)
    }

    override fun onStart() {
        super.onStart()
        start()
    }

    override fun onResume() {
        super.onResume()
        needsReload.setListener(object : ListenableBoolean.ChangeListener {
            override fun onChange() {
                if (needsReload.getValue()) {
                    loadMap()
                }
            }
        })
    }

    /**
     * Initiates the map
     */
    private fun start() {
        // Get preferences
        sharedPref = PreferenceManager.getDefaultSharedPreferences(frContext)

        // Set settings
        this.customMap.pinSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)

        this.customMap.setActivity(frActivity)

        // TODO: Remove when releasing
        with(sharedPref.edit()) {
            putInt("com.uu_uce.USER_POINTS", 0)
            apply()
        }

        // Start database and get pins from database
        this.customMap.setFieldbookViewModel(viewModel)

        this.customMap.setLifeCycleOwner(this)
        viewModel.allFieldbookEntries.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            this.customMap.setFieldbook(it)
        })

        // Get statusbar height
        resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        //add layers to map
        loadMap()

        needsReload.setListener(object : ListenableBoolean.ChangeListener {
            override fun onChange() {
                if(needsReload.getValue()){
                    loadMap()
                }
            }
        })

        started = true
    }

    // Respond to permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
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

    /**
     * Loads the different layers
     */
    private fun loadMap(){
        val mydir = File(frContext.getExternalFilesDir(null)?.path + "/Maps/")
        try{readStyles(mydir)}
        catch(e: Exception){Logger.error("GeoMap", "no style file available: "+ e.message)}
        try {
        val layerName = "Polygons"
        val polygons = File(mydir, layerName)
        val layerType = LayerType.Water
        customMap.addLayer(
            layerType,
            PolygonReader(polygons, layerType, true, polyStyles),
            toggle_layer_layout,
            0.5f,
            0,
            layerName
        )
        Logger.log(LogType.Info, "GeoMap", "Loaded layer at $mydir")
        }catch(e: Exception){
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }
        try {
        val layerName = "Heightlines"
        val heightlines = File(mydir, layerName)
        val layerType = LayerType.Height
        customMap.addLayer(
            layerType,
            HeightLineReader(heightlines,layerType),
            toggle_layer_layout,
            Float.MAX_VALUE,
            0,
            layerName
        )
            Logger.log(LogType.Info, "GeoMap", "Loaded layer at $heightlines")

        } catch (e: Exception) {
            Logger.error("GeoMap", "Could not load layer at $mydir.\nError: " + e.message)
        }

        //create camera based on layers
        customMap.initializeCamera()
        customMap.post {
            customMap.setCameraWAspect()
            customMap.redrawMap()
        }

        customMap.setCameraWAspect()
        needsReload.setValue(false)
        customMap.redrawMap()
    }

    private fun readStyles(dir: File){
        val file = File(dir, "styles")
        val reader = FileReader(file)

        val nrStyles = reader.readULong()
        polyStyles = List(nrStyles.toInt()) {
            val outline = reader.readUByte()
            val b = reader.readUByte()
            val g = reader.readUByte()
            val r = reader.readUByte()

            PolyStyle(
                floatArrayOf(
                    r.toFloat() / 255,
                    g.toFloat() / 255,
                    b.toFloat() / 255
                )
            )
        }
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

