package com.uu_uce.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.uu_uce.AllPins
import com.uu_uce.R
import com.uu_uce.database.PinConversion
import com.uu_uce.database.PinData
import com.uu_uce.database.PinViewModel
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.drawDeviceLocation
import com.uu_uce.mapOverlay.pointInAABoundingBox
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.pins.Pin
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.PinType
import com.uu_uce.services.LocationServices
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.services.degreeToUTM
import com.uu_uce.shapefiles.*
import com.uu_uce.ui.DoubleTapper
import com.uu_uce.ui.Scroller
import com.uu_uce.ui.ViewTouchParent
import com.uu_uce.ui.Zoomer
import diewald_shapeFile.files.shp.SHP_File
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : ViewTouchParent {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var smap : ShapeMap
    private var first = true

    private var loc : UTMCoordinate = UTMCoordinate(31, 'N', 0.0, 0.0)

    private val locationServices = LocationServices()

    private val pinTapBufferSize : Int = 10

    private val deviceLocPaint : Paint = Paint()
    private val deviceLocEdgePaint : Paint = Paint()

    private var pins : List<Pin> = emptyList()
    private lateinit var viewModel : PinViewModel
    private lateinit var lfOwner : LifecycleOwner

    private val pinList : MutableList<Pin> = mutableListOf(Pin(
        UTMCoordinate(31, 'N', 314968.0, 4677733.6),
        1,
        PinType.TEXT,
        "Test1",
        PinContent(
            "[\n" +
                    "        {\n" +
                    "            \"tag\"       : \"TEXT\",\n" +
                    "            \"text\"   : \"Image example\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"tag\"       : \"IMAGE\",\n" +
                    "            \"file_name\"   : \"test.png\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"tag\"       : \"IMAGE\",\n" +
                    "            \"file_name\"   : \"test.png\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"tag\"       : \"IMAGE\",\n" +
                    "            \"file_name\"   : \"test.png\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"tag\"       : \"IMAGE\",\n" +
                    "            \"file_name\"   : \"test.png\"\n" +
                    "        }\n" +
                    "    ]"
        ),
        ResourcesCompat.getDrawable(context.resources, R.drawable.pin, null) ?: error ("Image not found")
    ), Pin(
        UTMCoordinate(31, 'N', 313368.0, 4671833.6),
        1,
        PinType.IMAGE,
        "Test2",
        PinContent(
            "[\n" +
                    "        {\n" +
                    "            \"tag\"       : \"TEXT\",\n" +
                    "            \"text\"   : \"Video example\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"tag\"       : \"VIDEO\",\n" +
                    "            \"file_name\"   : \"zoo.mp4\",\n" +
                    "            \"thumbnail\" : \"zoothumbnail.png\",\n" +
                    "            \"title\" : \"zoo\"\n" +
                    "        }\n" +
                    "    ]"
        ),
        ResourcesCompat.getDrawable(context.resources, R.drawable.pin, null) ?: error ("Image not found")
    ))

    private var statusBarHeight = 0
    private val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")

    private var camera: Camera

    init{
        SHP_File.LOG_INFO = false
        SHP_File.LOG_ONLOAD_HEADER = false
        SHP_File.LOG_ONLOAD_CONTENT = false

        Logger.setTagEnabled("CustomMap", false)
        Logger.setTagEnabled("zoom", false)

        //setup touch events
        addChild(Zoomer(context, ::zoomMap))
        addChild(Scroller(context, ::moveMap))
        addChild(DoubleTapper(context, ::zoomOutMax))
        Logger.log(LogType.Info,"CustomMap", "Init")

        smap = ShapeMap(5, this)
        val dir = File(context.filesDir, "mydir")
        val timeParse = measureTimeMillis {
            smap.addLayer(LayerType.Water, dir, context)
        }

        camera = smap.initialize()
        //Log.i("CustomMap", "Parse file: $timeParse")

        deviceLocPaint.color = Color.BLUE
        deviceLocEdgePaint.color = Color.WHITE
        locationServices.startPollThread(context, 5000, 0F, ::updateLoc)

        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val waspect = width.toDouble() / height
        if(first){
            val z = 1.0 / (waspect)
            camera.maxZoom = z
            camera.setZoom(z)
            first = false
        }
        val res = camera.update()
        if(res == UpdateResult.NOOP){
            return
        }
        val viewport = camera.getViewport(waspect)
        val timeDraw = measureTimeMillis {
            canvas.drawColor(Color.rgb(234, 243, 245))
            smap.draw(canvas, width, height)

            drawDeviceLocation(
                coordToScreen(loc, viewport, width, height),
                canvas,
                deviceLocPaint,
                deviceLocEdgePaint,
                15F,
                4F)

            pins.map{ pin -> pin.draw(viewport, this, canvas) }

        }
        Logger.log(LogType.Continuous, "CustomMap", "Draw MS: $timeDraw")
        if(res == UpdateResult.ANIM)
            invalidate()
    }

    private fun updateLoc(newLoc : p2) {
        loc = degreeToUTM(newLoc)
        Logger.log(LogType.Event,"CustomMap", "${loc.east}, ${loc.north}")
    }

    fun updatePins(){
        viewModel.allPinData.observe(lfOwner, Observer { pins ->
            // Update the cached copy of the words in the adapter.
            pins?.let { setPins(it) }
        })
    }

    private fun setPins(pins: List<PinData>) {
        val processedPins = mutableListOf<Pin>()
        for(pin in pins) {
            processedPins.add(PinConversion(context).pinDataToPin(pin))
        }
        this.pins = processedPins
        camera.forceChanged()
        invalidate()
    }

    private fun zoomMap(zoom: Float){
        val deltaOne = 1.0 - zoom.toDouble().coerceIn(0.5, 1.5)
        camera.zoomIn(1.0 + deltaOne)
        if(camera.needsInvalidate())
            invalidate()
    }

    private fun moveMap(dxpxf: Float, dypxf: Float){
        Logger.log(LogType.Continuous, "CustomMap", "$dypxf")
        val dxpx = dxpxf.toDouble()
        val dypx = dypxf.toDouble()
        val dx = dxpx / width
        val dy = dypx / height
        camera.moveView(dx * 2, dy * -2)
        if(camera.needsInvalidate())
            invalidate()
    }

    private fun zoomOutMax(){
        camera.zoomOutMax(500.0)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun zoomToDevice(){
        camera.startAnimation(Triple(loc.east, loc.north, 0.02), 1500.0)
        if(camera.needsInvalidate())
            invalidate()
    }

    fun tapPin(tapLocation : p2, activity : Activity){
        val canvasTapLocation : p2 = Pair(tapLocation.first, tapLocation.second - statusBarHeight)
        pins.forEach{ p ->
            if(!p.inScreen) return@forEach
            if(pointInAABoundingBox(p.boundingBox.first, p.boundingBox.second, canvasTapLocation, pinTapBufferSize)){
                p.openPopupWindow(this, activity)
                Logger.log(LogType.Info, "CustomMap", "${p.title}: I have been tapped.")
                return
            }
        }
    }

    fun toggleLayer(l: Int){
        smap.toggleLayer(l)
    }

    fun setViewModel(vm: PinViewModel) {
        viewModel = vm
    }

    fun setLifeCycleOwner(lifecycleOwner: LifecycleOwner) {
        lfOwner = lifecycleOwner
    }

    fun allPins() {
        Log.i("test", "test123")
        val i = Intent(context, AllPins::class.java)
        startActivity(context, i, null)
    }
}