# HARİTALAR — YAŞAYAN PROJE ÇALIŞMA PROMPTU

Bu dosya projenin **çalışma anayasasıdır**. Her `Devam` işleminde yalnızca bu dosyaya değil, ayrıntılı proje hafızasına ve append-only işlem günlüğüne de bakılır.

## 0. ZORUNLU OKU → KONTROL ET → ÇALIŞ → KAYDET KURALI

Her `Devam` veya herhangi bir proje operasyonundan **önce** mutlaka:

1. `PROJECT_WORK_PROMPT.md` oku.
2. `PROJECT_DETAILS.md` oku.
3. `docs/PROJECT_ACTIVITY_LOG.md` son kayıtlarını oku.
4. GitHub `main` branch gerçek HEAD'ini kontrol et.
5. Son commitleri ve ilgili dosyaları gerçek GitHub'dan kontrol et.
6. Önceki iş planı ile gerçek kod durumunu karşılaştır.
7. En kritik gerçek eksiği seç.
8. Gerçek değişikliği yap.
9. Etkilenen testleri çalıştır/doğrula.
10. Commit oluştur.
11. GitHub'da commit ve dosyaları tekrar doğrula.
12. CI sonucunu gerçek GitHub'dan kontrol et.
13. APK etkileniyorsa gerçek artifact/build sonucunu kontrol et.
14. **Yapılan işlemi, en küçük değişiklik dahil, `docs/PROJECT_ACTIVITY_LOG.md` içine yaz.**
15. Proje durumu değiştiyse `PROJECT_DETAILS.md` ve gerekirse bu dosyayı güncelle.

Ana döngü:

`DOSYALARI OKU → GEÇMİŞİ KONTROL ET → GITHUB GERÇEKLİĞİ → PLANLA → EN KRİTİK GERÇEK EKSİK → DEĞİŞTİR → TEST → COMMIT → GITHUB DOĞRULAMA → CI → APK → LOG → HAFIZAYI SENKRONİZE ET`

**Bir virgül/noktalama değişikliği dahi işlem olarak kabul edilir ve günlüğe kaydedilir.** Başarısız denemeler de kaydedilir; silinmez.

## 1. Gerçeklik standardı

- Yapılmayan iş yapılmış gibi anlatılmaz.
- Çalıştırılmayan test çalıştırılmış gibi anlatılmaz.
- Başarısız CI başarılı gibi anlatılmaz.
- Build edilmemiş APK hazır/indirilebilir gibi ilan edilmez.
- Fiziksel cihaz testi yapılmadıysa yapılmış gibi söylenmez.
- Plan, gerçek uygulama durumu yerine geçirilmez.
- Eski HEAD güncel kabul edilmez.
- GitHub'da doğrulanmayan commit/run/artifact bilgisi kesin gerçek gibi yazılmaz.

## 2. Güvenlik / veri doğruluğu

Sahte trafik, ETA, radar/EDS, POI, koordinat, API response veya canlı veri üretilmez. Test fixture canlı veri değildir. OSM canlı trafik kaynağı değildir. Web scraping provider değildir. API key/secret public kaynak koda yazılmaz. Ücretli servis kullanıcı onayı olmadan etkinleştirilmez. Ücretsiz kota sınırsız varsayılmaz.

## 3. Dayanıklılık standardı

Kritik zincir:

`ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST`

## 4. Proje hafıza dosyaları

### `PROJECT_WORK_PROMPT.md`

Çalışma anayasasıdır. Her işlemden önce okunur.

### `PROJECT_DETAILS.md`

Ürünün amacı, vizyonu, mimarisi, özellikleri, teknik kararları, geçmişi, doğrulanmış CI/APK durumu, açık işleri ve kabul kriterlerini içerir.

### `docs/PROJECT_ACTIVITY_LOG.md`

Append-only operasyon günlüğüdür. **Her işlem** burada kayıt altına alınır. Geçmiş kayıtlar sessizce değiştirilmez; düzeltme yeni kayıt olarak yapılır.

## 5. Ürün / mimari

- Android Kotlin uygulaması.
- `app/` UI/platform, `core/` domain/data.
- MapLibre + OpenFreeMap.
- Nominatim geocoding.
- Valhalla routing.
- MainActivity gerçek Valhalla sonuçlarından 7 rota alternatifi üretir: en hızlı, en kısa, ücretsiz öncelikli, hızlı ücretli, hızlı feribotlu, feribotsuz, ücretsiz+feribotsuz.
- GPS, navigation progress, Türkçe TTS, yaklaşık 60 m off-route ve yaklaşık 15 s reroute cooldown mevcuttur.
- Toll/ferry yalnız gerçek routing sonucu veya doğrulanmış hesapla gösterilir.

## 6. Trafik çekirdeği

