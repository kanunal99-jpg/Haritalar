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
- `TomTomTrafficProvider.kt`

`TrafficProvider.kt` canlı trafik provider sınırını tanımlar ve core katmanı web sayfası scraping'ini trafik kaynağı olarak kabul etmez.

`TrafficProviderChain` canlı provider → cache → fallback sırasını uygular; provider id, expiry ve timestamp kontrolleri yapılır.

`TrafficRouteMatcher` gerçek provider geometry'sini rota geometry'siyle eşleştirmelidir; eşleşmeyen trafik verisi uygulanmaz.

`TrafficRouteRanking` kullanılabilir snapshot geldiğinde route ETA/order etkisi oluşturabilir. Expired, provider-mismatch veya LOW-confidence snapshot trafik maliyetine uygulanmaz.

## 6. TomTom provider — mevcut gerçek durum

`TomTomTrafficProvider` gerçek TomTom Traffic Flow Segment Data endpointine adapter sağlar.

Özellikler:
- API key boşsa provider pasiftir ve `supports()` false döner.
- Secret repoya konulmaz; key constructor/configuration üzerinden verilmelidir.
- Web sayfası scraping yapılmaz.
- Route geometry'den en fazla 8 örnek nokta alınır; sınırsız GPS başına çağrı yapılmaz.
- Route yoksa bounds merkezinden tek gerçek provider sorgusu yapılabilir.
- `currentSpeed`, `freeFlowSpeed` ve provider geometry doğrulanmadan segment oluşturulmaz.
- Provider geometry route matching için `TrafficSegment.geometry` içine taşınır.
- Alınan gözlemler 60 saniyelik yerel TTL ile snapshot'a konur; daha üst cache/refresh politikası ayrıca uygulanacaktır.
- Provider hiç geçerli segment parse edemezse snapshot LOW confidence ve boş segment ile döner; sahte trafik üretilmez.

Test:
- `TomTomTrafficProviderTest` API key güvenlik kapısını, gerçek response biçimi parser'ını ve malformed payload davranışını doğrular.

ÖNEMLİ: TomTom adapter'ı kodda mevcut olsa da henüz MainActivity/uygulama DI katmanına canlı API key ile bağlanmış değildir. Bu nedenle canlı trafik ürün özelliği tamamlanmış kabul edilmez.

## 7. Trafik provider planı

Öncelik ve kapsam veri türüne göre belirlenecek:

### Canlı trafik
- TomTom — adapter mevcut, credential/uygulama bağlantısı henüz tamamlanmadı
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

## 8. 7 rota

Uygulamadaki hedef rota kartları:
1. En hızlı
2. En kısa
3. Ücretsiz öncelikli
4. Ücretli hızlı
5. Feribot hızlı
6. Feribotsuz
7. Ücretsiz + feribotsuz

MainActivity'de bu yedi Valhalla isteği gerçek sonuçlardan oluşturuluyor. Ancak trafik ranking sonucu henüz bu UI kartlarının sıralama/ETA alanına bağlanmış değildir.

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

`main` HEAD bu turdaki son kod commitidir:
`656d97dd53e9587fcf795497dbd580714fbe12b2`

HEAD commit:
`fix(traffic): use existing coroutine test helper`

Bu turda önceki HEAD `0b6b0139d06d31c4aea71bd0530286d6983250c5` GitHub'dan doğrulandı. Ardından TomTom adapterı ve testleri eklendi; test dosyasında proje tarafından zaten kullanılan dependency-free coroutine test yardımcı yaklaşımı kullanıldı.

### Bu turdaki gerçek değişiklikler
- `core/src/main/kotlin/com/haritalar/core/traffic/TomTomTrafficProvider.kt` eklendi.
- `core/src/test/kotlin/com/haritalar/core/traffic/TomTomTrafficProviderTest.kt` eklendi.
- `PROJECT_WORK_PROMPT.md` bu değişikliklerin ardından yeniden güncellendi.

### CI doğrulaması

`main` push'ları GitHub Actions `Android APK` workflow'unu tetikliyor.

Bu turdaki son test commit'i `656d97dd53e9587fcf795497dbd580714fbe12b2` için run `275` GitHub'da `queued` durumunda doğrulandı. Bu belge güncellenirken bu run'ın sonucu henüz başarılı olarak doğrulanmamıştır.

Önceki doğrulanmış başarılı run:
- run `271`
- commit `0b6b0139d06d31c4aea71bd0530286d6983250c5`
- conclusion `success`
- debug APK artifact: `haritalar-debug-apk-271`
- unit test reports artifact: `haritalar-unit-test-reports-271`

Son başarılı APK, bu turdaki TomTom değişikliklerini içermediği için yeni APK hazır kabul edilmez.

## 16. Başarılı / mevcut işler

GitHub kodu ve önceki başarılı CI ile doğrulananlar:
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
- TomTom Flow provider adapter kodu ve parser güvenlik testleri (CI sonucu bu tur için henüz beklemede)

## 17. Eksik / sonraki gerçek işler

1. `656d97dd...` için CI sonucunu doğrula.
2. CI başarılı olduktan sonra APK artifact'ını doğrula.
3. TomTom credential/config katmanını secret güvenliğiyle uygulamaya bağla; ücret/kota koşullarını doğrulamadan canlı kullanım açma.
4. TomTom gerçek endpoint erişimini test fixture'ı dışında gerçek credential ile doğrula; response timestamp/freshness ve kota davranışını ayrıca değerlendir.
5. TrafficProviderChain → TrafficRouteRanking → RouteTrafficPresentation → MainActivity 7 rota kartları zincirini gerçek snapshot ile uçtan uca bağla ve test et.
6. Refresh/cooldown/cache katmanını GPS başına gereksiz çağrıyı önleyecek şekilde tamamla.
7. HERE adapterını ancak gerçek erişim koşulları doğrulanırsa değerlendir.

## 18. Kesinlikle gidilmeyecek yollar

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
- CI'da üretilmemiş APK'yı hazır göstermek

## 19. Bir sonraki hedef

İlk sıradaki teknik hedef:

**TomTom provider'ı güvenli configuration/credential katmanına bağlamadan önce mevcut CI sonucunu doğrulamak; ardından gerçek snapshot akışını TrafficProviderChain → TrafficRouteRanking → RouteTrafficPresentation → MainActivity 7 rota kartlarına bağlayan küçük, test edilebilir bir entegrasyon katmanı oluşturmak.**

Gerçek provider bulunmadan trafik değeri üretilmeyecek.
