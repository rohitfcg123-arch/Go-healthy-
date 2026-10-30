const CACHE='go-healthy-offline-v2';
const BASE='/Go-healthy-/';
const OFFLINE=BASE+'offline.html';

self.addEventListener('install',event=>{
  // Cache only the standalone offline screen. Do not let a root-page redirect
  // or a transient index.html response prevent the service worker installing.
  event.waitUntil(
    caches.open(CACHE)
      .then(cache=>cache.add(OFFLINE))
      .then(()=>self.skipWaiting())
  );
});

self.addEventListener('activate',event=>{
  event.waitUntil(
    caches.keys()
      .then(keys=>Promise.all(keys.filter(key=>key!==CACHE).map(key=>caches.delete(key))))
      .then(()=>self.clients.claim())
  );
});

self.addEventListener('fetch',event=>{
  const req=event.request;
  if(req.method!=='GET'||new URL(req.url).origin!==self.location.origin)return;

  if(req.mode==='navigate'){
    event.respondWith(
      fetch(req).catch(async()=>{
        const offlinePage=await caches.match(OFFLINE);
        return offlinePage || new Response(
          '<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><title>Offline</title><h2>You are offline</h2><p>Reconnect to the internet and try again.</p>',
          {headers:{'Content-Type':'text/html; charset=utf-8'}}
        );
      })
    );
  }
});
