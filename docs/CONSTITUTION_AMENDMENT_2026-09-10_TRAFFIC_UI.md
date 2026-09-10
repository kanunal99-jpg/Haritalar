# HARİTALAR — ANAYASA EKİ
## 2026-09-10 — Trafik + Rota Arayüzü Canlı Doğrulama Kaydı

Bu belge `PROJECT_WORK_PROMPT.md` içindeki çalışma anayasasının tamamlayıcı, append-only ek kaydıdır. Mevcut anayasa metni değiştirilmeden yeni kabul/öğrenim maddelerini kalıcılaştırır.

### 1. Kullanıcı canlı gözlemi

- Kontrol edilen uygulama: HARİTALAR Android uygulaması.
- Kullanıcı tarafından bildirilen gerçek sonuç: rota seçenekleri ekranında `trafik verisi uygulanmadı` mesajı görüldü.
- Ekranda üç rota seçeneği görünüyordu; trafik etkisi rota kartlarına uygulanmamıştı.
- Bu sonuç, GitHub CI'daki canlı TomTom API ve Android runtime credential testlerinin başarılı olmasının tek başına rota üzerinde trafik eşleştirmesinin başarılı olduğu anlamına gelmediğini doğruladı.
- Durum: **KULLANICI DOĞRULAMASI / BAŞARISIZ** (trafik rota üzerinde uygulanmadı).

### 2. Yeni teknik kabul kuralı

`TomTom API erişimi başarılı` ile `rota üzerinde trafik uygulandı` ayrı kabul kriterleridir.

Zorunlu zincir:

`BuildConfig credential → TomTom canlı Flow API → route-local traffic observation → geometry match → ETA/ranking → route-card UI`

Bu zincirin son halkaları canlı cihazda doğrulanmadan trafik özelliği `BAŞARILI` kabul edilmeyecektir.

### 3. Trafik örnekleme kararı

TomTom Flow Segment Data nokta tabanlı olduğu için alternatif rotaların tüm geometrilerini tek bir birleşik `TrafficRoute` olarak örneklemek, bazı rotaların yeterince gözlemlenmemesine yol açabilir.

Yeni uygulanan karar:

- Her rota için bir temsilci rota noktası seçilir.
- Her rota kendi `TrafficRoute` örneğiyle provider-chain'e gönderilir.
- Trafik segmenti yalnız ait olduğu rota geometrisiyle eşleştirilir.
- Rota sayısı kadar bounded provider gözlemi yapılır; sınırsız istek üretilmez.
- Provider başarısızsa güvenli fallback/base ETA korunur.

### 4. Rota arayüzü kabul kuralları

Rota kartları:

- canlı trafik gerçekten eşleşmişse bunu açıkça göstermeli;
- trafik verisi doğrulanmadıysa canlı trafik varmış gibi göstermemeli;
- trafik verisi mevcut olup ETA farkı yuvarlamada sıfır kalıyorsa yine `canlı trafik` durumu gösterilebilmeli;
- temel ETA ile trafik etkili ETA birbirine karıştırılmamalı;
- kullanıcıya belirsiz `trafik verisi uygulanmadı` mesajı veriliyorsa bunun gerçek nedeni ayrıca loglanabilir olmalı;
- ekran, arama klavyesi açıkken kullanılabilir rota kartlarını gereksiz yere kapatmamalı;
- rota sayısı ve trafik durumu kullanıcıya net gösterilmelidir.

### 5. 2026-09-10 uygulanan kod değişiklikleri

- `TrafficRouteRankingService.kt`: birleşik rota örneklemesi yerine rota-başına temsilci canlı trafik gözlemi ve route-local geometry matching uygulandı.
- `TrafficRouteRankingServiceTest.kt`: her rotanın kendi trafik gözlemini aldığı ve doğrulanmış segmentlerin ETA'yı etkilediği test kapsamına alındı.
- `RouteTrafficUiModel.kt`: doğrulanmış canlı trafik ETA farkı 0 dakikaya yuvarlansa bile UI modelinde kaybolmaması sağlandı; bu durumda `canlı trafik` etiketi üretilir.

### 6. Henüz tamamlanmayanlar

- Bu ek kaydın yazıldığı anda yeni kodun CI/APK sonucu henüz doğrulanmış kabul edilmez.
- Yeni APK'nın fiziksel cihazda rota üzerinde gerçek trafik gösterimi kullanıcı tarafından tekrar kontrol edilmelidir.
- Kullanıcı `KONTROL BAŞARILI` demeden trafik özelliği tamamlanmış sayılmaz.
- Görsel arayüzün daha kapsamlı yeniden tasarımı ayrıca yapılacak; ancak yeni tasarım trafik gerçeklik kurallarını zayıflatmayacaktır.
