# HARİTALAR — PROJE DETAYLARI / TEKNİK HAFIZA

> Bu dosya projenin geçmişten bugüne yaşayan ana teknik hafızasıdır. Amaç yalnızca ne yapıldığını değil; neden yapıldığını, hangi kararların alındığını, hangi hataların görüldüğünü, hangi doğrulamaların gerçekten yapıldığını ve sıradaki işin ne olduğunu kaybetmemektir.
>
> **Zorunlu çalışma kuralı:** Her yeni `Devam` veya herhangi bir geliştirme işleminden önce bu dosya ve `PROJECT_WORK_PROMPT.md` okunur; ardından `docs/PROJECT_ACTIVITY_LOG.md` içindeki son kayıtlar incelenir. Sonra GitHub gerçekliği kontrol edilir. İş tamamlandıktan sonra faaliyet günlüğüne kayıt eklenir.

---

## 1. Proje kimliği

- Proje: **HARİTALAR Android Navigasyon Platformu**
- Repository: `kanunal99-jpg/Haritalar`
- Ana branch: `main`
- Platform: Android
- Dil: Kotlin
- Proje hedefi: Profesyonel, gerçek veri kullanan, güvenli, dayanıklı ve mümkün olduğunca çevrimdışı çalışabilen harita/navigasyon uygulaması.
- Repository açıklaması: `Ai özellikli çevrimdışı çalışabilen harita`
- Çalışma biçimi: Gerçek GitHub kodu → gerçek test → gerçek CI → gerçek APK/artifact doğrulaması.

---

## 2. Ürün vizyonu

HARİTALAR'ın hedefi basit bir harita ekranı değil; kullanıcıyı gerçek bir yolculuk boyunca destekleyen tam navigasyon platformudur.

Uzun vadeli ürün hedefleri:

1. Harita üzerinde konum ve rota gösterimi.
2. Gerçek routing servisleri ile güvenilir rota hesaplama.
3. Birden fazla rota seçeneğini anlamlı kriterlerle karşılaştırma.
4. GPS tabanlı canlı navigasyon.
5. Türkçe sesli yönlendirme.
6. Rota dışına çıkıldığında güvenli ve kontrollü yeniden rota.
7. Feribot ve ücretli yol gibi rota özelliklerini gerçek routing sonucundan çıkarmak.
8. Gerçek trafik verisi mevcut olduğunda ETA/ranking'i trafik ile iyileştirmek.
9. Trafik sağlayıcısı çalışmadığında temel Valhalla ETA'sını korumak.
10. İleride doğrulanmış POI, güvenlik/radar ve başka veri katmanlarını yalnız gerçek ve uygun kaynaklarla eklemek.
11. Ağ kesintisi veya dış servis problemi uygulamanın temel navigasyon işlevini gereksiz yere tamamen kaybetmesine yol açmamak.

---

## 3. Ürün ilkeleri

### 3.1 Gerçeklik ilkesi

- Sahte trafik üretilmez.
- Sahte ETA üretilmez.
- Sahte radar/EDS verisi üretilmez.
- Sahte POI/koordinat/API cevabı üretilmez.
- Test fixture canlı veri değildir.
- OSM harita/veri kaynağı olabilir; **OSM canlı trafik kaynağı değildir**.
- Web scraping veri sağlayıcı kabul edilmez.
- Bilinmeyen ücretler tahmin edilerek gerçekmiş gibi gösterilmez.

### 3.2 Maliyet ilkesi

Öncelik sırası:

`yerel/açık kaynak → ücretsiz/resmi free tier → doğrulanmış harici servis → ücretli servis`

Kullanıcı açıkça onaylamadan ücret doğurabilecek servis etkinleştirilmez. Ücretsiz kotanın sınırsız olduğu varsayılmaz.

### 3.3 Güvenlik ilkesi

- Secret/API key kaynak koda gömülmez.
- Provider credential BuildConfig/env/property üzerinden beslenir.
- Public repository'ye gerçek secret yazılmaz.
- Dış veri doğrulanmadan UI'a güvenilir gerçek olarak aktarılmaz.
- Kritik işlemlerde güvenli varsayılan davranış korunur.

