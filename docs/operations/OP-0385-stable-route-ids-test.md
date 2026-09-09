# OP-0385 — Stable Route ID Traffic Ranking Test

- Tarih: 2026-09-09
- Tür: Test / güvenilirlik koruması
- Amaç: Trafik sıralaması yeniden düzenlendiğinde yedi standart rota kimliğinin kaybolmamasını ve `fastest` rotasının trafik uygulanmış sonuç taşımasını korumak.
- Değişen dosya: `core/src/test/kotlin/com/haritalar/core/traffic/TrafficRouteRankingStableRouteIdsTest.kt`
- Kod commit'i: `f4af1de8b987cfc3a8d83d684c7f87b293272a9b`
- Test kapsamı: `fastest`, `shortest`, `free-road`, `fast-toll`, `fast-ferry`, `no-ferry`, `free-no-ferry` kimliklerinin tamamı tam olarak birer kez korunuyor; sonuç sayısı ve benzersizlik doğrulanıyor; `fastest` için trafik uygulanması doğrulanıyor.
- CI run: #385 / `34394799014`
- CI sonucu: BAŞARILI
- Doğrulanan adımlar: Unit tests, Android instrumentation smoke test, instrumentation/live smoke diagnostics, debug APK build, APK artifact upload ve başarılı main-build APK release yayınlama adımı.
- APK artifact: `haritalar-debug-apk-385`
- APK boyutu: 21,666,655 bytes
- Artifact SHA-256: `64ac599cf4f43570e491901c93dc74d1104201e75f288e82201c488584a52c95`
- Supplemental diagnostics artifact SHA-256: `5e617ce68af10a75e11957099e9c65a4112e74f7a6941b40edfc0769862dbae9`
- Son durum: BAŞARILI
- Caution: Bu operasyon rota kimliklerinin trafik sıralamasında korunmasını test eder; yeni kullanıcı arayüzü özelliği veya gerçek TomTom canlı trafik erişimi sağlamaz. Fiziksel cihaz doğrulaması bu operasyon kapsamında yapılmamıştır.
- Sonraki adım: Trafik zincirinin gerçek uygulama çağrı noktası/route-generation entegrasyonunu incelemek ve yalnızca gerçek bir eksik bulunursa değiştirmek.
