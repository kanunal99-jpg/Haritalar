# HARİTALAR — YAŞAYAN PROJE ÇALIŞMA PROMPTU

Bu dosya yaşayan teknik hafızadır. Her gerçek geliştirme turunda GitHub gerçekliğiyle senkronize edilir. Eski HEAD, eski CI sonucu veya tamamlanmış gibi görünen fakat repoda bulunmayan işler güncel durum olarak kabul edilmez.

## Çalışma standardı

Her `Devam` işleminde:
1. `main` HEAD, son commitler, ilgili dosyalar, klasör yapısı ve CI GitHub'dan doğrulanır.
2. Mevcut kod okunmadan MainActivity veya kritik zincir körlemesine rewrite edilmez.
3. Kritik gerçek eksik seçilir; başarıyla doğrulanmış testler gereksiz yere tekrarlanmaz.
4. Gerekiyorsa gerçek kod değişikliği yapılır ve commitlenir.
5. CI sonucu ve APK artifact'ı gerçekten doğrulanır.
6. APK yalnızca ilgili HEAD için başarılı build/artifact doğrulanırsa hazır kabul edilir.
7. Tur sonunda bu dosya güncel HEAD, commit, değişen dosyalar, test/CI/APK durumu ve açık işlerle güncellenir.

## Gerçeklik / güvenlik

Sahte trafik, ETA, radar/EDS, POI, koordinat veya API response üretilmez. Test fixture canlı veri gibi gösterilmez. OSM canlı trafik kaynağı değildir. Web scraping trafik provider'ı değildir. Secret/API key public GitHub'a yazılmaz. Ücretli servis kullanıcı onayı olmadan etkinleştirilmez.

Kritik dayanıklılık zinciri:
`ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST`

## Mimari / ürün durumu

- `app/` Android UI/platform
- `core/` domain/data
- MapLibre + OpenFreeMap
- Nominatim geocoding
- Valhalla online routing
- MainActivity gerçek Valhalla sonuçlarından 7 rota seçeneği üretir: en hızlı, en kısa, ücretsiz öncelikli, ücretli hızlı, feribot hızlı, feribotsuz, ücretsiz+feribotsuz.
- Feribot/toll yalnız gerçek Valhalla sonucu veya doğrulanmış hesap üzerinden gösterilir; bilinmeyen ücret uydurulmaz.
- MainActivity'de GPS, MapLibre, Türkçe TTS, navigation progress, 60 m off-route ve 15 s reroute cooldown mevcuttur.

## Trafik çekirdeği

`core/src/main/kotlin/com/haritalar/core/traffic/`:
- TrafficProvider.kt
- TrafficProviderChain.kt
- TrafficRouteMatcher.kt
- TrafficRouteAdapter.kt
- TrafficRouteCostModel.kt
- TrafficRouteRanking.kt
- RouteTrafficIntelligence.kt
- TrafficRouteIntelligence.kt
- TrafficRouteOrchestrator.kt
- TomTomTrafficProvider.kt
- TrafficRouteRankingService.kt

`TrafficRouteRankingService` route seti için en fazla bir provider-chain snapshot alır ve geometry-aware canonical ranking'e verir. Empty/invalid route, provider/network failure, expired/mismatch/LOW-confidence traffic durumlarında base ETA korunur. TomTom route örneklemesi kontrollüdür.

`TrafficRouteRankingService.rankBlocking()` mevcut Executor tabanlı Android akışını suspend `rank()` ile aynı kaynak mantığı üzerinden bounded biçimde bağlar. Blocking çağrı ana thread'de kullanılmamalıdır; varsayılan timeout 120 saniyedir.

## TomTom

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointini kullanır; key boşsa pasiftir, scraping yoktur, snapshot TTL 60 saniyedir ve hız/geometry doğrulamaları yapılır.

`app/build.gradle.kts` TOMTOM_API_KEY'i Gradle property veya environment variable'dan alır ve `BuildConfig.TOMTOM_API_KEY` üretir. Secret kaynak koda yazılmaz. `TrafficEngineFactory` BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService zincirini kurar.

Gerçek credential sağlanmadığı için canlı TomTom trafiğinin bu repo/CI üzerinde aktif olduğu iddia edilmez. Key yokken base Valhalla ETA korunmalıdır.

## 7 rota kartı trafik entegrasyonu

MainActivity artık route generation tamamlanmadan traffic ranking çalıştırmaz. Yedi rota sonucu tamamlandığında tek snapshot/candidate set üzerinden ranking çalıştırılır; stale generation sonucu UI'a yazılmaz.

Akış:
`7 Valhalla route → TrafficRouteRankingService.rankBlocking → TrafficRouteRanking → RouteTrafficPresentation → route-card sorting/rendering`

ÖNEMLİ düzeltme: `TrafficRouteRanking` sonucu adjusted ETA'ya göre yeniden sıraladığı için presentation modelleri artık ranked candidate ile aynı `routeId` üzerinden eşleştirilir. Önceki hatalı yaklaşımda ranked modelleri input candidate listesiyle pozisyon bazlı zip'lemek route kimliğini kaydırabilirdi; bu düzeltilmiştir.

