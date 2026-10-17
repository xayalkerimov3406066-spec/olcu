# Ölçü Pasportu — telefona quraşdırma

## Paketdə nə var

```
index.html                 tətbiqin özü (bir fayl, kitabxanasız)
manifest.webmanifest       ad, ikon, rəng — telefonun tətbiq kimi tanıması üçün
sw.js                      service worker — oflayn işləmə
icon-192 / 512 / maskable  Android ikonları
apple-touch-icon.png       iPhone ana ekran ikonu
favicon-32.png             brauzer nişanı
```

---

## Addım 1 — yerləşdirmə (10 dəqiqə)

Bütün faylları **eyni qovluğa** qoy, məsələn `cinarmall.com/olcu/`.

**Tələb olunan tək şərt: HTTPS.** Service worker və kamera yalnız HTTPS-də işləyir. `cinarmall.com` artıq sertifikatlıdırsa, əlavə heç nə lazım deyil.

Yoxlama:
1. Telefonda `cinarmall.com/olcu/` ünvanını aç.
2. Chrome-da ekranın altında **«Telefona quraşdır»** düyməsi çıxmalıdır.
3. iPhone-da: Safari → Paylaş → **«Ana ekrana əlavə et»**.

Quraşdırıldıqdan sonra tətbiq brauzer çərçivəsi olmadan, öz ikonu ilə açılır və internetsiz işləyir.

### Serverin başlıqları
```
.webmanifest   →  Content-Type: application/manifest+json
sw.js          →  Cache-Control: no-cache
```
İkincisi vacibdir: əks halda tətbiqi yeniləyəndə istifadəçilərdə köhnə versiya qalır.

### Yeniləmə qaydası
`index.html`-i dəyişəndə `sw.js` faylında `CACHE = 'olcu-pasportu-v3'` sətrindəki rəqəmi artır (`v4`, `v5`…). Köhnə keş avtomatik silinir, istifadəçilər növbəti açılışda yeni versiyanı alır.

---

## Addım 2 — App Store və Google Play

Veb versiya ilə mağaza versiyası arasında **koda görə fərq yoxdur** — eyni `index.html` yerli qabığın içində işləyir. Fərq prosesdədir.

### Nə lazımdır

| | Google Play | App Store |
|---|---|---|
| Hesab | 25 $ (bir dəfəlik) | 99 $ / il |
| Lazım olan kompüter | istənilən | **yalnız Mac** (Xcode) |
| Baxış müddəti | 1–3 gün | 1–7 gün |
| Yeniləmə | hər dəfə yenidən baxış | hər dəfə yenidən baxış |

### Qabıq (Capacitor)

```bash
npm create @capacitor/app olcu-pasportu
cd olcu-pasportu
npm i @capacitor/core @capacitor/cli @capacitor/android @capacitor/ios
# paketdəki faylları www/ qovluğuna kopyala
npx cap add android
npx cap add ios          # yalnız Mac-də
npx cap sync
npx cap open android     # Android Studio açılır → Build → Signed Bundle (.aab)
npx cap open ios         # Xcode açılır → Archive → App Store Connect
```

`capacitor.config.json`:
```json
{
  "appId": "az.cinarmall.olcu",
  "appName": "Ölçü Pasportu",
  "webDir": "www",
  "plugins": { "SplashScreen": { "launchShowDuration": 600 } }
}
```

Kamera icazəsi üçün:
- Android — `AndroidManifest.xml`: `<uses-permission android:name="android.permission.CAMERA"/>`
- iOS — `Info.plist`: `NSCameraUsageDescription` = *«Bədən ölçülərinizi hesablamaq üçün kameradan istifadə olunur. Şəkil cihazınızdan çıxmır.»*

Bu izah mətnini diqqətlə yaz — **Apple-ın rədd etdiyi ən tez-tez rast gəlinən səbəb budur.**

### Mağaza baxışında gözlənilən suallar

Kamera və bədən ölçüsü toplayan tətbiq həssas kateqoriyadadır. Hazır olmalı sənədlər:
- **Məxfilik siyasəti səhifəsi** (ictimai URL — hər iki mağaza tələb edir)
- Şəklin cihazdan çıxmadığının açıq bəyanı
- Yaş məhdudiyyəti: **17+** seçilməlidir (Apple), Google Play-də anket düzgün doldurulmalıdır
- Apple Review-a demo hesab və 30 saniyəlik video göndər — kamera funksiyası olan tətbiqlərdə baxış bununla sürətlənir

### Praktiki məsləhət

**Əvvəlcə veb versiyanı yayımla.** Səbəbi sadədir: mağaza versiyasında hər kiçik düzəliş üçün yenidən baxış gözləyirsən, veb versiyada düzəliş dərhal canlıya çıxır. İlk aylarda kalibrasiya əmsallarını tez-tez dəyişəcəksən — bu, mağaza ritmi ilə uyuşmur.

Mağazaya çıxmağın əsl səbəbi funksiya deyil, **görünürlükdür**: «CinarMall» axtaranda nəticə çıxsın. Bunun üçün isə ölçü aləti yox, bütün marketpleysin tətbiqi lazımdır. Ölçü pasportunu həmin tətbiqin içində bir ekran kimi yerləşdirmək — ayrıca tətbiq buraxmaqdan həm ucuz, həm düzgündür.

---

## Bilinməli məhdudiyyətlər

1. **Kamera `file://` ünvanında işləmir.** Faylı birbaşa telefonda açsan skan işləməyəcək — HTTPS server lazımdır.
2. **İlk açılışda MediaPipe modeli ~3 MB yüklənir** (CDN-dən). Service worker bunu keşləmir, çünki başqa domendədir. Zəif internetdə ilk skan gecikir.
3. **Ölçü çevirmə cədvəlləri yaxınlaşmadır.** Markalar arasında yarım nömrəyə qədər fərq normaldır — tətbiq bunu istifadəçiyə açıq deyir.
4. **Antropometrik əmsallar hələ yerli deyil.** 300–400 nəfərlik ölçmə ilə kalibrasiya olunmalıdır; bu, sistemin dəqiqliyini təyin edən tək ən mühüm iş qalır.
