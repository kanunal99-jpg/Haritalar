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

## TomTom / credential / refresh

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointini kullanır; key boşsa pasiftir, scraping yoktur ve snapshot/segment doğrulamaları uygulanır. `app/build.gradle.kts` TOMTOM_API_KEY'i Gradle property veya environment variable'dan alır ve `BuildConfig.TOMTOM_API_KEY` üretir. `TrafficEngineFactory`: BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService.

Gerçek credential sağlanmadığı için canlı TomTom trafiğinin aktif olduğu iddia edilmez. Key yokken base Valhalla ETA korunur.

TomTom örnek noktaları route başına en fazla 8 ile sınırlıdır. Aynı yaklaşık 1e-6 derece sample noktası 30 saniyelik kısa provider-cache içinde tekrar HTTP çağrısı yapmadan kullanılabilir. Cache süresi dolduğunda gerçek provider yeniden sorgulanır. Bu cache yalnız gerçek TomTom segment cevabını tutar; sentetik trafik üretmez.

## 7 rota kartı trafik entegrasyonu

MainActivity'de 7 route generation tamamlandıktan sonra tek candidate set üzerinden `TrafficRouteRankingService.rankBlocking()` çağrılır. Stale `routeGeneration` sonucu UI'a yazılmaz. `RouteTrafficPresentation` ranked sonuçları UI-safe ETA modellerine çevirir.

Kritik route identity düzeltmesi tamamlandı: ranking adjusted ETA'ya göre sıralanabildiği için presentation modelleri artık `ranked` candidate ile aynı `routeId` üzerinden eşleştiriliyor; pozisyon bazlı input-candidate/model zip hatası kaldırıldı.

Trafik uygulanmadığında kart temel Valhalla ETA'sını gösterir. Doğrulanmış trafik varsa adjusted ETA/delay gösterilebilir; trafik gecikmesi uydurulmaz.

## Bu turda gerçek değişiklikler — 2026-09-08

- `43e3f0963dcefffe9504f133f57db88bb2897082` — TomTom örnek noktaları için bounded 30 saniyelik in-memory cache eklendi; süresi dolmuş cache kullanılmıyor ve credential yoksa provider inert kalıyor.
- `7ee7b2a4a378e2ac3eacbb4f93fd3691189f3ebf` — cache hit/cooldown ve TTL sonrası gerçek HTTP refresh davranışını doğrulayan unit testler eklendi.
- Bu doküman güncellemesi ile yaşayan durum yeniden senkronize ediliyor.

## CI / APK — doğrulanmış

Önceki doğrulanmış build:
- Run `303` (`34274419418`) **success** ve HEAD `1609972c4320855869d7d89126d61ebbee64ef4d` için unit tests + debug APK build başarılı.
- Run `303` APK artifact: `haritalar-debug-apk-303`.
- Run `303` artifact içindeki `app-debug.apk` SHA-256: `cb599f1c47e871e3daec0c7705b98bf57da3b26909c9a697f1a794110fe3fe59`.

Bu turdaki yeni cache/test commitleri için yeni CI sonucu henüz doğrulanmış değildir. Bu nedenle bu yeni HEAD için APK hazır denmez.

## Güncel HEAD

`PROJECT_WORK_PROMPT.md` güncellemesi bu turdaki kod/test commitlerinin üzerine yeni bir commit oluşturur. Bu dosyanın güncelleme commit SHA'sı GitHub'dan tekrar doğrulanacaktır.

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
- Run 303 için başarılı unit tests + debug APK

## Açık işler / sonraki hedef

1. Yeni cache/test HEAD için CI'ı doğrula; başarısızsa düzelt.
2. Gerçek TomTom credential olmadan canlı trafik iddiası yapma; credential sağlandığında gerçek smoke/integration test yap.
3. Traffic refresh/cooldown/cache mekanizmasını provider-chain seviyesinde route-generation ve navigation refresh ile güvenli biçimde tamamla.
4. Navigation sırasında live traffic refresh → ranking → UI zincirini bağla.
5. Off-route/reroute sonrasında aynı traffic ranking/presentation zincirini kontrollü şekilde yeniden çalıştır.
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

## Sonraki Devam hedefi

**Yeni cache/test HEAD için CI sonucunu doğrula. Yeşil ise TomTom/provider-chain refresh kontrolünü navigation akışına bağlamak için mevcut navigation kodunu incele; GPS başına HTTP çağrısı yapılmayacak şekilde cooldown + stale generation + bounded background execution ile küçük bir entegrasyon yap. Gerçek credential yoksa inert/fallback davranışını koru.**
