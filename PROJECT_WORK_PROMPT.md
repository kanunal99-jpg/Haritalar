# HARİTALAR — YAŞAYAN PROJE ÇALIŞMA PROMPTU

> Bu dosya statik bir talimat değildir. Her gerçek geliştirme turunda güncel repo durumu, klasörler, kodlar, kaynaklar, başarılı/başarısız işler, yasaklı yollar ve sıradaki hedef yeniden doğrulanarak güncellenecektir.

## 1. Çalışma kuralı

Her `Devam` komutunda GitHub repo gerçekten açılacak ve mevcut durum kontrol edilecektir. Önceki sohbetten tahmin edilerek kod yazılmayacaktır.

Zorunlu sıra:
1. `main` HEAD doğrula.
2. Son commitleri ve ilgili dosyaları oku.
3. Mevcut mimariyle karşılaştır.
4. Daha önce başarıyla yapılmış işi tekrar yapma.
5. Değişiklik gerekiyorsa gerçek kodu değiştir.
6. Commit oluştur.
7. Commiti GitHub'da doğrula.
8. İlgili test/CI/build durumunu kontrol et.
9. Başarısız sonucu başarılı gibi sunma.
10. Çalışma sonunda bu dosyanın proje durumu bölümünü güncelle.

## 2. Gerçeklik standardı

Asla uydurma:
- trafik hızı, yoğunluğu veya ETA
- radar/EDS koordinatı
- API cevabı
- yol kapanması
- ücret/tol bilgisi
- feribot bilgisi
- CI sonucu
- APK/release sonucu
- GitHub commit/değişikliği

Test fixture'ı gerçek veri gibi gösterme. Bir provider yalnızca sözleşme/interface ise canlı provider varmış gibi anlatma.

## 3. Proje hedefi

Profesyonel Android navigasyon uygulaması:

Search → Geocoding → Valhalla routing → 7 alternatif rota → toll/ferry → traffic → safety/radar → GPS → TTS → navigation → off-route detection → reroute → live refresh → route re-ranking.

Kritik zincirlerde hedef:

ANA SERVİS → ALTERNATİF → CACHE/FALLBACK → HATA YÖNETİMİ → GÜVENLİ VARSAYILAN → LOG/İZLEME → SMOKE TEST

## 4. Repo ve klasör yapısı

Ana modüller:
- `app/` — Android UI ve platform entegrasyonu
- `core/` — domain/data mantığı
- `core/src/main/kotlin/com/haritalar/core/traffic/` — trafik domain'i
- `core/src/main/kotlin/com/haritalar/core/safety/` — radar/güvenlik domain'i
- `core/src/main/kotlin/com/haritalar/core/navigation/` — routing/navigation domain'i
- `core/src/main/kotlin/com/haritalar/core/data/` — veri katmanı
- `.github/` — CI/CD

Ana Android ekranı:
- `app/src/main/java/com/haritalar/app/MainActivity.kt`

## 5. Mevcut trafik kodu

Gerçekten mevcut dosyalar:
- `TrafficProvider.kt`
- `TrafficProviderChain.kt`
- `TrafficRouteMatcher.kt`
- `TrafficRouteAdapter.kt`
- `TrafficRouteCostModel.kt`
- `TrafficRouteRanking.kt`
- `RouteTrafficIntelligence.kt`
- `TrafficRouteIntelligence.kt`
- `TrafficRouteOrchestrator.kt`

`TrafficProvider.kt` canlı trafik provider sınırını tanımlar ve core katmanı web sayfası scraping'ini trafik kaynağı olarak kabul etmez.

`TrafficProviderChain` hedef zinciri canlı provider → cache → fallback şeklindedir.

`TrafficRouteMatcher` gerçek provider geometry'sini rota geometry'siyle eşleştirmelidir; eşleşmeyen trafik verisi uygulanmaz.

`TrafficRouteRanking` gerçek kullanılabilir snapshot geldiğinde route ETA/order etkisi oluşturabilir. Snapshot yoksa temel Valhalla ETA/order korunmalıdır.

## 6. Trafik provider planı

