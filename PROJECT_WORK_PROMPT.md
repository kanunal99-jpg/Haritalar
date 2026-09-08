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

Test dosyası:
`core/src/test/kotlin/com/haritalar/core/traffic/TomTomTrafficProviderTest.kt`

ÖNEMLİ: Adapter henüz MainActivity/uygulama configuration katmanına gerçek API key ile bağlanmış değildir. Bu yüzden canlı trafik ürün özelliği tamamlanmış değildir.

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
`1111ff0ef1d7302431efe7dce220fbc0a1365609`

HEAD commit:
`docs: synchronize living project head`

Bu tur başlangıcında GitHub'da doğrulanan önceki HEAD:
`0b6b0139d06d31c4aea71bd0530286d6983250c5`

Bu turdaki kod commitleri:
- `e779899d1a55db7f63d7d9d5146c29b71ec61c26` — TomTom provider ilk ekleme
- `0caa53716b2486a1b6cbd1b57e1947b5d0680834` — TomTom provider sabitleri düzeltildi
- `5b0a47ae83a91c56024eb0558c28c2829a1656c1` — TomTom provider testleri eklendi
- `656d97dd53e9587fcf795497dbd580714fbe12b2` — test coroutine helper düzeltmesi
- `1111ff0ef1d7302431efe7dce220fbc0a1365609` — bu yaşayan prompt senkronizasyonu

## CI / APK — doğrulanmış gerçek durum

GitHub Actions workflow: `Android APK` (`.github/workflows/android.yml`).

Bu HEAD için run `276` GitHub Actions'ta tetiklenmiş olmalıdır; sonuç bu kayıt güncellenirken ayrıca doğrulanmamıştır. Son doğrulanmış kod CI run'ı:
- run `275`
- head `656d97dd53e9587fcf795497dbd580714fbe12b2`
- durum: önceki kontrolde `queued/in_progress` aşamasındaydı; başarılı olarak raporlanmamıştır.

Son kesin doğrulanmış başarılı run:
- run `271`
- head `0b6b0139d06d31c4aea71bd0530286d6983250c5`
- conclusion: `success`
- artifact: `haritalar-debug-apk-271`
- test report: `haritalar-unit-test-reports-271`

Bu nedenle TomTom değişikliklerini içeren yeni APK şu anda `hazır` kabul edilmez.

## Başarılı / mevcut

GitHub kodu ve önceki başarılı CI ile doğrulananlar:
- Android uygulama iskeleti
- MapLibre + OpenFreeMap
- GPS
- Nominatim arama
- Valhalla online routing
- 7 rota isteği/route card hedefi
- toll/ferry route seçenekleri
- Türkçe TTS/navigation çekirdeği
- safety/radar domain'i ve testleri
- trafik domain çekirdeği
- TrafficProviderChain
- TrafficRouteMatcher/Adapter/CostModel/Ranking/Intelligence/Orchestrator
- debug APK + unit test CI zinciri

TomTom adapter kodu ve parser testleri eklendi; yeni kodun CI sonucu kesinleşmeden bu madde ürün olarak tamamlanmış sayılmaz.

## Açık işler / sıradaki gerçek hedef

1. `main` HEAD `1111ff0e...` için GitHub Actions run sonucunu doğrula.
2. Başarılıysa yeni APK artifact'ını doğrula.
3. TomTom credential/configuration'ı secret güvenliğiyle uygulamaya bağla; kota/ücret/kullanım şartlarını doğrula.
4. Gerçek TomTom endpoint erişimini gerçek credential ile, fixture'dan ayrı olarak doğrula.
5. Refresh/cooldown/cache ile gereksiz GPS başına çağrıları engelle.
6. `TrafficProviderChain → TrafficRouteRanking → RouteTrafficPresentation → MainActivity` zincirini gerçek snapshot ile bağla.
7. 7 route card'ın traffic-adjusted ETA ve sıralamasını gerçek veriye bağlayan integration/smoke test ekle.
8. Sonraki provider olarak HERE ancak gerçek API erişimi ve kullanım şartları doğrulanırsa değerlendir.

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
