/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.sources.wmosevereweather

import android.graphics.Color
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.AlertSeverity
import com.google.maps.android.EncodedPolylineUtil
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.google.maps.android.model.LatLng
import org.breezyweather.common.exceptions.ParsingException
import org.breezyweather.sources.wmosevereweather.json.WmoSevereWeatherAlert
import org.breezyweather.sources.wmosevereweather.json.WmoSevereWeatherAlertCoord
import java.util.Date
import java.util.Locale

fun convert(location: Location, alertsResult: List<WmoSevereWeatherAlert>): List<Alert> {
    return alertsResult
        .filter {
            (it.expires == null || it.expires > Date()) && isAlertForLocation(location, it.coord)
        }.map {
            val severity = AlertSeverity.getInstance(it.s)
            Alert(
                alertId = (it.identifier ?: it.capURL ?: it.url)!!,
                startDate = it.onset ?: it.effective ?: it.sent,
                endDate = it.expires,
                headline = it.event?.replaceFirstChar { firstChar ->
                    if (firstChar.isLowerCase()) {
                        firstChar.titlecase(Locale.getDefault())
                    } else firstChar.toString()
                },
                description = it.description,
                severity = AlertSeverity.getInstance(it.s),
                color = when (severity) {
                    AlertSeverity.EXTREME -> Color.rgb(215, 46, 41)
                    AlertSeverity.SEVERE -> Color.rgb(254, 153, 0)
                    AlertSeverity.MODERATE -> Color.rgb(255, 255, 1)
                    AlertSeverity.MINOR -> Color.rgb(0, 255, 255)
                    else -> Color.rgb(51, 102, 255)
                }
            )
            // TODO: Use URL to get more info (description, instruction, translations)
            /*val url = it.url?.let { url ->
                WmoSevereWeatherService.WMO_ALERTS_URL_BASE_URL + url
            } ?: it.capURL?.let { capURL ->
                WmoSevereWeatherService.WMO_ALERTS_CAP_URL_BASE_URL + capURL
            }*/
        }
}

fun isAlertForLocation(location: Location, coords: List<WmoSevereWeatherAlertCoord>): Boolean {
    coords.forEach { coord ->
        if (coord.polygon != null) {
            coord.polygon.forEach {
                if (PolyUtil.containsLocation(
                        location.latitude,
                        location.longitude,
                        it.map { latLng ->
                            LatLng.parse(latLng)
                        },
                        true
                    )) {
                    return true
                }
            }
        } else if (coord.geocode != null) {
            coord.geocode.forEach { geocodeList ->
                geocodeList.forEach {
                    it.coordinates?.forEach { multipolygon ->
                        multipolygon.forEach { encodedPolygon ->
                            if (PolyUtil.containsLocation(
                                    location.latitude,
                                    location.longitude,
                                    EncodedPolylineUtil.decode(encodedPolygon),
                                    true
                                )) {
                                return true
                            }
                        }
                    }
                }
            }
        } /*else if (coord.geojson != null) {
            // TODO
        } else if (coord.circle != null) {
            // TODO
        }*/ else if (coord.marker != null) {
            if (
                SphericalUtil.computeDistanceBetween(
                    LatLng(location.latitude, location.longitude),
                    LatLng.parse(coord.marker)
                ) < WmoSevereWeatherService.WMO_MARKER_RADIUS
            ) {
                return true
            }
        } else {
            throw ParsingException()
        }
    }

    return false
}
