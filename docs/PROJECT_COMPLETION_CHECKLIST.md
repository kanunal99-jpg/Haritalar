# HARİTALAR — GERÇEK PROJE TAMAMLANMA KONTROL LİSTESİ

> **Durum yalnızca GitHub'da doğrulanmış gerçeklik üzerinden işaretlenir.**
> 
> Durumlar: `TAMAM` · `DEVAM EDİYOR` · `BAŞARISIZ` · `BEKLİYOR` · `KULLANICI DOĞRULAMASI`
>
> Bu belge ürünün "%100 tamamlandı" ilanı değildir. Fiziksel cihaz, uzun süreli saha, gerçek offline ve canlı sağlayıcı doğrulamaları yapılmadan tamamlanmış sayılmaz.

## 1. Harita altyapısı

- [TAMAM] Android/Kotlin uygulama iskeleti
- [TAMAM] MapLibre harita entegrasyonu
- [TAMAM] Harita style yükleme akışı
- [TAMAM] Kamera/viewport yönetimi
- [TAMAM] Kullanıcı konumu katmanı
- [TAMAM] Activity yaşam döngüsü ve MapView yeniden oluşturma smoke testi
- [KULLANICI DOĞRULAMASI] Fiziksel cihazda aç-kapat-yeniden aç yaşam döngüsü kontrolü kullanıcı tarafından "düzeldi" olarak doğrulandı
- [BEKLİYOR] Uzun süreli gerçek cihaz saha testi

## 2. GPS ve navigasyon

- [TAMAM] GPS konum akışı
- [TAMAM] Rota ilerleme/progress hesaplama
- [TAMAM] Off-route algılama
- [TAMAM] Yaklaşık 60 m yeniden rota eşiği
- [TAMAM] Reroute cooldown
- [TAMAM] Route-generation koruması
- [TAMAM] Eski rota sonucunun yeni rotaya yazılmasını engelleme
- [TAMAM] Türkçe sesli yönlendirme zinciri
- [TAMAM] Varış algılama ve navigasyon sonlandırma
- [BEKLİYOR] Gerçek cihazda baştan sona rota+navigasyon saha doğrulaması

## 3. Rota motoru

- [TAMAM] Valhalla tabanlı gerçek rota alma
- [TAMAM] Yedi rota varyantı
- [TAMAM] Stable routeId zinciri
- [TAMAM] Trafik sonrası rota sıralaması ve kart eşleşmesi
- [TAMAM] Feribot/toll varyantlarının gerçek rota motoru sonuçlarına bağlanması
- [BEKLİYOR] Rota sağlayıcısı tamamen başarısız olduğunda uçtan uca kullanıcı güvenli davranışı için saha doğrulaması
- [BEKLİYOR] Gerçek cihazda tüm rota varyantlarının uçtan uca doğrulanması

## 4. Trafik

- [TAMAM] TrafficProvider soyutlaması
- [TAMAM] Provider chain/fallback
- [TAMAM] Geometri tabanlı rota eşleştirme
- [TAMAM] Trafik cost model
- [TAMAM] Trafik ranking
- [TAMAM] TrafficRefreshCoordinator
- [TAMAM] Minimum 60 sn refresh politikası
- [TAMAM] In-flight koruması
- [TAMAM] Stale result / route-generation koruması
- [TAMAM] Expired snapshot fallback testi
- [TAMAM] Low-confidence snapshot fallback testi
- [TAMAM] Yedi stable routeId'nin trafik sıralamasında korunması testi
- [TAMAM] TomTom provider adapter kodu
- [BEKLİYOR] Canlı TomTom credential ile gerçek trafik sağlayıcısı doğrulaması
- [BEKLİYOR] Gerçek cihazda canlı trafik saha doğrulaması
- [BEKLİYOR] Yeni route generation sırasında eski in-flight trafik isteği yarışının uçtan uca doğrulaması

## 5. POI

- [TAMAM] OSM/Overpass viewport sorgusu
- [TAMAM] Viewport debounce
- [TAMAM] Refresh throttle
- [TAMAM] Throttle sonrasında gecikmeli retry
- [TAMAM] Son başarılı POI setini hata durumunda koruma
- [TAMAM] POI parser node/way koordinatları
- [TAMAM] Adres ve doğrulanmış opening_hours aktarımı
- [TAMAM] Market/supermarket sınıflandırması
- [TAMAM] Akaryakıt sınıflandırması
- [TAMAM] Dinlenme tesisi sınıflandırması
- [TAMAM] Otopark sınıflandırması
- [TAMAM] Restoran/kafe sınıflandırması
- [TAMAM] İbadethane sınıflandırması
- [TAMAM] Toplu taşıma sınıflandırması
- [TAMAM] Pazaryeri sınıflandırması
- [TAMAM] Kamu/devlet kurumu sınıflandırması
- [TAMAM] POI detay kartı
- [TAMAM] "Buraya git" ile mevcut routing zincirine bağlanma
- [TAMAM] Pazaryeri + kamu kurumu parser testi
- [TAMAM] POI viewport throttle retry testi
- [BEKLİYOR] Gerçek OSM verisiyle fiziksel cihaz saha testi
- [BEKLİYOR] Ayrı `brand` alanı ve marka odaklı UI (mevcut modelde dedicated brand field yok)
- [BEKLİYOR] İkinci POI sağlayıcısı/fallback

