# HARİTALAR — YAŞAYAN PROJE ÇALIŞMA PROMPTU

Bu dosya yaşayan teknik hafızadır. Her `Devam` işleminde GitHub gerçekliğiyle senkronize edilir. Eski HEAD veya eski CI/APK sonucu güncel kabul edilmez.

## Çalışma standardı

`Devam` = GITHUB → GÜNCEL DURUM → KOD → EN KRİTİK GERÇEK EKSİK → GERÇEK DEĞİŞİKLİK → TEST → COMMIT → GITHUB DOĞRULAMA → CI → APK → BU DOSYAYI GÜNCELLE.

Mevcut kod okunmadan MainActivity körlemesine rewrite edilmez. Başarılı testler yalnız değişiklik etkiliyorsa tekrarlanır. Yapılmayan iş yapılmış gibi, başarısız CI başarılı gibi, build edilmemiş APK hazır gibi raporlanmaz.

## Gerçeklik / güvenlik standardı

Sahte trafik, ETA, radar/EDS, POI, koordinat, API response veya canlı veri üretilmez. Test fixture canlı veri değildir. OSM canlı trafik kaynağı değildir. Web scraping provider değildir. API key/secret public kaynak koda yazılmaz. Ücretli servis kullanıcı onayı olmadan etkinleştirilmez.

Kritik zincir:
`ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST`

## Ürün / mimari

- Android Kotlin uygulaması
- `app/` UI/platform, `core/` domain/data
- MapLibre + OpenFreeMap
- Nominatim geocoding
- Valhalla routing
- MainActivity gerçek Valhalla sonuçlarından 7 alternatif üretir: en hızlı, en kısa, ücretsiz öncelikli, ücretli hızlı, feribot hızlı, feribotsuz, ücretsiz+feribotsuz.
- GPS, navigation progress, Türkçe TTS, 60 m off-route ve 15 s reroute cooldown mevcut.
- Feribot/toll yalnız gerçek routing sonucu veya doğrulanmış hesapla gösterilir; bilinmeyen ücret uydurulmaz.

## Trafik çekirdeği

`core/src/main/kotlin/com/haritalar/core/traffic/` altında TrafficProvider, TrafficProviderChain, TrafficRouteMatcher, TrafficRouteAdapter, TrafficRouteCostModel, TrafficRouteRanking, RouteTrafficIntelligence, TrafficRouteIntelligence, TrafficRouteOrchestrator, TomTomTrafficProvider ve TrafficRouteRankingService bulunur.

`TrafficRouteRankingService` route seti için en fazla bir provider-chain snapshot alır ve geometry-aware ranking uygular. Empty/invalid route, provider/network failure, expired/mismatch/LOW-confidence traffic durumlarında base ETA korunur. `rankBlocking()` suspend `rank()` fonksiyonunu bounded 120 saniyelik blocking bridge üzerinden mevcut Executor akışına bağlar ve ana thread'de kullanılmamalıdır.

Yeni `TrafficRefreshCoordinator`, navigation/route-card çağıran katmanların refresh zamanlamasını ve tekil in-flight ranking isteğini koordine eder. Varsayılan minimum yenileme aralığı 60 saniyedir. `reset()` yeni route generation için cooldown'u temizler ve çalışan eski ranking sonucunu `Stale` olarak işaretler; böylece eski generation sonucu yeni rotaya uygulanmaz. Coordinator provider/fallback mantığını kopyalamaz; verilen canonical ranking fonksiyonunu kullanır.

## TomTom / credential / refresh

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointini kullanır; key boşsa pasiftir, scraping yoktur ve snapshot/segment doğrulamaları uygulanır. `app/build.gradle.kts` TOMTOM_API_KEY'i Gradle property veya environment variable'dan alır ve `BuildConfig.TOMTOM_API_KEY` üretir. `TrafficEngineFactory`: BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService.

Gerçek credential sağlanmadığı için canlı TomTom trafiğinin aktif olduğu iddia edilmez. Key yokken base Valhalla ETA korunur.

TomTom örnek noktaları route başına en fazla 8 ile sınırlıdır. Aynı yaklaşık 1e-6 derece sample noktası 30 saniyelik kısa provider-cache içinde tekrar HTTP çağrısı yapmadan kullanılabilir. Cache süresi dolduğunda gerçek provider yeniden sorgulanır. Bu cache yalnız gerçek TomTom segment cevabını tutar; sentetik trafik üretmez.

## 7 rota kartı trafik entegrasyonu

MainActivity'de 7 route generation tamamlandıktan sonra tek candidate set üzerinden `TrafficRouteRankingService.rankBlocking()` çağrılır. Stale `routeGeneration` sonucu UI'a yazılmaz. `RouteTrafficPresentation` ranked sonuçları UI-safe ETA modellerine çevirir.

