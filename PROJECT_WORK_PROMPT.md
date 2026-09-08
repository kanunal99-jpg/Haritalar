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

## TomTom / credential

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointini kullanır; key boşsa pasiftir, scraping yoktur ve snapshot/segment doğrulamaları uygulanır. `app/build.gradle.kts` TOMTOM_API_KEY'i Gradle property veya environment variable'dan alır ve `BuildConfig.TOMTOM_API_KEY` üretir. `TrafficEngineFactory`: BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService.

Gerçek credential sağlanmadığı için canlı TomTom trafiğinin aktif olduğu iddia edilmez. Key yokken base Valhalla ETA korunur.

## 7 rota kartı trafik entegrasyonu

MainActivity'de 7 route generation tamamlandıktan sonra tek candidate set üzerinden `TrafficRouteRankingService.rankBlocking()` çağrılır. Stale `routeGeneration` sonucu UI'a yazılmaz. `RouteTrafficPresentation` ranked sonuçları UI-safe ETA modellerine çevirir.

Kritik route identity düzeltmesi tamamlandı: ranking adjusted ETA'ya göre sıralanabildiği için presentation modelleri artık `ranked` candidate ile aynı `routeId` üzerinden eşleştiriliyor; pozisyon bazlı input-candidate/model zip hatası kaldırıldı.

Trafik uygulanmadığında kart temel Valhalla ETA'sını gösterir. Doğrulanmış trafik varsa adjusted ETA/delay gösterilebilir; trafik gecikmesi uydurulmaz.

## Bu turda gerçek değişiklikler — 2026-09-08

1. `282a4084eb8cfa97e74619b2714cfbbe9ea7d288` — route-card traffic presentation identity fix için patch script güncellendi.
2. `848de857bea27ce6b941664f11658c9a0f0da757` — MainActivity 7 rota kartına ranking/presentation entegrasyonu GitHub Actions tarafından gerçek kaynak koda uygulandı.
3. `0a2bf30f71578f14bf9843ccd4a470969eb0af2b` — `TrafficRouteRankingService.rankBlocking()` içindeki coroutine invocation compile hatası düzeltildi; suspend lambda üzerinden `startCoroutine` kullanılıyor.
4. `8b4e0f8453000b3121f60e0a23ac3cf8b5b3d5df` — bu turdaki ara yaşayan durum kaydı.
5. `b27b5992ab1bedceb73929706563624d4d3f79b3` — CI'ın kaynak kodu değiştirmesini kaldıran workflow düzenlemesi.
6. `1609972c4320855869d7d89126d61ebbee64ef4d` — obsolete `scripts/integrate_traffic_ranking.py` silindi.
7. Bu doküman güncellemesi ile güncel HEAD tekrar doğrulanmış durumla senkronize ediliyor.

## CI / APK — doğrulanmış

- Run `299` (`34274046287`) **failure**: `TrafficRouteRankingService.kt:65` suspend `rank()` coroutine dışından çağrıldığı için compile error; APK build atlandı.
- Run `300` (`34274183521`) **success** ve HEAD `0a2bf30f71578f14bf9843ccd4a470969eb0af2b` için unit tests + debug APK build başarılı.
- Run `300` APK artifact: `haritalar-debug-apk-300`; artifact digest `sha256:81c662300474b06f6e2821bab6cc24e386546786d799b3b29d4a2a4387386a9c`.
- Run `300` artifact içindeki `app-debug.apk` SHA-256: `da2c122ef16c81558dca2aa14d8a32dc7f1f6e38938f0b3b6f5c8e6d820915a6`.
- Run `303` (`34274419418`) **success** ve güncel kaynak ağacındaki traffic integration cleanup sonrası HEAD `1609972c4320855869d7d89126d61ebbee64ef4d` için unit tests + debug APK build başarılı.
- Run `303` APK artifact: `haritalar-debug-apk-303`; artifact digest `sha256:57809a47d28ca3084f3459f5b61fd1ad9f96e2e456cce52b29070bee69821cb3`.
- Run `303` artifact içindeki `app-debug.apk` SHA-256: `cb599f1c47e871e3daec0c7705b98bf57da3b26909c9a697f1a794110fe3fe59`.
- Run `303` APK **gerçekten hazır/doğrulandı**.

## Güncel HEAD

**`1609972c4320855869d7d89126d61ebbee64ef4d`**

Son commit:
`chore: remove obsolete traffic integration patch script`

`main` branch bu committe. Bu HEAD, route-card traffic integration kodunu kalıcı olarak repoda tutuyor ve CI workflow artık kaynak kodu değiştiren self-mutating patch adımı içermiyor.

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
- güncel HEAD için başarılı unit tests
- güncel HEAD için başarılı debug APK

## Açık işler / sonraki hedef

1. Gerçek TomTom credential olmadan canlı trafik iddiası yapma; credential sağlandığında gerçek smoke/integration test yap.
2. Traffic refresh/cooldown/cache mekanizmasını GPS başına çağrı yapmayacak şekilde tamamla.
3. Navigation sırasında live traffic refresh → ranking → UI zincirini bağla.
4. Off-route/reroute sonrasında aynı traffic ranking/presentation zincirini kontrollü şekilde yeniden çalıştır.
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

## Sonraki Devam hedefi

**Güncel HEAD `1609972c...` üzerinden gerçek TomTom traffic refresh/cache/cooldown katmanını incele; credential yoksa güvenli inert/fallback davranışını koru. Sonra navigation live-refresh → ranking → route-card/UI → off-route/reroute zincirini küçük ve doğrulanabilir adımlarla geliştir. Her tur sonunda bu dosyayı yeniden GitHub gerçekliğiyle güncelle.**