### 3.4 Dayanıklılık ilkesi

Kritik zincir:

`ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST`

Örnek: trafik servisi yoksa navigasyon durmaz; base Valhalla ETA korunur.

---

## 4. Mimari

### Uygulama katmanı

- `app/`: Android UI ve platform entegrasyonları.
- `core/`: domain/data ve trafik/routing gibi çekirdek mantık.

### Harita

- MapLibre
- OpenFreeMap

### Arama / geocoding

- Nominatim

### Routing

- Valhalla

### Trafik

- Provider abstraction
- Provider chain
- Geometry-aware route matching
- ETA cost model
- Ranking
- Traffic intelligence/orchestration
- Refresh coordination
- TomTom adapter

---

## 5. Temel navigasyon yetenekleri

Mevcut mimaride bulunan temel zincir:

`GPS → route progress → off-route kontrolü → reroute → Türkçe TTS → arrival/stop`

Bilinen davranışlar:

- GPS konumu alınır.
- Navigasyon ilerlemesi izlenir.
- Türkçe sesli yönlendirme vardır.
- Yaklaşık 60 m off-route eşiği kullanılır.
- Reroute için yaklaşık 15 saniyelik cooldown vardır.
- Off-route olduğunda yeni route generation oluşturulur.
- Eski generation'a ait sonuçların yeni rotaya yazılması engellenir.

---

## 6. Yedi rota yaklaşımı

MainActivity gerçek Valhalla sonuçlarından yedi alternatif üretir:

1. En hızlı.
2. En kısa.
3. Ücretsiz yol öncelikli.
4. Hızlı ücretli.
5. Hızlı feribotlu.
6. Feribotsuz.
7. Ücretsiz + feribotsuz.

Kriterler gerçek routing sonucuna dayanır. Uydurma toll/ferry değeri kullanılmaz.

### Rota kimliği

Trafik ranking sonucunda sıralama değişebildiği için UI modelleri artık liste pozisyonuna göre değil, aynı `routeId` üzerinden eşleştirilir. Bu, adjusted ETA sonrası oluşabilecek yanlış kart eşleşmesini önler.

---

## 7. Trafik çekirdeği

`core/src/main/kotlin/com/haritalar/core/traffic/` altında aşağıdaki ana parçalar bulunur:

- `TrafficProvider`
- `TrafficProviderChain`
- `TrafficRouteMatcher`
- `TrafficRouteAdapter`
- `TrafficRouteCostModel`
- `TrafficRouteRanking`
- `RouteTrafficIntelligence`
- `TrafficRouteIntelligence`
- `TrafficRouteOrchestrator`
- `TomTomTrafficProvider`
- `TrafficRouteRankingService`
- `TrafficRefreshCoordinator`

### TrafficProviderChain

Amaç, birden fazla provider/fallback katmanını tek bir güvenli zincirde yönetmektir. Provider hatası bütün navigasyonu bozmaz.

### TrafficRouteMatcher

Trafik segmentlerini rota geometrisi ile eşleştirmek için geometry-aware yaklaşım kullanılır. Trafiğin alakasız bir segmente uygulanması engellenmeye çalışılır.

### TrafficRouteCostModel

Gerçek trafik bilgisi olduğunda ETA maliyetini/ranking girdisini hesaplar. Trafik verisi yoksa temel rota ETA'sı korunur.

### TrafficRouteRanking

Rotaları doğrulanmış trafik etkisi ve temel ETA gibi girdilerle sıralar. Empty/invalid/provider failure/expired/mismatch/LOW-confidence durumlarında güvenli biçimde base ETA korunur.

### TrafficRouteRankingService

- Route seti için en fazla bir provider-chain snapshot alır.
- Geometry-aware ranking uygular.
- Provider/network failure durumunda base ETA'ya döner.
- Expired/mismatch/LOW-confidence traffic sonucu güvenilir kabul edilmez.
- `rankBlocking()` bounded yaklaşık 120 saniyelik bridge'dir.
- Blocking bridge ana thread'de kullanılmamalıdır.

