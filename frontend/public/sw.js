const cacheName = 'transmoovi-shell-v1'
const shell = ['/', '/manifest.webmanifest', '/transmoovi-app-icon-192.png', '/transmoovi-app-icon-512.png']
self.addEventListener('install', (event) => event.waitUntil(caches.open(cacheName).then((cache) => cache.addAll(shell)).then(() => self.skipWaiting())))
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()))
self.addEventListener('fetch', (event) => { if (event.request.method !== 'GET' || event.request.mode === 'navigate') return; event.respondWith(caches.match(event.request).then((cached) => cached ?? fetch(event.request))) })
