# HARİTALAR — VİZYON & REFERANS EKOSİSTEMİ

> Bu belge HARİTALAR'ın gelişiminde referans alınacak gerçek, erişilebilir ve teknik olarak anlamlı projeleri/siteleri tanımlar. Amaç başka ürünleri kopyalamak değil; mimari fikirleri, güçlü yönleri, sınırları, lisansları, veri doğruluğunu ve uygulanabilirliği karşılaştırarak HARİTALAR'ın kendi vizyonunu geliştirmektir.

## 1. Referans kullanma kuralı

Her yeni özellik için referanslar şu sırayla değerlendirilir:

`Kaynak → Özellik → Teknik yaklaşım → Lisans/şartlar → Maliyet/kota → HARİTALAR'a uygunluk → Gerçek implementasyon → Test`

Hiçbir referans site canlı veri kaynağı olarak otomatik kabul edilmez. Bir servisin var olması, ücretsiz veya sınırsız olduğu anlamına gelmez. Kullanım şartları ve kota ayrıca doğrulanır.

---

## 2. Ana referanslar

### 2.1 OpenStreetMap — veri ekosistemi

Resmi site: https://www.openstreetmap.org/

**HARİTALAR için rolü:** Temel açık coğrafi veri ekosistemi.

**Referans alınacak konular:**
- Yol ve yol ağı verisi
- POI veri modeli
- OSM etiketleme yaklaşımı
- Açık coğrafi veri ekosistemi

**Kritik sınır:** OpenStreetMap harita/coğrafi veri kaynağıdır; canlı trafik sağlayıcısı olarak kabul edilmez.

**Vizyon:** Açık verinin üzerine gerçek zamanlı ve doğrulanmış servisleri ayrı katmanlar halinde koymak.

---

### 2.2 Valhalla — ana routing referansı

Resmi dokümantasyon: https://valhalla.github.io/valhalla/

**HARİTALAR'daki rolü:** Mevcut routing motoru.

Valhalla açık kaynak bir routing motorudur; OpenStreetMap verileriyle çalışır ve zaman/mesafe matrisleri, isochrone, map matching, tur optimizasyonu ve multimodal/time-based routing gibi yeteneklere sahiptir.

**HARİTALAR için referans alanları:**
- Dinamik ve özelleştirilebilir routing
- Farklı costing profilleri
- Multimodal/feribot senaryoları
- Map matching
- Manevra/turn-by-turn verisi
- Gelecekte rota maliyetlerinin daha akıllı modellenmesi

**Vizyon:** Valhalla yalnızca "rota çizme" motoru değil, HARİTALAR'ın route intelligence katmanının temel altyapılarından biri olarak ele alınacak.

---

### 2.3 MapLibre — harita/rendering referansı

Resmi Android dokümantasyonu: https://maplibre.org/maplibre-native/docs/book/platforms/android/

Android API: https://maplibre.org/maplibre-native/android/api/

**HARİTALAR'daki rolü:** Harita/rendering altyapısı.

**Referans alanları:**
- Android native harita rendering
- Vector map
- Style sistemi
- Kamera ve gesture yönetimi
- Rendering backend seçenekleri
- Modern Android performansı

**Vizyon:** Harita ekranı yalnız görsel katman değil; navigasyon state'i, route geometry, trafik, POI ve güvenlik katmanlarının birleştiği ana görsel platform olacak.

---

### 2.4 GraphHopper — alternatif routing/optimizasyon referansı

Resmi site: https://www.graphhopper.com/

GitHub: https://github.com/graphhopper/graphhopper

**Referans alanları:**
- Routing engine mimarisi
- Custom routing profiles
- Map matching
- Alternative routes
- Matrix/optimization yaklaşımı
- Performans ve ölçekleme

**HARİTALAR vizyonundaki kullanım:** Valhalla'nın alternatifi olarak doğrudan değiştirmek zorunlu değildir. Daha iyi routing algoritmaları veya profil tasarımları gerektiğinde karşılaştırmalı teknik referanstır.

---

### 2.5 OSRM — yüksek performanslı routing referansı

Resmi site: https://project-osrm.org/

GitHub: https://github.com/Project-OSRM/osrm-backend

**Referans alanları:**
- Yüksek performanslı route hesaplama
- Road-network preprocessing
- Table/matrix yaklaşımı
- Server-side routing optimizasyonu

**HARİTALAR vizyonundaki kullanım:** Özellikle performans, cache, route calculation latency ve büyük ağlarda ölçekleme kararlarında benchmark/referans.

---

### 2.6 openrouteservice — multimodal ve coğrafi servis referansı

Resmi site: https://openrouteservice.org/

API dokümantasyonu: https://openrouteservice.org/dev/

**Referans alanları:**
- Directions
- Geocoding
- Matrix
- POI
- Isochrones
- Farklı ulaşım profilleri
- OpenStreetMap tabanlı servis yaklaşımı

**HARİTALAR vizyonundaki kullanım:** Özellikle multimodal, erişilebilirlik, POI ve coğrafi analiz özelliklerinin ileride tasarlanmasında referans.

