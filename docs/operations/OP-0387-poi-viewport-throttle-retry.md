# OP-0387 — POI viewport throttle retry

- Tarih: 2026-09-09
- Tür: POI / reliability / CI verification
- Durum: BAŞARILI

## Amaç
Viewport değişiminden sonra 5 saniyelik POI refresh throttle penceresine denk gelen yenilemenin kaybolmasını önlemek.

## Önceki durum
`LiveNavigationPoiLayer` yeni viewport anahtarını kaydediyor, ancak refresh throttle nedeniyle çağrıyı erken bırakabiliyordu. Bu durumda yeni viewport throttle süresi sonrasında otomatik olarak yeniden denenmiyordu.

## Gerçek değişiklik
- Dosya: `app/src/main/java/com/haritalar/app/LiveNavigationPoiLayer.kt`
- Commit: `27c293ef5e39f796f01bd8aab1d563e41dd3f557`
- Değişiklik: throttle nedeniyle ertelenen viewport refresh için kalan throttle süresi kadar `postDelayed` retry eklendi; viewport değişmişse eski callback güvenli şekilde yok sayılıyor.

## CI doğrulaması
- Workflow run: #387
- Run ID: `34395579477`
- Head SHA: `27c293ef5e39f796f01bd8aab1d563e41dd3f557`
- Unit tests: BAŞARILI
- Android instrumentation smoke test: BAŞARILI
- Android instrumentation diagnostics: BAŞARILI
- Live Android smoke diagnostics: BAŞARILI
- Debug APK build: BAŞARILI
- APK artifact upload: BAŞARILI
- APK release publish: BAŞARILI

## Artifact
- `haritalar-debug-apk-387`
- Boyut: 21,666,624 bytes
- SHA-256: `7d5cfa78e4176f7d88d2268737ff5a6493d73fc0c52a9b075343ddc2630076b8`

## Sınırlar / uyarılar
- Bu operasyon CI/emulator ve build pipeline doğrulamasıdır; fiziksel cihaz doğrulaması değildir.
- POI veri kaynağı gerçek OSM/Overpass bağımlılığıdır; sahte POI verisi eklenmemiştir.
- Overpass erişiminin anlık kullanılabilirliği ayrıca runtime koşuludur.

## Sonraki adım
POI zincirinin kalan gerçek eksikleri yeniden denetlenecek: kategori kapsamı, parser alanları, viewport sınırları, hata/fallback davranışı ve bilgi kartından mevcut rota zincirine geçiş.