Öncelik ve kapsam veri türüne göre belirlenecek:

### Canlı trafik
- TomTom — gerçek canlı trafik provider adayı
- HERE — gerçek canlı trafik provider adayı
- İBB — yalnızca kapsadığı bölgede ve erişim koşulları uygunsa
- Kocaeli yerel kaynakları — yalnızca gerçek makine-okunabilir veri erişimi varsa

### Resmi yol olayları
- KGM — yol çalışması/yol durumu/kapanma-kısıtlama gibi gerçekten sağlanan veriler
- İçişleri/EGM — doğrulanabilir resmi trafik kontrol/EDS kaynakları

### Harita/routing
- OSM — yol ağı/geometri; canlı trafik kaynağı değildir
- Valhalla — routing motoru; canlı trafik sağlayıcısı değildir

### İkincil/community
- RadarKontrol — resmi kaynak gibi gösterilmez; otomatik toplu scraping yapılmaz.

Bir siteye ait web haritasının bulunması tek başına API olduğu anlamına gelmez. Her provider için API/makine-okunabilir veri, authentication, kota, lisans/kullanım şartları, güncellik ve geometry doğrulanmadan entegrasyon tamamlanmış sayılmaz.

## 7. TomTom kota stratejisi

Diğer kaynaklar TomTom'un kotasını artırmaz; TomTom'a yapılması gereken çağrı sayısını azaltabilir.

Bu nedenle:
- refresh throttle
- cache
- aynı bölge/rota için tekrar kullanım
- kapsama uygun provider seçimi
- gereksiz GPS başına API çağrısı yapmama
- başarısız provider'dan sonra fallback
uygulanacaktır.

TomTom/API anahtarı gerektiren servisler kullanıcı onayı olmadan ücret doğuracak şekilde etkinleştirilmeyecek ve secret repoya yazılmayacaktır.

## 8. 7 rota

Uygulamadaki hedef rota kartları:
1. En hızlı
2. En kısa
3. Ücretsiz öncelikli
4. Ücretli hızlı
5. Feribot hızlı
6. Feribotsuz
7. Ücretsiz + feribotsuz

Trafik gerçekten mevcutsa ranking sonucunun bu 7 kartın gerçek sıralamasına bağlanması gerekir.

Örnek UI:
`43 dk • trafik +3 dk`

Trafik verisi yoksa uydurma gecikme gösterilmez; Valhalla base ETA korunur.

## 9. Feribot

Gerçek Valhalla sonucuna göre:
KARA → DENİZ → FERİBOT → DENİZ → KARA
rotası mümkün olmalıdır.

`ALLOW_FERRY`, `AVOID_FERRY`, `PREFER_FERRY` davranışları korunur. Feribot bilgisi uydurulmaz.

## 10. Radar / Safety

Kaynak güven hiyerarşisi hedefi:
1. EGM EDS
2. İçişleri Bakanlığı
3. KGM
4. RadarKontrol
5. OSM
6. kullanıcı raporları

Resmi allow-list tek başına kaydı doğrulanmış yapmaz. Kaynak, HTTPS, timestamp, expiry ve koordinat kontrolleri uygulanır. Yaklaşık ±50 m aynı nokta deduplicate edilir ve daha yüksek otoriteli kaynak korunur.

## 11. Radar uyarı mesafesi — kesin

Birim METREDİR:
- ilk uyarı: 5.000 m = 5 km
- tekrar: her 500 m
- 500 m son uyarı
- sonra geçildi olayı

Sıra:
5.000 → 4.500 → 4.000 → 3.500 → 3.000 → 2.500 → 2.000 → 1.500 → 1.000 → 500 → geçildi

500 km şeklinde yazılmayacak. Geçilen aynı nokta GPS güncellemelerinde tekrar tekrar uyarılmayacak.

## 12. Offline/fallback

Uygulama veri yokluğunda bilgi uydurmaz.

Hedef:
CANLI → CACHE → OFFLINE/RESMİ YOL OLAYI → GÜVENLİ FALLBACK → NORMAL BASE ROUTE/ETA

