# OP-0402 — Verified route traffic segment map colors

- Tarih: 2026-09-09
- Tür: Yeni kullanıcıya görünen özellik
- Amaç: Doğrulanmış ve geometri-eşleşmiş trafik segmentlerini rota üzerinde gerçek congestion seviyesine göre renklendirmek.
- Önceki durum: Rota çizgisi tek mavi LineLayer idi; trafik yalnızca rota kartı/ETA sıralamasında görünüyordu.
- Kaynak gerçekliği: `TrafficRouteRankingService.DetailedResult.matchedSegmentsByRoute` yalnızca kullanılabilir, düşük güvenli olmayan ve rota geometrisiyle eşleşmiş provider segmentlerini döndürür. `TrafficSegment.congestion` provider verisidir; renkler bu alandan türetilir.
- Güvenlik: `UNKNOWN` trafik rengi üretilmez; temel rota mavi kalır. Trafik geometrisi yoksa overlay çizilmez. Bu değişiklik map-wide traffic eklemez ve canlı provider credential varmış gibi davranmaz.
- Yeni davranış: FREE=yeşil, LIGHT=sarı, MODERATE=turuncu, HEAVY=uyarı kırmızısı, SEVERE=koyu kırmızı. Renk yalnızca doğrulanmış matched provider geometry üzerinde gösterilir.
- CI/otomasyon: İlk tek-seferlik patch workflow'u Run #1'de job başlatmadan başarısız oldu; daha sonra mevcut ve doğrulanmış constitution workflow'u kontrollü patch yürütücüsü olarak kullanıldı. Run #9'da gerçek hata, OP-0402 dosyasının patch sırasında henüz mevcut olmamasıydı; bu dosya şimdi ayrıca oluşturuldu ve işlem yeniden kuyruğa alındı.
- Canlı kullanıcı kontrolü: BEKLEYOR — proje sahibi gerçek APK üzerinde kontrol etmelidir.
- Devam kuralı: Proje sahibi `KONTROL BAŞARILI` demeden map-wide traffic veya varış bayrağı gibi sonraki kullanıcı özelliklerine geçilmeyecek.
