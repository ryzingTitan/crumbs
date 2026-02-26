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

data class TrailInfo(
    val name: String?,
    val lengthMeters: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: String,
)

class GpxViewModel(application: Application) : AndroidViewModel(application) {
    private val _gpxUri = MutableStateFlow<Uri?>(null)
    val gpxUri: StateFlow<Uri?> = _gpxUri.asStateFlow()

    private val _gpxFileName = MutableStateFlow<String?>(null)
    val gpxFileName: StateFlow<String?> = _gpxFileName.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<Point>>(emptyList())
    val trailPoints: StateFlow<List<Point>> = _trailPoints.asStateFlow()

    private val _trailInfo = MutableStateFlow<TrailInfo?>(null)
    val trailInfo: StateFlow<TrailInfo?> = _trailInfo.asStateFlow()

    fun setGpxFile(uri: Uri, fileName: String) {
        _gpxUri.value = uri
        _gpxFileName.value = fileName
        viewModelScope.launch(Dispatchers.IO) {
            val (points, elevations) = try {
                getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { parseGpxPoints(it) } ?: Pair(emptyList(), emptyList())
            } catch (_: Exception) { Pair(emptyList<Point>(), emptyList<Double?>()) }
            _trailPoints.value = points
            _trailInfo.value = if (points.isNotEmpty()) {
                computeTrailInfo(points, elevations, fileName.removeSuffix(".gpx"))
            } else null
        }
    }

    fun clearRoute() {
        _gpxUri.value = null
        _gpxFileName.value = null
        _trailPoints.value = emptyList()
        _trailInfo.value = null
    }

    private fun parseGpxPoints(inputStream: InputStream): Pair<List<Point>, List<Double?>> {
        val points = mutableListOf<Point>()
        val elevations = mutableListOf<Double?>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "trkpt") {
                val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                var ele: Double? = null
                event = parser.next()
                while (!(event == XmlPullParser.END_TAG && parser.name == "trkpt")) {
                    if (event == XmlPullParser.START_TAG && parser.name == "ele") {
                        ele = parser.nextText().toDoubleOrNull()
                    }
                    event = parser.next()
                }
                if (lat != null && lon != null) {
                    points.add(Point.fromLngLat(lon, lat))
                    elevations.add(ele)
                }
            }
            event = parser.next()
        }
        return Pair(points, elevations)
    }

    private fun computeTrailInfo(
        points: List<Point>,
        elevations: List<Double?>,
        name: String?,
    ): TrailInfo {
        var lengthMeters = 0.0
        val results = FloatArray(1)
        for (i in 0 until points.size - 1) {
            android.location.Location.distanceBetween(
                points[i].latitude(), points[i].longitude(),
                points[i + 1].latitude(), points[i + 1].longitude(),
                results,
            )
            lengthMeters += results[0]
        }

        var elevGain = 0.0
        var elevLoss = 0.0
        val validElevations = elevations.filterNotNull()
        if (validElevations.size >= 2) {
            for (i in 0 until validElevations.size - 1) {
                val delta = validElevations[i + 1] - validElevations[i]
                if (delta > 0) elevGain += delta else elevLoss += -delta
            }
        }

        val lengthKm = lengthMeters / 1000.0
        val estimatedMinutes = ((lengthKm / 4.0) + (elevGain / 600.0)) * 60

        val difficulty = if (validElevations.size >= 2) {
            val distanceMiles = lengthMeters * 0.000621371
            val elevGainFeet = elevGain * 3.28084
            val baseScore = Math.sqrt(2.0 * elevGainFeet * distanceMiles)
            val averageGrade = if (lengthMeters > 0) (elevGain / lengthMeters) * 100 else 0.0
            val adjustedScore = baseScore * (1 + averageGrade / 100)
            when {
                adjustedScore < 50 -> "Easy"
                adjustedScore <= 100 -> "Moderate"
                else -> "Hard"
            }
        } else {
            when {
                lengthKm <= 8 -> "Easy"
                lengthKm <= 16 -> "Moderate"
                else -> "Hard"
            }
        }

        return TrailInfo(
            name = name,
            lengthMeters = lengthMeters,
            elevationGainMeters = elevGain,
            elevationLossMeters = elevLoss,
            estimatedTimeMinutes = estimatedMinutes.toInt(),
            difficulty = difficulty,
        )
    }
}
