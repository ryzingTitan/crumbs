package com.ryzingtitan.crumbs.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class GpxViewModel(application: Application) : AndroidViewModel(application) {
    private val _gpxUri = MutableStateFlow<Uri?>(null)
    val gpxUri: StateFlow<Uri?> = _gpxUri.asStateFlow()

    private val _gpxFileName = MutableStateFlow<String?>(null)
    val gpxFileName: StateFlow<String?> = _gpxFileName.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<Point>>(emptyList())
    val trailPoints: StateFlow<List<Point>> = _trailPoints.asStateFlow()

    fun setGpxFile(uri: Uri, fileName: String) {
        _gpxUri.value = uri
        _gpxFileName.value = fileName
        viewModelScope.launch(Dispatchers.IO) {
            val points = try {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { parseGpxPoints(it) } ?: emptyList()
            } catch (_: Exception) { emptyList() }
            _trailPoints.value = points
        }
    }

    fun clearRoute() {
        _gpxUri.value = null
        _gpxFileName.value = null
        _trailPoints.value = emptyList()
    }

    private fun parseGpxPoints(inputStream: InputStream): List<Point> {
        val points = mutableListOf<Point>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "trkpt") {
                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                if (lat != null && lon != null) points.add(Point.fromLngLat(lon, lat))
            }
            event = parser.next()
        }
        return points
    }
}