Trafik uygulanmadığında kart temel Valhalla süresini kullanır. Doğrulanmış trafik uygulandığında UI-safe adjusted duration/delay gösterilebilir; trafik yoksa gecikme uydurulmaz.

## Güncel doğrulanmış geliştirme turu — 2026-09-08

### HEAD zinciri
- Önceki gerçek HEAD: `9efc2dae5d6f4d08dd88bb54c9c839ea95e8ecb4` (`feat(traffic): add route-card integration patch script`)
- `282a4084eb8cfa97e74619b2714cfbbe9ea7d288` — ranking presentation route-identity fix script
- `848de857bea27ce6b941664f11658c9a0f0da757` — GitHub Actions bot tarafından gerçek MainActivity route-card integration commit
- Güncel kod düzeltmesi: `0a2bf30f71578f14bf9843ccd4a470969eb0af2b` — `fix(traffic): invoke suspend ranking from blocking bridge`
- Bu doküman güncellemesi bu HEAD'in üstüne commitlenecektir.

### Gerçek CI bulgusu
Run `299` (`34274046287`) gerçek kod derlemesinde başarısız oldu. Unit test aşamasında `TrafficRouteRankingService.kt:65` içinde suspend `rank()` fonksiyonunun coroutine dışından çağrılması derleme hatası verdi. Bu hata logdan doğrulandı; APK build bu nedenle çalışmadı.

Run `299` sırasında route-card identity düzeltmesi de gerçek GitHub workspace'ine uygulandı ve `848de857...` commit'i oluşturuldu. Bu commit GITHUB_TOKEN ile yapıldığı için yeni workflow tetiklemedi; dolayısıyla run 299'un head SHA'sı eski commit olarak kaldı. Bu durum APK'nın 848de857 veya 0a2bf30... HEAD'i için başarılı olduğunu göstermez.

### Yapılan gerçek düzeltmeler
1. `scripts/integrate_traffic_ranking.py` artık mevcut route-card integration içindeki ranked presentation eşlemesini güvenli şekilde düzeltir ve hedef bulunamazsa körlemesine patch yapmayı reddeder.
2. `TrafficRouteRankingService.rankBlocking()` içinde suspend fonksiyon doğru şekilde `suspend () -> ...` lambda üzerinden `startCoroutine` ile çağrılacak hale getirildi.
3. MainActivity route-card mapping'i `ranked.zip(RouteTrafficPresentation.fromRanked(ranked)).associate { routeId -> model }` mantığıyla route kimliğini koruyacak hale getirildi.
4. Traffic ranking yalnız yedi route generation tamamlandığında çalışır; kısmi sonuçlar base Valhalla kartları olarak gösterilebilir.

## CI / APK durumu

- Son temiz doğrulanmış eski build: Run `287`, artifact `haritalar-debug-apk-287`, SHA-256 `f9e6550dd279ac5458a7ff7c535ff1b538dc1dc48b5816d28207b56e8767912a`.
- Run `299`: **failure** — `TrafficRouteRankingService.rankBlocking()` compile error. APK **yok / hazır değil**.
- Güncel HEAD `0a2bf30...` için bu doküman commitinden sonra yeni CI sonucu ayrıca doğrulanmalıdır.
- Yeni HEAD için başarılı APK doğrulanana kadar APK hazır denmeyecektir.

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
- secure TomTom configuration factory
- route-generation tamamlanınca tek ranking snapshot yaklaşımı
- stale route-generation UI guard

## Açık işler / sıradaki hedef

1. Güncel HEAD için CI'ı temiz şekilde doğrula.
2. Unit test + debug APK artifact + SHA-256'yı güncel HEAD ile doğrula.
3. CI temizlendikten sonra geçici route-card patch scriptini ve workflow içindeki self-mutating patch adımını kaldır; CI kaynak kodu değiştirmemeli, yalnız test/build yapmalıdır.
4. Gerçek TomTom credential olmadan canlı trafik aktifmiş gibi davranma; key yokken base ETA fallback'ini test et.
5. Gerçek credential sağlandığında TomTom smoke/integration testi yap; kota/cache/throttling uygula.
6. GPS başına provider çağrısı yapma; refresh/cooldown/cache zincirini tamamla.
7. Navigation sırasında canlı refresh → ranking → UI ve off-route/reroute → ranking zincirini bağla.
8. HERE yalnız gerçek API erişimi ve kullanım şartları doğrulanırsa değerlendir.

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

## Sonraki hedef

**Güncel HEAD'in CI sonucunu doğrula. Temiz CI + güncel APK doğrulamasından sonra self-mutating patch mekanizmasını kaldır ve MainActivity 7 rota kartı trafik entegrasyonunu kalıcı kaynak kodu olarak koru. Ardından gerçek TomTom refresh/cache/ranking zincirini navigation akışına taşı.**
