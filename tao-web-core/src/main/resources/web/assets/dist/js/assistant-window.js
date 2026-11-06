/* The chat keeps executing in the main window: routes, credentials and tool events
 * always belong to that window. Only its existing DOM nodes change documents. */
(function () {
    "use strict";
    var popup = null, source = null, content = null, button = null, hostState = null;

    function isDetached() { return !!popup && !popup.closed; }

    function rebindChat(container) {
        container.querySelectorAll(".tao-llm-chat-initialized").forEach(function (chat) {
            if (typeof chat.taoRebindChatActions === "function") { chat.taoRebindChatActions(); }
        });
    }

    function dock() {
        var previous = popup;
        popup = null;
        if (source && content) {
            while (content.firstChild) { source.appendChild(content.firstChild); }
            rebindChat(source);
            source = content = null;
        }
        if (hostState) {
            hostState.element.style.display = hostState.display;
            hostState.element.setAttribute("aria-hidden", "false");
            hostState = null;
        }
        if (button) {
            button.title = "Move to separate window";
            button.setAttribute("aria-label", button.title);
        }
        if (previous && !previous.closed) { previous.close(); }
    }

    function detach() {
        if (isDetached()) { dock(); return; }
        var modal = document.getElementById("taoLlmChatFloating");
        var body = modal.querySelector(".modal-body");
        // Wait for fragment initialization; never initialize a second chat instance.
        if (!body.querySelector(".tao-llm-chat-initialized")) {
            window.alert("The assistant is still loading. Try again once the chat is ready.");
            return;
        }
        // Open synchronously in the click gesture, before requesting screen permission.
        var next = window.open("about:blank", "", "popup,width=560,height=720");
        if (!next) {
            window.alert("Allow popups for TAO to move the assistant to a separate window.");
            return;
        }
        popup = next;
        try {
            var doc = next.document;
            doc.title = "TAO Assistant";
            var base = doc.createElement("base");
            base.href = document.baseURI;
            doc.head.appendChild(base);
            // Copy styles, never scripts: event handlers and chat state stay in the opener.
            document.querySelectorAll('link[rel="stylesheet"], style').forEach(function (node) {
                var clone = node.cloneNode(true);
                if (node.tagName === "LINK") { clone.href = node.href; }
                doc.head.appendChild(clone);
            });
            doc.documentElement.lang = document.documentElement.lang;
            doc.body.className = document.body.className;
            var wrapper = doc.createElement("div");
            wrapper.id = "taoLlmChatFloating";
            wrapper.className = "modal in";
            wrapper.style.display = "block";
            wrapper.innerHTML = '<div class="modal-dialog"><div class="modal-content">'
                + '<div class="modal-header"><button type="button" class="btn btn-default btn-sm pull-right">Return to main window</button>'
                + '<h4 class="modal-title">TAO Assistant</h4></div><div class="modal-body"></div></div></div>';
            doc.body.appendChild(wrapper);
            var style = doc.createElement("style");
            style.textContent = 'body { margin:0; overflow:auto; }'
                + '#taoLlmChatFloating .modal-dialog { position:static; width:100%; height:100%; margin:0; }'
                + '#taoLlmChatFloating .modal-content { min-width:0; min-height:100vh; }'
                + '#taoLlmChatFloating .chatMessages { max-height:calc(100vh - 280px) !important; }';
            doc.head.appendChild(style);
            source = body;
            content = wrapper.querySelector(".modal-body");
            while (source.firstChild) { content.appendChild(source.firstChild); }
            rebindChat(content);
            wrapper.querySelector("button").addEventListener("click", dock);
            next.addEventListener("pagehide", function () { if (popup === next) { dock(); } });
            hostState = { element: modal, display: modal.style.display };
            modal.style.display = "none";
            modal.setAttribute("aria-hidden", "true");
            button.title = "Return assistant to main window";
            button.setAttribute("aria-label", button.title);
            next.focus();
        } catch (error) {
            dock();
            window.console.error("Could not detach the TAO assistant", error);
            window.alert("Could not move the assistant to a separate window. See the browser console for details.");
            return;
        }
        // Window Management is permission-gated. Denial leaves the normal popup usable.
        // HTTP deployments and browsers without this API can still open a normal popup.
        if (window.isSecureContext && typeof window.getScreenDetails === "function") {
            try {
                window.getScreenDetails().then(function (details) {
                    if (popup !== next || next.closed) { return; }
                    var screen = details.currentScreen;
                    next.moveTo(screen.availLeft + Math.max(0, screen.availWidth - 580), screen.availTop + 40);
                }).catch(function () {});
            } catch (error) {
                // A synchronous permissions-policy rejection must not break the popup.
            }
        }
    }

    window.taoAssistantWindow = {
        init: function () {
            button = document.querySelector('[data-action="detach-assistant-chat"]');
            if (button && typeof window.open === "function") {
                button.hidden = false;
                button.addEventListener("click", function (event) {
                    event.preventDefault();
                    event.stopPropagation();
                    detach();
                });
            }
        },
        isDetached: isDetached,
        focus: function () { if (isDetached()) { popup.focus(); } },
        dock: dock
    };
    window.addEventListener("pagehide", dock);
})();