Kritik route identity düzeltmesi tamamlandı: ranking adjusted ETA'ya göre sıralanabildiği için presentation modelleri artık `ranked` candidate ile aynı `routeId` üzerinden eşleştiriliyor; pozisyon bazlı input-candidate/model zip hatası kaldırıldı.

Trafik uygulanmadığında kart temel Valhalla ETA'sını gösterir. Doğrulanmış trafik varsa adjusted ETA/delay gösterilebilir; trafik gecikmesi uydurulmaz.

## Bu turda gerçek değişiklikler — 2026-09-09

- `2c34feb8d8ef56850fd718920165b315cd1fc2c1` — `TrafficRefreshCoordinator` eklendi. 60 saniyelik minimum refresh aralığı, tek in-flight ranking, reset/generation invalidation ve stale sonuç ayrımı sağlandı.
- `24f63830811337ecebc024ed779aeeeceb124609` — coordinator için cooldown, reset ve çalışan refresh'in stale olması unit testleri eklendi.
- `7a42a03f1f0f61c0b8f9e3bd2925f6632fbc8672` — önceki TomTom sample cache/test durumu dokümana işlendi.

## CI / APK — doğrulanmış

- Run `307` (`34275365434`) **success** ve HEAD `7a42a03f1f0f61c0b8f9e3bd2925f6632fbc8672` için unit tests + debug APK build başarılıdır. Build job'da unit tests, debug APK ve artifact/release adımları success durumundadır.
- Run `307` sonrası eklenen coordinator commitleri için yeni CI sonucu henüz doğrulanmadı. Bu nedenle yeni HEAD için APK hazır denmez.
- Önceki doğrulanmış Run `303` (`34274419418`) APK artifact: `haritalar-debug-apk-303`; `app-debug.apk` SHA-256: `cb599f1c47e871e3daec0c7705b98bf57da3b26909c9a697f1a794110fe3fe59`.

## Güncel HEAD

Son doğrulanan GitHub `main` HEAD: `24f63830811337ecebc024ed779aeeeceb124609` (`test(traffic): cover navigation refresh coordination`). Bu doküman güncellemesi üzerine yeni bir commit oluşturacaktır; doküman commit SHA'sı GitHub'dan tekrar doğrulanmalıdır.

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
- TomTom provider adapter
- secure TomTom configuration
- UI-safe traffic presentation
- route-generation tamamlanınca tek traffic ranking snapshot
- stale generation UI guard
- routeId-preserving traffic card presentation
- TomTom sample request bound (8)
- TomTom short-lived sample cache (30 s)
- TrafficRefreshCoordinator timing/concurrency/stale-generation primitive
- Run 303 için başarılı unit tests + debug APK
- Run 307 için başarılı unit tests + debug APK

## Açık işler / sonraki hedef

1. `24f638...`/doküman HEAD sonrası yeni CI'ı doğrula; başarısızsa gerçek hatayı düzelt.
2. Gerçek TomTom credential olmadan canlı trafik iddiası yapma; credential sağlandığında gerçek smoke/integration test yap.
3. `TrafficRefreshCoordinator`ı MainActivity navigation akışına bağla: GPS callback yalnız cooldown kontrolü yapsın; HTTP/provider çağrısı yalnız background executor'da ve refresh gerektiğinde çalışsın.
4. Navigation sırasında tüm mevcut route seti üzerinden tek traffic ranking snapshot al; seçili route ve UI stale-generation guard ile korunmalı.
5. Off-route/reroute sonrasında coordinator `reset()` ile eski traffic sonucunu geçersiz kılmalı ve yeni route generation için kontrollü refresh yapılmalı.
6. Gerçek traffic-adjusted ranking için integration/smoke coverage artır.
7. HERE yalnız gerçek API erişimi, authentication, kota ve kullanım şartları doğrulanırsa değerlendir.
8. Sonrasında safety/radar canlı veri kaynakları yalnız makine-okunabilir ve doğrulanmış resmi/topluluk API erişimi varsa entegre edilir.

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
- CI'ın kaynak kodu sessizce değiştirmesine kalıcı olarak izin vermek
- GPS başına provider HTTP çağrısı

## Sonraki Devam hedefi

**Yeni coordinator HEAD için CI sonucunu doğrula. Yeşil ise `MainActivity` navigation callback'ine coordinator bağla: GPS olayları yalnız cooldown/in-flight kontrolü yapacak, ranking background executor'da çalışacak, routeGeneration/reset stale sonucu UI'a yazılmasını engelleyecek. Sonra CI ve APK'yı gerçek GitHub sonucu ile doğrula.**