`core/src/main/kotlin/com/haritalar/core/traffic/` altında TrafficProvider, TrafficProviderChain, TrafficRouteMatcher, TrafficRouteAdapter, TrafficRouteCostModel, TrafficRouteRanking, RouteTrafficIntelligence, TrafficRouteIntelligence, TrafficRouteOrchestrator, TomTomTrafficProvider, TrafficRouteRankingService ve TrafficRefreshCoordinator bulunur.

`TrafficRouteRankingService` route seti için en fazla bir provider-chain snapshot alır ve geometry-aware ranking uygular. Empty/invalid route, provider/network failure, expired/mismatch/LOW-confidence trafik durumlarında base ETA korunur. `rankBlocking()` bounded yaklaşık 120 saniyelik bridge'dir ve ana thread'de kullanılmaz.

`TrafficRefreshCoordinator` navigation/route-card çağrılarını koordine eder. Varsayılan minimum refresh 60 saniyedir. Tek in-flight ranking korunur. `reset()` cooldown'u temizler ve çalışan eski sonucu `Stale` yapar. Coordinator provider/fallback mantığını kopyalamaz; canonical ranking fonksiyonunu kullanır.

`refreshBlocking()` mevcut background executor sahibi Android çağrıları için bridge'dir ve ana thread'de kullanılmaz.

## 7. TomTom

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointine yönelik adapterdır. Key boşsa pasiftir; scraping yoktur; validation vardır. `TOMTOM_API_KEY` Gradle property veya environment variable'dan alınır ve `BuildConfig.TOMTOM_API_KEY` ile kullanılır.

Factory zinciri:

`BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService`

Gerçek credential sağlanmadığı için canlı TomTom trafiğinin aktif olduğu iddia edilmez. Key yokken base Valhalla ETA korunur.

Route başına en fazla 8 sample noktası kullanılır. Aynı yaklaşık 1e-6 derece sample noktası 30 saniyelik kısa provider-cache içinde tekrar kullanılabilir. Cache sentetik veri üretmez.

## 8. Route-card trafik entegrasyonu

7 route generation tamamlandıktan sonra tek candidate set üzerinden `TrafficRefreshCoordinator.refreshBlocking()` ile canonical `TrafficRouteRankingService.rank()` çağrılır. `routeGeneration` stale sonuçları UI'dan korur.

`RouteTrafficPresentation` artık ranked candidate ile aynı `routeId` üzerinden eşleştirilir; positional zip kullanılmaz.

Trafik uygulanmazsa base Valhalla ETA gösterilir. Doğrulanmış trafik varsa adjusted ETA/delay gösterilir.

## 9. Navigation trafik entegrasyonu

GPS callback doğrudan provider/HTTP çağırmaz. Coordinator tetiklenir; ranking background `routeExecutor` üzerinde çalışır.

Zincir:

`GPS → caller-side AtomicBoolean gate → background executor → TrafficRefreshCoordinator → canonical ranking → routeGeneration guard → UI state`

Aktif route seti `trafficRouteOptions` olarak tutulur. Sonuç yalnız aynı generation aktifse uygulanır. Route kartları navigation sırasında gereksiz yere yeniden açılmaz. `lastTrafficByRoute` cache'i güncellenir.

Navigation refresh submit tarafında `trafficRefreshTaskInFlight` atomik gate'i aynı anda birden fazla bekleyen executor task'ını engeller. Coordinator'ın mevcut `inFlight` ve 60 saniyelik cooldown koruması ikinci savunma katmanı olarak korunur.

Yeni hedef/routing `reset()` ile eski traffic state'ini temizler. Off-route/reroute generation artırır, coordinator resetlenir ve yeni rota için kontrollü refresh başlar. Arrival/stop traffic state'i temizler.

## 10. Geçmişten bugüne doğrulanmış teknik zincir

### 2026-09-08

- `98b6854a35fc27581786c7541462df0fd426162d` — resilient provider chain.
- `772cb6994ffcc785be5fd1d57e4f5c20fb05301e` — provider chain fallback test.
- `762808033732ec20835f1bda111d6927bdd52bc0` — test coroutine dependency sadeleştirmesi.
- `4e2fdb6ca8edb68e79fc89d3075ebf0ef64281f4` — route ETA cost model.

### 2026-09-09

- `af9ffcb99d965978132fdea4a2c9cc0b93c1891f` — blocking bridge.
- `1b869fdf4fdee70a6281dba3b3d5251c37956a9b` — Result generic fix.
- `7d18119cc1d309ee55bdd46ec85adcda3d6cd29b` — navigation integration.
- `fe39634743175c4ca839314c6cda357e85bb69f1` — blocking bridge unit coverage.
- `b43cb23605b37d446e58535ead260a2d4e39fe7d` — documentation sync.
- `b37178681b9b52f33358ee95ef757ae2c67c6fa1` — navigation traffic refresh task coalescing gate.
- `161098fa08712ed80c19fb0006beb61331235778` — coordinator generation/in-flight race tests.
- `a55b1387df2e78d83108a9a37a63223822363843` — race test typing fix.
- `1af18dd05400797bd748c43a8d133923a656f274` — activity log sync for race-test failure/fix.
- `d1369ac746d0fdb96858cc9c82580e6d819fcddd` — project details sync with current race coverage/backlog.

