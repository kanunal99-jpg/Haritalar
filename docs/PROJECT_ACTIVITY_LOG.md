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
