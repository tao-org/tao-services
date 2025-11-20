/**
 * jQuery DraggableModal
 * Draggable modal window
 *
 * @author Nicu Stancioi
 *
 * Copyright (c) 2025 CS GROUP - Romania
 **/

(function ($, window, document) {
	'use strict';
	
	var DraggableModal = function (element, options) {
		var _self = this;
		
		// Callbacks
		if (typeof options === "string") {
			_self = $(element).data("draggableModal");
			
			// Simple callbacks
			switch (options) {
				case "show": return _self.show(); break;
				case "hide": return _self.hide(); break;
			}
			
			return false;
		// New
		} else {
			_self.options = $.extend({}, $.fn.draggableModal.defaults, options);
			
			_self.el = $(element);
			
			(typeof _self.el.prop("id") === "undefined") && _self.el.attr("id", 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) { var r = Math.random() * 16|0, v = c == 'x' ? r : (r&0x3|0x8); return v.toString(16); }))
			_self.id = _self.el.prop("id");
			_self.el.addClass("modal draggable-modal fade").attr({ "tabindex":"-1","role":"dialog","aria-hidden":"true","style":"display:none;" });
			var $el = $('<div class="modal-dialog">' +
							'<div class="modal-content" style="top:0em;right:0em;">' +
								'<div class="modal-header">' +
									'<button type="button" class="close" data-dismiss="modal"><span>&times;</span></button>' +
									'<h4 class="modal-title">Modal title</h4>' +
								'</div>' +
								'<div class="modal-body"></div>' +
							'</div>' +
						'</div>');
			$(">*", _self.el).appendTo($(".modal-body", $el));
			_self.el.html("").append($el);
			
			_self.el.modal({ backdrop: false, show: false });
			_self.el.on("shown.bs.modal", function (e) {
						_self.adjustTitleSize();
						clearTimeout(window.dmAdjustTimer); window.dmAdjustTimer = setTimeout(function () { _self.adjustPosition(); }, 400);
						(typeof _self.options.onShow === "function") && _self.options.onShow();
					}).on("hidden.bs.modal", function (e) {
						_self.clear();
						(typeof _self.options.onHide === "function") && _self.options.onHide();
					});
					
			_self.el.data({ "draggableModal": _self });
		}
	};
	
	DraggableModal.prototype = {
		clear: function () {
			var _self = this;
			
			// Reset title
			$(".modal-title", _self.el).css("font-size", "").text("");
			
			(typeof _self.options.onClear === "function") && _self.options.onClear();
		},
		adjustTitleSize: function () {
			var _self = this;
			
			var $title     = $(".modal-title", _self.el);
			var fontSize   = Math.round(parseFloat($title.css("font-size")));
			var lineHeight = parseFloat($title.css("line-height"));
			var actHeight  = $title.get(0).getBoundingClientRect().height;
			while (actHeight > lineHeight && lineHeight > 20) {
				fontSize --;
				$title.css({ "font-size": fontSize + "px" });
				lineHeight = parseFloat($title.css("line-height"));
				actHeight  = $title.get(0).getBoundingClientRect().height;
			}
		},
		adjustPosition: function () {
			var _self = this;
			
			if (_self.el.is(":visible")) {
				var $container = $(".modal-dialog"  , _self.el);
				var $draggable = $(".ui-draggable"  , _self.el);
				var $flexible  = $(".modal-flexible", _self.el);
				
				var top  = parseInt($draggable.css("top"          ));
				var left = parseInt($draggable.css("left"         ));
				if ($flexible.length) {
					var marginTop     = parseInt($draggable.css("margin-top"   ));
					var marginBottom  = parseInt($draggable.css("margin-bottom"));
					var paddingTop    = $flexible.offset().top - $draggable.offset().top;
					var paddingBottom = $draggable.outerHeight(false) - paddingTop - $flexible.outerHeight(true);
					
					var cMaxH = $container.outerHeight(false) - (top + marginTop + paddingTop + paddingBottom + marginBottom);
					(cMaxH <   0) && (cMaxH =  68);
					(cMaxH > 550) && (cMaxH = 550);
					$flexible.css({ "max-height": parseInt(cMaxH) + "px" });
				}
				
				var right = left + $draggable.outerWidth(true);
				if (right > $container.outerWidth(false)) {
					var cLeft = $container.outerWidth(false) - $draggable.outerWidth(true);
					(cLeft < 0) && (cLeft = 0);
					$draggable.css({ "left": parseInt(cLeft) + "px" });
				}
			}
		},
		show: function () {
			var _self = this;
			
			// Clear
			_self.clear();
			
			// Title
			$(".modal-title", _self.el).text(_self.options.title);
			_self.el.is(":visible") && _self.adjustTitleSize();
			
			// Content
			(typeof _self.options.setContent === "function") && _self.options.setContent(_self.options.data);
			
			// Make window draggable
			!$(".modal-content", _self.el).data("ui-draggable") &&
			 $(".modal-content", _self.el).draggable({
				cursor     : "move",
				handle     : ".modal-header",
				containment: "#" + _self.id + " .modal-dialog",
				stop       : function (event, ui) {
					ui.helper.css({ "right": "" });
					clearTimeout(window.dmAdjustTimer);
					window.dmAdjustTimer = setTimeout(function () { _self.adjustPosition(); }, 50);
				}
			});
			
			// Show modal
			_self.el.modal("show");
		},
		hide: function () {
			var _self = this;
			
			_self.el.modal("hide");
		}
	}
	
	$.fn.draggableModal = function (options) {
		return new DraggableModal(this, options);
	};
	
	$.fn.draggableModal.defaults = {
		title     : '',
		data      : {},
		onShow    : false,
		onHide    : false,
		onClear   : false,
		setContent: false
	};
	
})(jQuery, window, document);
//# sourceURL=draggableModal.js
