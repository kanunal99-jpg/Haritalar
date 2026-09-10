from pathlib import Path

MAIN = Path("app/src/main/java/com/haritalar/app/MainActivity.kt")
text = MAIN.read_text()

text = text.replace(
    'import com.haritalar.core.traffic.TrafficRefreshCoordinator\nimport com.haritalar.core.traffic.TrafficRouteRanking\n',
    'import com.haritalar.core.traffic.TrafficRefreshCoordinator\nimport com.haritalar.core.traffic.TrafficRouteMapPresentation\nimport com.haritalar.core.traffic.TrafficRouteRanking\nimport com.haritalar.core.traffic.TrafficRouteSegment\nimport com.haritalar.core.traffic.TrafficSegment\n',
)
text = text.replace(
    '        private const val ROUTE_LAYER = "haritalar-route-layer"\n',
    '        private const val ROUTE_LAYER = "haritalar-route-layer"\n        private const val TRAFFIC_ROUTE_SOURCE_PREFIX = "haritalar-traffic-route-source-"\n        private const val TRAFFIC_ROUTE_LAYER_PREFIX = "haritalar-traffic-route-layer-"\n        private const val GLOBAL_TRAFFIC_SOURCE_PREFIX = "haritalar-global-traffic-source-"\n        private const val GLOBAL_TRAFFIC_LAYER_PREFIX = "haritalar-global-traffic-layer-"\n',
)
text = text.replace(
    '    private var lastTrafficByRoute: Map<String, RouteTrafficUiModel> = emptyMap()\n',
    '    private var lastTrafficByRoute: Map<String, RouteTrafficUiModel> = emptyMap()\n    private var lastTrafficSegmentsByRoute: Map<String, List<TrafficRouteSegment>> = emptyMap()\n    private var mapTrafficRefreshInFlight = false\n    private var lastMapTrafficRefreshAt = 0L\n',
)
text = text.replace(
    '        lastTrafficByRoute = emptyMap()\n',
    '        lastTrafficByRoute = emptyMap()\n        lastTrafficSegmentsByRoute = emptyMap()\n',
)

# Route traffic rendering from the previous patch.
old = '''                    lastTrafficByRoute = trafficByRoute
                    if (!navigationActive) renderRouteOptions(snapshot, trafficByRoute)
'''
new = '''                    lastTrafficByRoute = trafficByRoute
                    if (!navigationActive) {
                        val detailedSegments = runCatching {
                            trafficRankingService.rankDetailed(
                                snapshot.map { option ->
                                    TrafficRouteRanking.RouteCandidate(
                                        routeId = routeSignature(option),
                                        coordinates = option.points.map { GeoCoordinate(it.latitude, it.longitude) },
                                        baseDurationSeconds = option.durationSeconds.coerceAtLeast(0.0).roundToLong(),
                                    )
                                },
                                System.currentTimeMillis(),
                            ).matchedSegmentsByRoute
                        }.getOrDefault(emptyMap())
                        lastTrafficSegmentsByRoute = detailedSegments
                        renderRouteOptions(snapshot, trafficByRoute)
                    }
'''
if old in text:
    text = text.replace(old, new, 1)

old = '''                    val trafficByRoute = ranked.zip(RouteTrafficPresentation.fromRanked(ranked))
                        .associate { (candidate, model) -> candidate.routeId to model }
                    runOnUiThread {
                        if (generation == routeGeneration && navigationActive) {
                            lastTrafficByRoute = trafficByRoute
                        }
                    }
'''
new = '''                    val trafficByRoute = ranked.zip(RouteTrafficPresentation.fromRanked(ranked))
                        .associate { (candidate, model) -> candidate.routeId to model }
                    val detailedSegments = runCatching {
                        trafficRankingService.rankDetailed(candidates, System.currentTimeMillis()).matchedSegmentsByRoute
                    }.getOrDefault(emptyMap())
                    runOnUiThread {
                        if (generation == routeGeneration && navigationActive) {
                            lastTrafficByRoute = trafficByRoute
                            lastTrafficSegmentsByRoute = detailedSegments
                            drawRoute(routePoints, detailedSegments[routeSignatureForCurrentRoute()].orEmpty())
                        }
                    }
'''
if old in text:
    text = text.replace(old, new, 1)

text = text.replace(
    '        drawRoute(option.points)\n',
    '        drawRoute(option.points, lastTrafficSegmentsByRoute[routeSignature(option)].orEmpty())\n',
    1,
)