## 6. Offline ve dayanıklılık

- [TAMAM] Trafik verisi yoksa base ETA'ya güvenli dönüş
- [TAMAM] Provider fallback
- [TAMAM] Expired/low-confidence traffic fallback
- [TAMAM] Stale route-generation güvenliği
- [TAMAM] POI ağ hatasında son başarılı veriyi koruma
- [BEKLİYOR] Ağsız durumda gerçek base-map davranışının saha testi
- [BEKLİYOR] Gerçek offline harita verisi
- [BEKLİYOR] Offline routing
- [BEKLİYOR] Tam offline navigasyon
- [BEKLİYOR] Ağ geri geldiğinde recovery saha testi

## 7. Test ve doğrulama

- [TAMAM] Unit test altyapısı
- [TAMAM] Trafik freshness/fallback testleri
- [TAMAM] Stable routeId testleri
- [TAMAM] POI parser/query testleri
- [TAMAM] Android instrumentation smoke test
- [TAMAM] MapView lifecycle smoke testi
- [TAMAM] Android diagnostics artifact üretimi
- [BEKLİYOR] Tam gerçek cihaz navigasyon testi
- [BEKLİYOR] Uzun süreli/batarya testi
- [BEKLİYOR] Ağ kesintisi + geri dönüş saha testi

## 8. CI / APK / release

- [TAMAM] `main` branch üzerinde GitHub Actions Android workflow
- [TAMAM] Run #393 final job success
- [TAMAM] Unit tests
- [TAMAM] Instrumentation smoke
- [TAMAM] Diagnostics upload
- [TAMAM] Debug APK build
- [TAMAM] Debug APK artifact upload
- [TAMAM] Başarılı main build için APK release publish
- [TAMAM] Run #393 APK artifact: `haritalar-debug-apk-393`
- [TAMAM] Run #393 APK boyutu: 21,666,704 byte
- [TAMAM] Run #393 APK SHA-256: `36934ca59e763fd8a7406270a2016f7e3426dfbc17313590c01d18fa2c4807d7`
- [BEKLİYOR] Fiziksel cihazda bu son APK'nın kurulum/açılış/navigasyon doğrulaması

## 9. Dokümantasyon / proje hafızası

- [TAMAM] `PROJECT_WORK_PROMPT.md`
- [TAMAM] Önceki sohbeti kontrol etme zorunluluğu
- [TAMAM] `PROJECT_DETAILS.md`
- [TAMAM] `docs/PROJECT_ACTIVITY_LOG.md` append-only standardı
- [TAMAM] Başarısız operasyonları gizlememe kuralı
- [TAMAM] CI/APK/device sonuçlarını birbirinden ayırma kuralı
- [TAMAM] `docs/operations/` operasyon kayıtları
- [TAMAM] Bu gerçeklik tabanlı tamamlanma checklist'i
- [BEKLİYOR] POI #393 sonrası merkezi faaliyet günlüğüne uygun kapanış kaydının eklenmesi

## 10. Güvenlik / veri doğruluğu

- [TAMAM] Kaynak koda secret gömmeme standardı
- [TAMAM] Provider credential soyutlaması
- [TAMAM] Sahte trafik/ETA/POI/koordinat üretmeme
- [TAMAM] Scraping'i veri sağlayıcısı gibi kullanmama
- [TAMAM] Kullanıcı onayı olmadan ücretli servis eklememe
- [TAMAM] OSM'yi trafik kaynağı gibi göstermeme

## Gerçek kapanış kriterleri

Proje ancak aşağıdaki kritik maddeler de doğrulandığında "tamamlandı" kabul edilebilir:

1. [ ] Gerçek cihazda uçtan uca navigasyon
2. [ ] Gerçek OSM POI saha doğrulaması
3. [ ] Ağ kesintisi/geri dönüş saha testi
4. [ ] Gerçek offline map/routing/navigasyon doğrulaması
5. [ ] Canlı trafik sağlayıcısı doğrulaması veya açıkça kapsam dışı bırakılmış sağlayıcı kararı
6. [ ] Uzun süreli/batarya saha ölçümü
7. [ ] Son operasyon kayıtlarının kapanışı

**Son GitHub gerçekliği:** `main` HEAD başlangıç kontrolünde `7dfb90affc60a4428033187555a94be7f2e7a675` idi; bu checklist commit'iyle HEAD ilerleyecektir. Run #393 doğrulanmış son Android CI çalışmasıdır ve final job sonucu `success`tir.
