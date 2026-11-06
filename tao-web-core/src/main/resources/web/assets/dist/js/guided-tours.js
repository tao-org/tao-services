/**
 * Guided tours for the main user operation flows.
 *
 * The application already ships Bootstrap Tour, so this module only defines
 * flow-specific steps and waits for SPA fragments before showing each step.
 */
(function (window, $) {
	"use strict";
	
	if (!$ || typeof window.Tour === "undefined") {
		return;
	}
	
	var activeTour = null;
	var routeReadyDelay = 350;
	var routes = {
		dashboard   : "admin/dashboard",
		search      : "my/explorer",
		sites       : "my/sites",
		repositories: "my/repositories",
		productSets : "my/queries",
		workflows   : "my/workflows"
	};
	
	window.__guidedTourContext = {
		active: false,
		workflowCreated: false,
		isBack: false
	}
	
	function currentRoute() {
		var hash = decodeURI(window.location.hash || "").replace(/^#/, "");
		return hash.split("::")[0];
	}
	
	function routeSelector(route) {
		return ".sidebar-menu .routed-" + route.replace(/\//g, "-") + " > a[href=\"#" + route + "\"]";
	}
	
	function waitForElement(selector, timeout) {
		var deferred = $.Deferred();
		var started = new Date().getTime();
		
		(function poll() {
			var $element = $(selector).filter(":visible").eq(0);
			if ($element.length) {
				deferred.resolve($element);
			} else if (new Date().getTime() - started >= timeout) {
				deferred.resolve($());
			} else {
				window.setTimeout(poll, 100);
			}
		})();
		
		return deferred.promise();
	}
	
	function ensureRoute(route, prepare) {
		var deferred = $.Deferred();
		
		function finish() {
			window.setTimeout(function () {
				var prepared = prepare ? prepare() : null;
				if (prepared && $.isFunction(prepared.then)) {
					prepared.then(function () { deferred.resolve(); });
				} else {
					deferred.resolve();
				}
			}, routeReadyDelay);
		}
		
		if (currentRoute() === route) {
			finish();
		} else {
			$(document).one("tao:route-loaded.guidedTours", function (e, loadedRoute) {
				if (loadedRoute === route) {
					finish();
				}
			});
			window.location.hash = route;
		}
		
		return deferred.promise();
	}
	
	function prepareExplorer() {
		var $modal = $("#myModalNodes");
		if ($("#queryExtent", $modal).length > 0) {
			return waitForElement("#myModalNodes #queryExtent", 100);
		}
		return waitForElement("#myModalNodes #queryExtent", 1200).then(function ($element) {
			if ($element.length) {
				return $element;
			}
			$('.top-controls [data-action="add"]').eq(0).trigger("click");
			return waitForElement("#myModalNodes #queryExtent", 4000);
		});
	}
	
	function prepareProductSets(tabAction) {
		var $tab = $('[data-toggle="tab"][data-action="' + tabAction + '"]');
		if ($tab.length && $tab.attr("aria-expanded") === "false") {
			$tab.eq(0).trigger("click");
		}
		return waitForElement("#panel-nodes", 2000);
	}
	
	function prepareRepositories(repoName) {
		if (repoName) {
			var repoNameUpper = repoName.trim().toUpperCase();
			var $tab = $('#panel_repo_wrapper a[data-toggle="tab"]').filter(function () {
				var action = $(this).data("action");
				return action && action.startsWith('browse_repository_') &&
					   $(this).text().trim().toUpperCase() === repoNameUpper;
			});
			if ($tab.length) {
				$tab.trigger("click");
			}
		}
		return waitForElement("#panel_repo_wrapper", 3000);
	}
	
	function tourTemplate() {
		return '' +
			'<div class="popover tao-guided-tour" role="tooltip">' +
				'<div class="arrow"></div>' +
				'<h3 class="popover-title"></h3>' +
				'<div class="popover-content"></div>' +
				'<div class="popover-navigation">' +
					'<div class="btn-group">' +
						'<button class="btn btn-sm btn-default" data-role="prev"><i class="fa fa-chevron-left"></i> Back</button>' +
						'<button class="btn btn-sm btn-primary" data-role="next">Next <i class="fa fa-chevron-right"></i></button>' +
					'</div>' +
					'<button class="btn btn-sm btn-default" data-role="end">End</button>' +
				'</div>' +
			'</div>';
	}
	
	function updateNextButton(step, tour) {
		var allowed = step.canProceed(tour);
		$(".popover.tao-guided-tour [data-role='next']")
			.prop("disabled", !allowed);
		if (!allowed) {
			window.__guidedTourContext.isBack = false;
		}
	}
	
	function safeCleanup() {
		$(".tour-backdrop").remove();
		$(".tour-step-backdrop").removeClass("tour-step-backdrop");
		$(".popover.tao-guided-tour").remove();
	}
	
	function makeStep(step) {
		var originalOnShow = step.onShow;
		var originalOnShown = step.onShown;
		var originalOnHide = step.onHide;
		var route = step.route;
		var prepare = step.prepare;
		var normalized = $.extend({
			orphan: true,
			backdrop: true,
			backdropPadding: 6,
			smartPlacement: true
		}, step);
		
		normalized.onShow = function (tour) {
			var ready = route ? ensureRoute(route, prepare) : $.Deferred().resolve().promise();
			if (originalOnShow) {
				return ready.then(function () { return originalOnShow(tour); });
			}
			return ready;
		};
		normalized.onShown = function(tour) {
			window.__guidedTourContext.active = true;
			if (originalOnShown) {
				originalOnShown(tour);
			}
			if (step.modalSelector) {
				var $popover = $('.popover.tao-guided-tour');
				var $backdrop = $('.tour-backdrop, .tour-step-backdrop');
				$(document).off('show.bs.modal.guidedTourModal').on('show.bs.modal.guidedTourModal', step.modalSelector, function() {
					$('.popover.tao-guided-tour').css("z-index", 1030);
					$('.tour-backdrop, .tour-step-backdrop').css("z-index", 1020);
				});
				$(document).off('hidden.bs.modal.guidedTourModal').on('hidden.bs.modal.guidedTourModal', step.modalSelector, function() {
					$('.popover.tao-guided-tour').css("z-index", "");
					$('.tour-backdrop, .tour-step-backdrop').css("z-index", "");
				});
			}
			if (!step.canProceed) {
				return;
			}
			updateNextButton(step, tour);
			// used to listen the workflow add action
			$(document).off("guidedTour:workflow-created").off("guidedTour:wf-editor-opened.guidedTour")
			.on("guidedTour:workflow-created", function() {
				updateNextButton(step, tour);
			}).on("guidedTour:wf-editor-opened.guidedTour", function() {
				updateNextButton(step, tour);
				if (step.autoProceed && step.canProceed(tour) && !window.__guidedTourContext.isBack) {
					tour.next();
				}
			});
		};

		normalized.onHide = function(tour) {
			window.__guidedTourContext.active = false;
			safeCleanup();
			if (originalOnHide) {
				return originalOnHide(tour);
			}
		};
		
		delete normalized.route;
		delete normalized.prepare;
		return normalized;
	}
	
	function makeTour(name, steps) {
		return new window.Tour({
			name: "tao-" + name,
			storage: false,
			backdrop: true,
			keyboard: true,
			autoscroll: true,
			template: tourTemplate(),
			onStart: function () {
				$("body").addClass("guided-tour-running");
			},
			onEnd: function () {
				$("body").removeClass("guided-tour-running");
				activeTour = null;
			},
			steps: $.map(steps, makeStep)
		});
	}
	
	function stopActiveTour() {
		if (activeTour) {
			activeTour.end();
			activeTour = null;
		}
	}
	
	var tourDefinitions = {
		"application-flow": [
			{
				route: routes.dashboard,
				element: ".content-header h1",
				placement: "bottom",
				title: "Application flow",
				content: "Start from the Dashboard to see active processing and recent execution history before moving into data discovery."
			},
			{
				element: routeSelector(routes.search),
				placement: "right",
				title: "Search",
				content: "Use Search when the products you need are not already available in your workspace or dedicated datasets."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#myModalNodes #queryExtent",
				placement: "right",
				title: "Dataset Explorer",
				content: "Choose a datasource or collection, filter by provider, collection, or sensor type, then parameterize the interrogation."
			},
			{
				route: routes.sites,
				element: ".content-header h1",
				placement: "bottom",
				title: "Reusable sites",
				content: "Sites store an area of interest and time interval, so searches can reuse the same geometry and dates."
			},
			{
				route: routes.repositories,
				prepare: prepareRepositories,
				element: "#panel_repo_wrapper",
				placement: "top",
				title: "Repositories",
				content: "Downloaded products, execution outputs, and uploaded files are managed here in an Explorer-like workspace."
			},
			{
				route: routes.productSets,
				prepare: function () { return prepareProductSets("all_productSets"); },
				element: "#panel-nodes .nav-pills",
				placement: "bottom",
				title: "Product sets",
				content: "Create product sets from selected repository products. They become reusable workflow inputs."
			},
			{
				route: routes.workflows,
				element: "#panel-nodes",
				placement: "top",
				title: "Workflows",
				content: "Build or choose a workflow, then run it against a product set. Results are written back to your repository."
			},
			{
				route: routes.dashboard,
				element: "#exec-list-panel",
				placement: "right",
				title: "Monitor execution",
				content: "Running jobs appear here while processing is active. Completed jobs move into execution history."
			}
		],
		"data-discovery": [
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#myModalNodes #queryExtent",
				placement: "right",
				title: "Find EO products",
				content: "The Dataset Explorer groups searchable collections by sensor type and datasource."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#listDatasources",
				placement: "right",
				title: "Collections",
				content: "Pick the provider, sensor, or collection that contains the products you want to interrogate."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#queryExtent .panel-control.filter",
				placement: "left",
				title: "Filters",
				content: "Use the filter control to narrow the datasource list by provider, collection, or category."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#myModalNodes #queryExtentParameters",
				placement: "left",
				title: "Search parameters",
				content: "After selecting a collection, define dates, geometry, credentials when required, limits, and coverage options."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#use_site",
				placement: "left",
				title: "Use a site",
				content: "When a search has AOI parameters, choose a saved site instead of redrawing the same footprint."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#startInterrogation",
				placement: "left",
				title: "Interrogate",
				content: "Start Interrogation returns candidate products. Select the products you need and download them to your workspace."
			}
		],
		"sites": [
			{
				route: routes.sites,
				element: ".content-header h1",
				placement: "bottom",
				title: "Sites",
				content: "A site is a reusable area of interest plus temporal interval for repeated EO searches."
			},
			{
				route: routes.sites,
				element: '#panel-nodes [data-action="plus"]',
				placement: "bottom",
				title: "Create site",
				content: "Add a site when you expect to search the same location and time range more than once."
			},
			{
				route: routes.sites,
				element: "#panel-nodes .search-container",
				placement: "bottom",
				title: "Find saved sites",
				content: "Filter the site list by name, then edit, clone, or delete a site from its card actions."
			},
			{
				route: routes.search,
				prepare: prepareExplorer,
				element: "#use_site",
				placement: "left",
				title: "Reuse in Search",
				content: "Saved sites appear in Search wherever the datasource supports geometry parameters."
			}
		],
		"repositories": [
			{
				route: routes.repositories,
				prepare: prepareRepositories,
				element: ".content-header h1",
				placement: "bottom",
				title: "Repositories",
				content: "Repositories hold EO products, auxiliary files, workflow outputs, and persistent storage targets."
			},
			{
				route: routes.repositories,
				prepare: prepareRepositories,
				element: '#panel_repo_wrapper [data-action="rp-plus"]',
				placement: "bottom",
				title: "Repository list",
				content: "Manage repository entries here, then browse a repository to work with its folders and files."
			},
			{
				route: routes.repositories,
				prepare: prepareRepositories,
				element: '#panel_repo_wrapper [data-action="rp-browse"]:visible:first',
				placement: "top",
				title: "Browse files",
				content: "Open a repository to inspect product folders, execution results, and uploaded files."
			},
			{
				route: routes.repositories,
				prepare: prepareRepositories,
				element: '#panel_repo_wrapper [data-showwhen="browse_repository"]',
				placement: "bottom",
				title: "File operations",
				content: "When browsing, the toolbar supports upload, folders, copy, paste, delete, metadata, map preview, and format actions."
			},
			{
				route: routes.repositories,
				prepare: prepareRepositories,
				element: '#panel_repo_wrapper [data-action="rp-product-set"]',
				placement: "bottom",
				title: "Create product set",
				content: "Select product folders and create a product set. Product sets become workflow inputs."
			},
			{
				route: routes.productSets,
				prepare: function () { return prepareProductSets("all_productSets"); },
				element: "#panel-nodes",
				placement: "top",
				title: "Product set list",
				content: "Created product sets are managed here together with saved queries."
			}
		],
		"workflow-authoring": [
			{
				route: routes.workflows,
				element: ".content-header h1",
				placement: "bottom",
				title: "Workflow authoring",
				content: "Workflows are graphs of processing modules that transform product-set inputs into results."
			},
			{
				route: routes.workflows,
				element: '#panel-nodes [data-action="wf-plus"]',
				placement: "bottom",
				title: "Create workflow",
				content: "Create a workflow record first, then open the graph editor from the workflow card.",
				canProceed: function() {return hasWorkflows() || window.__guidedTourContext.workflowCreated;},
				modalSelector: "#myModalNodes"
			},
			{
				route: routes.workflows,
				element: '#panel-nodes [data-action="wf-edit-workflow"]:visible:first',
				placement: "top",
				title: "Open graph editor",
				content: "Use Edit workflow to open the canvas. If no workflow card is visible yet, create or import one first.",
				canProceed: function() {return $("#wfIframeWrapper").is(":visible")},
				autoProceed: true
			},
			{
				route: routes.workflows,
				element: "#wfIframeWrapper",
				placement: "right",
				title: "Canvas",
				content: "Inside the editor, drag processing modules from the toolbox onto the canvas and connect target ports to source ports.",
				onShow: function() {
					if (!$("#wfIframeWrapper").is(":visible") && window.__guidedTourContext.isBack) {
						$('#panel-nodes button[data-action="wf-edit-workflow"]').eq(0).trigger("click");
						return waitForElement("#wfIframeWrapper", 3000);
					}
				},
				onHide: function() {
					if (!window.__guidedTourContext.active) {
						tao_closeWorkflow();
					}
				}
			},
			{
				route: routes.workflows,
				element: '#panel-nodes [data-action="wf-edit-description"]:visible:first',
				placement: "top",
				title: "Metadata and status",
				content: "Use the description action to edit metadata, tags, status, visibility, and activation before execution."
			}
		],
		"workflow-execution": [
			{
				route: routes.workflows,
				element: '#panel-nodes [data-action="wf-start"]:visible:first',
				placement: "top",
				title: "Start execution",
				content: "Start execution opens the run dialog. Pick a product set if the workflow does not already define one.",
				modalSelector: "#myModalNodes"
			},
			{
				route: routes.workflows,
				element: '#panel-nodes [data-action="wf-history"]:visible:first',
				placement: "top",
				title: "Execution history",
				content: "Inspect previous workflow runs, logs, and execution details from the workflow card.",
				modalSelector: "#myModalNodes"
			},
			{
				route: routes.workflows,
				element: '#panel-nodes [data-action="wf-view-results"]:visible:first',
				placement: "top",
				title: "Execution results",
				content: "Use Execution results to inspect outputs produced by completed workflow runs."
			},
			{
				route: routes.dashboard,
				element: "#exec-list-panel",
				placement: "right",
				title: "Running jobs",
				content: "While a run is active, follow its status and task progress from the Dashboard."
			},
			{
				route: routes.dashboard,
				element: "#exec-history-panel",
				placement: "left",
				title: "Completed runs",
				content: "Completed runs are listed in Executions History. Their generated files are available from the local repository."
			},
			{
				route: routes.repositories,
				prepare: function() { return prepareRepositories("LOCAL");},
				element: '#panel_repo_wrapper [data-action="rp-copy"]',
				placement: "bottom",
				title: "Preserve results",
				content: "To keep outputs, select them in the local repository, copy them, switch to Swift storage, and paste them there."
			},
			{
				route: routes.repositories,
				prepare: function() { return prepareRepositories("SWIFT");},
				element: '#panel_repo_wrapper [data-action="rp-paste"]',
				placement: "bottom",
				title: "Swift repository",
				content: "Swift is the persistent storage location and is also available from the Sandbox environment."
			}
		]
	};
	
	function startTour(name) {
		var steps = tourDefinitions[name];
		if (!steps) {
			return;
		}
		stopActiveTour();
		activeTour = makeTour(name, steps);
		activeTour.init(true);
		activeTour.restart();
	}
	
	function hasWorkflows() {
		return $("core-cell-list[id], core-cell-grid[id]","#paginatedcells .nppaginatedcells-panel-body").length > 0;
	}
	
	$(document)
		.on("click", ".guided-tour-menu [data-tour]", function (e) {
			e.preventDefault();
			startTour($(this).data("tour"));
		})
		.on("tao:route-changing", function () {
			$(".popover.tao-guided-tour").popover("destroy").remove();
			$(".tour-backdrop").remove();
			$(".tour-step-backdrop").removeClass("tour-step-backdrop");
		})
		.on("click", ".popover.tao-guided-tour [data-role='prev']", function(e) {
			window.__guidedTourContext.isBack = true;
		})
		.on("click", ".popover.tao-guided-tour [data-role='next']", function(e) {
			window.__guidedTourContext.isBack = false;
		});
	
	window.taoGuidedTours = {
		start: startTour,
		stop: stopActiveTour
	};
})(window, window.jQuery);