**Kural:** Harici API kullanımı ancak erişim, authentication, kota, maliyet ve kullanım şartları doğrulanırsa değerlendirilir.

---

### 2.7 Ferrostar — modern navigation SDK referansı

Resmi site: https://www.ferrostar.com/

GitHub: https://github.com/astute-labs/ferrostar

**Referans alanları:**
- Navigation SDK mimarisi
- Route progress
- Maneuver progression
- Rerouting
- Navigation state yönetimi
- Cross-platform yaklaşım

**HARİTALAR vizyonundaki kullanım:** Navigation state machine, route progress ve modern navigasyon SDK mimarisini değerlendirmek için referans.

**Kural:** Mimarinin doğrudan kopyalanması yerine HARİTALAR'ın Kotlin/Android + mevcut core yapısına uygun tasarım çıkarılır.

---

## 3. Referansların HARİTALAR vizyonuna bağlanması

### Katman 1 — Harita

`OpenStreetMap data → MapLibre rendering`

### Katman 2 — Routing

`Valhalla → route alternatives → route cost/ranking`

### Katman 3 — Navigation

`GPS → map matching/progress → maneuver → TTS → off-route → reroute`

### Katman 4 — Traffic Intelligence

`Real provider → geometry matching → confidence → ETA cost → ranking → safe fallback`

### Katman 5 — Context Intelligence

`POI → road context → toll/ferry → verified safety data → user preferences`

### Katman 6 — Resilience

`primary → alternate → cache → fallback → safe default → logging → smoke test`

Uzun vadeli hedef, bu katmanların birbirinden bağımsız test edilebildiği ve bir dış servis arızasının tüm navigasyonu bozmadığı bir platform oluşturmaktır.

---

## 4. Vizyon karşılaştırma matrisi

| Referans | Ana konu | HARİTALAR'daki karşılığı | Öncelik |
|---|---|---|---|
| OpenStreetMap | Açık coğrafi veri | Harita/veri tabanı | P0 |
| Valhalla | Routing/navigation | Ana routing motoru | P0 |
| MapLibre | Native map rendering | Ana harita UI | P0 |
| GraphHopper | Routing/profil/optimization | Alternatif teknik benchmark | P1 |
| OSRM | Routing performance | Performans benchmark | P1 |
| openrouteservice | Multimodal/geo services | Gelecek özellik referansı | P2 |
| Ferrostar | Navigation SDK | Navigation architecture benchmark | P1 |

---

## 5. Referanslardan öğrenilecek ama körü körüne alınmayacak konular

- Alternative route ranking
- Map matching
- Dynamic routing
- Time-dependent routing
- Multimodal routing
- Navigation state machines
- Traffic confidence
- Route geometry matching
- ETA recalculation
- Reroute strategies
- Offline/cache architecture
- Battery-aware GPS scheduling
- Rendering performance
- POI/data freshness
- Matrix/optimization

Her konu için önce mevcut HARİTALAR kodu incelenir. Aynı problem zaten çözülmüşse yeni paralel mimari oluşturulmaz.

---

## 6. Canlı veri ve servis politikası

Aşağıdakiler kesinlikle yapılmaz:

- Referans sitelerden scraping.
- Lisans şartlarını yok saymak.
- Public/demo endpoint'i sınırsız production servisi gibi kullanmak.
- Test verisini canlı trafik gibi göstermek.
- Ücretsiz kotayı sınırsız varsaymak.
- API key'i source code'a koymak.
- Doğrulanmamış radar/EDS/POI bilgisini kesin gerçek olarak göstermek.

Her harici servis için:

`erişim → authentication → kota → maliyet → lisans → veri tazeliği → fallback → test`

kontrol edilir.

---

## 7. Gelecek referans araştırma başlıkları

İleride ayrıca araştırılabilecek alanlar:

- OpenStreetMap routing ecosystem
- MapLibre navigation ecosystem
- OpenStreetMap-based offline navigation
- Valhalla advanced costing
- GraphHopper custom models
- OSRM performance architecture
- Ferrostar navigation state architecture
- Gerçek trafik provider karşılaştırması
- Open data POI kaynakları
- Türkiye için doğrulanabilir yol/ücret/feribot veri kaynakları
- Battery-aware navigation
- Offline map packaging
- Route learning/personalization

Bu araştırmaların hiçbir sonucu kod değişikliğine otomatik dönüşmez. Önce teknik/ürün gereksinimi ve gerçek veri kaynağı doğrulanır.

---

## 8. Sonuç

HARİTALAR'ın vizyonu:

> **Açık harita verisi + güçlü routing + gerçek trafik zekâsı + dayanıklı navigasyon + doğrulanmış veri katmanları + mümkün olduğunca offline çalışma + güvenli ve şeffaf kullanıcı deneyimi.**

Referans projeler HARİTALAR'ın sınırı değil, karşılaştırma laboratuvarıdır.

Her yeni özellikte soru şudur:

**"Başka biri bunu nasıl yapmış? Biz bunu daha güvenilir, daha dayanıklı, daha şeffaf ve HARİTALAR'ın mimarisine daha uygun nasıl yapabiliriz?"**
