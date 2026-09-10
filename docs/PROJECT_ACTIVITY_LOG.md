# HARİTALAR — PROJE FAALİYET / OPERASYON GÜNLÜĞÜ

> **APPEND-ONLY KAYIT SİSTEMİ**
>
> Bu dosya projenin işlem hafızasıdır. Geçmiş kayıtlar silinmez, sessizce değiştirilmez ve başarısız işlemler gizlenmez. Bir hata veya eski bilgi düzeltilecekse yeni bir kayıt eklenir.
>
> **Zorunlu kural:** Her yeni `Devam` öncesinde `PROJECT_WORK_PROMPT.md`, `PROJECT_DETAILS.md` ve bu dosyanın son kayıtları okunur. Her operasyon sonrasında yeni kayıt eklenir. Bir karakter/noktalama değişikliği dahi operasyon kapsamındadır.

---

## Kayıt standardı

Her işlem mümkün olduğunca şu alanları içerir:

- İşlem numarası
- Tarih/saat
- Tür
- Amaç
- Önceki durum
- Kontrol edilen kaynaklar/dosyalar
- Yapılan işlem
- Değişen dosyalar
- Test/doğrulama
- CI
- APK
- Commit
- Sonuç
- Hata/öğrenim
- Sonraki adım

**Not:** Eski proje dönemlerinde her mikro işlemin geçmiş konuşmalardan eksiksiz çıkarılması mümkün değilse, kayıt "geçmişten taşınan özet" olarak işaretlenir. Uydurma tarih, commit, test veya sonuç eklenmez.

---

# GEÇMİŞTEN BUGÜNE TAŞINAN PROJE HAFIZASI

## İşlem #0001 — Hafıza sisteminin oluşturulması

- **Tarih:** 2026-09-09
- **Tür:** Proje altyapısı / dokümantasyon
- **Amaç:** Projenin geçmişini, kararlarını ve çalışma kurallarını kalıcı GitHub hafızasına taşımak.
- **Önceki durum:** `PROJECT_WORK_PROMPT.md` yaşayan çalışma promptuydu; ancak ayrıntılı proje detayları ve append-only operasyon günlüğü ayrı dosyalarda yoktu.
- **Kontrol:** GitHub `main` branch ve mevcut `PROJECT_WORK_PROMPT.md` okundu.
- **Karar:** Üç katmanlı hafıza sistemi kurulması kararlaştırıldı:
  1. `PROJECT_WORK_PROMPT.md` — çalışma anayasası.
  2. `PROJECT_DETAILS.md` — ayrıntılı proje teknik hafızası.
  3. `docs/PROJECT_ACTIVITY_LOG.md` — her operasyonun append-only günlüğü.
- **Sonuç:** Hafıza sisteminin gereksinimi tanımlandı.
- **Sonraki adım:** Ayrıntılı proje dosyasını GitHub'a eklemek.

## İşlem #0002 — PROJECT_DETAILS oluşturuldu

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon
- **Amaç:** Geçmişten bugüne ürün ve teknik durumu tek ana belgede toplamak.
- **Kontrol edilenler:** `PROJECT_WORK_PROMPT.md`, güncel GitHub commit geçmişi, trafik mimarisi ve doğrulanmış CI bilgileri.
- **Yapılan:** `PROJECT_DETAILS.md` oluşturuldu.
- **Kapsam:** Ürün vizyonu, mimari, 7 rota sistemi, GPS/TTS, trafik çekirdeği, TomTom, coordinator, fallback, güvenlik, performans, CI/APK, geçmiş, hatalar, backlog, kabul kriterleri ve çalışma döngüsü.
- **Commit:** `e77231ac4ba217afe219025d9586006376d612da`
- **Sonuç:** Başarılı.
- **Sonraki adım:** Append-only faaliyet günlüğünü oluşturmak.

## İşlem #0003 — PROJECT_ACTIVITY_LOG oluşturuldu

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon / proje hafızası
- **Amaç:** Bundan sonraki bütün operasyonların kalem kalem kaydedileceği append-only günlüğü oluşturmak.
- **Önceki durum:** Günlük dosyası mevcut değildi.
- **Yapılan:** `docs/PROJECT_ACTIVITY_LOG.md` oluşturuldu.
- **Kural:** Geçmiş silinmez; başarısız denemeler saklanır; yeni düzeltmeler yeni kayıt olarak eklenir.
- **Sonuç:** Başarılı.
- **Not:** Bu kayıt günlüğün ilk kurulum işlemini temsil eder. Oluşan commit SHA, GitHub write işlemi tamamlandıktan sonra doğrulanacak ve sonraki kayıtla günlüğe işlenecektir.

---

# GEÇMİŞ PROJE GELİŞTİRME DÖNEMLERİ

## İşlem #0010 — Temel Android navigasyon platformu

- **Tür:** Geçmişten taşınan özet
- **Amaç:** Çalışan Android harita/navigasyon temelini oluşturmak.
- **Gerçekleşen kilometre taşları:**
  - Kotlin Android uygulama iskeleti.
  - MapLibre/OpenFreeMap harita katmanı.
  - Nominatim arama/geocoding.
  - Valhalla routing.
  - GPS konum ve navigasyon progress.
  - Türkçe TTS.
  - Off-route algılama.
  - Kontrollü reroute.
  - Toll/ferry ayrıştırması.
  - Birden fazla rota alternatifi.
