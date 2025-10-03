/*!
 * npNotificationsPanels jQuery plugin
 *
 * it only applies to a single DOM element and options must include end-point url
 * creates job execution lists and maintain pagination and data refresh
 *
 * Copyright CS ROMANIA, http://c-s.ro
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
 */

jQuery.fn.npNotificationsPanels = function (options) {
	var settings = $.extend({
		msgEmpty: "Empty dataset",
		url: ''
	}, options);
	
	var $panelWrapper = $(this)[0];
	var $notification = $(".box-notification", $panelWrapper);
	var $panel        = $(".panel-list"      , $panelWrapper);
	var $panelEmpty   = $(".panel-list-empty", $panelWrapper);
	$panel.hide();
	$panelEmpty.show();
	
	var data = [];
	var currentPage = 0;
	var pages = 0;
	
	var topics = {
		info     : { topic: "info"                    , title: "Info messages"   , badge: "badge label-success", icon: "fa fa-flag-o"              , color: "var(--status-success)", showFirst: true  },
		execution: { topic: "execution.status.changed", title: "Execution events", badge: "badge label-warning", icon: "fa fa-envelope-o"          , color: "var(--status-warning)", showFirst: false },
		error    : { topic: "error"                   , title: "Error messages"  , badge: "badge label-danger" , icon: "fa fa-exclamation-triangle", color: "var(--status-danger)" , showFirst: false }
	};
	
	var setNewData = function (d) {
		data = d;
		renderExecHistoryCurrentPage(data);
	};
	var getData = function (page) {
		$.ajax({
			cache    : false,
			url      : settings.url + (page + 1),
			dataType : "json",
			type     : "GET",
			headers  : {
				"Accept"      : "application/json",
				"Content-Type": "application/json",
				"X-Auth-Token": window.tokenKey,
				"user"        : settings.headerUser
			},
			success: function (response) {
				setNewData(response.data);
			}
		});
	};
	
	var renderExecHistoryCurrentPage = function (jobs) {
		$panel.empty();
		if (jobs) {
			var count = jobs.length;
			$.each(jobs, function (index, value) {
				var title   = (value?.topic || "info").toUpperCase().trim();
				var key     = "";
				var payload = "";
				try {
					payload = JSON.parse(value.data).Message;
				} catch (e) {
					payload = value.data.substring(12, 200);
				}
				switch (value.topic) {
					case "warn"                    :
					case "execution.status.changed": key = "execution"; break;
					case "error"                   : key = "error"    ; break;
					case "info"                    :
					case "authentication"          :
					case "transfer"                :
					case "product.progress"        :
					default                        : key = "info"     ; break;
				}
				var fromNow    = moment(value.timestamp).fromNow();
				var formatDate = moment(value.timestamp).format("YY-MM-DD HH:mm:ss");
				var $li = $('<li>' +
								'<div class="media payload">' +
									'<div class="media-left"><i class="' + topics[key].icon + ' dbl-size" style="color:#fff;background-color:' + topics[key].color + ';"></i></div>' +
									'<div class="media-body">' +
										'<p class="title bold">' + title + '</p>' +
										'<p class="text">' + payload + '</p>' +
										'<small class="pull-left"><i class="fa fa-clock-o"></i>&nbsp;&nbsp;' + formatDate + '</small>' +
										'<small class="pull-right">' + fromNow + '</small>' +
									'</div>' +
								'</div>' +
							'</li>');
				$panel.append($li);
			});
			
			// Show panel
			if (count > 0) {
				$panel.show();
				$panelEmpty.hide();
			} else {
				if (currentPage > 0) {
					$("p", $panelEmpty).html(settings.msgEnd);
				} else {
					$("p", $panelEmpty).html(settings.msgEmpty);
				}
				$panel.hide();
				$panelEmpty.show();
			}
			
			// Syncronize pagination
			$(".val-current-page",$panelWrapper).html(currentPage+1);
			if (count > 0) {
				$(".next", $panelWrapper).removeClass("disabled");
			} else {
				$(".next",$panelWrapper).addClass("disabled");
			}
			if (currentPage > 0) {
				$(".prev", $panelWrapper).removeClass("disabled");
			} else {
				$(".prev", $panelWrapper).addClass("disabled");
			}
		}
	};

	var ui_clearNotifications = function () {
		$.ajax({
			cache  : false,
			url    : settings.url + "clear",
			type   : "POST",
			headers: {
				"Accept"      : "application/json",
				"Content-Type": "application/json",
				"X-Auth-Token": window.tokenKey
			},
			success: function (response) {
				ui_showExecNotification();
				getData(currentPage);
				$(document).trigger("refresh:toolbar");
			}
		});
	}
	
	var ui_showExecNotification = function () {
		$notification.addClass("show");
		setTimeout(function () {
			$notification.removeClass("show");
		}, 3000);
	};
	
	$($panelWrapper)
	.on("click"," .pagination a", function (e) {
		e.preventDefault();
		if($(this).parent().hasClass("disabled")) {
			return;
		}
		var action = $(this).data("action");
		switch (action) {
			case "go-next"   : currentPage ++ ; ui_showExecNotification(); getData(currentPage); break;
			case "go-prev"   : currentPage -- ; ui_showExecNotification(); getData(currentPage); break;
			case "go-refresh": currentPage = 0; ui_showExecNotification(); getData(currentPage); break;
			case "go-clear"  : currentPage = 0; ui_clearNotifications(); break;
		}
	})
	.on("panel:refresh", function (e) {
		currentPage = 0;
		getData(currentPage);
	});
	
	// Initializing
	currentPage = 0;
	getData(currentPage);
	return this;
	};
