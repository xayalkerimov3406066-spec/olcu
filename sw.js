/* Ölçü Pasportu — oflayn keş.
   Yeniləyəndə CACHE adındakı rəqəmi artır, köhnə keş avtomatik silinir. */
const CACHE = 'olcu-pasportu-v5';
const ASSETS = ['./','./index.html','./manifest.webmanifest',
  './icon-192.png','./icon-512.png','./icon-maskable-512.png',
  './apple-touch-icon.png','./favicon-32.png'];

self.addEventListener('install', e=>{
  e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate', e=>{
  e.waitUntil(caches.keys()
    .then(ks=>Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k))))
    .then(()=>self.clients.claim()));
});
self.addEventListener('fetch', e=>{
  if(e.request.method!=='GET') return;
  const url = new URL(e.request.url);
  if(url.origin !== location.origin) return;      // CDN sorğularına toxunma
  e.respondWith(
    caches.match(e.request).then(hit => hit || fetch(e.request).then(res=>{
      const copy = res.clone();
      caches.open(CACHE).then(c=>c.put(e.request, copy)).catch(()=>{});
      return res;
    }).catch(()=> caches.match('./index.html')))
  );
});