---

## 8. TrafficRefreshCoordinator

Bu bileşen navigation ve route-card çağrılarında trafik refresh zamanlamasını merkezi hale getirir.

Temel kurallar:

- Varsayılan minimum refresh aralığı: **60 saniye**.
- Tekil in-flight ranking isteği korunur.
- `reset()` cooldown'u temizler.
- `reset()` eski çalışan ranking sonucunu `Stale` hale getirir.
- Route generation değiştiğinde eski sonuç yeni rotaya uygulanamaz.
- Coordinator provider/fallback mantığını kopyalamaz.
- Canonical ranking fonksiyonunu kullanır.
- `refreshBlocking()` mevcut background executor kullanan Android çağrıları için bounded bridge'dir.
- `refreshBlocking()` ana thread'de çağrılmaz.

---

## 9. Navigation sırasında trafik

MainActivity navigation GPS callback'i artık doğrudan HTTP/provider çağrısı yapmaz; coordinator'ı tetikler.

Zincir:

`GPS callback → TrafficRefreshCoordinator → routeExecutor/background → canonical ranking → routeGeneration guard → UI state`

Kurallar:

- GPS callback içinde provider HTTP çalışmaz.
- Trafik ranking background executor üzerinde çalışır.
- Aktif route seti `trafficRouteOptions` olarak tutulur.
- Tek candidate snapshot üzerinden canonical ranking kullanılır.
- Sonuç yalnız aktif `routeGeneration` ile aynıysa UI'a uygulanır.
- Navigation aktifken route kartı gereksiz yere yeniden açılmaz.
- `lastTrafficByRoute` cache'i güncellenebilir.
- Yeni hedef/routing başlatıldığında traffic state resetlenir.
- Off-route/reroute yeni generation üretir ve coordinator resetlenir.
- Arrival/navigation stop trafik state'ini temizler.

---

## 10. TomTom entegrasyonu

`TomTomTrafficProvider` gerçek TomTom Flow Segment Data endpointine yönelik adapterdır.

Kurallar:

- Key boşsa provider pasiftir.
- Scraping yapılmaz.
- Segment/snapshot validation vardır.
- `TOMTOM_API_KEY` Gradle property veya environment variable'dan alınır.
- `BuildConfig.TOMTOM_API_KEY` üzerinden uygulamaya aktarılır.
- Factory zinciri: `BuildConfig → TomTomTrafficProvider → TrafficProviderChain → TrafficRouteRankingService`.
- Gerçek credential verilmediği için canlı TomTom trafiğinin şu an aktif olduğu iddia edilmez.
- Key yokken base Valhalla ETA korunur.

### TomTom örnekleme/cache

- Route başına en fazla 8 örnek nokta.
- Yaklaşık 1e-6 derece seviyesinde aynı sample noktası 30 saniyelik kısa cache içinde tekrar HTTP çağrısı gerektirmez.
- Cache yalnızca gerçek provider cevabını tutar.
- Sentetik trafik üretmez.
- Cache süresi dolunca gerçek provider yeniden sorgulanır.

---

## 11. Güvenli trafik UI davranışı

Trafik uygulanmadığında:

`UI ETA = base Valhalla ETA`

Doğrulanmış trafik olduğunda:

`UI ETA = adjusted ETA`

Delay yalnız gerçek trafik hesabından geldiğinde gösterilir. Trafik gecikmesi uydurulmaz.

---

## 12. POI / güvenlik / radar vizyonu

POI, radar ve EDS gibi katmanlar ürün vizyonunun parçası olabilir; ancak veri kaynağı gerçek, makine-okunabilir, hukuken/teknik olarak uygun ve doğrulanmış olmadıkça canlı veri gibi gösterilmez.

Gelecekte kabul kriterleri:

- Kaynak kimliği bilinir.
- Güncelleme yöntemi bilinir.
- Lisans/kullanım şartları doğrulanır.
- Veri formatı makine-okunabilir olur.
- Fallback davranışı vardır.
- Stale/expired veri güvenli biçimde ele alınır.
- UI'da doğrulanmamış kesinlik iddiası yapılmaz.

