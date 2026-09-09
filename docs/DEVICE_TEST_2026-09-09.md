# HARİTALAR — GERÇEK CİHAZ DOĞRULAMA NOTU

- **Tarih:** 2026-09-09
- **Tür:** Gerçek cihaz kullanıcı doğrulaması
- **Amaç:** MainActivity'nin ikinci ve sonraki açılışlarda çökme yapıp yapmadığını doğrulamak.

## Kullanıcı doğrulaması

Kullanıcı uygulamayı gerçek Android cihazında açıp kapatarak tekrar açılışları kontrol etti ve **"Tamam düzeldi"** sonucunu bildirdi.

## Durum

- İkinci açılış çökmesi: **Kullanıcı raporuna göre düzeldi.**
- Fiziksel cihaz testi: **Kullanıcı tarafından gerçekleştirildi.**
- Bu kayıt CI/emülatör testi değildir.
- Önceki GitHub doğrulaması: `main` üzerindeki MapLibre `MapView` lifecycle smoke testi ve ilgili Android CI doğrulaması ayrı olarak kayıtlıdır.

## Gerçeklik notu

Bu belge kullanıcı tarafından bildirilen gerçek cihaz sonucunu kaydeder; cihaz logcat'i veya ekran kaydı alınmadığı için daha ayrıntılı crash-free istatistiği iddia edilmez.

## Sonraki adım

Mevcut ikinci-açılış problemi kullanıcı doğrulamasına göre kapatılmıştır. Bundan sonra proje çalışma anayasasındaki sıraya göre hafıza/CI durumu senkronize edilerek sonraki P1 geliştirmeye geçilecektir.