- **Commit:** Bu özet kaydı için geçmişten doğrulanmış tekil commit bilgisi kullanılmıyor.
- **Sonuç:** Sonraki trafik ve navigation çalışmalarının temeli oluşturuldu.

## İşlem #0011 — Yedi rota alternatifi yaklaşımı

- **Tür:** Geçmişten taşınan özet
- **Amaç:** Kullanıcıya farklı maliyet/zaman/feribot kriterleri sunmak.
- **Mevcut yaklaşım:**
  1. En hızlı.
  2. En kısa.
  3. Ücretsiz öncelikli.
  4. Hızlı ücretli.
  5. Hızlı feribotlu.
  6. Feribotsuz.
  7. Ücretsiz + feribotsuz.
- **Gerçeklik kuralı:** Toll/ferry yalnız gerçek routing sonucundan veya doğrulanmış hesaplamadan gelir.
- **Sonuç:** Route-card mimarisinin temel girdisi oluştu.

## İşlem #0012 — Trafik domain mimarisinin kurulması

- **Tür:** Geçmişten taşınan özet
- **Amaç:** Trafiği UI'dan bağımsız, provider değiştirilebilir ve fallback'li hale getirmek.
- **Eklenen/oluşan ana parçalar:** `TrafficProvider`, `TrafficProviderChain`, matcher, adapter, cost model, ranking, intelligence/orchestration bileşenleri.
- **Karar:** OSM canlı trafik kaynağı kabul edilmeyecek; scraping kullanılmayacak.
- **Sonuç:** Trafik için gerçek provider abstraction oluşturuldu.

## İşlem #0013 — Dayanıklı provider chain

- **Commit:** `98b6854a35fc27581786c7541462df0fd426162d`
- **Mesaj:** `feat(traffic): add resilient provider chain`
- **Tarih:** 2026-09-08
- **Amaç:** Trafik provider başarısız olduğunda güvenli fallback sağlamak.
- **Sonuç:** Provider chain dayanıklılığı geliştirildi.

## İşlem #0014 — Provider chain fallback testi

- **Commit:** `772cb6994ffcc785be5fd1d57e4f5c20fb05301e`
- **Mesaj:** `test(traffic): verify provider chain fallback behavior`
- **Tarih:** 2026-09-08
- **Amaç:** Provider chain fallback davranışını test etmek.
- **Sonuç:** Fallback davranışı için test eklendi.

## İşlem #0015 — Coroutine test bağımlılığı sadeleştirmesi

- **Commit:** `762808033732ec20835f1bda111d6927bdd52bc0`
- **Mesaj:** `test(traffic): avoid extra coroutine test dependency`
- **Tarih:** 2026-09-08
- **Amaç:** Test altyapısında gereksiz coroutine test bağımlılığını önlemek.
- **Sonuç:** Test bağımlılığı sadeleştirildi.

## İşlem #0016 — Route ETA cost model

- **Commit:** `4e2fdb6ca8edb68e79fc89d3075ebf0ef64281f4`
- **Mesaj:** `feat(traffic): add route ETA cost model`
- **Tarih:** 2026-09-08
- **Amaç:** Trafik etkili rota ETA/ranking maliyet modelini eklemek.
- **Sonuç:** Traffic ranking zincirinin maliyet katmanı güçlendirildi.

---

# 2026-09-09 TRAFİK REFRESH / NAVIGATION DÖNEMİ

## İşlem #0020 — TrafficRefreshCoordinator blocking bridge

- **Commit:** `af9ffcb99d965978132fdea4a2c9cc0b93c1891f`
- **Tür:** Kod
- **Amaç:** Mevcut background executor kullanan Android çağrılarının suspend refresh mekanizmasına kontrollü bridge ile bağlanması.
- **Karar:** `refreshBlocking()` yalnız background bağlamında kullanılacak; main thread'de kullanılmayacak.
- **Sonuç:** İlk bridge oluşturuldu.

## İşlem #0021 — Kotlin Result generic hatasının düzeltilmesi

- **Commit:** `1b869fdf4fdee70a6281dba3b3d5251c37956a9b`
- **Mesaj:** `fix(traffic): resolve coordinator coroutine result type`
- **Tür:** Bug fix
- **Amaç:** Coordinator bridge içindeki Kotlin `Result` generic çakışmasını düzeltmek.
- **Sonuç:** Hata düzeltildi.
- **Öğrenim:** Bridge API'lerinde tiplerin açık ve canonical tutulması gerekir.

## İşlem #0022 — Navigation coordinator entegrasyonu

- **Commit:** `7d18119cc1d309ee55bdd46ec85adcda3d6cd29b`
- **Mesaj:** `feat(traffic): wire refresh coordinator into navigation`
- **Tür:** Kod / entegrasyon
- **Amaç:** GPS navigation akışını TrafficRefreshCoordinator'a bağlamak.
- **Yapılan:**
  - GPS callback coordinator'ı tetikleyebilir hale getirildi.
  - Provider/HTTP GPS callback içinde çalıştırılmadı.
  - Background executor kullanıldı.
  - Route generation/stale koruması kullanıldı.
  - Yeni route/reroute için reset mantığı bağlandı.