---

## 13. Offline vizyonu

Uygulama temel harita/navigasyon zincirinin dış servis arızalarında dayanıklı olmalıdır. Gerçek offline routing/map-data kapsamı ayrıca doğrulanmalı ve yalnız implement edilmiş davranışlar mevcut kabul edilmelidir.

Offline iddiası ile ilgili hiçbir özellik yalnız dokümana bakılarak "tamamlandı" sayılmaz; gerçek kod ve test ile doğrulanır.

---

## 14. Performans ve batarya

Özellikle navigation GPS callback kritik yoldur.

Asla:

- GPS başına provider HTTP çağrısı yapılmaz.
- Main thread'de blocking ranking yapılmaz.
- Aynı route seti için gereksiz paralel traffic snapshot'ları başlatılmaz.
- Stale sonuç yeni generation'a uygulanmaz.

Navigation traffic refresh submit zincirinde artık ek bir caller-side atomik gate vardır:

`GPS event → AtomicBoolean gate → routeExecutor → TrafficRefreshCoordinator`

`trafficRefreshTaskInFlight` aynı anda birden fazla navigation refresh task'ının executor kuyruğuna eklenmesini engeller. Coordinator'ın kendi `inFlight` koruması bunun altında ikinci savunma katmanı olarak kalır. Task tamamlanınca gate `finally` ile serbest bırakılır; executor submit reddedilirse gate geri alınır.

Bu gate'in gerçek cihaz/batarya etkisi henüz ölçülmüş kabul edilmez; sonraki adım instrumentation/profile veya cihaz smoke testi ile gözlemlenebilir.

---

## 15. Geçmişten bugüne teknik ilerleme

Aşağıdaki kronoloji, GitHub'da doğrulanabilen son teknik geliştirme zinciri ve önceki proje hafızasında bulunan doğrulanmış kilometre taşlarının birleşimidir. Eski dönemlerde exact commit/date bilgisi mevcut olmayan maddeler **geçmişten taşınan özet** olarak değerlendirilir; uydurma commit/date oluşturulmaz.

### Temel ürün dönemleri — geçmişten taşınan özet

- Android Kotlin tabanlı uygulama iskeleti oluşturuldu.
- MapLibre/OpenFreeMap harita katmanı kuruldu.
- Nominatim arama/geocoding eklendi.
- Valhalla routing altyapısı kuruldu.
- Birden fazla rota ve rota seçenekleri geliştirildi.
- Toll/ferry ayrıştırması gerçek routing sonucuna bağlandı.
- GPS navigasyon ve progress zinciri geliştirildi.
- Türkçe TTS yönlendirmesi eklendi.
- Off-route algılama ve kontrollü reroute geliştirildi.
- Safety/radar domain temeli oluşturuldu.
- Trafik için provider abstraction ve dayanıklı provider chain tasarlandı.
- Geometry-aware traffic matcher/ranking geliştirildi.
- TomTom provider adapterı ve güvenli credential yapılandırması eklendi.
- Trafik route-card presentation katmanı oluşturuldu.
- TomTom sample request sınırı ve kısa süreli cache eklendi.

### 2026-09-08 — trafik çekirdeğinin derinleştirilmesi

Doğrulanabilen GitHub commit zincirinde:

- `98b6854a35fc27581786c7541462df0fd426162d` — `feat(traffic): add resilient provider chain`
- `772cb6994ffcc785be5fd1d57e4f5c20fb05301e` — `test(traffic): verify provider chain fallback behavior`
- `762808033732ec20835f1bda111d6927bdd52bc0` — `test(traffic): avoid extra coroutine test dependency`
- `4e2fdb6ca8edb68e79fc89d3075ebf0ef64281f4` — `feat(traffic): add route ETA cost model`

Bu dönem trafik sağlayıcı zinciri, fallback davranışı, test altyapısı ve ETA cost modelinin sağlamlaştırıldığı dönemdir.

