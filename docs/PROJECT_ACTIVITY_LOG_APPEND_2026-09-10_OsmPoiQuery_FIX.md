# HARİTALAR — FAALİYET GÜNLÜĞÜ EK KAYDI
## 2026-09-10 — OsmPoiQuery derleme hatasının düzeltilmesi

- **İşlem türü:** CI hata ayıklama → gerçek kod düzeltmesi.
- **Önceki gerçek durum:** `main` HEAD `67182f0c309c0e9807cab623b19c928875375121` idi. Android APK workflow #431, TomTom credential ve canlı Flow API smoke testlerini başarıyla geçti; ardından `gradle test --stacktrace` aşamasında `OsmPoiQuery.kt` satır 73-92 aralığında Kotlin derleme hatası nedeniyle başarısız oldu. APK/build aşamasına geçilemedi.
- **Kök neden:** `categoryClauses()` içindeki normal Kotlin stringlerinde Overpass sorgusunun çift tırnakları Kotlin string sınırlarını bozuyordu; ör. `"fuel"`, `"place_of_worship"`, `"parking"` vb. kaçışlanmamıştı.
- **Yapılan gerçek değişiklik:** `core/src/main/java/com/haritalar/core/navigation/OsmPoiQuery.kt` içindeki kategoriye özel Overpass stringleri Kotlin sözdizimine uygun şekilde escape edildi. Overpass sorgu içeriğinin semantiği korunmuştur.
- **Commit:** `7099dd6f482a71160144bc8721918fcddf955a2a`.
- **Güvenlik:** Secret/API key değiştirilmedi; gerçek TomTom anahtarı kaynak koda yazılmadı.
- **Test/CI:** Bu düzeltmenin yeni CI sonucu henüz doğrulanmadı; `BEKLİYOR`.
- **APK:** Henüz oluşturulmadı/doğrulanmadı.
- **Canlı kullanıcı doğrulaması:** Henüz yapılmadı. Önce CI → APK → gerçek cihaz kontrolü zorunludur.
- **Sonraki adım:** `7099dd6f...` HEAD üzerinde Android APK workflow'unun tamamlanmasını kontrol etmek; başarısızsa gerçek log üzerinden düzeltmek, başarılıysa artifact/SHA-256 ve ardından canlı cihaz trafik + POI kontrolüne geçmek.
