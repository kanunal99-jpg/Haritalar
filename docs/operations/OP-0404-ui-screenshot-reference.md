# OP-0404 — UI ekran görüntüsü referansının kalıcı proje hafızasına eklenmesi

- **Tarih:** 2026-09-10
- **Tür:** UI referans / dokümantasyon
- **Amaç:** Proje sahibinin gerçek cihazdan gönderdiği ekran görüntüsünü gelecekteki arayüz düzeltmelerinde kullanılacak kalıcı bir referans kaydına dönüştürmek.
- **Kontrol edilen mevcut durum:** Trafik renk özelliği canlı kontrolde başarısız; gönderilen görüntüde rota mavi kalıyor. Run #410 da `TOMTOM_API_KEY` kontrolünde başarısız oldu.
- **Gerçek değişiklik:** `docs/UI_REFERENCE_2026-09-10.md` oluşturuldu. Görüntünün kaynağı, ekran üzerindeki gerçek UI durumu ve gelecekteki UI değişiklikleri için kabul kuralları kaydedildi.
- **Görsel dosya durumu:** Sohbet ekindeki JPEG bu GitHub connector yazma akışıyla ikili dosya olarak doğrudan yüklenemedi; bu nedenle repo kaydında görüntü uydurulmadı veya sahte dosya bağlantısı oluşturulmadı. Görsel, bu oturumun kullanıcı-provided ekran görüntüsü olarak referanslandı.
- **Test:** Dokümantasyon içeriği oluşturuldu; uygulama davranışı bu işlem kapsamında değiştirilmedi.
- **CI:** Bu dokümantasyon commit'i sonrasında GitHub Actions sonucu ayrıca kontrol edilecektir.
- **APK:** Bu işlem APK üretmedi.
- **Risk/kural:** Görsel referans, canlı doğrulamanın yerine geçmez. Trafik renk özelliği proje sahibi tarafından `KONTROL BAŞARILI` ilan edilmeden yeni özellik geliştirmesine geçilmeyecek.
- **Nihai durum:** `BAŞARILI` (UI referans kaydı oluşturuldu); trafik özelliği durumu bundan bağımsız olarak `KULLANICI DOĞRULAMASI BEKLEYOR`.
- **Sonraki adım:** TomTom credential zincirinin çözülmesi ve trafik renk özelliğinin gerçek APK üzerinde canlı doğrulanması.