### 2026-09-09 — refresh coordination ve navigation entegrasyonu

Doğrulanmış commit zinciri:

- `af9ffcb99d965978132fdea4a2c9cc0b93c1891f` — `TrafficRefreshCoordinator` için ilk blocking bridge.
- `1b869fdf4fdee70a6281dba3b3d5251c37956a9b` — coordinator coroutine/result type düzeltmesi.
- `7d18119cc1d309ee55bdd46ec85adcda3d6cd29b` — coordinator navigation akışına bağlandı.
- `fe39634743175c4ca839314c6cda357e85bb69f1` — `refreshBlocking()` unit coverage.
- `b43cb23605b37d446e58535ead260a2d4e39fe7d` — documentation sync.

GitHub compare ile doğrulanan `c66c1d3` → `fe396347` aralığında 4 commit ve 3 dosya değişti:

- `app/src/main/java/com/haritalar/app/MainActivity.kt`
- `core/src/main/kotlin/com/haritalar/core/traffic/TrafficRefreshCoordinator.kt`
- `core/src/test/kotlin/com/haritalar/core/traffic/TrafficRefreshCoordinatorTest.kt`

### 2026-09-09 — navigation traffic submit coalescing

- `b37178681b9b52f33358ee95ef757ae2c67c6fa1` — `perf: coalesce navigation traffic refresh tasks`
- `MainActivity.kt` içine caller-side `AtomicBoolean` gate eklendi.
- Amaç: GPS event'leri sırasında aynı anda bekleyen traffic refresh executor task'larını azaltmak.
- Coordinator'ın mevcut in-flight/cooldown koruması kaldırılmadı; iki katman birlikte çalışıyor.
- Kod GitHub'da commit sonrası tekrar doğrulandı.
- CI Run `343` bu commit için başlatıldı; sonuç gözlem sırasında `in_progress` idi.

---

## 16. Önemli hata/öğrenim kayıtları

### Kotlin Result generic çakışması

`TrafficRefreshCoordinator` blocking bridge geliştirilirken Kotlin `Result` generic kullanımında çakışma oluştu. `1b869fdf...` commit'i ile düzeltildi. Bu, bridge API'sinde tiplerin açık tutulmasının önemini gösterdi.

### Positional route presentation hatası

Traffic ranking adjusted ETA'ya göre route sırasını değiştirebildiğinden, input listesi ile ranked listeyi pozisyon bazında zip etmek hatalıydı. Çözüm: `routeId` üzerinden eşleştirme.

### Stale generation riski

Navigation sırasında eski traffic request yeni route'a dönebilir. Çözüm: `routeGeneration` guard + coordinator `reset()` + stale invalidation.

### Main thread blocking riski

`rankBlocking()` ve `refreshBlocking()` yalnız mevcut background executor bağlamında kullanılmalıdır. UI/main thread'de kullanılmaz.

### Canlı trafik ile test verisini karıştırmama

Provider key yokken base ETA korunur. Test fixture'ları canlı trafik olarak sunulmaz.

### MainActivity overwrite recovery olayı

2026-09-09 tarihinde çalışma sırasında `MainActivity.kt` yanlışlıkla `__PLACEHOLDER__` ile değiştirildi. Hatalı commit `ae493b6b45444df62d41d5bf3ad5241db8101144` idi. Önceki blob/tree doğrulanarak recovery commit'i `a467415ff40862bc7ad89b79cd1cc0eca25bf45c` ile dosya restore edildi. Olay activity log'a kaydedildi ve sonrasında gerçek coalescing değişikliği ayrı commit olarak yapıldı.

---

## 17. CI/CD ve APK doğrulama geçmişi

### Run 303

- Run: `303`
- Run ID: `34274419418`
- Başarılı unit tests + debug APK doğrulandı.
- APK artifact: `haritalar-debug-apk-303`
- `app-debug.apk` SHA-256: `cb599f1c47e871e3daec0c7705b98bf57da3b26909c9a697f1a794110fe3fe59`

### Run 308