# Map-wide traffic refresh, independent of a route.
text = text.replace(
    '''                map.addOnCameraIdleListener {
                    livePoiLayer?.scheduleRefresh(map.projection.visibleRegion.latLngBounds)
                }''',
    '''                map.addOnCameraIdleListener {
                    livePoiLayer?.scheduleRefresh(map.projection.visibleRegion.latLngBounds)
                    refreshMapTraffic(map)
                }''',
    1,
)
text = text.replace(
    '''                livePoiLayer?.scheduleRefresh(map.projection.visibleRegion.latLngBounds)
            }
        }
        requestLocationPermission()''',
    '''                livePoiLayer?.scheduleRefresh(map.projection.visibleRegion.latLngBounds)
                refreshMapTraffic(map, true)
            }
        }
        requestLocationPermission()''',
    1,
)

marker = '    private fun decodePolyline6(encoded: String): List<LatLng> {'
methods = r'''    private fun refreshMapTraffic(map: MapLibreMap, force: Boolean = false) {
        if (BuildConfig.TOMTOM_API_KEY.isBlank()) return
        if (map.cameraPosition.zoom < 11.0) return
        val now = System.currentTimeMillis()
        if (!force && now - lastMapTrafficRefreshAt < 60_000L) return
        if (mapTrafficRefreshInFlight) return
        mapTrafficRefreshInFlight = true
        lastMapTrafficRefreshAt = now
        val bounds = map.projection.visibleRegion.latLngBounds
        routeExecutor.execute {
            try {
                val rows = if (map.cameraPosition.zoom >= 15.0) 4 else 3
                val cols = rows
                val segments = buildList {
                    for (r in 0 until rows) {
                        val y = r.toDouble() / (rows - 1).coerceAtLeast(1)
                        for (c in 0 until cols) {
                            val x = c.toDouble() / (cols - 1).coerceAtLeast(1)
                            val point = GeoCoordinate(
                                bounds.latitudeSouth + (bounds.latitudeNorth - bounds.latitudeSouth) * y,
                                bounds.longitudeWest + (bounds.longitudeEast - bounds.longitudeWest) * x,
                            )
                            addAll(TrafficEngineFactory.fetchMapTrafficAtPointBlocking(point))
                        }
                    }
                }.distinctBy { segment ->
                    segment.geometry.joinToString(";") { "%.5f,%.5f".format(it.latitude, it.longitude) }
                }
                runOnUiThread { map.style?.let { drawGlobalTrafficSegments(it, segments) } }
            } catch (e: Exception) {
                Log.w("MainActivity", "Harita geneli trafik yenilemesi başarısız", e)
            } finally {
                mapTrafficRefreshInFlight = false
            }
        }
    }

    private fun drawGlobalTrafficSegments(style: Style, segments: List<TrafficSegment>) {
        TrafficRouteMapPresentation.Severity.values().forEach { severity ->
            val key = severity.name.lowercase(Locale.US)
            style.removeLayer(GLOBAL_TRAFFIC_LAYER_PREFIX + key)
            style.removeSource(GLOBAL_TRAFFIC_SOURCE_PREFIX + key)
        }
        TrafficRouteMapPresentation.Severity.values().forEach { severity ->
            val features = segments.filter { TrafficRouteMapPresentation.severityOf(it) == severity }.mapNotNull { segment ->
                if (segment.geometry.size < 2) return@mapNotNull null
                Feature.fromGeometry(LineString.fromLngLats(segment.geometry.map { point ->
                    org.maplibre.geojson.Point.fromLngLat(point.longitude, point.latitude)
                }))
            }
            if (features.isEmpty()) return@forEach
            val key = severity.name.lowercase(Locale.US)
            val sourceId = GLOBAL_TRAFFIC_SOURCE_PREFIX + key
            val layerId = GLOBAL_TRAFFIC_LAYER_PREFIX + key
            style.addSource(GeoJsonSource(sourceId, org.maplibre.geojson.FeatureCollection.fromFeatures(features)))
            style.addLayer(LineLayer(layerId, sourceId).withProperties(
                lineWidth(6f),
                lineColor(TrafficRouteMapPresentation.colorHex(severity)),
            ))
        }
    }

'''
if 'private fun refreshMapTraffic(map: MapLibreMap' not in text:
    text = text.replace(marker, methods + marker, 1)