- **Sonuç:** Navigation trafik refresh zinciri oluşturuldu.

## İşlem #0023 — refreshBlocking unit coverage

- **Commit:** `fe39634743175c4ca839314c6cda357e85bb69f1`
- **Mesaj:** `test(traffic): cover blocking coordinator bridge`
- **Tür:** Test
- **Amaç:** Blocking bridge'in canonical ranker delegasyonu ve route/time aktarımını doğrulamak.
- **Sonuç:** Unit coverage eklendi.

## İşlem #0024 — Navigation coordinator dokümantasyon senkronizasyonu

- **Commit:** `b43cb23605b37d446e58535ead260a2d4e39fe7d`
- **Mesaj:** `docs: sync navigation traffic coordinator integration`
- **Tür:** Dokümantasyon
- **Amaç:** Navigation trafik coordinator entegrasyonunu yaşayan proje promptuna işlemek.
- **Sonuç:** `PROJECT_WORK_PROMPT.md` güncellendi.

---

# CI / APK DOĞRULAMA GEÇMİŞİ

## İşlem #0030 — Run 303 doğrulaması

- **Tür:** CI/APK
- **Run:** `303`
- **Run ID:** `34274419418`
- **Sonuç:** Başarılı unit tests + debug APK.
- **Artifact:** `haritalar-debug-apk-303`
- **APK SHA-256:** `cb599f1c47e871e3daec0c7705b98bf57da3b26909c9a697f1a794110fe3fe59`
- **Not:** Bu, daha önce doğrulanmış bir APK üretim kilometre taşıdır.

## İşlem #0031 — Run 308 doğrulaması

- **Tür:** CI/APK
- **HEAD:** `2c34feb8d8ef56850fd718920165b315cd1fc2c1`
- **Run:** `308`
- **Sonuç:** `success`
- **Artifact:** `haritalar-debug-apk-308`
- **Digest:** `sha256:764c45cbfe4470199192657e183644f52e512b7e85a95e4055e0c39d0660d819`

## İşlem #0032 — Run 309 doğrulaması

- **Tür:** CI/APK
- **HEAD:** `24f63830811337ecebc024ed779aeeeceb124609`
- **Run:** `309`
- **Sonuç:** `success`
- **Artifact:** `haritalar-debug-apk-309`
- **Digest:** `sha256:839811646c32f606237cecfc6c8136861dc2217c81b31c999650d5514b810a0d`

## İşlem #0033 — Run 335 güncel doğrulaması

- **Tarih:** 2026-09-09
- **Tür:** CI/APK doğrulaması
- **Workflow:** `Android APK`
- **Run:** `335`
- **Run ID:** `34339025583`
- **HEAD:** `b43cb23605b37d446e58535ead260a2d4e39fe7d`
- **Event:** `push`
- **Status:** `completed`
- **Conclusion:** `success`
- **Build job:** `success`
- **Unit tests:** `success`
- **Debug APK build:** `success`
- **Artifact upload:** `success`
- **APK release publication step:** `success`
- **Debug artifact:** `haritalar-debug-apk-335`
- **Artifact size:** `21,665,141` bytes
- **Artifact digest:** `sha256:f4b103f0f0bba2cd4f64ce2f5b13b45841d898193a25e7478c25eea9cfcc94df`
- **Artifact expires:** 2026-12-08
- **Unit test reports:** `haritalar-unit-test-reports-335`
- **Sonuç:** Güncel HEAD için CI yeşil ve debug APK artifact üretimi doğrulandı.
- **Sınır:** Fiziksel Android cihazda kurulum/smoke testinin yapıldığı anlamına gelmez.

---

# BUGÜNKÜ HAFIZA SİSTEMİ KURULUMU

## İşlem #0040 — Güncel durumun hafızaya alınması

- **Tarih:** 2026-09-09
- **Amaç:** Geçmişten bugüne teknik durumu yeni hafıza sistemine geçirmek.
- **Güncel HEAD:** `b43cb23605b37d446e58535ead260a2d4e39fe7d`
- **Güncel CI:** Run `335`, success.
- **Güncel APK artifact:** `haritalar-debug-apk-335`
- **Canlı TomTom:** Gerçek credential olmadığı için aktif kabul edilmiyor.
- **Sonraki teknik öncelik:** Navigation GPS → coordinator → executor submit akışında gereksiz task kuyruğu oluşup oluşmadığını ölçmek; gerekiyorsa atomik due/in-flight gate eklemek.

## İşlem #0041 — Sonraki çalışma kuralının tanımlanması

- **Tür:** Proses
- **Kural:** Bundan sonraki her `Devam` şu sırayı takip eder:

`DOSYALARI OKU → SON KAYITLARI OKU → GITHUB HEAD/COMMIT KONTROLÜ → İLGİLİ KODU OKU → PLANLA → EN KRİTİK GERÇEK EKSİĞİ SEÇ → DEĞİŞTİR → TEST → COMMIT → GITHUB DOĞRULA → CI → APK → ACTIVITY LOG'A YAZ → PROJECT_DETAILS'I GÜNCELLE`

- **Sonuç:** Proje hafızası operasyonun parçası haline getirildi.

