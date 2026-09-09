# HARİTALAR — 2026-09-09 İŞLEM KAYDI

## İşlem — Operasyon kayıt standardının anayasa içine gömülmesi

- **Tarih:** 2026-09-09
- **Tür:** Dokümantasyon / çalışma anayasası / süreç standardı
- **Amaç:** Kullanıcının talebi doğrultusunda yapılan her işlemin sonuç, hata, dikkat edilen noktalar, test, CI, APK/artifact ve sonraki adımlarıyla birlikte gerçek GitHub kaydına alınmasını zorunlu hale getirmek.
- **Kontrol edilen:** `PROJECT_WORK_PROMPT.md` mevcut gerçek GitHub içeriği ve blob SHA `00511bf72be890725ffe057060add700a01bba34`.
- **Yapılan değişiklik:** `PROJECT_WORK_PROMPT.md` içine zorunlu ayrıntılı işlem kayıt standardı eklendi.
- **Yeni zorunlu kayıt alanları:** işlem no, tarih/saat, tür, amaç, kontrol edilen kaynaklar, önceki durum, yapılan değişiklik, dikkat edilen riskler/kısıtlar, test, test sonucu, CI, APK/artifact, commit/doğrulama, nihai sonuç, başarısızlık nedeni/düzeltme, doğrulanmayan noktalar ve sonraki adım.
- **Sonuç etiketleri:** `BAŞARILI`, `BAŞARISIZ`, `BEKLİYOR`, `KULLANICI DOĞRULAMASI`.
- **Kritik kural:** Başarısız işlemler silinmeyecek veya başarılı gösterilmeyecek; düzeltme ayrı kayıtla takip edilecek.
- **Kritik kural:** Kullanıcı gerçek cihaz sonucu ile CI/emülatör sonucu birbirine karıştırılmayacak.
- **Kritik kural:** Doğrulanmamış CI/APK/artifact/commit bilgisi kesin başarı olarak yazılmayacak.
- **Kritik kural:** Yarım kalan işlem sonraki işe geçilmeden önce tamamlanacak ve durum kaydedilecek.
- **Commit:** `2f92c5e1ecdb3890dc09cb8551602bc4ec206a7b`
- **GitHub doğrulaması:** Prompt değişikliği GitHub `main` üzerinde başarılı şekilde oluşturuldu.
- **Activity log durumu:** `docs/PROJECT_ACTIVITY_LOG.md` append-only olduğu ve geçmişte yanlışlıkla bozulup recovery gerektirdiği için bu işlemde körlemesine full-content overwrite yapılmadı. Ana activity log'a güvenli append işlemi ayrıca yapılmalıdır; bu kayıt ara/kanıt kaydıdır.
- **Sonuç:** **BAŞARILI — anayasa güncellendi.**
- **Sonraki adım:** Ana activity log'a bu işlemin ve aynı oturumdaki gerçek cihaz doğrulama kaydının güvenli append olarak eklenmesi; ardından CI sonucunun gerçek GitHub'dan doğrulanması.
