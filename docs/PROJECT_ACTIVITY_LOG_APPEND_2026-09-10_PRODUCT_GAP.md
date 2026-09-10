# HARİTALAR — FAALİYET GÜNLÜĞÜ EK KAYDI
## 2026-09-10 — Kullanıcı geri bildirimi sonrası ürün kapsamı ve harita katmanları

- **İşlem türü:** Canlı kullanıcı gözlemi → ürün açığı analizi → anayasa güncellemesi → gerçek kod geliştirmesi.
- **Kullanıcı doğrulaması:** **BAŞARISIZ**. Gerçek cihazda trafik verisi görünmedi; kullanıcı ayrıca trafik ışıkları, hız kameraları/radar, tramvay, tren/metro, AVM, cami, benzin istasyonu, pazar yeri ve kategori arama deneyiminin eksik olduğunu bildirdi.
- **Kontrol edilen ana dosyalar:** `PROJECT_WORK_PROMPT.md`, `PROJECT_DETAILS.md`, `docs/UI_REFERENCE_2026-09-10.md`, `MainActivity.kt`, `LiveNavigationPoiLayer.kt`, `NavigationPoiModel.kt`, `NavigationPoiDetails.kt`, `OsmPoiQuery.kt`, `OsmPoiParser.kt`, `TrafficRouteRankingService.kt`, `TrafficRouteMatcher.kt`, `TrafficRouteAdapter.kt`, `TomTomTrafficProvider.kt`.
- **Referans davranış araştırması:** Google Maps katman/trafik/POI davranışı, Waze canlı trafik ve yeniden rota davranışı, Yandex rota/trafik/toplu taşıma davranışı incelendi. Bunlar görsel kopya değil, ürün kabul kriteri olarak kaydedildi.
- **Önemli kök neden bulgusu:** Tekil Flow Segment Data çağrıları rota üzerinde veri üretse bile harita genelinde görünür trafik katmanı sağlamıyordu. TomTom'un resmi Raster Flow Tiles servisi doğrudan harita üstüne bindirilebilir renkli trafik karoları sağlıyor. Bu nedenle gerçek harita trafik katmanı için raster trafik kaynağı eklendi.
- **Kod değişikliği:** `NavigationPoiModel.kt` trafik ışığı, hız kamerası/radar, tramvay, tren/metro kategorileriyle genişletildi.
- **Kod değişikliği:** `OsmPoiQuery.kt` trafik kontrolü ve raylı sistem sorguları ile kategori kapsamlı sorgu fonksiyonuyla genişletildi.
- **Kod değişikliği:** `OsmPoiParser.kt` isimsiz trafik kontrolü/toplu taşıma noktaları için güvenli varsayılan kategori adları eklendi.
- **Kod değişikliği:** `NavigationPoiDetails.kt` kategori etiketleri Türkçeleştirildi.
- **Kod değişikliği:** `LiveNavigationPoiLayer.kt` TomTom `relative0` raster trafik katmanını MapLibre üzerine ekliyor; POI noktaları kategoriye göre daha görünür çiziliyor.
- **Kod değişikliği:** `TrafficRouteRankingService.kt` her alternatif rota için dört dengeli trafik örnek noktası kullanıyor; tek midpoint kaçırma riski azaltıldı.
- **Anayasa eki:** `docs/CONSTITUTION_AMENDMENT_2026-09-10_PRODUCT_GAP_AND_MAP_LAYERS.md` ile ürün kabul zinciri genişletildi.
- **Güvenlik:** OSM hız kamerası/radar kayıtları resmi enforcement verisi gibi sunulmayacak. Google Places gibi maliyet doğurabilecek servisler kullanıcı onayı olmadan eklenmeyecek.
- **Test durumu:** Yeni değişikliklerin CI sonucu bu kayıt anında doğrulanmış değildir.
- **APK durumu:** Yeni APK henüz doğrulanmış değildir.
- **Sonuç:** **BEKLİYOR** — CI → APK → gerçek cihazda trafik + POI + bilgi kartı + rota akışı kontrolü.
- **Açık UI işi:** Arama alanına gerçek kategori kısayol çubuğu (`AVM`, `Cami`, `Benzin`, `Market`, `Pazar`, `Otopark`, `Hastane`, `Eczane`, `Restoran`, `Tren`, `Metro`, `Tramvay`) eklenmesi.
- **Kabul kuralı:** Kullanıcı `KONTROL BAŞARILI` demeden bu özellikler tamamlanmış sayılmayacak.