---

# CANLI GÜNLÜK BÖLÜMÜ

> Bundan sonraki kayıtlar bu bölümün altına eklenir. Eski kayıtlar değiştirilmez.

## İşlem #0100 — İlk yeni `Devam` işlemi için bekleyen kayıt alanı

- **Durum:** BEKLEMEDE
- **Not:** Bir sonraki gerçek geliştirme başlamadan önce üç hafıza dosyası okunacak ve GitHub HEAD yeniden doğrulanacaktır. Bu satır plan/şablondur; gerçek işlem yapılmış sayılmaz.

## İşlem #0101 — MainActivity yanlışlıkla placeholder ile değiştirilmesi ve geri alınması

- **Tarih:** 2026-09-09
- **Tür:** HATA / RECOVERY / GitHub operasyonu
- **Amaç:** Navigation traffic refresh executor task-submit davranışını incelemek amacıyla `MainActivity.kt` üzerinde çalışma başlatılması.
- **Önceki durum:** `main` HEAD `503f3d7ffafc87cbe99c421d1f3cf80fa8fb809c` idi ve `MainActivity.kt` blob SHA `5d052bf1d7f7fdebcb3508e9124a4b49123b1966` olarak doğrulanmıştı.
- **Hata:** GitHub write işlemi sırasında `MainActivity.kt` içeriği yanlışlıkla `__PLACEHOLDER__` ile değiştirildi.
- **Hatalı commit:** `ae493b6b45444df62d41d5bf3ad5241db8101144`
- **Hatalı commit mesajı:** `perf: coalesce navigation traffic refresh tasks`
- **Gerçek etki:** `MainActivity.kt` 1000 satırlık mevcut içerikten placeholder'a düşürüldü. Bu değişiklik gerçek özellik olarak kabul edilmedi.
- **Recovery:** Önceki commit `503f3d7ffafc87cbe99c421d1f3cf80fa8fb809c` ve orijinal `MainActivity.kt` blob'u yeniden doğrulandı. Eski tree taban alınarak `MainActivity.kt` orijinal blob SHA'sına geri getirildi.
- **Recovery commit:** `a467415ff40862bc7ad89b79cd1cc0eca25bf45c`
- **Recovery mesajı:** `revert: restore MainActivity after accidental overwrite`
- **Recovery doğrulaması:** `main` HEAD `a467415ff40862bc7ad89b79cd1cc0eca25bf45c`; recovery tree SHA `ec51c05e6cf7aa98ab427640fae04c0603fb3adb`; `MainActivity.kt` tekrar blob SHA `5d052bf1d7f7fdebcb3508e9124a4b49123b1966`.
- **Kod değişikliği:** Planlanan traffic coalescing gate bu işlemde uygulanmadı.
- **Test:** Bu recovery sonrasında henüz Android build/unit test çalıştırılmadı.
- **CI:** Recovery commit için henüz doğrulanmadı.
- **APK:** Recovery commit için yeni APK doğrulanmadı.
- **Sonuç:** Hatalı overwrite geri alındı; önceki uygulama kodu restore edildi. Olay gizlenmedi ve kayda geçirildi.
- **Öğrenim:** Büyük dosya üzerinde full-content GitHub update işlemi, içerik tamamen elde edilip doğrulanmadan kullanılmamalı. Bundan sonraki değişikliklerde küçük/deterministik dosya veya güvenli tree/blob yaklaşımı tercih edilecek.
- **Sonraki adım:** Recovery sonrası HEAD ve ilgili dosyaları tekrar doğrulamak; ardından task queue problemini güvenli şekilde ele almak.

## İşlem #0102 — Activity log'un recovery olayını içerecek şekilde güncellenmesi

- **Tür:** Dokümantasyon / operasyon günlüğü
- **Amaç:** #0101 recovery olayını append-only proje hafızasına eklemek.
- **Yapılan:** Mevcut `docs/PROJECT_ACTIVITY_LOG.md` içeriği korunarak #0101 ve #0102 kayıtları sona eklendi.
- **Test:** İçerik değişikliğinin GitHub commit'i oluşturulması bekleniyor.
- **CI/APK:** Bu dokümantasyon değişikliği için henüz yeni CI sonucu doğrulanmadı.
- **Sonuç:** Recovery olayı kalıcı log'a işlendi.
- **Sonraki adım:** Yeni HEAD'i doğrula ve teknik traffic task coalescing çalışmasına devam et.

## İşlem #0103 — Navigation traffic refresh task coalescing gate