# Preserve the verified route-segment rendering implementation.
old = '''    private fun drawRoute(points: List<LatLng>) {
        mapView.getMapAsync { map ->
            val style = map.style
            if (style != null) {
                style.removeLayer(ROUTE_LAYER)
                style.removeSource(ROUTE_SOURCE)
                if (points.size >= 2) {
                    val coords = points.map { org.maplibre.geojson.Point.fromLngLat(it.longitude, it.latitude) }
                    style.addSource(GeoJsonSource(ROUTE_SOURCE, Feature.fromGeometry(LineString.fromLngLats(coords))))
                    style.addLayer(LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(lineWidth(6f), lineColor("#1976D2")))
                }
            }
        }
    }
'''
new = '''    private fun routeSignatureForCurrentRoute(): String =
        if (routePoints.isEmpty()) "" else routeSignature(
            RouteOption(
                title = "",
                subtitle = "",
                points = routePoints,
                cumulativeMeters = routeCumulativeMeters,
                totalMeters = routeTotalMeters,
                distanceKm = routeTotalMeters / 1000.0,
                durationSeconds = 0.0,
                toll = false,
                tollAmountTry = null,
                tollName = null,
                ferryUsed = false,
                maneuvers = currentManeuvers,
            ),
        )

    private fun drawRoute(points: List<LatLng>, trafficSegments: List<TrafficRouteSegment> = emptyList()) {
        mapView.getMapAsync { map ->
            val style = map.style ?: return@getMapAsync
            style.removeLayer(ROUTE_LAYER)
            style.removeSource(ROUTE_SOURCE)
            TrafficRouteMapPresentation.Severity.values().forEach { severity ->
                val key = severity.name.lowercase(Locale.US)
                style.removeLayer(TRAFFIC_ROUTE_LAYER_PREFIX + key)
                style.removeSource(TRAFFIC_ROUTE_SOURCE_PREFIX + key)
            }
            if (points.size < 2) return@getMapAsync
            val coords = points.map { org.maplibre.geojson.Point.fromLngLat(it.longitude, it.latitude) }
            style.addSource(GeoJsonSource(ROUTE_SOURCE, Feature.fromGeometry(LineString.fromLngLats(coords))))
            style.addLayer(LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(lineWidth(6f), lineColor("#1976D2")))
            TrafficRouteMapPresentation.Severity.values().forEach { severity ->
                val features = trafficSegments.filter { TrafficRouteMapPresentation.severityOf(it.traffic) == severity }.mapNotNull { segment ->
                    val geometry = segment.traffic.geometry
                    if (geometry.size < 2) return@mapNotNull null
                    Feature.fromGeometry(LineString.fromLngLats(geometry.map { point ->
                        org.maplibre.geojson.Point.fromLngLat(point.longitude, point.latitude)
                    }))
                }
                if (features.isEmpty()) return@forEach
                val key = severity.name.lowercase(Locale.US)
                val sourceId = TRAFFIC_ROUTE_SOURCE_PREFIX + key
                val layerId = TRAFFIC_ROUTE_LAYER_PREFIX + key
                style.addSource(GeoJsonSource(sourceId, org.maplibre.geojson.FeatureCollection.fromFeatures(features)))
                style.addLayer(LineLayer(layerId, sourceId).withProperties(
                    lineWidth(7f),
                    lineColor(TrafficRouteMapPresentation.colorHex(severity)),
                ))
            }
        }
    }
'''
if old in text:
    text = text.replace(old, new, 1)

MAIN.write_text(text)

# Extend the traffic factory with a cached canonical provider and a bounded blocking bridge.
factory = Path("app/src/main/java/com/haritalar/app/TrafficEngineFactory.kt")
factory.write_text('''package com.haritalar.app

import com.haritalar.core.navigation.GeoCoordinate
import com.haritalar.core.traffic.TomTomTrafficProvider
import com.haritalar.core.traffic.TrafficBounds
import com.haritalar.core.traffic.TrafficProviderChain
import com.haritalar.core.traffic.TrafficRoute
import com.haritalar.core.traffic.TrafficRouteRankingService
import com.haritalar.core.traffic.TrafficSegment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

object TrafficEngineFactory {
    private val tomTomProvider = TomTomTrafficProvider(BuildConfig.TOMTOM_API_KEY)

    fun createRankingService(): TrafficRouteRankingService = TrafficRouteRankingService(
        providerChain = TrafficProviderChain(providers = listOf(tomTomProvider)),
    )

    fun fetchMapTrafficAtPointBlocking(point: GeoCoordinate, timeoutMs: Long = 15_000L): List<TrafficSegment> {
        if (!tomTomProvider.supports(point)) return emptyList()
        val bounds = TrafficBounds(
            south = point.latitude,
            west = point.longitude,
            north = point.latitude,
            east = point.longitude,
        )
        val completed = CountDownLatch(1)
        val result = AtomicReference<Result<List<TrafficSegment>>?>()
        val block: suspend () -> List<TrafficSegment> = {
            tomTomProvider.fetchTraffic(bounds, TrafficRoute(listOf(point))).segments
        }
        block.startCoroutine(object : Continuation<List<TrafficSegment>> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<List<TrafficSegment>>) {
                result.set(value)
                completed.countDown()
            }
        })
        if (!completed.await(timeoutMs, TimeUnit.MILLISECONDS)) return emptyList()
        return result.get()?.getOrNull().orEmpty()
    }
}
''')