- HEAD: `2c34feb8d8ef56850fd718920165b315cd1fc2c1`
- Başarı: `success`
- Artifact: `haritalar-debug-apk-308`
- Digest: `sha256:764c45cbfe4470199192657e183644f52e512b7e85a95e4055e0c39d0660d819`

### Run 309

- HEAD: `24f63830811337ecebc024ed779aeeeceb124609`
- Başarı: `success`
- Artifact: `haritalar-debug-apk-309`
- Digest: `sha256:839811646c32f606237cecfc6c8136861dc2217c81b31c999650d5514b810a0d`

### Run 335 — bugünün güncel doğrulaması

GitHub Actions REST API üzerinden doğrulandı:

- Workflow: `Android APK`
- Run number: **335**
- Run ID: `34339025583`
- HEAD: `b43cb23605b37d446e58535ead260a2d4e39fe7d`
- Event: `push`
- Status: `completed`
- Conclusion: **success**
- Build job: **success**
- Unit tests: **success**
- Debug APK build: **success**
- Debug APK artifact upload: **success**
- APK release publication step: **success**
- Debug APK artifact: `haritalar-debug-apk-335`
- Artifact size: `21,665,141` bytes
- Artifact digest: `sha256:f4b103f0f0bba2cd4f64ce2f5b13b45841d898193a25e7478c25eea9cfcc94df`
- Artifact expiration: 2026-12-08
- Unit test reports artifact: `haritalar-unit-test-reports-335`

**Önemli:** CI'nin APK üretmesi, fiziksel cihazda kurulum veya gerçek cihaz smoke testinin yapıldığı anlamına gelmez. Cihaz testi ayrıca doğrulanmalıdır.

### Run 343 — navigation traffic coalescing

- Run ID: `34342979779`
- HEAD: `b37178681b9b52f33358ee95ef757ae2c67c6fa1`
- Workflow: `Android APK`
- Status: `in_progress` olarak gözlendi.
- Sonuç: Henüz `success` veya `failure` olarak kabul edilmedi.

---

## 18. Güncel durum — 2026-09-09

### Son teknik kod commit'i

`b37178681b9b52f33358ee95ef757ae2c67c6fa1`

Commit:

`perf: coalesce navigation traffic refresh tasks`

### Son dokümantasyon/log commit'i

`9b1110e355a258c4f3f2e267075695f3a0e26742`

Bu commit activity log'a recovery ve coalescing işlemlerini kaydetmiştir ve `main` üzerinde son HEAD'i taşır.

### Navigation traffic zinciri

`GPS → AtomicBoolean caller gate → routeExecutor → TrafficRefreshCoordinator → canonical ranking → routeGeneration guard → UI state`

### Şu an canlı trafik durumu

Gerçek TomTom credential sağlanmadığı için **canlı TomTom trafik aktif** denmez. Key yoksa base Valhalla ETA güvenli varsayılandır.

### Doğrulama durumu

Coalescing kodu GitHub'da doğrulanmıştır. Run `343` halen `in_progress` gözlenmiştir; bu nedenle build/APK başarısı henüz ilan edilmez.

---

## 19. Açık işler / backlog

### P0 — doğruluk ve güvenlik

1. Güncel HEAD CI/APK sonucunu her önemli değişiklikten sonra gerçek GitHub üzerinden doğrula.
2. Gerçek TomTom credential verilirse canlı traffic smoke/integration testi yap.
3. Secret'ların public repository'ye sızmadığını kontrol et.
4. Trafik başarısızlığında base ETA'nın korunmasını test etmeye devam et.

### P1 — navigation trafik performansı

5. ~~GPS → coordinator → executor submit akışında gereksiz task kuyruğu oluşup oluşmadığını ölç.~~ **Kod seviyesi tespit yapıldı; caller-side gate eklendi. Gerçek cihaz/performance ölçümü ayrı açık iştir.**
6. ~~Gerekiyorsa atomik due/in-flight gate ekle.~~ **`AtomicBoolean trafficRefreshTaskInFlight` eklendi.**
7. Navigation refresh integration/smoke coverage artır.
8. Route generation stale sonuç testlerini genişlet.

