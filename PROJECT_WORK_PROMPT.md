# HARİTALAR — YAŞAYAN PROJE ÇALIŞMA PROMPTU

Bu dosya yaşayan teknik hafızadır. Her gerçek geliştirme turunda GitHub'daki durumla senkronize edilir. Eski HEAD, CI, APK veya dosya bilgisi güncel durumla çelişiyorsa düzeltilir.

## Çalışma standardı

Her `Devam` işleminde:
1. `main` HEAD, branch ve son commitler GitHub'dan doğrulanır.
2. `PROJECT_WORK_PROMPT.md`, ilgili kod, testler, klasör yapısı ve CI okunur.
3. En kritik gerçek eksik seçilir; gereksiz tekrar yapılmaz.
4. Gerekiyorsa gerçek kod değişikliği yapılır.
5. Test çalıştırılır; commit GitHub'dan doğrulanır.
6. GitHub Actions sonucu kontrol edilir.
7. APK yalnızca gerçekten build/artifact/release doğrulanırsa hazır kabul edilir.
8. Tur sonunda bu dosya tekrar güncellenir.

## Gerçeklik / güvenlik

Kesinlikle sahte trafik, ETA, radar/EDS, POI, koordinat, API response, yol olayı veya feribot verisi üretilmez. Test fixture gerçek veri gibi gösterilmez. Web scraping trafik provider'ı olarak kullanılmaz. OSM canlı trafik kaynağı değildir. API secret public GitHub'a yazılmaz. Ücretli servis kullanıcı onayı olmadan etkinleştirilmez.

Kritik zincir hedefi:
`ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST`

## Repo yapısı

- `app/` — Android UI/platform
- `core/` — domain/data
- `core/src/main/kotlin/com/haritalar/core/traffic/` — trafik
- `core/src/main/kotlin/com/haritalar/core/safety/` — safety/radar
- `core/src/main/kotlin/com/haritalar/core/navigation/` — routing/navigation
- `core/src/main/kotlin/com/haritalar/core/data/` — veri
- `.github/` — CI/CD
- `app/src/main/java/com/haritalar/app/MainActivity.kt` — ana UI/akış

## Trafik mimarisi

Mevcut trafik sınıfları:
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

Hedef zincir:
`GERÇEK LIVE PROVIDER → ALTERNATİF → CACHE → FALLBACK → BASE VALHALLA ETA`

`TrafficProviderChain` provider desteği, priority, provider id, timestamp ve expiry kontrolleriyle çalışır. `TrafficRouteRanking` expired/provider-mismatch/LOW-confidence snapshot'ı trafik maliyetine uygulamaz. Geometry eşleşmeden trafik route'a uygulanmaz.

## TomTom — mevcut durum

`TomTomTrafficProvider.kt` gerçek TomTom Traffic Flow Segment Data endpointine credential-gated adapter sağlar.

- API key boşsa provider pasiftir.
- Secret kodda tutulmaz.
- Web scraping yoktur.
- Route geometry'den en fazla 8 örnek nokta alınır.
- Route yoksa bounds merkezi tek örnek olarak kullanılabilir.
- `currentSpeed`, `freeFlowSpeed` ve en az iki geçerli geometry noktası olmadan segment oluşturulmaz.
- Provider geometry `TrafficSegment.geometry` içine taşınır.
- Snapshot TTL: 60 saniye.
- Geçerli segment yoksa LOW confidence/boş segment döner; trafik uydurulmaz.
- HTTP bağlantısı `try/finally` ile kapatılır; Kotlin `Closeable.use` ile `HttpURLConnection` üzerinde hatalı kullanım yapılmaz.

Test:
`core/src/test/kotlin/com/haritalar/core/traffic/TomTomTrafficProviderTest.kt`

ÖNEMLİ: Adapter henüz MainActivity/uygulama configuration katmanına gerçek API key ile bağlanmış değildir. Bu yüzden canlı trafik ürün özelliği tamamlanmış değildir.

## Trafik sunumu — mevcut durum

`RouteTrafficPresentation.kt` ranking çıktısını UI-safe modele dönüştürür.

- Trafik uygulanmadıysa yalnızca base/adjusted duration gösterilir; uydurma gecikme yazılmaz.
- Trafik uygulanıp adjusted süre base süreden büyükse `47 dk • trafik +7 dk` biçimi desteklenir.
- Negatif trafik gecikmesi gösterilmez.

Test:
`core/src/test/kotlin/com/haritalar/core/traffic/RouteTrafficPresentationTest.kt`

Bu katman henüz MainActivity route card rendering'ine bağlanmış değildir.

## Routing / 7 rota

Valhalla gerçek routing motorudur. MainActivity şu hedef seçenekleri gerçek Valhalla istekleriyle üretir:
1. En hızlı
2. En kısa
3. Ücretsiz öncelikli
4. Ücretli hızlı
5. Feribot hızlı
6. Feribotsuz
7. Ücretsiz + feribotsuz

Feribot yalnızca gerçek Valhalla sonucunda varsa gösterilir. Toll/ferry tutarı bilinmiyorsa uydurulmaz.

## Navigation / safety

Mevcut MainActivity'de GPS, MapLibre, Nominatim arama, Valhalla routing, TTS, navigation progress, 60 m off-route eşiği ve 15 s reroute cooldown kodu bulunur. Radar/safety domain çekirdeği 5.000 m'den başlayıp 500 m'de biten 500 m aralıklı uyarı standardını kullanır.

