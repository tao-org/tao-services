/**
 * Copyright  CS ROMANIA, http://c-s.ro
 *
 * Licensed under the ....;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * file details: core functions and general functionality, request router for single page app.
 *
 **/

var routeTags     = [];
var routeFilters  = {};
var userOpStack   = [];  //holder for aditional user action parameters

function refreshToken() {
	var refreshed = false;
	
	var tokenRefresh = _settings.readCookie("refreshToken");
	var userId = _settings.readCookie("TaoUserId");
	if (tokenRefresh !== "" && tokenRefresh != null && userId !== "" && userId != null) {
		// Refresh token
		$.ajax({
			"async"  : false,	// !!! Important: this call MUST be synchronous otherwise some ajax call will fail
			"cache"  : false,
			"url"    : baseRestApiURL + "auth/refresh",
			"method" : "POST",
			"headers": { "Content-Type": "application/x-www-form-urlencoded" },
			"data"   : { "user" : userId, "token": tokenRefresh },
			"success": function (r) {
				var apiData = chkTSRF(r);
				if (apiData.token && apiData.refreshToken) {
					console.log("Token refreshed [" + apiData.token + "]");
					// Restore token cookies
					_settings.createCookie("tokenKey", apiData.token, apiData.expiresInSeconds/60/60/24);
					_settings.createCookie("refreshToken", apiData.refreshToken);
					// Restore global token variable
					window.tokenKey = apiData.token;
					refreshed = true;
				} else {
					// Failed to retrieve new token.
                    _settings.createCookie("tokenKey",'');
                    _settings.createCookie("userMatrix",'{}');
                    _settings.createCookie("refreshToken","");
                    _settings.createCookie("userRole","");
                    _settings.createCookie("TaoUserId","");
                    _settings.createCookie("TaoUserName","");
                    _settings.createCookie("JSESSIONID","");
					window.location = "login.html";
				}
			},
			"error": function (jqXHR, status, textStatus) {
				// Could not refresh the token
                _settings.createCookie("tokenKey",'');
                _settings.createCookie("userMatrix",'{}');
                _settings.createCookie("refreshToken","");
                _settings.createCookie("userRole","");
                _settings.createCookie("TaoUserId","");
                _settings.createCookie("TaoUserName","");
                _settings.createCookie("JSESSIONID","");
				window.location = "login.html";

			}
		});
	} else {
        _settings.createCookie("tokenKey",'');
        _settings.createCookie("userMatrix",'{}');
        _settings.createCookie("refreshToken","");
        _settings.createCookie("userRole","");
        _settings.createCookie("TaoUserId","");
        _settings.createCookie("TaoUserName","");
        _settings.createCookie("JSESSIONID","");
        window.location = "login.html"
	}
	return refreshed;
}

// Catch ajax calls and check for token validity
(function() {
	var _oldAjax = $.ajax;
	$.ajax = function(options) {
		var tokenKey = _settings.readCookie("tokenKey");
		if ((typeof options.headers !== "undefined") && (typeof options.headers["X-Auth-Token"] !== "undefined") && (tokenKey === "" || tokenKey == null)) {
			// Refresh token
			if (refreshToken()) {
				options.headers["X-Auth-Token"] = window.tokenKey;
			}
		}
		return _oldAjax(options);
	 };
})();

