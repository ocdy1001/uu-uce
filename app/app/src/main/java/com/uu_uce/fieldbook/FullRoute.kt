package com.uu_uce.fieldbook

import android.graphics.Path
import android.os.Build
import android.util.JsonReader
import android.view.View
import com.uu_uce.allpins.PinConversion
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
import java.io.StringReader
import java.time.LocalTime

class FullRoute (routeString: String) {
    private var routeList: List<RoutePoint>
    private var linesList: FloatArray

    init {
        routeList = getRoute(routeString)
        linesList = FloatArray(routeList.size * 4)
    }

    fun drawLines(viewPort: Pair<p2,p2>,view: View): FloatArray {
        //TODO: calculate center/zoom/boundaries/relative distance on init & use those on drawing
        val length = routeList.size

        if (linesList.isEmpty())
            coordToScreen(routeList[0].first,viewPort,view.width,view.height).also {
                linesList.plus(listOf(it.first,it.second))
            }

        var i = 1

        while (i < length-1) {
            coordToScreen(routeList[i++].first,viewPort,view.width,view.height).also{c ->
                val toAdd = listOf(c.first,c.second)
                repeat(2) {
                    linesList.plus(toAdd)
                }
            }
        }

        if(i==routeList.lastIndex)
            coordToScreen(routeList[0].first,viewPort,view.width,view.height).also {
                linesList.plus(listOf(it.first, it.second))
            }

        return linesList
    }

    private fun getRoute(routeString: String): List<RoutePoint> {
        val reader = JsonReader(StringReader(routeString))

        return readRoute(reader)
    }

    private fun readRoute(reader: JsonReader): List<RoutePoint> {
        val route: MutableList<RoutePoint> = mutableListOf()

        reader.beginArray()
        while (reader.hasNext()) {
            route.add(readRoutePoint(reader))
        }
        reader.endArray()
        return route
    }

    private fun readRoutePoint(reader: JsonReader): RoutePoint {
        var coordinate = UTMCoordinate(31, 'N', 0.0, 0.0)
        var localtime  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalTime.MIDNIGHT
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "coordinate" -> {
                    coordinate = PinConversion.stringToUtm(reader.nextString())
                    //TODO: what if this goes wrong?
                }
                "localtime" -> {
                    localtime = LocalTime.parse(reader.nextString())
                    //TODO: what if this fails & API?
                }
                else -> {
                    error("Wrong format")
                }
            }
        }
        reader.endObject()
        return RoutePoint(coordinate,localtime)
    }
}