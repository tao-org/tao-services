const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const web = path.join(__dirname, '../../main/resources/web');
const bundle = fs.readFileSync(path.join(web, 'assets/libs/sockjs-1.6.1.js'), 'utf8');
// Run the actual event utility in isolation so cleanup can be tested without a server.
const start = bundle.indexOf("var random = require('./random');");
const end = bundle.indexOf('}).call(this)', start);

function harness() {
    const events = {};
    let id = 0;
    const context = {
        module: { exports: {} },
        require: () => ({ string: () => String(++id) }),
        global: { addEventListener(name, callback) { events[name] = callback; } },
        setTimeout: callback => callback()
    };
    vm.runInNewContext(bundle.slice(start, end), context);
    return { events, utils: context.module.exports };
}

test('SockJS registers pagehide rather than the policy-blocked unload event', () => {
    const h = harness();
    assert.equal(h.events.unload, undefined);
    assert.equal(typeof h.events.pagehide, 'function');
    assert.equal(typeof h.events.pageshow, 'function');
});

test('pagehide cleans active transports once and excludes removed callbacks', () => {
    const h = harness();
    let closed = 0;
    h.utils.unloadAdd(() => closed++);
    const removed = h.utils.unloadAdd(() => assert.fail('removed transport was closed'));
    h.utils.unloadDel(removed);
    h.events.pagehide(); h.events.pagehide();
    assert.equal(closed, 1);
});

test('restoring from back-forward cache allows new transports until the next pagehide', () => {
    const h = harness();
    h.events.pagehide();
    h.events.pageshow({ persisted: true });
    let closed = 0;
    h.utils.unloadAdd(() => closed++);
    assert.equal(closed, 0);
    h.events.pagehide();
    assert.equal(closed, 1);
});

test('both TAO entry pages load the patched local bundle', () => {
    for (const name of ['sap.html', 'nodemonitor.html']) {
        const html = fs.readFileSync(path.join(web, name), 'utf8');
        assert.match(html, /src="\.\/assets\/libs\/sockjs-1\.6\.1\.js"/);
        assert.doesNotMatch(html, /src="[^"\n]*(?:cdnjs|jsdelivr)[^"\n]*sockjs/);
        assert.doesNotMatch(html, /window\.onunload\s*=/);
    }
});
