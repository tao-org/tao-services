/**
 * jQuery NiceToggle
 * Adds a button at the right of the control
 *
 * @author Nicu Stancioi
 *
 * Copyright (c) 2025 CS GROUP - Romania
 **/

(function ($, window, document) {
	'use strict';
	
	function parseObject(str) {
		if (str) {
			str = str.trim();
			
			if (str.startsWith('{') && str.endsWith('}')) {
				str = str.slice(1, -1);
			}
			
			const entries = [];
			let start = 0;
			let depth = 0;
			
			// Split on top-level commas
			for (let i = 0; i <= str.length; i++) {
				const ch = str[i];
				
				if (ch === '{') depth++;
				else if (ch === '}') depth--;
				
				if ((ch === ',' && depth === 0) || i === str.length) {
					const pair = str.slice(start, i).trim();
					
					if (pair) {
						const colonIndex = pair.indexOf(':');
						
						const key = pair.slice(0, colonIndex).trim();
						let value = pair.slice(colonIndex + 1).trim();
						
						if (value.startsWith('{') && value.endsWith('}')) {
							value = parseObject(value);
						}
						
						entries.push([key, value]);
					}
					
					start = i + 1;
				}
			}
			
			return Object.fromEntries(entries);
		}
		return void 0;
	}
	
	function getElementWidth(el, unit) {
		const px = el.offsetWidth;
		let width = px + "px";
		switch(unit || "px") {
			case "em":
				const fontSize = parseFloat(getComputedStyle(el).fontSize);
				width = (px / fontSize) + "em";
				break;
		}
		return width;
	}
	
	var NiceToggle = function (element, options) {
		var _self = this;
		
		// Define dialog
		_self.el = $(element);
		if (_self.el.length === 0) {
			return;
		}
		
		_self.options = $.extend(true, {}, $.fn.niceToggle.defaults, options);
		
		// Link css file
		var $niceScript = $("script[src*='nice-toggle.js']");
		var $niceCss    = $("link[href*='nice-toggle.css']");
		if ($niceScript.length > 0 && $niceCss.length == 0) {
			var scriptSrc = $niceScript.attr("src");
			var version = scriptSrc.indexOf("?") >= 0 ? scriptSrc.substring(scriptSrc.indexOf("?")) : "";
			var cleanScriptSrc = scriptSrc.indexOf("?") >= 0 ? scriptSrc.substring(0, scriptSrc.indexOf("?")) : scriptSrc;
			var scriptPath = cleanScriptSrc.lastIndexOf("/") >= 0 ? cleanScriptSrc.substring(0, cleanScriptSrc.lastIndexOf("/")) + "/" : "";
			$('head').append(`<link type="text/css" rel="stylesheet" href="${scriptPath}nice-toggle.css" />`);
		}
		
		_self.container = $(
			`<label class="nice-toggle">
				<span class="nice-bkg nice-on">${_self.options.on}</span>
				<span class="nice-bkg nice-off">${_self.options.off}</span>
				<span class="nice-btn ${_self.el.get(0).checked ? _self.options.onstyle.btn.style : _self.options.offstyle.btn.style}">
					<span class="nice-on">${_self.options.on}</span>
					<span class="nice-off">${_self.options.off}</span>
				</span>
			</label>`
		);
		$(`<span>${_self.options.on }</span>` ).text().trim() === "" && $('.nice-on' , _self.container).addClass("icon-only");
		$(`<span>${_self.options.off}</span>` ).text().trim() === "" && $('.nice-off', _self.container).addClass("icon-only");
		
		_self.el.after(_self.container);
		_self.el.prependTo(_self.container);
		
		setTimeout(function () {
			let onWidth  = parseFloat(getElementWidth($('~.nice-on' , _self.el)[0], "em"));
			let offWidth = parseFloat(getElementWidth($('~.nice-off', _self.el)[0], "em"));
			let elWidth = Math.max(onWidth, offWidth);
			$('~.nice-bkg,~.nice-btn' , _self.el).css({ "min-width": elWidth + "em" });
		}, 0);
		
		_self.container.addClass(_self.options.size);
		_self.container.addClass(_self.options.style);
		
		_self.container[0].style.setProperty('--ntg-on-color' , _self.options.onstyle.color);
		_self.container[0].style.setProperty('--ntg-off-color', _self.options.offstyle.color);
		_self.container[0].style.setProperty('--ntg-on-bkg'   , _self.options.onstyle.background);
		_self.container[0].style.setProperty('--ntg-off-bkg'  , _self.options.offstyle.background);
		$('.nice-btn', _self.container)[0].style.setProperty('--ntg-btn-on-color' , _self.options.onstyle.btn.color);
		$('.nice-btn', _self.container)[0].style.setProperty('--ntg-btn-off-color', _self.options.offstyle.btn.color);
		$('.nice-btn', _self.container)[0].style.setProperty('--ntg-btn-on-bkg'   , _self.options.onstyle.btn.background);
		$('.nice-btn', _self.container)[0].style.setProperty('--ntg-btn-off-bkg'  , _self.options.offstyle.btn.background);
		
		_self.initEvents();
		
		_self.el.data("niceToggle", _self);
	};
	
	NiceToggle.prototype = {
		refreshButtonStyle: function (checked) {
			var _self = this;
			
			$('~.nice-btn', _self.el).attr({ "class": `nice-btn ${checked ? _self.options.onstyle.btn.style : _self.options.offstyle.btn.style}` });
		},
		
		initEvents: function () {
			var _self = this;
			
			// Intercept any change of the checkbox state, done programmatically
			const descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'checked' );
			Object.defineProperty(_self.el.get(0), 'checked', {
				get() {
					return descriptor.get.call(this);
				},
				set(value) {
					_self.refreshButtonStyle(value);
					descriptor.set.call(this, value);
				}
			});
			
			// Intercept any change of the checkbox state, done in browser
			_self.el.on("change", function (e) {
				_self.refreshButtonStyle(e.target.checked);
			});
		}
	}
	
	$.fn.niceToggle = function (options) {
		return new NiceToggle(this, options);
	};
	
	$.fn.niceToggle.defaults = {
		on      : '<i class="fa fa-check"></i>',
		off     : '<i class="fa fa-ban"></i>',
		onstyle : { color: '#919191', background: '#CFD4DE', btn: { color: '#4CAF50', background: '#EFF1F7', style: '' } },
		offstyle: { color: '#898989', background: '#B3B3B3', btn: { color: '#F44336', background: '#EFF1F7', style: '' } },
		size    : 'normal',
		style   : 'normal'
	};
	
	$(document).ready(function () {
		$.each($('[data-ntg="true"]'), function (idx, ntg) {
			let options = {};
			options.on       = $(ntg).attr('data-ntg-on' );
			options.off      = $(ntg).attr('data-ntg-off');
			options.onstyle  = parseObject($(ntg).attr('data-ntg-onstyle' ));
			options.offstyle = parseObject($(ntg).attr('data-ntg-offstyle'));
			options.size     = $(ntg).attr('data-ntg-size');
			options.style    = $(ntg).attr('data-ntg-style');
			$(ntg).niceToggle(options);
			$(ntg).removeAttr("data-ntg data-ntg-on data-ntg-off data-ntg-onstyle data-ntg-offstyle data-ntg-size data-ntg-style");
		});
	});

})(jQuery, window, document);
//# sourceURL=niceToggle.js