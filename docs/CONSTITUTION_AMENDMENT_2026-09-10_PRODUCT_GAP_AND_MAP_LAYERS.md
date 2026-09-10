# HARİTALAR — ANAYASA EKİ
## 2026-09-10 — Ürün Açığı / Harita Katmanları / Arama UX

Bu kayıt proje anayasasına tamamlayıcı append-only kabul kriterleri ekler.

### Kullanıcı canlı doğrulaması

Gerçek cihaz ekranında şu açıklar doğrulandı:

- TomTom trafik bilgisi rota ekranında uygulanmadı; rota mavi kaldı.
- Harita üzerinde beklenen trafik renk katmanı görünmedi.
- Trafik ışıkları, hız kamerası/radar bilgi noktaları, tramvay, tren/metro ve kapsamlı POI katmanları kullanıcı beklentisindeki görünürlük seviyesinde değil.
- Arama alanında kategori kısayolları (`AVM`, `Cami`, `Benzin`, `Market`, `Pazar yeri` vb.) yok.
- POI seçimi sonrası bilgi kartı → rota seçenekleri akışı istenen ürün deneyimi seviyesine ulaşmadı.
- Kullanıcı ürünü Google Haritalar, Waze ve Yandex Haritalar seviyesindeki temel harita keşfi, trafik, POI ve rota akışıyla kıyaslıyor.

### Ürün kabul modeli

Haritalar yalnızca bir `adres → rota` uygulaması olarak kabul edilmeyecektir. Temel ürün döngüsü:

`Haritayı keşfet → katmanı seç → noktayı/işletmeyi seç → bilgi kartını gör → rota seçeneklerini aç → rotayı seç → canlı trafikle navigasyon → yeniden rota / tekrar ölç`

### Harita katmanları

Zorunlu katman ailesi:

1. **Canlı trafik:** TomTom gerçek trafik akışı. Harita genelinde görünür renk katmanı; rota üzerinde ayrıca doğrulanmış route-local trafik segmentleri.
2. **Trafik kontrolleri:** OSM'den trafik ışıkları ve hız kamerası/radar noktaları. Bu veriler `OSM harita bilgisi` olarak etiketlenecek; doğrulanmış resmi enforcement verisi gibi sunulmayacak.
3. **Toplu taşıma:** tren, metro, tramvay ve duraklar.
4. **POI:** AVM/market, cami/ibadethane, benzin istasyonu, pazar yeri, otopark, hastane, eczane, restoran/kafe, okul, şarj istasyonu vb.

### POI etkileşim kabulü

- Haritada POI görünür olmalı.
- İlk dokunuşta bilgi kartı açılmalı.
- Kartta isim, kategori, mümkünse adres ve çalışma saatleri gösterilmeli.
- Karttan `Rota seçenekleri / Buraya git` aksiyonu ile mevcut çoklu rota motoru açılmalı.
- Rota seçimi sonrası seçilen rota haritada gösterilmeli.
- POI verisi yoksa sahte kayıt oluşturulmayacak.

### Arama UX kabulü

Arama alanı yalnızca serbest metin değil, hızlı kategori keşfi de sağlamalı:

`AVM | Cami | Benzin | Market | Pazar | Otopark | Hastane | Eczane | Restoran | Kafe | Tren | Metro | Tramvay`

Kategori seçimi mevcut görünüm/konum çevresindeki gerçek OSM POI verisini getirmeli; sonuçlar haritada görünmeli ve seçilebilir olmalıdır.

### Referans ürün davranışı

Google Haritalar trafik, toplu taşıma, yakındaki yerler ve harita katmanlarını ayrı ayrı görünür kılar; trafik renkleri ve olay ikonları etkileşimlidir. citeturn0search0turn0search2

Waze'in temel referans davranışı canlı trafik uyarıları ve gerçek zamanlı yeniden rotalamadır. citeturn0search5turn0search10

Yandex Haritalar rota seçimi sırasında trafik koşullarını rota rengine yansıtır ve toplu taşıma durak/hatlarını etkileşimli gösterir. citeturn0search8turn0search16

Bu referanslar görsel kopyalama talimatı değildir; ürün davranışı için kabul kriteridir.

### TomTom trafik kararı

TomTom Traffic API'nin raster flow tile servisi doğrudan renkli trafik akışını harita karoları olarak sunmaktadır. `relative0` stili serbest akışa göre yoğunluğu vurgular. Bu nedenle Haritalar'da yalnızca tekil Flow Segment Data çağrılarına güvenmek yerine, harita genelinde TomTom raster trafik katmanı kullanılması ürün kabul zincirine eklendi. citeturn3search1

### Maliyet / güvenlik

- Google Places gibi ücretli bir POI API'si kullanıcı onayı olmadan eklenmeyecek.
- Mevcut OSM/Overpass ücretsiz katmanları önceliklidir.
- TomTom anahtarı public source'a yazılmayacak; mevcut `BuildConfig` zinciri kullanılacak.
- TomTom anahtarının Android paketinden çıkarılabilir olması ayrı bir hardening işidir; canlı trafik işlevinden sonra proxy/key restriction planlanacaktır.

### 2026-09-10 gerçek kod değişiklikleri

- POI kategorileri trafik ışığı, hız kamerası/radar, tramvay ve tren/metro ile genişletildi.
- Overpass sorgusu bu kategorileri kapsayacak şekilde genişletildi.
- İsimsiz trafik kontrolü/toplu taşıma noktaları artık güvenli kategori adıyla gösterilebiliyor.
- POI görsel işaretleri kategoriye göre daha belirgin hale getirildi.
- Gerçek TomTom `relative0` raster trafik katmanı MapLibre haritasına eklendi.
- Rota trafik örneklemesi rota başına dört noktaya çıkarıldı.
- POI bilgi kartı kategori adları Türkçeleştirildi.

### Henüz tamamlanmamış / doğrulanmamış

- Yeni kodun CI sonucu bu kayıt anında tamamlanmış kabul edilmez.
- Yeni APK fiziksel cihazda test edilmeden trafik ve POI katmanları `BAŞARILI` ilan edilmeyecek.
- Kategori kısayol çubuğu ve gelişmiş arama UX'i ayrı bir UI işi olarak açık kalacaktır.
- Kullanıcı `KONTROL BAŞARILI` demeden ürün kabulü yapılmayacaktır.