Offline harita için OSMF'nin public tile servisleri zorla bulk/offline cache kaynağı yapılmayacak; uygun lisanslı/self-hosted vector tile çözümü değerlendirilecek.

## 13. Maliyet ve güvenlik

Öncelik: ücretsiz → açık kaynak → yerel → free tier → ücretli.

Kullanıcı onayı olmadan ücret doğuracak servis etkinleştirilmez.

API key/secret kaynak koduna veya public GitHub'a konulmaz.

## 14. CI/APK standardı

Her gerçek kod değişikliğinden sonra ilgili GitHub Actions sonucu kontrol edilir:
- unit test
- build
- APK artifact
- release/publish

APK gerçekten oluşmadıysa indirme linki varmış gibi davranılmaz.

## 15. Güncel doğrulanmış proje durumu

Tarih: 2026-09-08

`main` HEAD:
`9a8d5cf106c84f051a9a23657aac2633f7ae0fd5`

HEAD commit:
`test(traffic): align orchestrator fallback types`

Mevcut HEAD trafik orchestrator testindeki `Double`/`Long` fallback tip uyumsuzluğunu düzeltiyor.

### Başarılı / mevcut
- Android uygulama iskeleti
- MapLibre harita
- OpenFreeMap harita görünümü
- GPS
- Nominatim arama
- Valhalla online routing
- çoklu rota seçenekleri
- toll/ferry route seçenekleri
- Türkçe TTS/navigation çekirdeği
- safety/radar domain çekirdeği ve testleri
- trafik domain çekirdeği
- TrafficProviderChain live/cache/fallback sözleşmesi
- TrafficRouteMatcher/Adapter/CostModel/Ranking/Intelligence/Orchestrator
- GitHub Actions debug APK + unit test zinciri

### Eksik / sonraki gerçek işler
- doğrulanmış gerçek canlı trafik provider adapter'ı
- TomTom/HERE için gerçek API erişim ve parser, kullanım şartları/kota doğrulaması
- resmi/yerel kaynakların yalnızca gerçekten erişilebilir makine-okunabilir verileri için provider'lar
- trafik ranking sonucunun MainActivity'deki gerçek 7 route card akışına bağlanmasının uçtan uca doğrulanması
- gerçek trafik snapshot'ı ile integration/smoke test

### Kesinlikle gidilmeyecek yollar
- sahte trafik snapshot
- sahte radar koordinatı
- OSM'yi canlı trafik API'si gibi kullanmak
- web sayfası scraping'i ile izinsiz trafik provider'ı oluşturmak
- test fixture'ını canlı veri diye göstermek
- doğrulanmamış KGM/EGM/İBB/TomTom/HERE API varmış gibi kodlamak
- ücretsiz kotayı sınırsız varsaymak
- API secret'ını repoya koymak
- büyük MainActivity dosyasını okunmadan komple değiştirmek
- başarısız CI'ı başarılı göstermek

## 16. Her turda prompt yenileme zorunluluğu

Bu dosya her gerçek geliştirme turunun sonunda yeniden doğrulanacaktır.

Aşağıdaki alanlardan değişenler güncellenecek:
- HEAD/commit
- klasör yapısı
- dosyalar ve önemli sınıflar
- provider'lar
- referans siteler ve doğrulanmış erişim durumu
- trafik/radar/ferry/routing durumu
- testler
- CI
- APK
- başarılı işler
- başarısız/eksik işler
- gidilmeyecek yollar
- sıradaki gerçek kod hedefi

Eski bilgi güncel durumla çelişiyorsa eski bilgi silinecek/düzeltilecek. Bu belge projenin yaşayan teknik çalışma hafızasıdır.

## 17. Bir sonraki hedef

İlk sıradaki teknik hedef:

**Gerçek trafik provider entegrasyonunu güvenli biçimde hazırlamak ve ardından TrafficProviderChain → TrafficRouteRanking → RouteTrafficPresentation → MainActivity 7 rota kartları zincirini gerçek verili integration testiyle doğrulamak.**

Gerçek provider bulunmadan trafik değeri üretilmeyecek.