## Güncel doğrulanmış durum — 2026-09-08

`main` HEAD:
`0d7181d19f97d4eb40027517078bc6bd1ef49132`

HEAD commit:
`test(traffic): cover safe route traffic presentation`

Bu turdaki gerçek değişiklik zinciri:
- `7f3a5b953954e7e62918d93084ef049ad634fddd` — `fix(traffic): close TomTom connection without Closeable use`
- `0f979d88f55bb1d2e8b41f0442e4e4d56fc33956` — `feat(traffic): add safe route traffic presentation`
- `0d7181d19f97d4eb40027517078bc6bd1ef49132` — `test(traffic): cover safe route traffic presentation`

Önceki dokümantasyon HEAD'i:
`3965f67bf34fc7ead3908bb8d458e97fd238d58f`

## Bu turda çözülen problem

GitHub Actions run 275 ve 276, TomTom provider içindeki `HttpURLConnection.use {}` kullanımında Kotlin derleme hatası vererek başarısız olmuştu. Hata `HttpURLConnection` için `Closeable.use` sözleşmesinin uygulanmamasından kaynaklanıyordu. Kod gerçek `try/finally` kapanışına çevrildi ve GitHub'a commit edildi.

## CI / APK — doğrulanmış gerçek durum

Workflow: `Android APK` (`.github/workflows/android.yml`).

- run `275` — `656d97dd...` — failure: TomTom `connection.use` compile error.
- run `276` — `1111ff0e...` — failure: aynı compile error.
- run `277` — `3965f67...` — failure: aynı eski TomTom compile error.
- run `278` — `7f3a5b95...` — bu kaydın başında tamamlanmış olması henüz doğrulanmadı; son kontrol sırasında CI ilerliyordu.
- run `279` — `0f979d88...` — son kontrolde `in_progress` idi.
- run `280` — `0d7181d1...` — son kontrolde `queued/in_progress` aşamasındaydı.

Bu nedenle yeni TomTom/presentation kodunu içeren başarılı CI sonucu henüz doğrulanmış değildir.

Son kesin doğrulanmış başarılı run:
- run `271`
- head `0b6b0139d06d31c4aea71bd0530286d6983250c5`
- conclusion `success`
- artifact `haritalar-debug-apk-271`
- test report `haritalar-unit-test-reports-271`

Yeni kodu içeren APK şu anda `hazır` kabul edilmez.

## Başarılı / mevcut

GitHub kodu ve doğrulanmış geçmiş CI ile mevcut:
- Android uygulama iskeleti
- MapLibre + OpenFreeMap
- GPS
- Nominatim arama
- Valhalla online routing
- 7 rota istekleri/route card akışı
- toll/ferry route seçenekleri
- Türkçe TTS/navigation çekirdeği
- safety/radar domain'i ve testleri
- trafik domain çekirdeği
- TrafficProviderChain
- TrafficRouteMatcher/Adapter/CostModel/Ranking/Intelligence/Orchestrator
- TomTom provider adapterı ve parser testleri kodda mevcut
- RouteTrafficPresentation modeli ve testleri kodda mevcut

Ancak yeni kodların CI sonucu kesinleşmeden yeni sürümün build edilmiş/başarılı olduğu söylenmez.

## Açık işler / sıradaki gerçek hedef

1. `0d7181d1...` için güncel CI sonucunu doğrula.
2. Başarılıysa yeni APK artifact/release çıktısını doğrula.
3. TomTom credential/configuration'ı secret güvenliğiyle uygulamaya bağla; kota/ücret/kullanım şartlarını doğrula.
4. Gerçek TomTom endpoint erişimini gerçek credential ile fixture'dan ayrı doğrula.
5. Refresh/cooldown/cache ile GPS başına gereksiz çağrıları engelle.
6. `TrafficProviderChain → TrafficRouteRanking → RouteTrafficPresentation → MainActivity` zincirini küçük bir entegrasyon katmanıyla bağla.
7. 7 route card için traffic-adjusted ETA ve ranking sıralamasını gerçek snapshot ile integration/smoke test et.
8. HERE ancak gerçek API erişimi ve kullanım şartları doğrulanırsa değerlendir.

## Gidilmeyecek yollar

- sahte trafik/ETA/radar/EDS/POI/API/koordinat
- test verisini canlı veri gibi göstermek
- OSM'yi canlı trafik sanmak
- web scraping
- public API key/secret
- ücretsiz kotayı sınırsız varsaymak
- kullanıcı onayı olmadan ücretli trafik servisi
- okunmadan MainActivity rewrite
- başarısız CI'ı başarılı göstermek
- build edilmemiş APK'yı hazır göstermek

## Referans kaynaklar

EGM EDS, İçişleri, KGM, İBB, TomTom, HERE, OpenStreetMap ve Valhalla kaynakları yalnızca gerçek makine-okunabilir API/veri erişimi doğrulanırsa entegrasyon kaynağı kabul edilir. Web sayfasının varlığı API varlığını kanıtlamaz.

## Sonraki hedef

**CI temizliğini tamamladıktan sonra gerçek TomTom snapshot akışını mevcut TrafficProviderChain ve TrafficRouteRanking üzerinden RouteTrafficPresentation'a, ardından MainActivity'nin gerçek 7 rota kartlarına bağlamak. Trafik verisi yoksa UI temel Valhalla ETA'sına aynen dönmeli; hiçbir yerde sentetik trafik farkı gösterilmemelidir.**
