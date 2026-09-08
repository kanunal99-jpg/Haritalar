# Haritalar

AI destekli, offline-first Android sürüş ve navigasyon uygulaması.

## Durum
- Android uygulama iskeleti hazır.
- MapLibre Native harita motoru eklendi.
- OpenFreeMap tabanlı OSM harita görünümü bağlandı.
- GPS izin/konum takibi bağlandı.
- Gerçek Valhalla rota servisine ilk online rota bağlantısı eklendi; haritaya dokunarak hedef seçilip rota çizilebiliyor.
- Adres arama (Nominatim), çoklu rota seçenekleri ve Türkçe sesli turn-by-turn çekirdeği bağlandı.
- Radar/güvenlik uyarı çekirdeği testlerle başladı.
- GitHub Actions debug APK + unit test zinciri çalışıyor ve başarılı main build'lerinde sürümlü GitHub Release APK'sı yayınlanıyor.

## v0.0.47 kilometre taşı
- İndirilebilir APK sürümü olarak yayınlanır.
- Mevcut arama, rota alternatifleri, ücretli/ücretsiz rota tercihleri, KGM köprü ücret tahmini ve navigasyon çekirdeğini doğrulama için sabit sürüm noktasıdır.
- Ücret bilgisi doğrulanamadığında uygulama tutarı sıfır varsaymaz.

## İlk hedefler
- GPS tabanlı rota takibi
- Turn-by-turn navigasyon
- Offline harita ve rota desteği
- OSM tabanlı yol/POI verisi
- Radar, hız kamerası ve güvenlik noktaları için otomatik sürüş uyarıları
- Canlı → önbellek → offline → güvenli sessiz varsayılan veri zinciri
- AI'nin yalnızca doğrulanmış/erişilebilir veriyi yorumlaması

## Online routing
Geliştirme aşamasında Valhalla'nın kamu demo API'si kullanılmaktadır. Kamu servisi adil kullanım/rate-limit kurallarına tabidir; üretim için uygulamaya gömülü bağımlılık yerine kendi Valhalla instance'ımız veya uygun bir sağlayıcı planlanacaktır. Hedef, aynı `RoutingEngine` arayüzü üzerinden online → cache → offline geçişidir.

## Radar / güvenlik uyarı davranışı
Aktif rota ve GPS sürekli değerlendirilir. Uygun bir nokta rota üzerinde ve sürüş yönünde ise kullanıcı ayrıca sormadan uyarı zinciri başlar:

`5.000 km → 4.500 km → 4.000 km → 3.500 km → … → 1.000 km → 500 km → geçildi`

İlk uyarı 5.000 km'de başlar ve 500 km aralıklarla tekrarlanır. 500 km son uyarıdır; nokta geçildiğinde tek bir "geçildi" olayı üretilir ve sonraki GPS güncellemelerinde aynı nokta tekrar uyarılmaz.

GPS sapması, rota değişimi, sürüş yönü ve aynı noktanın tekrar tetiklenmesi için durum koruması uygulanır. Veri kaynağı bulunamazsa uygulama güvenlik noktası uydurmaz.

## Güvenlik veri güveni
Doğrulanmış trafik kontrolü verileri için resmi kaynak allow-list'i uygulanır:
- Karayolları Genel Müdürlüğü (KGM)
- Emniyet Genel Müdürlüğü (EGM)
- T.C. İçişleri Bakanlığı

Bir kaynağın allow-list'te olması tek başına veri kaydını doğrulanmış saymaz. Kayıt ayrıca doğrulama durumu, HTTPS kaynak, yayın zamanı, geçerlilik süresi ve koordinat kontrollerinden geçmelidir.

## Trafik veri mimarisi
Trafik verileri Türkiye genelinde tek bir belediye kaynağına bağlanmaz. İBB gibi yerel sağlayıcılar yalnızca kapsadıkları bölgelerde provider olarak kullanılabilir. Hedef provider yaklaşımı:

`konum + rota → kapsama uygun canlı trafik sağlayıcıları → resmi yol olayları → cache/offline → güvenli fallback → rota maliyet/ETA hesabı`

İstanbul için İBB, Kocaeli için yerel trafik kaynakları gibi bölgesel veriler ilerleyen aşamada ayrı provider'lar olarak eklenebilir. Ticari sağlayıcılar yalnızca izin verilen/lisanslı API veya veri erişimi mevcut olduğunda kullanılacaktır; web sayfası scraping'i varsayılan entegrasyon değildir.

## Harita veri politikası
Harita görüntüleme için OSM türevi veri kullanılabilir. OpenStreetMap'in kendi tile sunucuları offline/bulk indirmeye izin vermediği için offline harita hedefinde uygun lisanslı/self-hosted vector tile kaynağı kullanılacaktır; OSMF servisleri zorla offline cache için kullanılmayacaktır.

## Güvenlik
Bu sistem sürücüyü hız limitlerine uymaya teşvik eden güvenlik uyarıları içindir. Kolluk faaliyetlerinden kaçınmaya yönelik yönlendirme üretmez. Veri yoksa uygulama bilgi uydurmaz.

## Mimari
- `core/safety`: radar/kamera/güvenlik uyarı motoru
- `core/navigation`: rota ve navigasyon domain'i
- `core/data`: canlı/önbellek/offline veri katmanları
- `app`: Android kullanıcı arayüzü ve platform entegrasyonu

## APK
Her `main` push'unda GitHub Actions unit test çalıştırır ve başarılı olursa `app-debug.apk` artifact'i üretir; başarılı main build'i ayrıca sürümlü GitHub Release'e `app-debug.apk` ekler.