## 11. CI / APK — güncel gerçek durum

Önceden GitHub Actions'tan doğrulanmış yeşil kilometre taşı:

- Run: **335**
- Run ID: `34339025583`
- Workflow: `Android APK`
- HEAD: `b43cb23605b37d446e58535ead260a2d4e39fe7d`
- Status: `completed`
- Conclusion: **success**
- Unit tests: success.
- Debug APK build: success.
- Artifact upload: success.
- Artifact: `haritalar-debug-apk-335`.
- Artifact digest: `sha256:f4b103f0f0bba2cd4f64ce2f5b13b45841d898193a25e7478c25eea9cfcc94df`.
- Artifact size: `21,665,141` bytes.
- Expiration: 2026-12-08.

Bu CI sonucu APK artifact üretildiğini doğrular; fiziksel cihaz kurulumu/testi anlamına gelmez.

## 12. Son CI olayları

- Run `343` / ID `34342979779`: navigation traffic coalescing commit'i için çalıştı ve sonraki proje kayıtlarında **success** olarak doğrulandı; debug APK artifact'i üretildi.
- Run `348` / ID `34344278516`: coordinator race test ilk denemesi **failure**. Kotlin test derleme hatası nedeniyle APK adımları çalışmadı.
- Run `350` / ID `34344440387`: `a55b1387...` race-test typing fix için başlatıldı; sonuç kesinleşmeden success kabul edilmez.
- Run `351` / ID `34344489681`: `1af18dd...` activity-log commit'i için queued durumda gözlendi; sonuç kesinleşmeden success kabul edilmez.

## 13. Açık işler — öncelik

### P0

1. Önemli her değişiklikten sonra güncel GitHub CI/APK sonucunu gerçek kaynaktan doğrula.
2. Gerçek TomTom credential verilirse gerçek smoke/integration testi yap.
3. Secret güvenliğini koru.
4. Traffic failure → base ETA fallback testlerini koru.

### P1

5. GPS → coordinator → executor submit davranışının gerçek cihaz/performance etkisini ölç.
6. Navigation traffic integration/smoke coverage artır.
7. ~~Generation/stale testlerini genişlet.~~ **Coordinator-level stale/race coverage genişletildi; navigation/UI integration coverage hâlâ açık.**
8. ~~Coalescing gate'in lifecycle/race davranışını testlerle genişlet.~~ **Coordinator in-flight race testleri eklendi; caller-side gate için lifecycle/submit-rejection ve gerçek cihaz coverage ayrıca değerlendirilecek.**

### P2

9. HERE yalnız erişim/auth/quota/terms doğrulanırsa değerlendir.
10. İkinci gerçek provider eklenirse chain fallback testlerini genişlet.

### P3

11. Gerçek POI kaynakları.
12. Gerçek makine-okunabilir safety/radar/EDS kaynakları.
13. Gerçek offline veri kapsamının doğrulanması.
14. Cache stratejisi/performance.

## 14. Mikro değişiklik kuralı

Bir satır, bir karakter, import, test, config, workflow, doküman, commit, başarısız deneme, rollback, CI sonucu veya karar değişikliği dahi kayıt kapsamındadır.

**Her işlem sonunda activity log güncellenir.**

## 15. Gidilmeyecek yollar

- Sahte trafik/ETA/radar/EDS/POI/koordinat/API.
- Test verisini canlı veri diye göstermek.
- OSM'yi canlı trafik sanmak.
- Scraping.
- Public secret/API key.
- Sınırsız ücretsiz kota varsayımı.
- Kullanıcı onayı olmadan ücretli servis.
- Okunmadan MainActivity rewrite.
- Başarısız CI'ı başarılı göstermek.
- Build edilmemiş APK'yı hazır göstermek.
- Cihaz testini yapılmış gibi göstermek.
- GPS başına provider HTTP.
- Stale sonucu yeni generation'a uygulamak.
- Büyük dosyada içerik doğrulanmadan full-content overwrite.

## 16. Güncel sonraki hedef

**Bir sonraki `Devam` işleminde önce üç hafıza dosyası ve gerçek GitHub HEAD okunacak. Ardından Run 350 ve Run 351 sonuçları gerçek GitHub'dan kesinleştirilecek. Run 350 başarılıysa yeni APK artifact'i doğrulanacak; başarısızsa gerçek CI hatası düzeltilip tekrar doğrulanacak. Sonrasında navigation/UI traffic integration-smoke coverage ve gerçek cihaz/performance/battery profiling önceliklendirilecek.**
