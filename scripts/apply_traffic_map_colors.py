from pathlib import Path

MAIN = Path("app/src/main/java/com/haritalar/app/MainActivity.kt")
text = MAIN.read_text()

def keep_one(value: str, needle: str) -> str:
    first = value.find(needle)
    if first < 0:
        return value
    head = value[:first + len(needle)]
    tail = value[first + len(needle):].replace(needle, "")
    return head + tail

# Normalize duplicate declarations left by earlier one-shot patch attempts.
for line in [
    '        private const val TRAFFIC_ROUTE_SOURCE_PREFIX = "haritalar-traffic-route-source-"\n',
    '        private const val TRAFFIC_ROUTE_LAYER_PREFIX = "haritalar-traffic-route-layer-"\n',
    '        private const val GLOBAL_TRAFFIC_SOURCE_PREFIX = "haritalar-global-traffic-source-"\n',
    '        private const val GLOBAL_TRAFFIC_LAYER_PREFIX = "haritalar-global-traffic-layer-"\n',
    '    private var lastTrafficSegmentsByRoute: Map<String, List<TrafficRouteSegment>> = emptyMap()\n',
]:
    text = keep_one(text, line)

if 'import com.haritalar.core.traffic.TrafficSegment\n' not in text:
    text = text.replace(
        'import com.haritalar.core.traffic.TrafficRouteSegment\n',
        'import com.haritalar.core.traffic.TrafficRouteSegment\nimport com.haritalar.core.traffic.TrafficSegment\n',
        1,
    )

# Remove repeated state-clearing statements; one is sufficient and keeps the code readable.
needle = '        lastTrafficSegmentsByRoute = emptyMap()\n'
text = keep_one(text, needle)

# The map-wide traffic implementation must remain present and route-independent.
required = [
    'private fun refreshMapTraffic(map: MapLibreMap',
    'private fun drawGlobalTrafficSegments(style: Style, segments: List<TrafficSegment>)',
    'GLOBAL_TRAFFIC_SOURCE_PREFIX',
]
for item in required:
    if item not in text:
        raise SystemExit(f"global traffic implementation missing: {item}")

MAIN.write_text(text)

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
        val bounds = TrafficBounds(point.latitude, point.longitude, point.latitude, point.longitude)
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

log = Path("docs/PROJECT_ACTIVITY_LOG.md")
entry = '''\n## İşlem #LIVE-TRAFFIC-MAP-3 — Rota bağımsız trafik kod temizliği ve doğrulama hazırlığı\n\n- **Tarih:** 2026-09-10\n- **Tür:** Bug fix / patch cleanup\n- **Amaç:** Önceki tek-seferlik patch denemelerinin oluşturduğu yinelenen Kotlin bildirimlerini temizlemek ve rota bağımsız canlı trafik katmanını derlenebilir hale getirmek.\n- **Yapılan:** MainActivity duplicate constant/field bildirimleri normalize edildi; TrafficSegment importu güvenceye alındı; canonical TomTom provider için bounded blocking bridge TrafficEngineFactory içinde tutuldu.\n- **Canlı kullanıcı doğrulaması:** BEKLİYOR.\n'''
text = log.read_text()
if 'İşlem #LIVE-TRAFFIC-MAP-3' not in text:
    log.write_text(text.rstrip() + '\n' + entry)