### P2 — trafik sağlayıcıları

9. Gerçek erişim, authentication, kota ve kullanım şartları doğrulanırsa HERE alternatifini değerlendir.
10. İkinci/alternatif gerçek provider eklenirse provider chain fallback testleri genişlet.

### P3 — ürün veri katmanları

11. POI kaynaklarını doğrula.
12. Safety/radar/EDS için gerçek makine-okunabilir kaynak araştır.
13. Offline veri kapsamını gerçek kod ve testlerle netleştir.
14. Harita/rota cache stratejisini ölç.

### P4 — kalite

15. UI regression/smoke testleri artır.
16. Navigation lifecycle testlerini artır.
17. Battery/performance profiling yap.
18. Release APK sürecini ayrıca doğrula.

---

## 20. Kabul kriterleri

Bir özellik **tamamlandı** denebilmesi için mümkün olan yerde:

1. Kod mevcut olmalı.
2. İlgili test mevcut olmalı veya neden test edilemediği kayıt altına alınmalı.
3. CI sonucu gerçek GitHub'dan doğrulanmalı.
4. APK etkileniyorsa artifact/build sonucu doğrulanmalı.
5. Gerçek cihaz gerektiren davranışlarda cihaz testinin yapılıp yapılmadığı açıkça belirtilmeli.
6. Fallback/hata davranışı kontrol edilmeli.
7. Activity log'a kayıt düşülmeli.
8. Gerekirse bu dosya ve `PROJECT_WORK_PROMPT.md` güncellenmeli.

---

## 21. Her `Devam` için zorunlu çalışma döngüsü

### A — Hafızayı oku

1. `PROJECT_WORK_PROMPT.md`
2. `PROJECT_DETAILS.md`
3. `docs/PROJECT_ACTIVITY_LOG.md` son kayıtlar

### B — GitHub gerçekliğini kontrol et

- Branch
- HEAD
- Son commitler
- İlgili dosyalar
- Son CI
- Gerekirse artifact

### C — Önceki plan ile karşılaştır

- Tamamlandı mı?
- Eksik mi?
- Başarısız mı?
- Eski bilgi mi kaldı?

### D — En kritik gerçek eksik

En yüksek risk/etki alanından başlanır. Sırf kolay olduğu için önemsiz iş seçilmez.

### E — Gerçek değişiklik

Kod/config/test/dokümantasyon gerekiyorsa GitHub'da gerçek değişiklik yapılır.

### F — Test

Yalnız etkilenmesi muhtemel testler çalıştırılır; başarılı ve gereksiz testler körlemesine tekrarlanmaz.

### G — Commit

Her anlamlı değişiklik commit edilir. Commit mesajı ne yapıldığını açıkça anlatır.

### H — GitHub doğrulama

Commit SHA ve değişen dosyalar tekrar kontrol edilir.

### I — CI/APK

CI tetiklenmişse gerçek sonuç beklenir ve doğrulanır. APK build edilmişse artifact gerçek kaynaktan doğrulanır.

### J — Hafızaya yaz

Yapılan işlem, sonuç, test, CI, commit ve sıradaki adım activity log'a eklenir.

---

## 22. Mikro-değişiklik politikası

Bu proje hafızası "sadece büyük kod değişikliklerini" kaydetmez.

Şunların tamamı kayıt kapsamındadır:

- Bir satır kod.
- Bir karakter.
- Noktalama işareti.
- Bir import.
- Bir test.
- Bir test düzeltmesi.
- Bir config değişikliği.
- Bir workflow değişikliği.
- Bir doküman düzeltmesi.
- Bir commit.
- Başarısız deneme.
- Geri alma.
- CI başarısızlığı.
- CI iptali.
- APK üretimi.
- APK doğrulama sonucu.
- Tasarım/teknik karar değişikliği.

Her işlem `docs/PROJECT_ACTIVITY_LOG.md` içine kaydedilir.

---

## 23. Başarısızlıkların kaydı

Başarısız işlemler silinmez ve başarılıymış gibi yeniden yazılmaz.

