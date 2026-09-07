# Haritalar

AI destekli, offline-first Android sürüş ve navigasyon uygulaması.

## Durum
- Android uygulama iskeleti hazır.
- MapLibre Native harita motoru eklendi.
- OpenFreeMap tabanlı OSM harita görünümü bağlandı.
- GPS izin/konum takibi bağlandı.
- Radar/güvenlik uyarı çekirdeği testlerle başladı.
- GitHub Actions debug APK + unit test zinciri eklendi.

## İlk hedefler
- GPS tabanlı rota takibi
- Turn-by-turn navigasyon
- Offline harita ve rota desteği
- OSM tabanlı yol/POI verisi
- Radar, hız kamerası ve güvenlik noktaları için otomatik sürüş uyarıları
- Canlı → önbellek → offline → güvenli sessiz varsayılan veri zinciri
- AI'nin yalnızca doğrulanmış/erişilebilir veriyi yorumlaması

## Radar uyarı davranışı
Aktif rota ve GPS sürekli değerlendirilir. Uygun bir nokta rota üzerinde ve sürüş yönünde ise kullanıcı ayrıca sormadan uyarı zinciri başlar:

`4 km → 3.5 km → 3 km → 2.5 km → 2 km → 1.5 km → 1 km → 500 m → geçildi`

GPS sapması, rota değişimi ve aynı noktanın tekrar tetiklenmesi için durum koruması uygulanır.

## Harita veri politikası
Harita görüntüleme için OSM türevi veri kullanılabilir. OpenStreetMap'in kendi tile sunucuları offline/bulk indirmeye izin vermediği için offline harita hedefinde uygun lisanslı/self-hosted vector tile kaynağı kullanılacaktır; OSMF servisleri zorla offline cache için kullanılmayacaktır.

## Güvenlik
Bu sistem sürücüyü hız limitlerine uymaya teşvik eden güvenlik uyarıları içindir. Kolluk faaliyetlerinden kaçınmaya yönelik yönlendirme üretmez. Veri yoksa uygulama bilgi uydurmaz.

## Mimari
- `core/safety`: radar/kamera/güvenlik uyarı motoru
- `core/navigation`: rota ve navigasyon domain'i
- `core/data`: canlı/önbellek/offline veri katmanları
- `app`: Android kullanıcı arayüzü ve platform entegrasyonları

## APK
Her `main` push'unda GitHub Actions unit test çalıştırır ve başarılı olursa `app-debug.apk` artifact'i üretir. APK üretimi CI kanıtı görülmeden başarılı kabul edilmez.