- **Tarih:** 2026-09-09
- **Tür:** Kod / performans / dayanıklılık
- **Amaç:** GPS callback'in her konum güncellemesinde `routeExecutor` kuyruğuna yeni trafik refresh task'ı bırakmasını engellemek.
- **Önceki gerçek durum:** `TrafficRefreshCoordinator` zaten tek bir ranking işlemini `inFlight` ile koruyordu; ancak MainActivity tarafında aynı anda bekleyen executor task'ları yine de oluşabiliyordu. Bu task'lar daha sonra coordinator tarafından `Skipped` dönebilse de gereksiz executor kuyruğu yaratabiliyordu.
- **Yapılan:** `MainActivity.kt` içine `AtomicBoolean trafficRefreshTaskInFlight` eklendi. `refreshTrafficForNavigation()` artık task submit edilmeden önce atomik `compareAndSet(false, true)` ile tekil task kapısı uyguluyor. Task tamamlandığında `finally` ile kapı serbest bırakılıyor. Executor task submit reddederse gate güvenli şekilde sıfırlanıyor ve uyarı loglanıyor.
- **Ek güvenlik:** Activity destroy sırasında gate temizleniyor.
- **Değişen dosya:** `app/src/main/java/com/haritalar/app/MainActivity.kt`
- **Commit:** `b37178681b9b52f33358ee95ef757ae2c67c6fa1`
- **Commit mesajı:** `perf: coalesce navigation traffic refresh tasks`
- **GitHub doğrulaması:** Değişen dosyanın blob SHA'sı `706b17f8800d383c81c434d2b0c1ffe020423c98` olarak doğrulandı; `AtomicBoolean` import'u ve yeni gate alanı GitHub'daki dosyada mevcut.
- **Test:** Yerel Android build çalıştırılmadı; GitHub Actions build doğrulaması bekleniyor.
- **CI:** Run `343` / Run ID `34342979779` `in_progress` durumunda gözlendi; sonuç henüz kesinleşmedi.
- **APK:** Henüz doğrulanmadı.
- **Sonuç:** Kod değişikliği gerçek GitHub'a yazıldı ve doğrulandı; CI sonucu bekleniyor.
- **Sonraki adım:** Run `343` sonucunu doğrula. Başarılıysa APK artifact'i kontrol et; başarısızsa job logundan gerçek derleme hatasını düzelt.

## İşlem #0104 — PROJECT_DETAILS senkronizasyonu

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon / proje hafızası
- **Amaç:** Coalescing gate ve recovery olayını ana teknik hafızaya geçirmek; eski P1 maddelerini güncel durumla uyumlu hale getirmek.
- **Yapılan:** `PROJECT_DETAILS.md` güncellendi; performans bölümüne caller-side atomik gate, geçmişe coalescing commit'i ve overwrite/recovery öğrenimi eklendi; backlog gerçek durumla senkronize edildi.
- **Commit:** `8c1c9842beea4dc15e456a0c053baaa23361b7df`
- **Test:** Dokümantasyon değişikliğinde uygulama testi çalıştırılmadı.
- **CI:** Bu commit için yeni CI tetiklendi; sonradan doğrulanacak.
- **APK:** Dokümantasyon commit'i için yeni APK sonucu henüz doğrulanmadı.
- **Sonuç:** Başarılı GitHub yazımı ve commit.

## İşlem #0105 — PROJECT_WORK_PROMPT senkronizasyonu

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon / çalışma anayasası
- **Amaç:** Zorunlu çalışma promptunun navigation traffic submit zincirindeki yeni gate'i ve güncel sonraki hedefi yansıtması.
- **Yapılan:** `PROJECT_WORK_PROMPT.md` güncellendi. Navigation zincirine `AtomicBoolean` caller-side gate eklendi; büyük dosya full-content overwrite yasağı açıklaştırıldı; sonraki hedef CI/APK doğrulaması ve gerçek device/performance profiling olarak güncellendi.
- **Commit:** `4b2865a86abf8490fd997e952e59f85539df6104`
- **Test:** Dokümantasyon değişikliğinde uygulama testi çalıştırılmadı.
- **CI:** Run `346` / ID `34343140564` `queued` olarak gözlendi.
- **APK:** Henüz doğrulanmadı.
- **Sonuç:** Başarılı GitHub yazımı ve commit.

## İşlem #0106 — Activity log ile ana hafıza senkronu

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon / operasyon günlüğü
- **Amaç:** #0104 ve #0105 operasyonlarını append-only activity log'a kaydetmek.
- **Yapılan:** Bu kayıt eklendi.
- **Sonraki CI:** Bu log commit'i yeni Android APK workflow run'ı tetikleyecektir.

## İşlem #0107 — TrafficRefreshCoordinator generation/in-flight yarış testleri

- **Tarih:** 2026-09-09
- **Tür:** Test / dayanıklılık
- **Amaç:** Navigation traffic refresh zincirindeki stale-generation ve eşzamanlı in-flight korumasını daha güçlü unit coverage ile doğrulamak.
- **Önceki gerçek durum:** `TrafficRefreshCoordinatorTest.kt` mevcut cooldown, reset, running-refresh→Stale ve blocking bridge senaryolarını içeriyordu; ancak stale sonucun sonraki generation'ın sonucunu/cooldown'unu bozmadığı ve ikinci eşzamanlı refresh'in ranking başlatmadığı ayrı testlerle doğrulanmıyordu.
- **Kontrol edilen dosyalar:** `TrafficRefreshCoordinator.kt`, `TrafficRefreshCoordinatorTest.kt`.
- **Yapılan:** `TrafficRefreshCoordinatorTest.kt` içine iki odaklı test eklendi:
  1. `stale refresh does not poison next generation result or cooldown` — reset sonrası eski sonuç Stale kalırken yeni generation'ın hemen refresh edilebildiğini ve yeni cooldown'un doğru kurulduğunu doğrular.
  2. `in flight refresh suppresses a concurrent duplicate` — çalışan ranking sırasında ikinci refresh'in `Skipped` olduğunu ve ranker'ın yalnız bir kez çağrıldığını doğrular.
