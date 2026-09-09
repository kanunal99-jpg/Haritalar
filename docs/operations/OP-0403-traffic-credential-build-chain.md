# OP-0403 — Traffic credential build-chain verification

- **Tarih:** 2026-09-10
- **Tür:** Production-blocking traffic verification / CI diagnosis
- **Amaç:** Route traffic segment coloring feature'ın gerçek veri zincirini doğrulamak ve mavi fallback'in nedenini kanıtlamak.
- **Önceki durum:** Run #408 başarıyla APK üretti; ancak proje sahibinin canlı kontrolünde rota düz mavi kaldı. Bu nedenle özellik canlı doğrulamadan geçmedi.
- **Kod zinciri kontrolü:** `MainActivity` traffic ranking service'i `TrafficEngineFactory` üzerinden oluşturuyor. `TrafficEngineFactory`, `BuildConfig.TOMTOM_API_KEY` ile `TomTomTrafficProvider` oluşturuyor. Gradle tarafında `TOMTOM_API_KEY` yoksa değer boş varsayılanına düşüyor.
- **Gerçek değişiklik:** `.github/workflows/android.yml` içine `TOMTOM_API_KEY: ${{ secrets.TOMTOM_API_KEY }}` bağlandı ve secret boşsa APK üretimini durduran fail-fast kontrolü eklendi.
- **Commit:** `0c1ea3eb3a435ec80cf9088471483834dac2b5af`
- **CI:** Android APK Run #409 (`34405485980`), job `102647319745`.
- **CI sonucu:** **BAŞARISIZ.** `Verify live traffic credential is configured` adımı exit code 1 ile durdu; log ortamında `TOMTOM_API_KEY` boş geldi. Unit test, instrumentation, APK build ve release adımları bu nedenle çalışmadı.
- **APK:** Run #409 için APK üretilmedi; boyut/SHA-256 yoktur.
- **Canlı kontrol:** **BAŞARISIZ / BEKLEYOR.** Proje sahibinin cihazındaki görüntüde rota mavi kaldı; gerçek trafik segment verisinin uygulamaya ulaşması doğrulanamadı.
- **Güvenlik:** API anahtarı loglara yazılmadı ve sohbet içinde istenmedi.
- **Kritik kural:** Bu özellik canlı olarak `KONTROL BAŞARILI` ilan edilmeden başka yeni özelliğe geçilmeyecek.
- **Sonraki adım:** GitHub Actions repository secret `TOMTOM_API_KEY` proje sahibi tarafından eklenmeli. Secret eklendikten sonra yeni CI sonucu doğrulanacak; başarılı APK çıkarsa boyut + SHA-256 + gerçekleşen özellik listesi raporlanacak ve proje sahibinin gerçek cihaz kontrolü beklenecek.
- **Final durum:** `KULLANICI DOĞRULAMASI BEKLEYOR`
