/**
 * jQuery InsertFunction
 * Show a list of predefined functions to choose from
 *
 * @author Nicu Stancioi
 *
 * Copyright (c) 2025 CS GROUP - Romania
 **/

(function ($, window, document) {
	'use strict';
	
	var InsertFunction = function (element, options) {
		var _self = this;
		_self.options = $.extend({}, $.fn.insertFunction.defaults, options);
		
		_self.el = $(element);
		
		// Get the user selection from input control to be replaced with the selected function definition
		// !!! this function must be called before defining the dialog, which changes the focus and rewrites the user selection
		_self.getUserSelection();
		
		// Define modal dialog (destroy previously defined dialog)
		_self.dialog = _self.defineDialog();
		
		// Define events
		_self.defineEvents();
		
		// Get all functions definitions and populate list
		if (typeof _self.options.source === "function") {
			$.when(_self.options.source()).done(function (data) {
				_self.populateFunctions(data);
				_self.selectFirstOption();
			});
		} else if (typeof _self.options.source === "object") {
			_self.populateFunctions(_self.options.source);
			_self.selectFirstOption();
		}
	};
	
	InsertFunction.prototype = {
		// Refresh selection filters (search, categories, filtered functions list)
		updateFilters: function () {
			var _self = this;
			
			var searchValue = $('[name="fn_search"  ]', _self.dialog).val().toUpperCase();
			var fnCategory  = $('[name="fn_category"]', _self.dialog).val();
			
			$('[name="fn_list"]>option', _self.dialog).addClass("hidden");
			$('[name="fn_list"]>option' + (fnCategory == 'All' ? '' : '[data-category="' + fnCategory + '"]') + (searchValue != '' ? '[value*="' + searchValue + '"]' : ''), _self.dialog).removeClass("hidden");
			
			_self.selectFirstOption();
		},
		
		// Automatically selects the first entry from the funtions list
		selectFirstOption: function () {
			var _self = this;
			
			$($('[name="fn_list"]>option:not(.hidden)', _self.dialog)[0]).prop("selected", true);
			$('[name="fn_list"]', _self.dialog).trigger("change");
		},
		
		// Updates the output function definition to match the currently selected funtion
		triggerSelection: function () {
			var _self = this;
			
			$(".ui-icon-check", _self.dialog.closest(".ui-dialog")).parent().get(0).click();
		},
		
		// Get the user selection to be replaced with the output function definition
		getUserSelection: function () {
			var _self = this;
			
			_self.inputSelStart = "";
			_self.inputSelEnd   = "";
			
			_self.el.css({ "background-color": _self.options.backcolor });
			if (_self.el.is("input")) {
				// Input field is a standard html input control
				_self.inputSelStart = _self.el.val().substring(0, _self.el.get(0).selectionStart);
				_self.inputSelEnd   = _self.el.val().substring(   _self.el.get(0).selectionEnd  );
			} else if (_self.el.is("div")) {
				// Input field is a div (can have other level 1 children elements)
				var sel = window.document.getSelection();
				if ($.inArray(_self.el.get(0), $(sel.anchorNode).parents()) == -1 || $.inArray(_self.el.get(0), $(sel.focusNode ).parents()) == -1 ) {
					// User selection starts or ends outside input field:
					// - output function definition will be placed at the end of input text
					_self.inputSelStart = _self.el.text();
					_self.inputSelEnd   = "";
				} else {
					if (sel.isCollapsed || sel.anchorNode == sel.focusNode) {
						// User selection starts and ends within the same div subelement
						// - output function definition will replace the selection of the subelement
						$.each($(sel.anchorNode.parentElement).prevAll(), function () { _self.inputSelStart = this.textContent + _self.inputSelStart; });
						_self.inputSelStart += sel.anchorNode.textContent.substring(0, Math.min(sel.anchorOffset, sel.focusOffset));
						
						_self.inputSelEnd = sel.focusNode.textContent.substring(Math.max(sel.anchorOffset, sel.focusOffset));
						$.each($(sel.focusNode.parentElement).nextAll(), function () { _self.inputSelEnd += this.textContent; });
					} else if ($(sel.anchorNode.parentElement).nextAll().filter(sel.focusNode.parentElement).length !== 0) {
						// Left to right user selection: selection starts on an element and ends on another which is positioned after the first one
						// - output function definition will replace portions of first and last selected subelements and all in-between subelements
						$.each($(sel.anchorNode.parentElement).prevAll(), function () { _self.inputSelStart = this.textContent + _self.inputSelStart; });
						_self.inputSelStart += sel.anchorNode.textContent.substring(0, sel.anchorOffset);
						
						_self.inputSelEnd = sel.focusNode.textContent.substring(sel.focusOffset);
						$.each($(sel.focusNode.parentElement).nextAll(), function () { _self.inputSelEnd += this.textContent; });
					} else {
						// Right to left user selection: selection starts on an element and ends on another which is positioned before the first one
						// - output function definition will replace portions of first and last selected subelements and all in-between subelements
						$.each($(sel.focusNode.parentElement).prevAll(), function () { _self.inputSelStart = this.textContent + _self.inputSelStart; });
						_self.inputSelStart += sel.focusNode.textContent.substring(0, sel.focusOffset);
						
						_self.inputSelEnd = sel.anchorNode.textContent.substring(sel.anchorOffset);
						$.each($(sel.anchorNode.parentElement).nextAll(), function () { _self.inputSelEnd += this.textContent; });
					}
				}
			}
		},
		
		// Apply output function definition to the input element
		applySelection: function () {
			var _self = this;
			
			// Apply selection on input element
			var selectedFunction = $(".fn-definition", _self.dialog).text();
			if (_self.el.is("input")) {
				_self.el.val(_self.inputSelStart + selectedFunction + _self.inputSelEnd);
			} else if (_self.el.is("div")) {
				_self.el.html(_self.inputSelStart + selectedFunction + _self.inputSelEnd);
			}
			_self.el.selectRange(_self.inputSelStart.length, _self.inputSelStart.length + selectedFunction.length);
			_self.el.focus();
			
			// Callback 
			if (typeof _self.options.selection === "function") {
				_self.options.selection (
					_self.inputSelStart + selectedFunction + _self.inputSelEnd,	// text
					_self.inputSelStart.length,									// selectionStart
					_self.inputSelStart.length + selectedFunction.length		// selectionEnd
				);
			}
		},
		
		// Define modal dialog
		defineDialog: function () {
			var _self = this;
			
			// Clear previous dialog instances
			var $container = $(_self.options.container) || $("body");
			$(">.insert-function", $container).data("uiDialog") && $(">.insert-function", $container).dialog("close");
			
			// Define new dialog instance
			var $dialog = $(
				'<div id="insert_function" class="insert-function">' +
				'	<ul>' +
				'		<li><span class="fn-sel"></span></li>' +
				'		<li>' +
				'			<label>Search for a function:</label>' +
				'			<div class="fn-search">' +
				'				<input type="text" placeholder="Search..." name="fn_search" value="" />' +
				'				<i tabindex="1000" class="clear-search fa fa-close" title="Clear"></i>' +
				'				<label>Or select a category:</label>' +
				'				<select name="fn_category">' +
				'					<option value="All" selected>All</option>' +
				'				</select>' +
				'			</div>' +
				'		</li>' +
				'		<li>' +
				'			<label>Select a function:</label>' +
				'			<div class="fn-selection">' +
				'				<select name="fn_list" size="7"></select>' +
				'				<div class="fn-definition"></div>' +
				'				<div class="fn-description"></div>' +
				'			</div>' +
				'		</li>' +
				'	</ul>' +
				'</div>'
			);
			$container.append($dialog);
			
			// Resolve $.fn.button conflict between jQuery UI and Bootstrap
			if (typeof $.fn.button.noConflict === "function") {
				var btn = $.fn.button.noConflict();	// Reverts $.fn.button to jQuery UI btn
				$.fn.btn = btn;						// Assigns Bootstrap button functionality to $.fn.btn
			}
			
			// Apply dialog control
			$dialog.dialog({
				title        : "Insert Function",
				width        : "450px",
				autoOpen     : true,
				modal        : false,
				resizable    : false,
				closeOnEscape: true,
				position     : { my: "center top", at: "center top+200px", of: window },
				dialogClass  : "clean-slate",
				containment  : $(".nav-tabs-custom"),
				buttons      : [
					{
						text : "Ok",
						icon : "ui-icon-check",
						click: function (e) {
							_self.applySelection();
							$(this).dialog("close");
						}
					},
					{
						text : "Cancel",
						icon : "ui-icon-closethick",
						click: function (e) {
							$(this).dialog("close");
						}
					}
				],
				close: function (e, ui) {
					_self.el.css({ "background-color": "" });
					_self.el.focus();
					$(this).dialog("destroy").remove();
				},
				open: function (e, ui) {
					$('[name="fn_search"]', $dialog).focus();
				}
			});
			
			if (!$container.is("body")) {
				$dialog.data("ui-dialog").uiDialog.appendTo($container);
			}
			return $dialog;
		},
		
		// Populate the list of funtions
		populateFunctions: function (data) {
			var _self = this;
			
			$.each(data, function (key, functions) {
				$('[name="fn_category"]', _self.dialog).append('<option vlaue="' + key + '">' + key + '</option>');
				$.each(functions, function (idx, func) {
					var $opt = $('<option data-category="' + key + '" value="' + func.name + '">' + func.name + '</option>');
					$opt.data({ "func": func});
					$('[name="fn_list"]', _self.dialog).append($opt);
				});
			});
		},
		
		// Define events
		defineEvents: function (data) {
			var _self = this;
			
			// Update output function definition when category is changed
			$('[name="fn_category"]', _self.dialog).on("change", function (e) {
				_self.updateFilters();
			});
			
			// Update output function definition when a function is selected from the list
			$('[name="fn_list"]', _self.dialog).on("change", function (e) {
				var dna = $(">option:selected", this).data("func");
				if (typeof dna !== "undefined") {
					var args = "";
					$.each(dna.arguments, function (idx, arg) {
						args = args + (args == '' ? '' : ',') + arg.name;
					});
					$(".fn-definition", _self.dialog).html(dna.name + '(' + args + ')');
					$(".fn-sel", _self.dialog).html('[ <i>' + _self.inputSelStart + '</i><b>' + dna.name + '(' + args + ')' + '</b><i>' +_self.inputSelEnd + '</i> ]'.replaceAll(' ', '&nbsp;'));
					
					var args1 = '<span class="indent1"><b>=[' + dna.returnType + ']</b></span><span class="indent2">' + dna.description + '</span>';
					var args2 = '';
					$.each(dna.arguments, function (idx, arg) {
						args2 = args2 + '<span class="indent1"><b>' + arg.name + '</b> [' + arg.type + ']</span><span class="indent2">' + arg.description + '</span>';
					});
					$(".fn-description", _self.dialog).html('<p class="fn-def">' + args1 + '</p><p class="fn-args">' + args2 + '</p>');
				}
			});
			
			// Apply selection when double-clicking a function in the list
			$('[name="fn_list"]', _self.dialog).on("dblclick", function (e) {
				_self.triggerSelection();
			});
			
			// Clear the seaarch field
			$('.clear-search', _self.dialog).on("click", function (e) {
				$('[name="fn_search"]', _self.dialog).val("");
				_self.updateFilters();
			});
			
			// Manage user keyboard input
			_self.dialog.off("keyup");
			_self.dialog.on("keyup", function (e) {
				if (e.key == "Enter" && e.target.name == "fn_list") {
					// Apply user selection on Enter while focus is on the functions list
					_self.triggerSelection();
				} else if (e.target.name == "fn_search") {
					// Update filters on each change of the search field
					_self.updateFilters();
				}
			});
		}
	}
	
	// Define control to select text in a compatible html element
	$.fn.selectRange = function (start, end) {
		!end && (end = start);
		return this.each(function () {
			if('selectionStart' in this) {
				this.selectionStart = start;
				this.selectionEnd = end;
			} else if (this.setSelectionRange) {
				this.setSelectionRange(start, end);
			} else if (this.createTextRange) {
				var range = this.createTextRange();
				range.collapse(true);
				range.moveEnd('character', end);
				range.moveStart('character', start);
				range.select();
			}
		});
	};
	
	// "InsertFunction" control definition
	$.fn.insertFunction = function (options) {
		return new InsertFunction(this, options);
	};
	
	// "InsertFunction" default values
	$.fn.insertFunction.defaults = {
		container: false,
		source   : {},
		selection: false,
		backcolor: "#e2c8ff"
	};

})(jQuery, window, document);

//# sourceURL=insertFunction.js
