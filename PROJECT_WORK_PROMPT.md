# HARİTALAR — YAŞAYAN PROJE ÇALIŞMA PROMPTU

Bu dosya yaşayan teknik hafızadır. Her gerçek geliştirme turunda GitHub gerçekliğiyle senkronize edilir.

## Çalışma standardı

Her `Devam` işleminde:
1. `main` HEAD, son commitler, ilgili dosyalar ve CI GitHub'dan doğrulanır.
2. Kritik gerçek eksik seçilir; başarılı testler gereksiz yere tekrarlanmaz.
3. Gerekiyorsa gerçek kod değişikliği yapılır ve commitlenir.
4. CI sonucu ve APK artifact'ı gerçekten doğrulanır.
5. APK yalnızca ilgili HEAD için başarılı build/artifact doğrulanırsa hazır kabul edilir.
6. Tur sonunda bu dosya güncellenir.

## Gerçeklik / güvenlik

Sahte trafik, ETA, radar/EDS, POI, koordinat veya API response üretilmez. Test fixture canlı veri gibi gösterilmez. OSM canlı trafik kaynağı değildir. Web scraping trafik provider'ı değildir. Secret/API key public GitHub'a yazılmaz. Ücretli servis kullanıcı onayı olmadan etkinleştirilmez.

Kritik zincir:
`ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST`

## Mimari

- `app/` Android UI/platform
- `core/` domain/data
- MapLibre + OpenFreeMap
- Nominatim geocoding
- Valhalla online routing
- MainActivity gerçek 7 rota seçeneğini üretir: en hızlı, en kısa, ücretsiz öncelikli, ücretli hızlı, feribot hızlı, feribotsuz, ücretsiz+feribotsuz.
- Feribot/toll yalnız gerçek Valhalla sonucu veya doğrulanmış hesap üzerinden gösterilir; bilinmeyen ücret uydurulmaz.

## Trafik çekirdeği

`core/src/main/kotlin/com/haritalar/core/traffic/` altında:
- `TrafficProvider.kt`
- `TrafficProviderChain.kt`
- `TrafficRouteMatcher.kt`
- `TrafficRouteAdapter.kt`
- `TrafficRouteCostModel.kt`
- `TrafficRouteRanking.kt`
- `RouteTrafficIntelligence.kt`
- `TrafficRouteIntelligence.kt`
- `TrafficRouteOrchestrator.kt`
- `TomTomTrafficProvider.kt`
- `RouteTrafficPresentation.kt`
- `TrafficRouteRankingService.kt`

Hedef:
`GERÇEK LIVE PROVIDER → ALTERNATİF → CACHE → FALLBACK → BASE VALHALLA ETA`

`TrafficRouteRankingService` route seti için en fazla bir provider-chain snapshot alır ve canonical geometry-aware ranking'e verir. Empty/invalid route, provider/network failure, expired/mismatch/LOW-confidence traffic durumlarında base ETA korunur. Provider tarafında route örnekleme 8 noktayla sınırlıdır.

`RouteTrafficPresentation` trafik uygulanmadıysa yalnız temel süreyi, doğrulanmış trafik uygulandıysa gerekirse `47 dk • trafik +7 dk` biçimini üretir; negatif/sahte gecikme göstermez.

## TomTom

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointini kullanır; key boşsa pasiftir, scraping yoktur, snapshot TTL 60 saniyedir, `currentSpeed/freeFlowSpeed/geometry` doğrulanmadan segment oluşturulmaz.

Bu turda uygulama configuration katmanı eklendi:
- `app/build.gradle.kts`: `TOMTOM_API_KEY` Gradle property veya environment variable'dan okunuyor.
- `BuildConfig.TOMTOM_API_KEY` üretiliyor.
- Secret source code'a yazılmıyor; key yoksa boş string ile provider inert kalıyor.
- `app/src/main/java/com/haritalar/app/TrafficEngineFactory.kt`: BuildConfig key ile `TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService` oluşturuyor.

