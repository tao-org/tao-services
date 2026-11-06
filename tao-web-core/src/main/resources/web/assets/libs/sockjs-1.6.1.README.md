SockJS client 1.6.1, distributed under the accompanying MIT license.

Source: https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.js

Local change in `lib/utils/event.js` (bundled module 46): register transport
cleanup on `pagehide` instead of `unload`. Modern Chromium permissions policies
can prohibit `unload` registration. Reset the teardown flag on `pageshow` so
transports created after a back-forward cache restoration remain usable.

Both `sap.html` and `nodemonitor.html` load this local bundle. Preserve this
change, or verify equivalent upstream behavior, when updating SockJS.

Test from the workspace root:
`node --test tao-services/tao-web-core/src/test/js/sockjs-lifecycle.test.cjs`
