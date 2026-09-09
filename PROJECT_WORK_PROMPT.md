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

`TrafficRefreshCoordinator`, navigation/route-card çağıran katmanların refresh zamanlamasını ve tekil in-flight ranking isteğini koordine eder. Varsayılan minimum yenileme aralığı 60 saniyedir. `reset()` yeni route generation için cooldown'u temizler ve çalışan eski ranking sonucunu `Stale` olarak işaretler; böylece eski generation sonucu yeni rotaya uygulanmaz. Coordinator provider/fallback mantığını kopyalamaz; verilen canonical ranking fonksiyonunu kullanır.

Coordinator ayrıca Android gibi zaten background executor sahibi çağıranlar için bounded `refreshBlocking()` bridge sağlar. Bu bridge suspend `refresh()` fonksiyonunu tek kaynak olarak kullanır; ana thread'de çağrılmamalıdır.

## TomTom / credential / refresh

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointini kullanır; key boşsa pasiftir, scraping yoktur ve snapshot/segment doğrulamaları uygulanır. `app/build.gradle.kts` TOMTOM_API_KEY'i Gradle property veya environment variable'dan alır ve `BuildConfig.TOMTOM_API_KEY` üretir. `TrafficEngineFactory`: BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService.

Gerçek credential sağlanmadığı için canlı TomTom trafiğinin aktif olduğu iddia edilmez. Key yokken base Valhalla ETA korunur.

TomTom örnek noktaları route başına en fazla 8 ile sınırlıdır. Aynı yaklaşık 1e-6 derece sample noktası 30 saniyelik kısa provider-cache içinde tekrar HTTP çağrısı yapmadan kullanılabilir. Cache süresi dolduğunda gerçek provider yeniden sorgulanır. Bu cache yalnız gerçek TomTom segment cevabını tutar; sentetik trafik üretmez.

## 7 rota kartı trafik entegrasyonu

MainActivity'de 7 route generation tamamlandıktan sonra tek candidate set üzerinden `TrafficRefreshCoordinator.refreshBlocking()` ile canonical `TrafficRouteRankingService.rank()` çağrılır. `routeGeneration` kontrolü stale sonucu UI'a yazdırmaz. `RouteTrafficPresentation` ranked sonuçları UI-safe ETA modellerine çevirir.

Kritik route identity düzeltmesi tamamlandı: ranking adjusted ETA'ya göre sıralanabildiği için presentation modelleri artık `ranked` candidate ile aynı `routeId` üzerinden eşleştiriliyor; pozisyon bazlı input-candidate/model zip hatası kaldırıldı.

Trafik uygulanmadığında kart temel Valhalla ETA'sını gösterir. Doğrulanmış trafik varsa adjusted ETA/delay gösterilebilir; trafik gecikmesi uydurulmaz.

## Navigation sırasında trafik yenileme

MainActivity navigation GPS callback'i artık `TrafficRefreshCoordinator`ı tetikler. GPS olayında provider/HTTP çalışmaz; trafik ranking `routeExecutor` üzerinde background çalışır. Coordinator 60 saniyelik cooldown ve tek in-flight kontrolünü yapar.

Navigation sırasında aktif route seti `trafficRouteOptions` olarak korunur ve tek candidate snapshot üzerinden canonical `TrafficRouteRankingService.rank()` çağrılır. Sonuç yalnız aynı `routeGeneration` hâlâ aktifse UI-state'e yazılır. Navigation aktifken route kartı yeniden açılmaz; `lastTrafficByRoute` cache'i güncellenir.

Yeni hedef/routing başlatma `reset()` ile eski traffic state'ini temizler. Off-route sonrası reroute için `routeGeneration` artırılır, coordinator `reset()` edilir ve yeni tek rota üzerinden kontrollü trafik refresh'i başlatılır. Varış ve navigation stop durumlarında coordinator ve traffic state temizlenir.

## Bu turda gerçek değişiklikler — 2026-09-09

