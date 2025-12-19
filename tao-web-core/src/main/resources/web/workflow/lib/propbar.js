/**
 * jQuery PropBar
 * Manage workflow node properties
 *
 * @author Nicu Stancioi
 *
 * Copyright (c) 2025 CS GROUP - Romania
 **/

(function ($, window, document) {
	'use strict';
	
	var PropBar = function (element, options) {
		var _self = this;
		_self.options = $.extend({}, $.fn.propBar.defaults, options);
		
		// Callbacks
		if (typeof options === "string") {
			_self = $(element).data("propBar");
			
			// Simple callbacks
			_self.nid = options;
			_self.renderNode();
			_self.nid && _self.ready();
			
			return false;
		// New
		} else {
			_self.el = $(element);
			_self.el.addClass("propbar");
			_self.el.data({ "propBar": _self });
			
			_self.nid      = _self.options.nid;
			_self.ntype    = _self.options.ntype;
			_self.nodeData = _self.options.nodeData;
			
			// Define events
			_self.defineEvents();
			
			_self.topologyList = [];
			
			_self.init();
			
			return _self;
		}
	}
	
	PropBar.prototype = {
		ready: function () {
			var _self = this;
			
			(typeof _self.options.ready === "function") && _self.options.ready();
		},
		
		done: function () {
			var _self = this;
			
			(typeof _self.options.done === "function") && _self.options.done();
		},
		
		clean: function () {
			var _self = this;
			
			closePolygonMap();
			$(".tbl-edt-body", _self.el).html('');
			$(".app-card"    , _self.el).hide();
		},
		
		init: function () {
			var _self = this;
			
			_self.renderNode();
			_self.nid && _self.ready();
		},
		
		isEmpty: function (value) {
			var _self = this;
			
			var empty = (value === null) || (typeof value === "undefined");
			if (!empty) {
				if (typeof value === "string") {
					empty = (value === "") || (value === "null") || (value === "_null");
				} else if ($.isArray(value)) {
					empty = (value.length === 0) || (value.length === 1 && _self.isEmpty(value[0]));
				} else if (typeof value === "object") {
					empty = $.isEmptyObject(value);
				} else if (typeof value === "boolean") {
					empty = value;
				}
			}
			return empty;
		},
		
		save: function () {
			var _self = this;
			
			var cV = [];
			// Parse custom values
			$.each($(".val-row", _self.el), function (idx, row) {
				var $row = $(row);
				
				var parentName = $(".var-parent-name", $row).html();
				var pName      = (_self.isEmpty(parentName) ? '' : parentName + "~") + $(".var-name", $row).html();
				var pValue     = null;
				
				var selDataType = $(".var-dataType", $row).html();
				var hasValueset = $(".var-value-list option", $row).length > 0;
				
				if (selDataType.indexOf("[]") > -1) {
					pValue = hasValueset ? $(".var-value-list", $row).val() : $(".var-value-string",$row).val();
					pValue = _self.isEmpty(pValue) ? null : "[" + pValue + "]";
				} else {
					pValue = hasValueset ? $(".var-value-list", $row).val() : $(".var-value",$row).val();
					pValue = _self.isEmpty(pValue) ? null : pValue;
				}
				
				var varType    = $(".var-type", $row).html();
				var defaultVal = $(".var-default", $row).html();
				if (
					(pValue  !== defaultVal || (pValue === defaultVal && hasValueset)) &&
					(pValue  !== ""         || (pValue === "" && hasValueset)        ) &&
					(pValue  !== "[]"                                                ) &&
					(pValue  !== null                                                ) &&
					(varType !== "TEMPLATE"                                          )
				) {
					cV.push({ parameterName: pName, parameterValue: pValue });
				}
			});
			
			_self.nodeData.customValues = cV;
			_self.nodeData.name = $(".val-propbar-name", _self.el).html();
			var putOneComponent = $.ajax({
				cache   : false,
				url     : baseRestApiURL + "workflow/node?workflowId=" + currentWfID,
				type    : "PUT",
				dataType: "json",
				data    : JSON.stringify(_self.nodeData),
				headers : {
					"Accept"      : "application/json",
					"Content-Type": "application/json",
					"X-Auth-Token": window.parent.tokenKey
				}
			});
			$.when(putOneComponent)
				.done(function (putOneComponentResponse) {
					if (putOneComponentResponse.status === "SUCCEEDED") {
						$(".v-lastaction", "#infoband").html("custom values updated");
						wfPlumbCanvasData.nodes[_self.nid] = putOneComponentResponse.data;
						canvasRenderer.updateNodeById(_self.nid, putOneComponentResponse.data);
					} else {
						alert(putOneComponentResponse.message);
					}
				})
				.fail(function (jqXHR, status, textStatus) {
					alert("Could not update custom values", "ERROR");
				});
		},
		
		sideControlAction: function (action, $control) {
			var _self = this;
			
			// Get the list of functions
			var getConfigFunctions = function () {
				var deferred = $.Deferred();
					if (typeof window.configFunctions !== "undefined") {
						deferred.resolve(window.configFunctions);
					} else {
						$.ajax({
							url    : baseRestApiURL + "config/functions",
							type   : "GET",
							cache  : false,
							headers: { "X-Auth-Token": window.tokenKey },
							error  : function (jqXHR, textStatus, errorThrown) { deferred.resolve({}); },
							success: function (response) {
								if (response?.status === responseStatus.success && typeof response.data !== "undefined") {
									window.configFunctions = response.data;
									deferred.resolve(window.configFunctions);
								} else {
									deferred.resolve({});
								}
							}
						});
					}
				return deferred.promise();
			};
			var $input = $control.prev(".has-side-control");
			if ($input.length > 0) {
				switch (action) {
					case "togglePassword":
						// Toggle password visibility
						if ($control.hasClass('fa-eye')) {
							$input.attr("type", "text");
							$control.removeClass("fa-eye").addClass("fa-eye-slash").attr("title", "hide password");
						} else if ($control.hasClass('fa-eye-slash')) {
							$input.attr("type", "password");
							$control.removeClass("fa-eye-slash").addClass("fa-eye").attr("title", "show password");
						}
						break;
					case "showMap":
						// Load poly map
						closePolygonMap();
						openPolygonMap($("#propbarMap"), $input);
						break;
					case "insertFunction":
						$input.insertFunction({
							container: $("body"),
							source   : function () { return getConfigFunctions(); },
							selection: function (response) {
								$input.trigger("change");
							}
						});
						break;
				}
			}
		},
		
		renderModule: function (nid, lcl_n, componentTemplate, nodeTokens, sourcesIndex) {
			var _self = this;
			
			var html_details = "";
			html_details += '<span class="thin-scrollbar onhover"><strong>version:</strong> '     + componentTemplate.version      + '</span>';
			html_details += '<span class="thin-scrollbar onhover"><strong>description:</strong> ' + componentTemplate.description  + '</span>';
			html_details += '<span class="thin-scrollbar onhover"><strong>authors:</strong> '     + componentTemplate.authors      + '</span>';
			html_details += '<span class="thin-scrollbar onhover"><strong>copyright:</strong> '   + componentTemplate.copyright    + '</span>';
			html_details += '<span class="thin-scrollbar onhover"><strong>affinity:</strong> '    + componentTemplate.nodeAffinity + '</span>';
			$(".val-propbar-details", _self.el).html(html_details);
			
			var helper_putValue = function ($el, name, fullname, type, value, valueset) {
				var currentCustomValueSet = _.find(_self.nodeData.customValues, function (item) { return (item.parameterName === fullname); });
				if (typeof currentCustomValueSet === "object" && currentCustomValueSet.hasOwnProperty("parameterValue")) {
					if ((currentCustomValueSet.parameterValue === "[]" || currentCustomValueSet.parameterValue === "") && valueset !== null) {
						value = null;
					} else {
						value = currentCustomValueSet.parameterValue;
					}
				}
				if (_self.isEmpty(valueset)) {
					$('input.var-value', $el).val(value);
					if (type.indexOf("[]") > -1) {
						// Multivalue: convert input field to tags control
						$('input.var-value-string', $el).addClass("tag-format");
						if(value !== null && value!== "") {
							$('input.var-value', $el).val(value.substring(1, value.length-1));
							$('input.var-value-string', $el).val(value.substring(1, value.length-1)).show();
						}
					} else if (type === "date") {
						// Date: convert input field to date
						$('input.var-value-string', $el).attr("type", "date");
						if (value != null) {
							$('input.var-value-string', $el).val(value.split("T")[0]).show();
						} else {
							$('input.var-value-string', $el).show();
						}
					} else if ((type === "int") || (type === "long") || (type === "double") || (type === "short") || (type === "float") || (type === "number")) {
						// Number: convert input field to number
						$('input.var-value-string', $el).attr("type", "number");
						$('input.var-value-string', $el).val(value).show();
					} else if (type === "polygon" || type === "wkt") {
						// Add side button that opens up the map, when clicked
						$('input.var-value-string', $el).sideControl({
							iconclass: 'fa fa-globe fa-larger ol',
							tooltip  : "show map",
							onclick  : function (e) { _self.sideControlAction("showMap", $(e.currentTarget)); }
						});
						$('input.var-value-string', $el).val(value).show();
					} else {
						// Other types: string, bool, TEMPLATE
						if (name === "username") {
							$('input.var-value-string', $el).attr("placeholder", "Enter username");
							$('input.var-value-string', $el).sideControl({
								iconclass: 'fa fa-user-o'
							});
						} else if (name === "password") {
							// Password: convert input field to password
							$('input.var-value-string', $el).attr("type", "password");
							$('input.var-value-string', $el).attr("placeholder", "Enter password");
							// Add side button that toggles the password visibility, when clicked
							$('input.var-value-string', $el).sideControl({
								iconclass: 'toggle-password fa fa-eye',
								tooltip  : "show password",
								onclick  : function (e) { _self.sideControlAction("togglePassword", $(e.currentTarget)); }
							});
						} else {
							// Add side button that shows a dialog with all functions list, when clicked
							$('input.var-value-string', $el).sideControl({
								iconclass: "insert-function",
								icontext : 'f<sub>x</sub>',
								autohide : true,
								tooltip  : "insert function",
								onclick  : function (e) { _self.sideControlAction("insertFunction", $(e.currentTarget)); }
							});
						}
						$('input.var-value-string', $el).val(value).show();
					}
				} else {
					$('input.var-value', $el).val(value);
					var $sel = $('select.var-value-list', $el);
					var options = '<option class="empty-option" value=""' + (_self.isEmpty(value) ? ' selected' : '') + '>Empty value</option>';
					$.each(valueset, function(i, v) {
						options += _self.isEmpty(v) ? '' : '<option value="' + v + '"' + (v === value ? ' selected' : '') + '>' + v + '</option>';
					});
					$sel.html(options);
					
					if (type.indexOf("[]") > -1) {
						$sel.attr("id", name);
						$sel.removeClass("form-control collapse");
						$sel.prop("multiple", "multiple")
						$('.empty-option', $sel).remove();
						
						$sel.select2({
							width         : "100%",
							allowClear    : true,
							placeholder   : "Select item...",
							dropdownParent: $el
						});
						if (_self.isEmpty(value)){
							$sel.val(null);
							$sel.trigger("change");
						} else {
							var valArray = value.substring(1, value.length - 1);
							valArray = valArray.split(",");
							$sel.val(valArray);
							$sel.trigger("change");
						}
					} else {
						$sel.show();
					}
				} 
			};
			
			var helper_addSTblEdtRow = function(payload, objParent){
				var obj = {
					"id"          : "",
					"dataType"    : "string",
					"defaultValue": "",
					"description" : "",
					"format"      : null,
					"label"       : "",
					"notNull"     : false,
					"type"        : "REGULAR",
					"unit"        : null,
					"validator"   : null,
					"valueSet"    : [],
					"value"       : null,
					"parameters"  : []
				};
				$.extend(obj, payload);
				
				var $el = $(".tpl-sample-row", _self.el).clone().addClass("val-row").removeClass("tpl-sample-row");
				$('span.var-id'         , $el).html(obj.id);
				$('span.var-name'       , $el).html(obj.name);
				$('span.var-label'      , $el).html(obj.label);
				$('span.var-type'       , $el).html(obj.type);
				$('span.var-description', $el).html(obj.description);
				$('span.var-dataType'   , $el).html(obj.dataType);
				
				if (obj.defaultValue !== '') {
					$('span.var-default', $el).html(obj.defaultValue);
				} else {
					$('span.var-default-label', $el).addClass('collapse');
				}
				
				var fullname = (objParent ? objParent.name + "~" : "") + obj.name
				if (obj.dataType === "bool") {
					if (obj.value === null) {
						helper_putValue($el, obj.name, fullname, obj.dataType, obj.defaultValue, [ "true", "false" ]);
					} else {
						helper_putValue($el, obj.name, fullname, obj.dataType, obj.value, [ "true", "false" ]);
					}
				} else {
					helper_putValue($el, obj.name, fullname, obj.dataType, obj.value, obj.valueSet);
				}
				
				if (obj.type === "TEMPLATE") {
					$(".textviewWrapper", $el).removeClass("textviewWrapper");
					$(".div-params", $el).css("display","none");
				}
				
				if (typeof objParent === "undefined") {
					$(".tbl-edt-body", _self.el).append($el);
				} else {
					$('span.var-parent-id'  , $el).html(objParent.id);
					$('span.var-parent-name', $el).html(objParent.name);
					objParent.container.append($el);
				}
				
				if (typeof obj.parameters[0] !== "undefined"){
					$el.find("td").append('<table class="tbl-extra-param"><tbody></tbody></table>');
					$.each(obj.parameters, function (i, param) {
						obj.container = $(".tbl-extra-param>tbody", $el);
						helper_addSTblEdtRow(param, obj);
					});
				}
				
				return $el;
			};
			
			// Parse additional info (affinity, format)
			if (typeof lcl_n.additionalInfo !== "undefined") {
				var format      =  _.findWhere(lcl_n.additionalInfo, { "parameterName": "formatName"       });
				var formatNames = (_.findWhere(lcl_n.additionalInfo, { "parameterName": "formatNameValues" })?.parameterValue || "").split(",");
				lcl_n.additionalInfo.forEach(element => {
					if (element.parameterName != "formatNameValues") {
						var obj = {
							label       : element.parameterName,
							description : element.parameterName,
							dataType    : "string",
							defaultValue: element.parameterValue,
							required    : element.parameterName != "formatName",
							valueSet    : element.parameterName != "formatName" ? _self.topologyList : formatNames,
							value       : element.parameterValue,
							name        : element.parameterName
						};
						helper_addSTblEdtRow(obj);
					}
				});
			}
			
			// Parse parameters
			componentTemplate.parameterDescriptors.sort(function(a, b) { return (a.id > b.id ? 1 : (a.id < b.id ? -1 : 0)); });
			$.each(componentTemplate.parameterDescriptors, function(i, parameter) {
				if (parameter.dataType === "string" && _self.isEmpty(parameter.valueSet[0]) && parameter.type !== "TEMPLATE") {
					var value = {
						id          : parameter.id,
						dataType    : parameter.dataType,
						defaultValue: "",
						description : parameter.description,
						name        : parameter.name,
						label       : parameter.label,
						notNull     : parameter.notNull,
						type        : parameter.type,
						valueSet    : parameter.valueSet,
						parameters  : parameter.parameters
					};
					var elValue = helper_addSTblEdtRow(value);
					var elFilename = $('.var-value-string', elValue)[0];
					var elValueVal = $('.var-value', elValue)[0];
					//if ($.inArray(parameter.name, [ "username", "password" ]) == -1) {
					if ($("+i.insert-function", elFilename).length) {
						// Call filename plugin on element
						$(elFilename).npFileName({
							tags         : nodeTokens,
							output       : elValueVal,
							submitOnEnter: false,
							sourcesIndex : sourcesIndex
						});
					}
					
				} else {
					helper_addSTblEdtRow(parameter);
				}
			});
			
			// Parse targets
			if (componentTemplate.category !== "CONDITION") {
				$.each(componentTemplate.targets, function (i, target) {
					if ((wfTools.isOldVersion && typeof target.dataDescriptor !== "undefined") || !wfTools.isOldVersion) {
						var value = {
							id          : target.id,
							name        : target.name,
							label       : target.name,
							defaultValue: wfTools.isOldVersion ? target.dataDescriptor.location : target.location,
							description : "Output parameter",
							valueSet    : [ "null" ]
						};
						var elTarget    = helper_addSTblEdtRow(value);
						var elFilename  = $('.var-value-string', elTarget)[0];
						var elTargetVal = $('.var-value'       , elTarget)[0];
						// Call filename plugin on element
						$(elFilename).npFileName({
							tags         : nodeTokens,
							output       : elTargetVal,
							submitOnEnter: false,
							sourcesIndex : sourcesIndex
						});
					}
				});
			}
			
			if (componentTemplate.parameterDescriptors.length > 0 || lcl_n.additionalInfo.length > 0) {
				// Accomodate side controls to all npFileName parameters
				$.each($(".textviewWrapper:has(+.side-control)", _self.el), function () {
					$(".input", this).addClass("has-side-control").after($(this).next());
				});
				
				// Format tags
				$(".tag-format", _self.el).tagsInput({
					minChars	   : 0,
					maxChars	   : null,
					limit		   : null,
					interactive	   : true,
					autocomplete   : null,
					unique		   : false,
					allowDuplicates: true,
					placeholder	   : "Add an element",
					delimiter	   : ","
				});
				
				// Show parameters
				$(".tpl-sample-row", _self.el).closest(".app-card").show();
				$.each($(".val-row .npsyntaxeditor", _self.el), function () {
					$(this).css({ "width": ($(this).closest(".div-params").width() - 4) + "px" });
				});
			}
		},
		
		renderNode: function () {
			var _self = this;
			
			_self.clean();
			
			var lcl_n = _self.nodeData = wfPlumbCanvasData.nodes[ _self.nid || "" ];
			if (typeof lcl_n === "undefined") { return false; }
			
			var naBackgroundURL   = "./media/module01.png";
			var componentTemplate = null;
			var lclTBOID          = "tboid" + jsHashCode(lcl_n.componentId);
			switch (lcl_n.componentType) {
				case "PROCESSING":	_self.ntype = "PROCESSING"; naBackgroundURL = "./media/module01.png" ; wfTools.toolboxnodes.pc[lclTBOID] && (componentTemplate = wfPlumbCanvasData.nodeTemplates.pc[lclTBOID].dna); break;
				case "CONDITION" :	_self.ntype = "CONDITION" ; naBackgroundURL = "./media/cond-comp.png"; wfTools.toolboxnodes.cc[lclTBOID] && (componentTemplate = wfPlumbCanvasData.nodeTemplates.cc[lclTBOID].dna); break;
			}
			var backgroundURL = naBackgroundURL;
			if (componentTemplate) {
				$.each(lcl_n.additionalInfo, function (idx, info) {
					if (($.inArray(info.parameterName, [ "formatName", "formatNameValues" ]) == -1) && (typeof componentTemplate[info.parameterName] === "undefined")) {
						componentTemplate[info.parameterName] = info.parameterValue;
					}
				});
				(_self.ntype == "PROCESSING") && (backgroundURL = "./media/" + componentTemplate.containerId + ".png");
			}
			
			$(".val-propbar-name"       , _self.el).html(lcl_n.name);
			$(".val-propbar-componentid", _self.el).html('<span>' + lcl_n.componentId + '</span> <i class="fa fa-caret-up"></i><i class="fa fa-caret-down"></i>');
			$('.val-propbar-logo'       ,_self.el).attr("src", backgroundURL).off("error").one("error", function (e) { $(this).attr("src", naBackgroundURL); });
			
			// Topologies
			var getTopologies = function () {
				return $.ajax({
					cache    : false,
					url      : baseRestApiURL + "topology/available",
					type     : "GET",
					dataType : "json",
					headers  : {
						"Accept"      : "application/json",
						"Content-Type": "application/json",
						"X-Auth-Token": window.parent.tokenKey
					}
				});
			};
			// Tokens
			var getTokens = function () {
				return $.ajax({
					cache   : false,
					url     : baseRestApiURL + "component/naming/find?nodeId=" + lcl_n.id,
					type    : "GET",
					dataType: "json",
					headers : {
						"Accept"      : "application/json",
						"Content-Type": "application/json",
						"X-Auth-Token": window.parent.tokenKey
					}
				});
			};
			$.when(getTopologies(), getTokens())
				.done(function (getTopologiesResponse, getTokensResponse) {
					// Topologies
					_self.topologyList = getTopologiesResponse[0].data;
					
					// Tokens
					var sourcesIndex = [];
					var prepareTokens = function (dd) {
						var tags = [];
						var ddUniqueBySensor = dd.filter((v, i, a) => a.findIndex(t => (t.sensor === v.sensor)) === i);
						ddUniqueBySensor.forEach(function (d, idx) {
							if (!$.isEmptyObject(d.tokens)) {
								sourcesIndex.push({ "index": d.minIndex, "sensor": d.sensor });
								Object.keys(d.tokens).sort().forEach(function (key) {
									tags.push({ "key": key, "desc": d.tokens[key] + " - " + d.sensor, "range": { "minIndex": d.minIndex, "maxIndex": d.maxIndex } });
								});
							}
						});
						return tags;
					}
					var nodeTokens = prepareTokens(getTokensResponse[0].data);
					_self.renderModule(_self.nid, lcl_n, componentTemplate, nodeTokens, sourcesIndex);
				});
		},
		
		defineEvents: function () {
			var _self = this;
			
			_self.el
				// Prevent keystrokes propagation outside propBar
				.on("keyup keydown", function (e) {
					e.stopPropagation();
				})
				// Edit node name
				.on("click", ".pni-edit-btn", function (e) {
					$(".pni-editable"      , _self.el).addClass("edit");
					$(".pni-edit-input", _self.el).val($(".val-propbar-name", _self.el).text()).focus();
				})
				// Save node name
				.on("blur keyup", ".pni-edit-input", function (e) {
					if (e.type === "focusout" || (e.type === "keyup" && e.keyCode === 13)) {
						$(".val-propbar-name", _self.el).text($(".pni-edit-input", _self.el).val());
						$(".pni-editable"    , _self.el).removeClass("edit");
					}
				})
				.on("click", ".val-propbar-componentid", function (e) {
					$(".val-propbar-details", _self.el).toggleClass("minimized");
				})
				.on("click", ".tbl-prop-expand-action", function (e) {
					e.preventDefault();
					if($(this).hasClass("active")){
						$(".val-row-plus").css({"display": "none"});
						$(this).removeClass("active");
						return;
					}
					$(".val-row-plus").css({"display": "none"});
					$(".tbl-prop-expand-action").removeClass("active");
					$(this).addClass("active");
					$(this).closest("tr").next().css({"display": "table-row"});
				})
				.on("change",".var-value-string, .var-value-string2, .var-value-list",function(e){
					if ($(".var-value-string2:visible", $(this).parent()).length > 0) {
						var d1 = $(".var-value-string" , $(this).parent()).val();
						var d2 = $(".var-value-string2", $(this).parent()).val();
						var ds = (d1.length > 0 && d2.length > 0) ? "[" + d1 + "," + d2 + "]" : (d1.length > 0 ? d1 : d2);
						$(this).siblings(".var-value").val(ds);
					} else {
						$(this).siblings(".var-value").val($(this).val());
						if($(this)[0].type == "date"){
							var endstring = $(this).siblings(".var-default").html().split("T")[1];
							$(this).siblings(".var-value").val($(this).val() + "T" + endstring);
						}
					}
				})
				.on("click", '[type="submit"]',function(e){
					e.preventDefault();
					e.stopPropagation();
					if(_self.ntype === "PROCESSING" || _self.ntype === "CONDITION"){
						_self.save();
					}
					
					_self.clean();
					_self.done();
				})
				.on("click", ".var-default", function (e) {
					var dval = $(this).html();
					var $vstr = $(".var-value-string:visible", $(this).parent());
					if ($vstr.length) {
						$vstr.val(/\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z)/.test(dval) ? dval.split("T")[0] : dval);
						$vstr.trigger("change");
					}
					var $vlist = $(".var-value-list:visible", $(this).parent());
					if ($vlist.length) {
						$vlist.val(dval);
						$vlist.trigger("change");
					}
				});
		}
	}
	
	// "PropBar" control definition
	$.fn.propBar = function (options) {
		return new PropBar(this, options);
	};
	
	// "PropBar" default values
	$.fn.propBar.defaults = {
		nid     : null,
		ntype   : null,
		nodeData: null,
		ready   : false,
		done    : false
	};
	
})(jQuery, window, document);

//# sourceURL=propBar.js