// ROUTER
$(function () {
    var taoDynPageDiv = $(".tao-async-cw");
    var DEFAULT_PAGE_URL = "#admin/dashboard";
    var UNDEFINED_PAGE_URL = "#undefined/page";
    var hashRoutesMap = [];
    
	function getSidebarMenu() {
		var deferred = $.Deferred();
			$.ajax({
				url    : baseRestApiURL + "config/menu",
				type   : 'GET',
				cache  : false,
				headers: { "X-Auth-Token": window.tokenKey },
				success: function (response) {
					if (typeof response.status === "string" && response.status === responseStatus.success && typeof response.data !== "undefined") {
						deferred.resolve(response.data);
					} else {
						var jqXHR = {
							"data"   : typeof response.data    === "undefined"                            ? null                : response.data,
							"status" : typeof response.status  === "undefined"                            ? responseStatus.fail : response.status,
							"message": typeof response.message === "undefined" || response.message === "" ? "Error"             : response.message
						}
						deferred.reject(jqXHR.data, jqXHR.status, jqXHR.message);
					}
				},
				error: function (jqXHR, textStatus, errorThrown) {
					deferred.reject(jqXHR, textStatus, "Untracked error");
				}
			});
		return deferred.promise();
	}
	var iconMapping = {
		"Dashboard"              : "fa fa-columns"     ,
		"My Datasets"            : "fa fa-database"    ,
			"Search"             : "fa fa-search"      ,
			"Sites"              : "fa fa-map"         ,
			"Repositories"       : "fa fa-folder-open" ,
			"Product Sets"       : "fa fa-cubes"       ,
			"Shared Datasets"    : "fa fa-database"    ,
			"My Database"		 : "fa fa-table"	   ,
		"My Workflows"           : "fa fa-sitemap"     ,
			"Workflows"          : "fa fa-sitemap"     ,
			"Remote Services"    : "fa fa-plug"        ,
			"Scheduled Jobs"     : "fa fa-clock-o"     ,
		"My Components"          : "fa fa-briefcase"   ,
			"Components"         : "fa fa-server"      ,
			"Containers"         : "fa fa-archive"     ,
		"My Agent"           	 : "fa fa-comments"    ,
		"My Assistant"           : "fa fa-agent-tao"   ,
			"LLM Configuration"	 : "fa fa-gears"	   ,
			"Assistant"	 		 : "fa fa-comments"	   ,
		"Admin area"             : "fa fa-sliders"     ,
			"System Dashboard"   : "fa fa-columns"     ,
			"Toolbox Containers" : "fa fa-archive"     ,
			"Processing Modules" : "fa fa-microchip"   ,
			"Public Repositories": "fa fa-folder-open" ,
			"Remote Web Services": "fa fa-globe"       ,
			"Data Sources"       : "fa fa-database"    ,
			"Topology"           : "fa fa-topology-tao",
			"Configuration"      : "fa fa-gears"       ,
			"Users"              : "fa fa-users"       ,
		"How To"                 : "fa fa-book"
	}
	// Get dynamic menu data
	$.when(getSidebarMenu()).done(function (menu) {
		var sidebarMenu = [];
		
		// Clone top header user menu entries
		sidebarMenu.push({
			"position": -1001,
			"title"   : $(".user-menu .user-header>div").html(),
			"items"   : $.map($(".user-menu .menu>li>a"), function (el, idx) { return el.attributes["data-action"] ? { "name": el.innerHTML, "action": el.attributes["data-action"].value } : null; })
		});
		
		// Navigation
		sidebarMenu.push({"position":-1000,"title":"NAVIGATION"});
		
		// Actual menu entries from server
		$.each(menu, function (index, entry) {
			entry.icon = iconMapping[entry.title] || "fa fa-bars";
			entry.items = $.map(entry.items, function (item) { if ($.inArray(taoUserProfile.userRole, item.visibility) > -1) { return item; } });
			if (entry.items.length > 0) {
				entry.items.sort(function (a, b) { return (a.position < b.position ? -1 : (a.position > b.position ? 1 : 0)); });
				$.each(entry.items, function (idx, item) {
					item.icon = iconMapping[item.name] || "fa fa-bars";
				});
				sidebarMenu.push(entry);
			}
		});
		sidebarMenu.sort(function (a, b) { return (a.position < b.position ? -1 : (a.position > b.position ? 1 : 0)); });
		
		// Documentation
		sidebarMenu.push({"position":1001,"title":"DOCUMENTATION"});
		sidebarMenu.push({"items":[{"name":"How To","route":"howto/intro","location":"./fragments/tao-howto.fragment.html","visibility":["USER","ADMIN"],"section":"DOCUMENTATION","position":1}],"position":1002,"title":"How To","icon":iconMapping["How To"]});
		// Quota resource & time
		sidebarMenu.push({"position":1003,"title":"RESOURCE USAGE","wrapper_id":"#wrapper-quota"     });
		sidebarMenu.push({"position":1004,"title":"TIME USAGE"    ,"wrapper_id":"#wrapper-quota-time"});
		
		var $hul = $(".sidebar-menu").html('');
		$.each(sidebarMenu, function (index, entry) {
			var $hli = $('<li class="routed"><a href="#default">' + (entry.icon ? '<i class="' + entry.icon + '"></i>' : '') + '<span class="title">' + entry.title + '</span></a></li>');
			if ($.isArray(entry.items)) {
				if (entry.items.length == 1) {
					// Single menu entry
					hashRoutesMap[entry.items[0].route] = entry.items[0].location;
					$("a", $hli).attr("href", "#" + entry.items[0].route);
					var cls = "routed-" + entry.items[0].route.replaceAll("/", "-");
					$hli.addClass(cls);
				} else {
					// Submenu
					$hli.addClass("treeview");
					$("a", $hli).append('<i class="fa fa-angle-left"></i>');
					var $ul = $('<ul class="treeview-menu">');
					$.each(entry.items, function (idx, item) {
						var $li = '';
						if (item.location) {
							hashRoutesMap[item.route] = item.location;
							var cls = "routed-" + item.route.replaceAll("/", "-");
							$hli.addClass(cls);
							$li = $('<li class="routed ' + cls + '"><a href="#' + item.route + '">' + (entry.icon ? '<i class="' + item.icon + '"></i>' : '') + '<span class="title">' + item.name + '</span></a></li>');
						} else if (item.action) {
							$li = $('<li class="routed"><a href="#default" data-action="' + item.action + '">' + item.name + '</a></li>');
						}
						$ul.append($li);
					});
					$hli.append($ul);
				}
			} else {
				$hli = $('<li class="header">' + (entry.title ? '<span class="title">' + entry.title + '</span>' : '') + '</li>');
				$(entry.wrapper_id).length &&  $hli.append($(entry.wrapper_id));
			}
			$hul.append($hli);
		});
		hashRoutesMap["undefined/page"] = "./fragments/tao-howto.fragment.html";
		
		//trigger a hashchange to start app
		$(window).trigger('hashchange');
	}).fail(function (jqXHR, textStatus, errorThrown) {
		console.log("Could not load dynamic menu!");
	});
	
    // Event handlers for frontend navigation
    $(window).on('hashchange', function(e) {
        // check authtoken
        clearTimeout(tokenTimer);
		var tokenKey = _settings.readCookie("tokenKey");
		if (tokenKey === "" || tokenKey == null) {
			if (!refreshToken()) {
				$("body").empty();
				window.location = 'login.html';
			}
        } else {
            window.tokenKey = tokenKey;

            // check ws connection status
            if(window.wsController){
                window.wsController.checkConnection();
            }
        }
        // timer set as token expiration
        tokenTimer = setTimeout(function verifyTokenExpired(){
            var tokenKey = _settings.readCookie("tokenKey");
                if (tokenKey === "" || tokenKey == null) {
                    $("body").empty();
                    window.location = 'login.html';
                }
        }, 1800*1000);
        navRouter(decodeURI(window.location.hash));
    });
	
	// Remove any remaining elements from previous navigations
	function hashNavCleanup() {
		$(document).trigger("tao:route-changing", [ routeTags.join("/") ]);
		$('.modal-backdrop').remove();
		$('body').removeClass("modal-open");
		
		if (typeof window.navCleanup === "function") {
			window.navCleanup();
			window.navCleanup = void 0;
		}
	}
	
    // Navigation/router function will call appropriate functions.
    function navRouter(hash) {
        var fragmentUrl = "";
        var unsolvedRoute = true;
        if (!hash || (typeof hash === "string" && hash === "") || (typeof hash === "string" && hash === "#")){
            hash = DEFAULT_PAGE_URL;
        }
        if( hash.charAt(0)==="#"){
            routeFilters = {};
            hash = hash.substring(1);
            var hParts = hash.split("::");
            hash = hParts[0];
            if(hParts[1]){
                var hExtra = hParts[1];
                hExtra = hExtra.split("|");
                _.map(hExtra, function(s){ var a=s.split("="); routeFilters[a[0]] = a[1];});
            }
        }
        if(hashRoutesMap[hash] === undefined){
            hash = UNDEFINED_PAGE_URL;
            hash = hash.substring(1);
        }
        routeTags = hash.split('/');
		
        fragmentUrl = hashRoutesMap[hash];
        unsolvedRoute = false;
		
        //set viewMode before loading fragment
        var preferences_viewMode = "grid";
        var username = _settings.readCookie("TaoUserName");
        var pageName = routeTags[0] +'-'+ routeTags[1];
        var currentPref = [];
        var extended;
        if (hash == "admin/dashboard") {
            extended = true;
        }
        $.when(_userPref.getUserPreferences(username, extended))
        .done(function(responseProfile){
            var r = responseProfile.data;
            currentPref = r.preferences;
        });
        $.each(currentPref, function(index,value){
            keyValue = value.key;
            if(keyValue.includes(pageName) === true){
                preferences_viewMode = value.value;
                return false;
            }
        });
        preferences.viewMode = preferences_viewMode;
        
		hashNavCleanup();
		$.ajax({
            cache: false,
            url: fragmentUrl
        })
        .done(function (data) {
			taoDynPageDiv.html(data);
			$(document).trigger("tao:route-loaded", [ hash, fragmentUrl ]);
        })
        .fail(function (jqXHR, textStatus) {
			taoDynPageDiv.html('');
			alert("Could not load page content... Try later.")
        });
        // Sync active menu. Remove active from navigation, and generically mark branch as active.
		$(".routed"                                     , ".sidebar-menu").removeClass("active opened").closest("ul:not(.sidebar-menu)").removeClass("menu-open").css("display", "none" );
		$(".routed-" + routeTags[0] + "-" + routeTags[1], ".sidebar-menu").addClass   ("active opened").closest("ul:not(.sidebar-menu)").addClass   ("menu-open").css("display", "block");
		
        if(routeTags[0] === "account"){
            if(routeTags[1] === "profile"){
                //to do something custom
            }
        }
        if(unsolvedRoute){
            console.log("router could not solve route");
            renderErrorPage();
        }
		
		if (!window.location.hash.includes("llm-chat")) {
			$("a[data-action = 'open-assistant-chat']", ".main-header nav.navbar").removeClass("inactive");
		} else {
			$("a[data-action = 'open-assistant-chat']", ".main-header nav.navbar").addClass("inactive");
			window.closeFloatingAssistant?.();
		}
        taoDynPageDiv.addClass('visible');
    }
	
    // Shows the home in case of error.
    function renderErrorPage(){
        window.location.hash = DEFAULT_PAGE_URL;
    }
});