- **Ek düzenleme:** `TimeUnit` import edilerek mevcut beklemeler sadeleştirildi; davranış değişikliği yoktur.
- **Değişen dosya:** `core/src/test/kotlin/com/haritalar/core/traffic/TrafficRefreshCoordinatorTest.kt`
- **Commit:** `161098fa08712ed80c19fb0006beb61331235778`
- **Commit mesajı:** `test(traffic): cover coordinator generation and in-flight races`
- **Yerel test:** Bu ortamda Android/Gradle build çalıştırılmadı.
- **CI:** Run `348` / Run ID `34344278516` `completed/failure` oldu. `Unit tests` adımında Kotlin derleme hatası oluştu; APK build/upload adımları bu nedenle `skipped` kaldı.
- **Hata:** `TrafficRefreshCoordinatorTest.kt:155` civarında `secondResult.get().ranked` ifadesinde sealed `Result` tipinin ortak `ranked` alanı olmadığı için Kotlin tipi çıkaramadı; ayrıca `ranked` unresolved reference raporlandı.
- **Sonuç:** İlk test değişikliği derleme hatası verdi; hata gerçek CI logundan tespit edildi ve gizlenmedi.
- **Sonraki adım:** Hatalı test assertion'ını açık `Skipped` tipine daraltarak düzeltmek ve yeni CI sonucunu doğrulamak.

## İşlem #0108 — Coordinator yarış testi derleme hatasının düzeltilmesi

- **Tarih:** 2026-09-09
- **Tür:** Bug fix / test
- **Amaç:** Run `348`'de görülen Kotlin sealed-result tip çıkarımı hatasını düzeltmek.
- **Yapılan:** `secondResult.get().ranked` doğrudan erişimi kaldırıldı; `assertIs<TrafficRefreshCoordinator.Result.Skipped>(...)` dönüşü `skipped` değişkenine alınarak `skipped.ranked` üzerinden tip güvenli assertion yapıldı.
- **Değişen dosya:** `core/src/test/kotlin/com/haritalar/core/traffic/TrafficRefreshCoordinatorTest.kt`
- **Commit:** `a55b1387df2e78d83108a9a37a63223822363843`
- **Commit mesajı:** `fix(traffic): correct coordinator race test typing`
- **Yerel test:** Bu ortamda Android/Gradle build çalıştırılmadı.
- **CI:** Run `350` / Run ID `34344440387` tamamlandı ve **success** oldu.
- **CI ayrıntısı:** Unit tests, debug APK build, artifact upload ve başarılı main build APK publication adımları success oldu.
- **APK artifact:** `haritalar-debug-apk-350`
- **APK boyutu:** `21,665,316` bytes
- **APK digest:** `sha256:b06b290789180762b7211509f3fb1e3a6d37cf4f921481477dd44fa136a3870e`
- **Unit test reports:** `haritalar-unit-test-reports-350`
- **Sonuç:** Race/stale test düzeltmesi CI ve APK üretimiyle doğrulandı.
- **Sonraki adım:** Dokümantasyon backlog'unu güncel CI sonucuyla senkronize et ve navigation/UI integration-smoke coverage ile gerçek cihaz/performance profiling'e ilerle.

## İşlem #0109 — PROJECT_DETAILS ve PROJECT_WORK_PROMPT backlog senkronizasyonu

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon / proje hafızası
- **Amaç:** Coordinator race/stale testlerinin tamamlanması ve Run `350` başarısının ardından yaşayan proje hafızasındaki açık işler ve güncel doğrulama durumunu güncellemek.
- **Yapılan:** `PROJECT_DETAILS.md` güncellendi; coordinator-level stale/race coverage tamamlanmış olarak işaretlendi, Run `348` failure ve Run `350` success kaydedildi, güncel backlog navigation/UI integration, gerçek cihaz/performance/battery profiling ve TomTom integration yönüne taşındı. `PROJECT_WORK_PROMPT.md` güncellendi; Run `350`/`351` doğrulaması ve sonraki teknik öncelikler yeni çalışma döngüsüne işlendi.
- **Değişen dosyalar:** `PROJECT_DETAILS.md`, `PROJECT_WORK_PROMPT.md`
- **Commitler:** `d1369ac746d0fdb96858cc9c82580e6d819fcddd`, `aff1641d38cae0e1b5974e6b458512e2786e4a8b`
- **Test:** Dokümantasyon değişiklikleri için uygulama testi çalıştırılmadı.
- **CI:** Dokümantasyon commitleri için Run `352` ve `353` tetiklendi; sonuçları ayrıca doğrulanacak.
- **APK:** Bu dokümantasyon commitleri için yeni APK sonucu henüz doğrulanmadı.
- **Sonuç:** Yaşayan proje hafızası test kapsamı ve güncel backlog ile senkronize edildi.
- **Sonraki adım:** Run `352`/`353` sonuçlarını doğrula; ardından navigation/UI integration-smoke ve gerçek cihaz/performance profiling işine geç.

## İşlem #0110 — CI doğrulaması ve navigation/UI entegrasyon incelemesi