Örnek kayıt:

`DENEME → SONUÇ: FAILED → GERÇEK HATA → NEDEN → DÜZELTME → YENİ TEST`

Bir önceki hatalı yaklaşım ileride tekrar denenmemesi için korunur.

---

## 24. Kaynak doğruluk sırası

1. Gerçek GitHub dosyası/commit.
2. Gerçek GitHub Actions sonucu.
3. Gerçek artifact metadata/digest.
4. Gerçek test çıktısı.
5. Proje dokümantasyonu.
6. Plan/öneri.

Plan hiçbir zaman gerçek uygulama durumu yerine geçmez.

---

## 25. Gidilmeyecek yollar

- Sahte trafik.
- Sahte ETA.
- Sahte radar/EDS.
- Sahte POI.
- Sahte koordinat/API cevabı.
- Test verisini canlı veri diye sunmak.
- OSM'yi canlı trafik olarak kullanmak.
- Web scraping.
- Public API key/secret.
- Ücretsiz kotayı sınırsız varsaymak.
- Kullanıcı onayı olmadan ücretli trafik.
- MainActivity'yi okunmadan körlemesine rewrite etmek.
- Başarısız CI'ı başarılı göstermek.
- Build edilmemiş APK'yı hazır göstermek.
- Fiziksel cihaz testi yapılmadıysa yapılmış gibi söylemek.
- GPS callback başına provider HTTP.
- Stale traffic sonucunu yeni route generation'a uygulamak.

---

## 26. Proje hafıza dosyaları

### `PROJECT_WORK_PROMPT.md`

Çalışma anayasası ve zorunlu işlem sırası.

### `PROJECT_DETAILS.md`

Bu dosya: ürün, mimari, teknik kararlar, geçmiş, mevcut durum, backlog ve kabul kriterleri.

### `docs/PROJECT_ACTIVITY_LOG.md`

Append-only ayrıntılı operasyon günlüğü. Her işlem kalem kalem kaydedilir.

---

## 27. Bugünkü başlangıç noktası

**Tarih:** 2026-09-09

**Kod HEAD:** `b37178681b9b52f33358ee95ef757ae2c67c6fa1`

**Log HEAD:** `9b1110e355a258c4f3f2e267075695f3a0e26742`

**Son CI:** Run `343` — `in_progress` olarak gözlendi.

**Son başarılı APK:** `haritalar-debug-apk-335` artifact; önceki başarılı HEAD `b43cb23605b37d446e58535ead260a2d4e39fe7d`.

**Son teknik değişiklik:** Navigation traffic refresh task coalescing gate.

**Canlı TomTom:** Credential olmadığı için aktif kabul edilmiyor.

---

## 28. Değişiklik yönetimi

Bu dosyanın herhangi bir maddesi eski kaldığında eski bilgi sessizce silinip yok edilmez. Güncel durum yeni bölüm/kayıt ile düzeltilir. Gerekirse eski bilginin neden geçersiz olduğu açıklanır.

Bu sayede proje hafızası yalnız "son hal" değil, **neden bu hale geldiğini de bilen bir sistem** olarak kalır.

---

## 29. 2026-09-09 — Coalescing gate sonrası durum

- `MainActivity.kt` navigation traffic refresh submit zincirinde `AtomicBoolean` caller-side gate ile güncellendi.
- Gate, coordinator'ın mevcut `inFlight` korumasının yerine geçmez; iki savunma katmanı birlikte korunur.
- Executor reddi ve Activity destroy durumlarında gate temizlenir.
- Gerçek GitHub commit'i: `b37178681b9b52f33358ee95ef757ae2c67c6fa1`.
- Bu kod commit'i için Run `343` / ID `34342979779` gözlem sırasında `in_progress` idi.
- Hatalı overwrite/recovery olayı `docs/PROJECT_ACTIVITY_LOG.md` içine kalıcı olarak kaydedildi.
- Yeni `Devam` öncesi bu dosya, `PROJECT_WORK_PROMPT.md` ve activity log tekrar okunmalıdır.
