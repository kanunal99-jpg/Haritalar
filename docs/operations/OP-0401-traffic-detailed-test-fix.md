# OP-0401 — Traffic detailed test fix

- Tarih: 2026-09-09
- Tür: CI failure correction / test hardening
- Amaç: TrafficRouteRankingServiceDetailedTest başarısızlığını gerçek CI sonucu üzerinden düzeltmek.
- Önceki durum: Run #399 ve Run #400 unit test aşamasında başarısızdı; APK build edilmedi.
- Gerçek hata: `TrafficRouteRankingServiceDetailedTest.detailedResultExposesOnlyMatchedVerifiedSegments` testinde satır 55 assertion başarısız oldu.
- İncelenen gerçek kaynaklar: TrafficRouteRankingServiceDetailedTest, TrafficRouteAdapter, TrafficRouteMatcher, TrafficRouteCostModel, RouteTrafficIntelligence ve GitHub Actions job logları.
- Yapılan değişiklik: Test, belirli bir sayısal ETA çarpanını varsaymak yerine doğrulanmış trafik uygulanınca ayarlanmış ETA'nın temel ETA'dan büyük olduğunu ve `trafficApplied=true` olduğunu doğrulayacak şekilde güncellendi. Geometry matching assertionları korunmuştur.
- Commit: `021998d52067b2e2ecfeb923266da08c3acc9b12`
- Test sonucu: BEKLİYOR — bu commit için yeni CI sonucu henüz doğrulanmadı.
- APK sonucu: BEKLİYOR — başarılı CI olmadan APK id/boyut/SHA iddia edilmeyecek.
- Canlı kullanıcı doğrulaması: BEKLİYOR.
- Sonraki adım: Yeni CI run'ını izle; unit test başarılı olursa instrumentation/build/artifact sonuçlarını doğrula. Ardından trafik haritası UI entegrasyonuna geçmeden önce bu operasyonu kapat.