- **Tarih:** 2026-09-09
- **Tür:** CI doğrulama / teknik inceleme
- **Amaç:** Önceki dokümantasyon senkronlarının gerçek GitHub sonucunu doğrulamak ve sıradaki navigation/UI traffic integration-smoke işinin mevcut kod durumunu incelemek.
- **Yapılan:** Run `354` / Run ID `34344685422` kontrol edildi; `Android APK` workflow **completed / success**. Unit tests, debug APK build, artifact upload ve successful-main-build APK publication adımları success. Artifact `haritalar-debug-apk-354`, boyut `21,665,324` bytes, digest `sha256:f9c7eaacca1d79af4217276be1284382e02ec047a76b7258334295e77b3e45f7` olarak doğrulandı. `MainActivity.kt`, `TrafficRefreshCoordinator.kt` ve app/core traffic presentation testleri yeniden incelendi.
- **Kod bulgusu:** Navigation refresh zincirinde caller-side `AtomicBoolean` gate + coordinator `inFlight` + `routeGeneration` guard zaten mevcut. Route-card/navigation traffic sonucu routeId tabanlı map'e dönüştürülüyor; coordinator race/stale testleri mevcut. Bu incelemede güvenli ve küçük bir production değişikliği için yeterli gerçek boşluk tespit edilmedi; körlemesine MainActivity rewrite yapılmadı.
- **Test:** Bu inceleme turunda yerel Android/Gradle test çalıştırılmadı.
- **CI:** Run `354` gerçek GitHub'dan success olarak doğrulandı.
- **APK:** `haritalar-debug-apk-354` gerçek artifact olarak doğrulandı; fiziksel cihaz kurulumu/testi yapılmadı.
- **Backlog güncellemesi:** Navigation/UI integration-smoke ve gerçek cihaz/performance/battery profiling P1 olarak korunuyor. Mevcut test altyapısında `app/src/androidTest` bulunmadığı görüldü; bu nedenle gerçek cihaz smoke coverage ayrı bir sonraki altyapı işi olarak ele alınacak.
- **Sonuç:** Run `354` başarıyla kapatıldı; mevcut traffic generation/coalescing zincirinde gereksiz production değişikliği yapılmadı.
- **Sonraki adım:** Android instrumentation/smoke test altyapısının mevcut Gradle yapılandırmasına uygun en küçük güvenli şekilde kurulup kurulamayacağını incele; ardından navigation lifecycle ve traffic UI akışını cihaz üzerinde doğrulanabilir hale getir.

## İşlem #0111 — Activity log bütünlüğünün yanlışlıkla bozulması ve geri yüklenmesi

- **Tarih:** 2026-09-09
- **Tür:** HATA / RECOVERY / Dokümantasyon
- **Amaç:** #0110 CI doğrulama kaydını activity log'a eklemek.
- **Hata:** `docs/PROJECT_ACTIVITY_LOG.md` güncellemesi sırasında append-only dosyanın tamamı yerine yalnız son kayıtlar yazıldı ve önceki tarihçe geçici olarak dosya içeriğinden çıkarıldı.
- **Etkilenen dosya:** `docs/PROJECT_ACTIVITY_LOG.md`
- **Gerçek durum:** Önceki tam log blob'u `9bbe0ba400e548dbb0944e7ae00643d944a0d8d` olarak GitHub'dan yeniden elde edildi.
- **Recovery:** Tam tarihçe yeniden oluşturulup #0110 ve bu recovery kaydı sona eklendi. Eski kayıtların korunması esas alındı.
- **Öğrenim:** Append-only log üzerinde update yapılırken önceki tam blob mutlaka korunmalı; yalnızca son satırların gönderilmesi kabul edilemez.
- **Test:** Dokümantasyon içeriği GitHub write ile doğrulanacak.
- **CI/APK:** Bu recovery commitinden sonra yeni Android APK workflow sonucu ayrıca doğrulanacak.
- **Sonuç:** Tarihçe kaybı kalıcı olmadan düzeltildi ve hata gizlenmeden kaydedildi.
- **Sonraki adım:** Recovery commitini GitHub'da doğrula, CI/APK sonucunu kontrol et ve bundan sonra instrumentation/smoke test altyapısına geç.

## İşlem #0112 — MainActivity smoke testindeki zaman bağımlı assertion düzeltildi

