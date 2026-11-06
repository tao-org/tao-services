/**
 * jQuery Toolbox
 * Show a list of predefined functions to choose from
 *
 * @author Nicu Stancioi
 *
 * Copyright (c) 2025 CS GROUP - Romania
 **/

(function ($, window, document) {
	'use strict';
	
	var Toolbox = function (element, options) {
		var _self = this;
		_self.options = $.extend({}, $.fn.toolbox.defaults, options);
		
		// Callbacks
		if (typeof options === "string") {
			_self = $(element).data("toolbox");
			
			// Simple callbacks
			switch (options) {
				case "show": return _self.show(); break;
				case "hide": return _self.hide(); break;
			}
			
			return false;
		// New
		} else {
			_self.el = $(element);
			_self.el.data({ "toolbox": _self });
			
			document.documentElement.style.setProperty('--peep-size', _self.options.peep || "2em");
			_self.container = $(
				'<div class="toolbox' + (_self.options.peep ? ' peep' : '') + (_self.options.closed ? ' outside' : '') + '">' +
					'<div class="toolbox-header">' +
						'<span class="title">' + _self.options.title + '</span>' +
						'<button class="toolbox-close" title="Close Toolbox"><i class="fa fa-times"></i></button>' +
						'<div class="toolbox-handler"><i class="fa ' + (_self.options.position === "left" ? 'fa-caret-right' : 'fa-caret-left') + '"></i></div>' +
					'</div>' +
					'<div class="toolbox-content"></div>' +
				'</div>'
			);
			_self.el.before(_self.container);
			_self.el.appendTo($(".toolbox-content", _self.container));
			_self.container.css({ "width": _self.options.width, "z-index": _self.options.zIndex });
			
			(_self.options.position === "left" ) && _self.container.addClass("at-left");
			(_self.options.position === "right") && _self.container.addClass("at-right");
			(_self.options.position === "right") && $(".toolbox-header .toolbox-close", _self.container).insertBefore($(".toolbox-header .title", _self.container));
			
			if (_self.options.backdrop) {
				var $bdrop = $('<div class="toolbox-backdrop" style="z-index:' + (_self.options.zIndex - 1) + '"></div>');
				$bdrop.on("click", function (e) { _self.hide(); });
				_self.container.after($bdrop);
			}
			
			// Define events
			_self.defineEvents();
			
			return _self;
		}
	};
	
	Toolbox.prototype = {
		// Show toolbox
		show: function () {
			var _self = this;
			
			_self.container.removeClass("outside");
		},
		
		// Hide toolbox
		hide: function () {
			var _self = this;
			
			_self.container.addClass("outside");
		},
		
		// Define events
		defineEvents: function () {
			var _self = this;
			
    	    _self.container.on("click", ".toolbox-close", function (e) {
                e.stopPropagation();
    	        _self.hide();
            })
            .on("click", function(e) {
				e.stopPropagation();
				if (_self.container.hasClass("outside")) {
					_self.show();
				}
            });
		}
	}
	
	// "Toolbox" control definition
	$.fn.toolbox = function (options) {
		return new Toolbox(this, options);
	};
	
	// "Toolbox" default values
	$.fn.toolbox.defaults = {
		width   : "15em",
		position: "left",
		title   : "Toolbox",
		peep    : false,
		closed  : false,
		backdrop: false,
		zIndex  : 1
	};

})(jQuery, window, document);

//# sourceURL=toolbox.js
