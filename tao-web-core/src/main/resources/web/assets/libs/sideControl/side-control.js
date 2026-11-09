/**
 * jQuery SideControl
 * Adds a button at the right of the control
 *
 * @author Nicu Stancioi
 *
 * Copyright (c) 2025 CS GROUP - Romania
 **/

(function ($, window, document) {
	'use strict';
	
	var SideControl = function (element, options) {
		var _self = this;
		_self.options = $.extend({}, $.fn.sideControl.defaults, options);
		
		// Define dialog
		_self.el = $(element);
		_self.el.after('<i class="side-control' + (_self.options.iconclass !== "" ? ' ' + _self.options.iconclass : '') + (_self.options.autohide ? ' autohide' : '') + '" title="' + _self.options.tooltip + '">' + _self.options.icontext + '</i>');
		_self.el.addClass("has-side-control");
		_self.btn = $("+i", _self.el);
		
		if (typeof _self.options.onclick === "function") {
			_self.btn.on("click", function (e) {
				_self.options.onclick(e);
			});
		}
	};
	
	SideControl.prototype = {
		foo: function () {
			var _self = this;
		}
	}
	
	$.fn.sideControl = function (options) {
		return new SideControl(this, options);
	};
	
	$.fn.sideControl.defaults = {
		iconclass: '',
		icontext : '',
		tooltip  : '',
		autohide : false,
		onclick  : false
	};

})(jQuery, window, document);
//# sourceURL=sideControl.js