- **Tarih:** 2026-09-09
- **Tür:** Test / CI bug fix
- **Amaç:** Run `364`'te başarısız olan MainActivity instrumentation smoke testini gerçek hata nedenine göre stabilize etmek.
- **Önceki gerçek durum:** `main` HEAD `d582dd3d85acc6b8a257dca1d9afb2d88e2e4a44` idi; Run `364` / Run ID `34381820850` **completed / failure** durumundaydı. Unit tests başarıyla tamamlanmış, failure instrumentation smoke testinde oluşmuştu.
- **CI bulgusu:** `MainActivity` açıldı; ancak smoke testi `Haritalar • GPS bekleniyor` metnini doğrulayamadan başarısız oldu. `MainActivity` içinde bu durum metni ilk anda veriliyor, MapLibre style callback sonrasında `Harita hazır • adres ara veya haritaya dokun` olarak değiştiriliyor. Bu nedenle test geçici bir UI durumuna zaman bağımlıydı.
- **Gerçek neden:** Smoke testi uygulama açılışını doğrulamak yerine kısa ömürlü status text'e bağlanmıştı.
- **Yapılan:** `app/src/androidTest/java/com/haritalar/app/MainActivitySmokeTest.kt` içindeki assertionlar daha stabil başlangıç kontrollerine çevrildi. Search alanının `Nereye gitmek istiyorsun?` hint'i ve `Ara` butonunun görünür olması doğrulanıyor.
- **Değişen dosya:** `app/src/androidTest/java/com/haritalar/app/MainActivitySmokeTest.kt`
- **Commit:** `3f8446cbf37b4f835659373b97c709f031f65b9d`
- **Commit mesajı:** `test(android): stabilize MainActivity smoke assertion`
- **GitHub doğrulaması:** Commit ve yeni blob SHA `d37e66f72ed2603ce0743e41afe86c0a82b7f4ff` gerçek GitHub'dan tekrar okundu.
- **Test:** Yeni CI sonucu bu log kaydı oluşturulurken henüz kesinleşmemiştir.
- **CI:** Run `364` failure olarak kalır; bu kayıt sonrasında yeni push ile yeni workflow run tetiklenmesi beklenir.
- **APK:** Yeni düzeltme için APK henüz doğrulanmadı.
- **Sonuç:** Run `364` failure nedenine yönelik en küçük güvenli test değişikliği GitHub'a uygulandı ve commit doğrulandı.
- **Sonraki adım:** Yeni CI run'ını gerçek GitHub'dan doğrula; smoke test yeşil olursa debug APK/artifact'i doğrula. Başarısızsa yeni failure loguna göre devam et.

## İşlem #0113 — MainActivity instrumentation testinde location permission önkoşulu eklendi

- **Tarih:** 2026-09-09
- **Tür:** Test altyapısı / CI bug fix
- **Amaç:** Run `366` failure'ında MainActivity kontrollerinin sistem konum izni diyaloğu arkasında kalabilmesi nedeniyle smoke testinin yanlış negatif üretmesini gidermek.
- **Önceki gerçek durum:** `main` HEAD `706310f74b38bff8208296875f87e6bd5ee3a05b`; Run `366` / Run ID `34382658235` **completed / failure**. Unit tests success; instrumentation smoke test failure; debug APK adımları test failure nedeniyle skipped.
- **CI bulgusu:** Run `366` emulator üzerinde `MainActivity` testine başladı; test "initial controls were not verified" ile başarısız oldu. Aynı akışta uygulama `onCreate()` sonunda `requestLocationPermission()` çağırıyor. Manifest'te FINE/COARSE location izinleri mevcut. Smoke test bu runtime izinlerini önceden vermiyordu.
- **Yapılan:** `app/build.gradle.kts` içine `androidx.test:rules:1.6.1` androidTest bağımlılığı eklendi. `MainActivitySmokeTest.kt` içine `GrantPermissionRule` ile FINE ve COARSE location izinleri Activity launch öncesinde verildi.
- **Değişen dosyalar:** `app/build.gradle.kts`, `app/src/androidTest/java/com/haritalar/app/MainActivitySmokeTest.kt`
- **Commitler:** `351059d8a321a28cf70e2143af3a881e171e505d` (`test(android): add runtime permission test rule`), `5e491ffe443dbe5d033617268bc35b80e1e21f24` (`test(android): grant location permissions before MainActivity smoke`)
- **GitHub doğrulaması:** Her iki commit doğrudan `main` branch'e yazıldı ve GitHub write sonucu SHA ile doğrulandı.
- **Yerel test:** Bu ortamda Android/Gradle test çalıştırılmadı.
- **CI:** Yeni CI bu kayıt yazılırken henüz kesinleşmedi.
- **APK:** Yeni düzeltme için henüz doğrulanmadı.
- **Sonuç:** Smoke testin bilinen runtime permission önkoşulu gerçek test harness'inde karşılandı; production navigation kodu değiştirilmedi.
- **Öğrenim:** Android instrumentation testleri, Activity'nin gerçek runtime permission lifecycle'ını açıkça hazırlamalı; yalnız UI assertionlarını değiştirmek yeterli değildir.
- **Sonraki adım:** Yeni GitHub Actions run'ını doğrula. Failure devam ederse diagnostics artifact/log üzerinden kalan gerçek nedeni izole et; success olursa APK artifact'i doğrula.

## İşlem #LIVE-TRAFFIC-MAP-3 — Rota bağımsız trafik kod temizliği ve doğrulama hazırlığı

- **Tarih:** 2026-09-10
- **Tür:** Bug fix / patch cleanup
- **Amaç:** Önceki tek-seferlik patch denemelerinin oluşturduğu yinelenen Kotlin bildirimlerini temizlemek ve rota bağımsız canlı trafik katmanını derlenebilir hale getirmek.
- **Yapılan:** MainActivity duplicate constant/field bildirimleri normalize edildi; TrafficSegment importu güvenceye alındı; canonical TomTom provider için bounded blocking bridge TrafficEngineFactory içinde tutuldu.
- **Canlı kullanıcı doğrulaması:** BEKLİYOR.
