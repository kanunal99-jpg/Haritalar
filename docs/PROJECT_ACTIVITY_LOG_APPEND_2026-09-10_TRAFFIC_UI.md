# HARİTALAR — FAALİYET GÜNLÜĞÜ EK KAYDI

> Bu dosya `docs/PROJECT_ACTIVITY_LOG.md` için 2026-09-10 tarihli append-only ek kayıttır. Ana günlükteki geçmiş kayıtlar silinmemiş veya değiştirilmemiştir.

## İşlem — 2026-09-10 / canlı trafik kullanıcı doğrulaması sonrası düzeltme

- **Tür:** Canlı kullanıcı doğrulaması → hata analizi → trafik çekirdeği düzeltmesi → UI düzeltmesi → anayasa eki.
- **Amaç:** Kullanıcının gerçek cihazda gördüğü `trafik verisi uygulanmadı` durumunun nedenini gidermek ve arayüzde doğrulanmış canlı trafik bilgisini kaybetmemek.
- **Kontrol edilenler:** `PROJECT_WORK_PROMPT.md`, `PROJECT_DETAILS.md`, `docs/PROJECT_ACTIVITY_LOG.md`, `MainActivity.kt`, `TrafficRouteRankingService.kt`, `TrafficRouteMatcher.kt`, `TrafficRouteRanking.kt`, `TrafficRouteAdapter.kt`, `TrafficRouteCostModel.kt`, `RouteTrafficIntelligence.kt`, `RouteTrafficUiModel.kt`.
- **Önceki gerçek durum:** CI'da TomTom canlı API erişimi ve Android runtime credential testi başarılıydı; ancak kullanıcı gerçek uygulama ekranında rota kartlarında `trafik verisi uygulanmadı` gördü.
- **Kullanıcı sonucu:** **KULLANICI DOĞRULAMASI / BAŞARISIZ**.
- **Kök neden:** Traffic Flow Segment Data nokta tabanlı olduğu halde alternatif rotaların geometrileri tek birleşik trafik rotası olarak örnekleniyordu. Bu, her alternatifin canlı trafik gözlemi almasını garanti etmiyordu. Ayrıca UI modeli doğrulanmış trafik olup ETA farkı yuvarlandığında trafik durumunu tamamen gizliyordu.
- **Uygulanan değişiklik 1:** `TrafficRouteRankingService.kt` rota-başına temsilci nokta kullanacak ve provider-chain gözlemini ilgili rota ile sınırlayacak şekilde değiştirildi.
- **Uygulanan değişiklik 2:** `TrafficRouteRankingServiceTest.kt` her rota için ayrı canlı trafik gözlemi ve doğrulanmış segmentin ETA etkisini test edecek şekilde genişletildi.
- **Uygulanan değişiklik 3:** `RouteTrafficUiModel.kt` doğrulanmış trafik ETA farkı 0.5 saniye altında olsa bile trafik bilgisini `canlı trafik` olarak koruyacak şekilde düzeltildi.
- **Dokümantasyon:** `docs/CONSTITUTION_AMENDMENT_2026-09-10_TRAFFIC_UI.md` oluşturuldu; canlı trafik ile credential testinin ayrı kabul kriterleri olduğu ve UI kuralları kalıcılaştırıldı.
- **Commitler:** `eaed2c54ef40327f196f84f2de97a7ddbd5987cb`, `52f2a078c705704e821ec996c8c54f9035015418`, `8ba2bcc9ef27ab36f565e49f8a3440f8a72a693b`, `baed605b3d1ad4d15b95d04000aa6eb4a40a2f05`.
- **Test:** Yeni değişikliklerin CI sonucu bu kayıt oluşturulduğu anda henüz doğrulanmadı.
- **APK:** Yeni düzeltme APK'sı henüz doğrulanmadı.
- **Sonuç:** **BEKLİYOR** — CI → APK → fiziksel cihaz canlı trafik kontrolü.
- **Sonraki adım:** Yeni HEAD için Android CI sonucunu kontrol etmek; başarısızsa ilk hatayı düzeltmek; başarılıysa yeni APK artifact/sha256/release bilgisini doğrulamak; ardından kullanıcıdan gerçek cihaz rota kontrolü almak.