- `af9ffcb99d965978132fdea4a2c9cc0b93c1891f` — `TrafficRefreshCoordinator` için ilk blocking bridge commit'i oluşturuldu.
- `1b869fdf4fdee70a6281dba3b3d5251c37956a9b` — blocking bridge içindeki Kotlin `Result` generic çakışması düzeltildi.
- `7d18119cc1d309ee55bdd46ec85adcda3d6cd29b` — MainActivity navigation akışına coordinator bağlandı; GPS callback background refresh tetikliyor, route generation/reset stale koruması eklendi.
- `fe39634743175c4ca839314c6cda357e85bb69f1` — `refreshBlocking()` için unit test eklendi; canonical ranker delegasyonu ve zaman/route aktarımı doğrulanıyor.
- GitHub compare sonucu `c66c1d3` → `fe396347` arasında 4 commit, 3 dosya değişikliği doğrulandı: MainActivity, TrafficRefreshCoordinator ve coordinator testleri.

## CI / APK — doğrulanmış geçmiş sonuçlar

- Run `309` HEAD `24f63830811337ecebc024ed779aeeeceb124609` için **success** ve debug APK üretildi.
- Run `309` artifact: `haritalar-debug-apk-309`; artifact digest: `sha256:839811646c32f606237cecfc6c8136861dc2217c81b31c999650d5514b810a0d`.
- Run `308` HEAD `2c34feb8d8ef56850fd718920165b315cd1fc2c1` için **success** ve debug APK üretildi.
- Run `308` artifact: `haritalar-debug-apk-308`; artifact digest: `sha256:764c45cbfe4470199192657e183644f52e512b7e85a95e4055e0c39d0660d819`.
- Önceki doğrulanmış Run `303` (`34274419418`) APK artifact: `haritalar-debug-apk-303`; `app-debug.apk` SHA-256: `cb599f1c47e871e3daec0c7705b98bf57da3b26909c9a697f1a794110fe3fe59`.

**Güncel navigation-coordinator commitleri için yeni CI sonucu henüz doğrulanmış değildir. APK bu nedenle hazır/indirilebilir olarak ilan edilmez.**

## Güncel HEAD

GitHub `main` HEAD artık `fe39634743175c4ca839314c6cda357e85bb69f1` (`test(traffic): cover blocking coordinator bridge`). Önceki doğrulanmış doküman HEAD'i `c66c1d3` idi; bu turda 4 yeni commit ile ilerlenmiştir.

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
- Navigation GPS → coordinator → background canonical ranking entegrasyonu
- Reroute/new route/arrival/stop için traffic reset + stale invalidation
- `refreshBlocking()` unit coverage
- Run 303, 308 ve 309 için başarılı unit tests + debug APK

## Açık işler / sonraki hedef

1. `fe396347...` HEAD için GitHub Actions CI sonucunu doğrula; başarısızsa gerçek hatayı düzelt.
2. Yeni CI yeşil olduktan sonra debug APK artifact/release bilgisini gerçek run üzerinden doğrula.
3. Gerçek TomTom credential olmadan canlı trafik iddiası yapma; credential sağlandığında gerçek smoke/integration test yap.
4. Navigation refresh'in GPS çağrılarında gereksiz executor task kuyruğu oluşturmadığını ölç; gerekiyorsa coordinator ile thread-submit arasına atomik due/in-flight gate ekle.
5. Gerçek traffic-adjusted ranking için integration/smoke coverage artır.
6. HERE yalnız gerçek API erişimi, authentication, kota ve kullanım şartları doğrulanırsa değerlendir.
7. Sonrasında safety/radar canlı veri kaynakları yalnız makine-okunabilir ve doğrulanmış resmi/topluluk API erişimi varsa entegre edilir.

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

**`fe396347` HEAD CI sonucunu gerçek GitHub'dan doğrula. CI yeşilse debug APK artifact/release'i doğrula. Ardından navigation refresh task-submit davranışını ölç ve gerekiyorsa atomik due/in-flight gate ile executor kuyruğunu sıkılaştır.**
