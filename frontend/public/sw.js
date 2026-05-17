/**
 * Service Worker for Podcastia
 * Helps with background audio playback and app persistence
 */

const CACHE_NAME = 'podcastia-v1';
const STATIC_CACHE = [
  '/',
  '/index.html',
  '/manifest.json',
  '/favicon.ico'
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        // Cache assets individually to handle failures gracefully
        return Promise.all(
          STATIC_CACHE.map(url => {
            return fetch(url)
              .then(response => {
                if (response.ok) {
                  return cache.put(url, response);
                }
                console.warn('Service Worker: Failed to cache', url);
              })
              .catch(err => {
                console.warn('Service Worker: Error caching', url, err.message);
              });
          })
        );
      })
      .then(() => {
        return self.skipWaiting();
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME) {
              return caches.delete(cacheName);
            }
          })
        );
      })
      .then(() => {
        return self.clients.claim();
      })
  );
});

// Fetch event - handle network requests
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  
  // Handle audio files differently - always try network first
  if (url.pathname.includes('/api/podcasts/') && url.pathname.includes('/audio')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          // Cache successful audio responses
          if (response.ok) {
            const responseClone = response.clone();
            caches.open(CACHE_NAME).then((cache) => {
              cache.put(event.request, responseClone);
            });
          }
          return response;
        })
        .catch(() => {
          // If network fails, try cache
          return caches.match(event.request);
        })
    );
    return;
  }
  
  // Handle API requests - network first, then cache
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .then((response) => {
          // Cache successful API responses (GET only)
          if (response.ok && event.request.method === 'GET') {
            const responseClone = response.clone();
            caches.open(CACHE_NAME).then((cache) => {
              cache.put(event.request, responseClone);
            });
          }
          return response;
        })
        .catch(() => {
          // If network fails, try cache for GET requests
          if (event.request.method === 'GET') {
            return caches.match(event.request);
          }
          throw new Error('Network request failed');
        })
    );
    return;
  }
  
  // Handle static assets - cache first, then network (GET only)
  if (event.request.method !== 'GET') {
    return;
  }

  event.respondWith(
    caches.match(event.request)
      .then((response) => {
        if (response) {
          return response;
        }
        
        return fetch(event.request)
          .then((response) => {
            // Cache successful responses
            if (response.ok) {
              const responseClone = response.clone();
              caches.open(CACHE_NAME).then((cache) => {
                cache.put(event.request, responseClone);
              });
            }
            return response;
          });
      })
  );
});

// Message event - handle messages from the main app
self.addEventListener('message', (event) => {
  const data = event.data;
  
  switch (data.type) {
    case 'KEEP_ALIVE':
      // Respond to keep-alive ping
      event.ports[0].postMessage({ type: 'KEEP_ALIVE_RESPONSE' });
      break;
      
    case 'AUDIO_STATE':
      // Handle audio state updates
      break;
      
    case 'SKIP_WAITING':
      // Force the service worker to become active
      self.skipWaiting();
      break;
      
    default:
      // Unknown message type - ignore
  }
});

// Push event - handle push notifications (future feature)
self.addEventListener('push', (event) => {
  if (event.data) {
    const data = event.data.json();
    
    // Show notification for new podcast updates (future feature)
    if (data.type === 'NEW_PODCAST') {
      event.waitUntil(
        self.registration.showNotification('New Podcast Available', {
          body: data.title,
          icon: '/favicon.ico',
          badge: '/favicon.ico',
          tag: 'new-podcast',
          requireInteraction: false,
          actions: [
            {
              action: 'listen',
              title: 'Listen Now'
            }
          ]
        })
      );
    }
  }
});

// Notification click event
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  if (event.action === 'listen') {
    // Open the app to the specific podcast
    event.waitUntil(
      clients.openWindow('/')
    );
  } else {
    // Just open the app
    event.waitUntil(
      clients.openWindow('/')
    );
  }
});

// Background sync event - handle background sync (future feature)
self.addEventListener('sync', (event) => {
  if (event.tag === 'background-sync') {
    event.waitUntil(
      // Handle background sync tasks (e.g., sync playback progress)
      doBackgroundSync()
    );
  }
});

// Periodic sync event - handle periodic background sync (future feature)
self.addEventListener('periodicsync', (event) => {
  if (event.tag === 'periodic-sync') {
    event.waitUntil(
      // Handle periodic sync tasks (e.g., download new podcasts)
      doPeriodicSync()
    );
  }
});

// Helper functions
async function doBackgroundSync() {
  // Sync playback progress, download queue, etc.
}

async function doPeriodicSync() {
  // Download new podcasts, update recommendations, etc.
}

// Keep the service worker alive during audio playback
let heartbeatInterval;
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'START_HEARTBEAT') {
    // Start heartbeat to keep service worker alive
    if (heartbeatInterval) {
      clearInterval(heartbeatInterval);
    }
    
    heartbeatInterval = setInterval(() => {
      // Send heartbeat to main thread
      self.clients.matchAll().then(clients => {
        clients.forEach(client => {
          client.postMessage({ type: 'HEARTBEAT' });
        });
      });
    }, 20000); // Every 20 seconds
    
  } else if (event.data && event.data.type === 'STOP_HEARTBEAT') {
    // Stop heartbeat
    if (heartbeatInterval) {
      clearInterval(heartbeatInterval);
      heartbeatInterval = null;
    }
    
  }
});

// Prevent service worker from being terminated during audio playback
let isAudioPlaying = false;

self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'AUDIO_PLAYING') {
    isAudioPlaying = event.data.isPlaying;
    
    if (isAudioPlaying) {
      // Start keep-alive mechanism
      startKeepAlive();
    } else {
      // Stop keep-alive mechanism
      stopKeepAlive();
    }
  }
});

function startKeepAlive() {
  // Use various techniques to keep the service worker alive
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
  }
  
  heartbeatInterval = setInterval(() => {
    // Perform a minimal operation to keep the worker alive
  }, 30000); // Every 30 seconds
}

function stopKeepAlive() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
    heartbeatInterval = null;
  }
}

// Handle service worker termination
self.addEventListener('beforeunload', () => {
  stopKeepAlive();
});
