/* Resolve UI identity when a message is sent. Never keep a conversation-wide entity ID. */
(function (window, $) {
    "use strict";
    var changingRoute = false;

    function reference(type, id) {
        if (typeof type !== "string" || !type.trim() ||
            (typeof id !== "string" && typeof id !== "number") || !String(id).trim()) { return null; }
        return { type: type.trim(), id: String(id).trim() };
    }

    function visible(element) {
        if (!element || !element.isConnected) { return false; }
        for (var node = element; node && node.nodeType === 1; node = node.parentElement) {
            var style = window.getComputedStyle(node);
            if (node.hidden || style.display === "none" || style.visibility === "hidden") { return false; }
        }
        return true;
    }

    function fragmentEntityType() {
        var fragment = window.taoCurrentFragmentName || "";
        var types = [
            [/^workflows?(?:-|$)/i, "workflow"], [/^components?(?:-|$)/i, "component"],
            [/^containers?(?:-|$)/i, "container"], [/^datasources?(?:-|$)/i, "datasource"],
            [/^sites?(?:-|$)/i, "site"], [/^topology(?:-|$)/i, "node"],
            [/^users?(?:-|$)/i, "user"], [/^repositories(?:-|$)/i, "repository"],
            [/^scheduling(?:-|$)/i, "schedule"], [/^remoteServices(?:-|$)/i, "remote_service"]
        ];
        for (var i = 0; i < types.length; i++) {
            if (types[i][0].test(fragment)) { return types[i][1]; }
        }
        return null;
    }

    // Fragments without a standard entity dialog can annotate their own visible root.
    window.setTaoEntityContext = function (element, type, id) {
        var entity = reference(type, id);
        if (!element) { return; }
        if (entity) {
            element.setAttribute("data-tao-entity-type", entity.type);
            element.setAttribute("data-tao-entity-id", entity.id);
        } else {
            element.removeAttribute("data-tao-entity-type");
            element.removeAttribute("data-tao-entity-id");
        }
    };

    function annotatedEntity(root) {
        if (root.nodeType === 1 && root.getAttribute && visible(root)) {
            var own = reference(root.getAttribute("data-tao-entity-type"), root.getAttribute("data-tao-entity-id"));
            if (own) { return own; }
        }
        var elements = root.querySelectorAll("[data-tao-entity-type][data-tao-entity-id]");
        for (var i = elements.length - 1; i >= 0; i--) {
            if (visible(elements[i])) {
                return reference(elements[i].getAttribute("data-tao-entity-type"), elements[i].getAttribute("data-tao-entity-id"));
            }
        }
        return null;
    }

    window.getCurrentTaoEntity = function () {
        if (changingRoute) { return null; }
        var modals = window.document.querySelectorAll(".modal.in, .modal.show");
        for (var i = modals.length - 1; i >= 0; i--) {
            var modal = modals[i];
            if (!visible(modal) || modal.id === "taoLlmChatFloating" || modal.querySelector(".tao-llm-chat")) { continue; }
            var annotated = annotatedEntity(modal);
            if (annotated) { return annotated; }
            if (Object.prototype.hasOwnProperty.call(modal, "taoEntityContext")) {
                return modal.taoEntityContext; // null for a new/unsaved entity, even over an open editor.
            }
        }
        var explicit = annotatedEntity(window.document);
        if (explicit) { return explicit; }
        if (fragmentEntityType() === "workflow") {
            var frame = window.document.querySelector("#wfIframeWrapper > iframe");
            if (visible(frame)) {
                try { return reference("workflow", frame.contentWindow.currentWfID); }
                catch (_) { return null; } // A cross-origin or unavailable editor has no known identity.
            }
        }
        return null;
    };

    $(window.document).on("show.bs.modal.taoEntityContext", ".modal", function (event) {
        if (event.target !== this || this.id === "taoLlmChatFloating" || this.querySelector(".tao-llm-chat")) { return; }
        var stack = window.userOpStack;
        var operation = Array.isArray(stack) ? stack[0] : stack;
        var dna = operation && operation.dna;
        this.taoEntityContext = reference(fragmentEntityType(), dna && dna.id);
    }).on("hidden.bs.modal.taoEntityContext", ".modal", function (event) {
        if (event.target === this) { delete this.taoEntityContext; }
    }).on("tao:route-changing.taoEntityContext", function () {
        changingRoute = true;
        window.userOpStack = [];
        window.document.querySelectorAll(".modal").forEach(function (modal) { delete modal.taoEntityContext; });
    }).on("tao:route-loaded.taoEntityContext", function () { changingRoute = false; });
})(window, jQuery);
