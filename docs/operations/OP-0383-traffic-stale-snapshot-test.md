# OP-0383 — Traffic stale-snapshot fallback test

- Tarih: 2026-09-09
- Tür: Test / CI doğrulama
- Amaç: TrafficRouteRankingService zincirinde süresi dolmuş veya düşük güvenli trafik snapshot'ının güvenli şekilde temel ETA'ya dönmesini doğrulamak.
- Değişiklik: `core/src/test/kotlin/com/haritalar/core/traffic/TrafficRouteRankingServiceFreshnessTest.kt` eklendi.
- Test kapsamı: expired snapshot ve LOW confidence snapshot için `trafficApplied=false` ve temel ETA'nın korunması.
- Commit: `9d8c92cc9f6d2b4c7051e34e62cbbf59a3688d63`
- CI Run: #383 / `34393671726`
- Unit tests: BAŞARILI
- Android instrumentation smoke test: BAŞARILI
- Debug APK build: BAŞARILI
- APK artifact: `haritalar-debug-apk-383`, 21,666,660 bytes
- APK SHA-256: `sha256:21d1a00c3231f8927bb9fcb712673cb12122065b78b94b6e3911f723976f81c4`
- Live smoke diagnostics: oluşturuldu ve CI adımı başarılı.
- Genel sonuç: BAŞARILI
- Dikkat: Bu operasyon yeni kullanıcı özelliği değildir; trafik güvenilirliği/fallback test kapsamını güçlendirir. Gerçek canlı TomTom trafik verisi doğrulanmış sayılmaz.
- Sonraki adım: traffic route-generation entegrasyonunda eski generation sonucunun yeni rota sonucunu ezememesini ve stabil routeId davranışını kapsayan testleri incelemek.