ÖNEMLİ: Gerçek credential verilmediği için canlı TomTom trafiğinin çalıştığı iddia edilmez. MainActivity 7 route card rendering'i de henüz ranking/presentation zincirine bağlanmış değildir.

## Navigation / safety

MainActivity'de GPS, MapLibre, TTS, navigation progress, 60 m off-route eşiği ve 15 s reroute cooldown mevcuttur. Safety/radar domain çekirdeği testlidir.

## Bu tur — doğrulanmış gerçek durum — 2026-09-08

CI önceki ranking test fixture sorununu düzelten committen sonra temiz geçti.

- `7c1a027ac78ee80c20dd8f095a03c36ad9d6d41b` — `docs: synchronize living state after ranking test fix`
- GitHub Actions run `287` bu HEAD için **success**.
- Unit Tests: success.
- Build debug APK: success.
- APK artifact: `haritalar-debug-apk-287`.
- Unit test report: `haritalar-unit-test-reports-287`.
- APK artifact SHA-256: `f9e6550dd279ac5458a7ff7c535ff1b538dc1dc48b5816d28207b56e8767912a`.

Bu turdaki yeni gerçek kod commitleri:
- `dbe05a50c67c9ca8fd16ea5d032f9d474babe59b` — secure TomTom BuildConfig input.
- `d108e67a24c8b88755dd8774cbd5a5976feff4ad` — `TrafficEngineFactory`.

`PROJECT_WORK_PROMPT.md` bu durumdan sonra yeniden güncelleniyor; dolayısıyla yeni HEAD bu doküman commitidir ve onun CI sonucu ayrıca doğrulanmalıdır.

## Başarılı / mevcut

- Android app iskeleti
- MapLibre/OpenFreeMap
- GPS
- Nominatim arama
- Valhalla routing
- 7 route akışı
- toll/ferry tespiti
- Türkçe TTS/navigation
- off-route/reroute
- safety/radar domain
- traffic provider chain
- geometry-aware traffic matching/ranking
- TomTom real provider adapterı
- UI-safe traffic presentation
- route ranking orchestration
- secure TomTom configuration factory

## Açık işler / sıradaki kritik hedef

1. Bu prompt güncellemesinin yeni CI run'ını doğrula.
2. MainActivity'yi komple rewrite etmeden `TrafficEngineFactory → TrafficRouteRankingService → RouteTrafficPresentation` zincirini gerçek 7 route card rendering'e bağla.
3. Ranking'i her kısmi route sonucu için değil, route generation tamamlandığında kontrollü biçimde çalıştır; stale generation sonuçlarını UI'a yazma.
4. GPS başına provider çağrısı yapma: refresh/cooldown/cache mekanizması kur.
5. Gerçek TomTom credential yalnız kullanıcı/CI secret sağlandığında smoke-test edilir; key yokken base Valhalla ETA korunur.
6. Gerçek traffic-adjusted ranking için integration/smoke test ekle.
7. Sonrasında navigation sırasında canlı refresh ve off-route/reroute ile yeniden ranking zincirini bağla.
8. HERE ancak gerçek API erişimi ve kullanım şartları doğrulanırsa değerlendir.

## Gidilmeyecek yollar

- sahte trafik/ETA/radar/EDS/POI/API/koordinat
- test verisini canlı veri gibi göstermek
- OSM'yi canlı trafik sanmak
- web scraping
- public secret/API key
- ücretsiz kotayı sınırsız varsaymak
- kullanıcı onayı olmadan ücretli trafik
- okunmadan MainActivity rewrite
- başarısız CI'ı başarılı göstermek
- build edilmemiş APK'yı hazır göstermek

## Sonraki hedef

**Yeni configuration commitlerinin CI sonucunu doğrula. Temiz CI sonrası MainActivity'nin gerçek 7 rota kartına traffic ranking + UI-safe ETA zincirini kontrollü şekilde bağla. Trafik verisi yoksa UI temel Valhalla ETA'sına aynen dönmelidir.**
