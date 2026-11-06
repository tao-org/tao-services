/*!
 * npPaginatedCells jQuery plugin
 *
 * it only applies to a single DOM element and options must include end-point url
 * creates paginated cell view, grid or list
 *
 * Copyright  CS ROMANIA, https://www.c-s.ro
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

jQuery.fn.npPaginatedCells = function(options){
    var selectedObjects = this;
    var $selectedEl = $(this[0]);
    var $body = $('.nppaginatedcells-panel-body', $selectedEl);
    var $pagination = $('.nppaginatedcells-panel-pagination', $selectedEl);
    var $cellTemplateGrid = $( ".cell-templates core-cell-grid", $selectedEl);
    var $cellTemplateList = $( ".cell-templates core-cell-list", $selectedEl);
    var $searchBox = $(".search-box",$selectedEl);
    var $like = $(".like",$selectedEl);
    var $clearSearch = $(".clear-search",$selectedEl);
    var $tags = $(".tags",$selectedEl);

    var $valItemFrom = $(".val-itemfrom",$selectedEl);
    var $valItemTo = $(".val-itemto",$selectedEl);
    var $valItemCount = $(".val-itemscount",$selectedEl);
    var $valCountFilters = $(".val-count-filters",$selectedEl);
    var $emptyMsg = $(".nppaginatedcells-empty",$selectedEl);
    var $refreshNotification = $(".box-notification",$selectedEl);
    var $searchForm = $("form.search-container",$selectedEl);
    var $orderEl =  $(".nppaginatedcells-order",$selectedEl);

    var settings = $.extend({
        itemsOnPage: 8,
        url: '',
        order:[],
        filterAttribute: "",
        viewMode: "list",
        loading: false,
        serverSide: false,
        totalUrl: "",
        tagsUrl: "",
        fullUrl: "",
        pageNumberParam: "pageNumber",
        pageSizeParam: "pageSize",
        sortByParam: "sortBy",
        sortDirectionParam: "sortDirection",
        serverSideFilter: false,
        getRequestParams: function() { return {}; },
        animateCells: true,
		doAction: function (actionName) {},
        doDataPreprocessing: function(data){return data;},
        doCustomRender: function($el, data){return;},
		doDataPostprocessing : function(){return;}
    }, options );

    var dataPagination = {
        directPagesNo: 3,
        itemsOnPage: settings.itemsOnPage,
        currentPage: 0
    };
    var getDefaultOrderIndex = function() {
        return Math.max.apply(Math, $.merge([-1], $.map(settings.order, function (o, i) { return o.selected || o.default ? i : null; })));
    };
	var orderIndex = getDefaultOrderIndex();
    var dataFilter = {
        selectedTags: [],
        searchString: ""
    };
    var dataAllTags = [];

    var data = [];
    var dataFull = [];
    var dataTotal = 0;
    var initialized = false;
    var tagClientSideMode = false;

    var addQueryParams = function(url, params) {
        if ($.isEmptyObject(params)) {
            return url;
        }
        var separator = url.indexOf("?") === -1 ? "?" : "&";
        return url + separator + $.param(params, true);
    };

    var getCurrentSort = function() {
        if (settings.order.length === 0) {
            return null;
        }
        if (orderIndex === -1) {
            orderIndex = 0;
        }
        return settings.order[orderIndex];
    };

    var getRequestUrl = function(url, fullList) {
        if (!settings.serverSide) {
            return url;
        }
        var params = {};
        var sort = getCurrentSort();
        if (!fullList) {
            params[settings.pageNumberParam] = dataPagination.currentPage + 1;
            params[settings.pageSizeParam] = dataPagination.itemsOnPage;
        }
        if (sort) {
            params[settings.sortByParam] = sort.objAttributeName;
            params[settings.sortDirectionParam] = sort.direction === "DSC" ? "DESC" : sort.direction;
        }
        if (settings.serverSideFilter) {
            $.extend(params, settings.getRequestParams({
                fullList: fullList,
                selectedTags: dataFilter.selectedTags,
                searchString: $searchBox.val()
            }));
        }
        return addQueryParams(url, params);
    };

    var getTotalUrl = function() {
        if (!settings.serverSideFilter) {
            return settings.totalUrl;
        }
        return addQueryParams(settings.totalUrl, settings.getRequestParams({
            fullList: false,
            selectedTags: dataFilter.selectedTags,
            searchString: $searchBox.val()
        }));
    };

    var getRequestUrls = function(fullList) {
        if (!fullList || settings.fullUrl === "") {
            return settings.url;
        }
        return settings.fullUrl instanceof Array ? settings.fullUrl : [settings.fullUrl];
    };
	
    var getFromEndpoint = function(options){
        options = options || {};
        var fullList = settings.serverSide && options.fullList === true;
        tagClientSideMode = fullList;
        var requestUrls = getRequestUrls(fullList);
        var req = [];
        for (var j = 0; j < requestUrls.length; j++){
            var getDataAjax = $.ajax({
                cache: false,
                url: getRequestUrl(requestUrls[j], fullList),
                dataType : 'json',
                type: 'GET',
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "X-Auth-Token": window.tokenKey
                }
            });
            req.push(getDataAjax);
        }
        if (settings.serverSide && !fullList && settings.totalUrl !== "") {
            req.push($.ajax({
                cache: false,
                url: getTotalUrl(),
                dataType : 'json',
                type: 'GET',
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "X-Auth-Token": window.tokenKey
                }
            }));
        }
        if (settings.serverSide && settings.tagsUrl !== "") {
            req.push($.ajax({
                cache: false,
                url: settings.tagsUrl,
                dataType : 'json',
                type: 'GET',
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "X-Auth-Token": window.tokenKey
                }
            }));
        }
        var showLoading = options.loading !== false && settings.loading && (!settings.serverSide || !initialized);
		showLoading && contentLoading("show");
        $.when.apply($, req).done(function () {
            var doneArgs = arguments;
            var retData = [];
            var sets = {};
            var dataRequests = requestUrls.length;
            var totalRequestIndex = settings.serverSide && !fullList && settings.totalUrl !== "" ? dataRequests : -1;
            var tagsRequestIndex = settings.serverSide && settings.tagsUrl !== "" ? dataRequests + (totalRequestIndex !== -1 ? 1 : 0) : -1;
            var responseAt = function(index) {
                return req.length === 1 ? doneArgs[0] : doneArgs[index][0];
            };
            if (dataRequests == 1) {
                retData = responseAt(0).data;
            } else if (req.length > 1) {
                var response = [];
				for (var i = 0; i < dataRequests; i++) {
                    response = $.merge(response, responseAt(i).data);
					sets[requestUrls[i]] = responseAt(i).data;
                }
                retData = response;
            }
            if (totalRequestIndex !== -1) {
                dataTotal = parseInt(responseAt(totalRequestIndex).data) || 0;
            } else if (fullList) {
                dataTotal = retData.length;
            }
            retData = settings.doDataPreprocessing(retData, sets);
			
			var currentPage = $("section.content-header").find("li.active")[0].textContent.toLowerCase();
			if (!settings.serverSide && currentPage.indexOf("workflows") >= 0) {
				var action = $("a[data-toggle='tab'][aria-expanded='true']", ".nav-tabs-custom").attr("data-action");
				var active = (action === "system_nodes" || action === "active_nodes");
				var system = (action === "system_nodes");
				
				dataFull = retData.filter(function(item) {
					if (system && item.visibility.toLowerCase() === "system") {
						return item;
					} else if (!system && item.visibility.toLowerCase() !== "system") {
						if (typeof item.active !== "undefined" && item.active === active) {
							return item;
						} else if (typeof item.active === "undefined") {
							return item;
						}
					}
				});
			} else {
				dataFull = retData;
			}

            if (settings.url[0] === baseRestApiURL + "orchestrator/queued/jobs") {
                $.each(dataFull, function(index, job) {
                    job.tags = [];
                    job.tags[0] = job.userId;
                });
            }

            if (settings.url[0] ===  baseRestApiURL + "admin/users?status=ACTIVE") {
                if (typeof dataFull[0].online !== 'undefined') {
                    $.each(dataFull, function (index, user) {
                        user.tags = [];
                        user.tags[0] = user.online ? "ONLINE" : "OFFLINE";
                    });
                }
            }

            if (settings.url[0] === baseRestApiURL + "repository/") {
                $.each(dataFull, function(index, job) {
                    job.tags = [];
                    job.tags[0] = job.type;
                });
            }

            if (settings.url[0] === baseRestApiURL + "component/naming/") {
               settings.filterAttribute = "sensor";
               settings.order = [
                    { "label": "Name ascending" , "objAttributeName": "sensor", "direction": "ASC" },
                    { "label": "Name descending", "objAttributeName": "sensor", "direction": "DSC" }
                ]
            }
			
            if ((orderIndex === -1) && (settings.order.length>0)) {
                orderIndex = 0;
            }
            if (orderIndex !== -1) {
                repaintOrder();
                changeOrder();
            }
            if (tagsRequestIndex !== -1) {
                dataAllTags = responseAt(tagsRequestIndex).data || [];
            } else {
                dataAllTags = [];
                for (var j = 0; j < dataFull.length; j++) {
                    if (dataFull[j].tags != null) {
                        dataAllTags = _.union(dataAllTags, dataFull[j].tags);
                    }
                }
            }
            dataAllTags.sort();
            repaintTags();
            repaintSearchBox();
            repaint(options.notify !== false, showLoading);
            initialized = true;
        }).fail(function(jqXHR, textStatus, errorThrown) {
			showLoading && contentLoading("hide");
			chkXHR(jqXHR.status);
        });
    };

    var clearCells = function() {
        $body.empty();
    };
    var isServerPageMode = function() {
        return settings.serverSide && !tagClientSideMode;
    };
    var renderCells = function() {
        var visibleRange = {
            first: dataPagination.currentPage * dataPagination.itemsOnPage,
            last: dataPagination.currentPage* dataPagination.itemsOnPage+dataPagination.itemsOnPage
        };
        var serverPageMode = isServerPageMode();
        var total = serverPageMode ? dataTotal : data.length;
        if (serverPageMode) {
            $valItemFrom.html(data.length > 0 ? visibleRange.first + 1 : 0);
            $valItemTo.html(Math.min(visibleRange.first + data.length, total));
            $valItemCount.html(total);
        } else {
            $valItemFrom.html( ((visibleRange.first+1 < data.length))? visibleRange.first+1 : data.length);
            $valItemTo.html( ((visibleRange.last < data.length))? visibleRange.last : data.length);
            $valItemCount.html(data.length);
        }
		
        $.each(data, function( index, value ) {
            if(serverPageMode || ((index >= visibleRange.first) && (index < visibleRange.last))){
                drawCell(index, value);
            }
        });
		settings.doDataPostprocessing();
    };
    var drawCell = function(index, elX) {
        var elID = guid();
        var $elClone;
        var tags = "";
        if(settings.viewMode ==="grid"){
            $elClone = $cellTemplateGrid.clone();
        }else{
            $elClone = $cellTemplateList.clone();
        }
		
		// for system workflow keep only clone and execute buttons
		if (routeTags[1] === "workflows" && $("a[data-action='system_nodes']").length && $("a[data-action='system_nodes']").parent().hasClass("active")) {
			$("button",$elClone).not("[data-action='wf-clone'], [data-action='wf-start']").remove();
		}
        //iterate values and populate all inner html of val-propname with value of propname.
        //call custom cell alterations
        for (var property in elX) {
            if (elX.hasOwnProperty(property)) {
                $(".val-"+property, $elClone).html(elX[property]);
                if(property === 'description'){
                    $(".val-"+property, $elClone).prop('title',''+elX[property]+'');
                }
				if (property === "online") {
					$(".fa.fa-circle", $elClone).addClass(elX[property] ? "online": "offline");
				}
            }
        }
        if(elX.tags != null){
            for (var j = 0; j < elX.tags.length; j++){
                if(elX.tags[j] !== ""){
                    tags+="<span>"+elX.tags[j]+"</span>";
                }
            }
        }
        $(".box-top-tags", $elClone).html(tags);
        $elClone.addClass("node-root");
        if (!settings.serverSide && settings.animateCells) {
            $elClone.hide();
        }
		$elClone.prop("id", "node-"+elID ).appendTo( $body ).data("dna", elX);
		var currentPage = $("section.content-header").find("li.active")[0].textContent.toLowerCase();//workaround for inherited active property
        //render custom
        settings.doCustomRender($elClone, elX);
        if (!settings.serverSide && settings.animateCells) {
            $elClone.fadeIn('slow');
        } else {
            $elClone.show();
        }
    };
	//show/hide/append ui elements based on dataset and configuration
    var repaintSearchBox = function(){
        if((settings.filterAttribute === undefined) || (settings.filterAttribute === '')){
            $searchForm.hide();
        }else{
            $searchForm.show();
        }
    };

    var repaintTags = function(){
        $tags.empty();
        if(dataAllTags.length === 0 ){
            $tags.append('<p>No tags to show.</p>');
        }
        for (var j = 0; j < dataAllTags.length; j++){
            $tags.append('<span class="'+ (( $.inArray(dataAllTags[j], dataFilter.selectedTags)>=0) ? "selected" : "") +'" data-tag="'+dataAllTags[j]+'">'+dataAllTags[j]+'<i class="selected-hidden fa fa-square-o fa-fw" aria-hidden="true"></i><i class="selected-shown fa fa-check-square-o fa-fw" aria-hidden="true"></i></span>');
        }
    };
    var repaintOrder = function(){
        html = '<label for="npOrderIndex" class="master-label">Sort by:&nbsp;</label><select id="npOrderIndex" name="npOrderIndex" class="field-master-tool">';
        for (var j = 0; j < settings.order.length; j++){
            html += '<option value="'+j+'"'+((orderIndex === j) ?' selected="selected"':'')+'>'+settings.order[j]["label"]+'</option>';
        }
        html += "</select>";
        $orderEl.empty().append(html).show();
    };
    var repaintPagination = function(){
        var total = isServerPageMode() ? dataTotal : data.length;
        var tP = Math.ceil(parseFloat(total/dataPagination.itemsOnPage) || 0);
        if((dataPagination.currentPage >= tP) || isNaN(dataPagination.currentPage) || (dataPagination.currentPage<0)){
            dataPagination.currentPage = 0;
        }
        var fP = dataPagination.currentPage - dataPagination.directPagesNo;
        var lP = dataPagination.currentPage + dataPagination.directPagesNo;
        $pagination.empty();

        if (tP>1){
            //append previous
            if(dataPagination.currentPage>0){
                $pagination.append('<li><a href="#" data-page="'+(dataPagination.currentPage-1)+'">«</a></li>');
            }
            if(fP >= 0){
                $pagination.append('<li class="disabled"><span>...</span></li>');
            }else{
                fP = 0;
            }
            //append pages handlers
            for(i = fP; ((i <= lP) && (i < tP)); i++) {
                $pagination.append('<li class="'+((i === dataPagination.currentPage) ? 'active' : '')+'"><a href="#" data-page="'+i+'">'+(i+1)+'</a></li>');
            }
            if(lP <= tP){
                $pagination.append('<li class="disabled"><span>...</span></li>');
            }
            //append next
            if((dataPagination.currentPage+1)<tP) {
                $pagination.append('<li><a href="#" data-page="'+(dataPagination.currentPage+1)+'">»</a></li>');
            }
        }
        $pagination.toggle(tP > 1);
    };

    var refreshSearchUI = function(){
        var q = $searchBox.val();
        if(q === ""){
            $like.hide("fast");
        } else{
            $like.show("fast");
            $(".val-q", $like).html(q);
        }
    };

    var readSelectedTags = function() {
        dataFilter.selectedTags = [];
        $("span.selected",$tags).each(function() {
            dataFilter.selectedTags.push($(this).data('tag'));
        });
        return dataFilter.selectedTags;
    };
    var filterData = function(){
        dataFilter.searchString = $searchBox.val();
        data = [];
        readSelectedTags();
        if (settings.serverSideFilter && isServerPageMode()) {
            data = dataFull.slice();
            refreshEmptyWarning();
            return;
        }
        //filter by tags
        for (var j = 0; j < dataFull.length; j++){
            var tagsIntersecttion  = -1;
            if(dataFull[j].tags != null){
                tagsIntersecttion = arrayIntersection(dataFilter.selectedTags,dataFull[j].tags);
            }
            if ((dataFilter.selectedTags.length === 0) || (tagsIntersecttion.length>0)){
                data.push(dataFull[j]);
            }
        }
        //filter by text
        if (dataFilter.searchString !== ''){
            var re = new RegExp(dataFilter.searchString, 'i');
            data = $.map(data,function(val,key){
                //if( val.label.match(re) ) return val;
                if( val[settings.filterAttribute].match(re) ) return val;
            });
        }
        //show empty results warning
        refreshEmptyWarning();
    };
    var refreshEmptyWarning = function(){
        //show empty results warning
        if(dataFull.length>0){
            $emptyMsg.find(".results-empty").hide();
            $emptyMsg.find(".filter-results-empty").show();
        }else{
            $emptyMsg.find(".results-empty").show();
            $emptyMsg.find(".filter-results-empty").hide();
        }
        if(data.length<1){
            $emptyMsg.show();
        } else{
            $emptyMsg.hide();
        }
    };
    var repaint = function(showNotification, hideLoading){
        if (showNotification !== false) {
            ui_showRefreshNotification();
        }
        filterData();
        clearCells();
        repaintPagination();
        renderCells();
        if (hideLoading !== false) {
		    contentLoading("hide");
        }
    };
    var ui_showRefreshNotification = function(){
        $refreshNotification.addClass("show");
        setTimeout(function(){
            $refreshNotification.removeClass("show");
        }, 3000);
    };

    var changeOrder = function(){
        var objAttribute = settings.order[orderIndex]["objAttributeName"];
        var direction = settings.order[orderIndex]["direction"];
        if((direction === undefined) && (direction !== "ASC") && (direction !== "DSC")){
            direction = "ASC";
        }
        function compare(a,b) {
            if(a[objAttribute] && b[objAttribute]){
                return a[objAttribute].localeCompare(b[objAttribute]);
            }else{
                return 0;
            }
        }
        dataFull.sort(compare);
        if(direction === "DSC"){
            dataFull.reverse();
        }
    };

    //add events handlers
    $searchForm.on("submit",function(e){
        e.preventDefault(e);
    });

    $orderEl
        .on("change", 'select' ,function(){
            orderIndex = parseInt(this.value);
            dataPagination.currentPage = 0;
           // getFromEndpoint();
        repaintOrder();
           if (isServerPageMode()) {
               getFromEndpoint({ loading: false, notify: false });
           } else {
		       changeOrder();
		       repaint();
           }
        });

    $selectedEl
        .on("click", '.btn-master-tool', function(event){
            event.preventDefault();
            var action = $(this).data("action");
            if(action === "op-filter"){
                if( $tags.is(":visible") ){
                    $tags.hide("fast");
                }else{
                    $tags.show("fast");
                }
            }
            if(action === "view-list"){
        
                var username = _settings.readCookie("TaoUserName");
                var id = taoUserProfile.id;
                var pageName = routeTags[0] +'-'+ routeTags[1];
                $.when(_userPref.getUserPreferences(id))
                .done(function(responseProfile){
                    var r = responseProfile.data;
                    currentPref = r.preferences;
                });
                var changed = false;
                $.each(currentPref, function(index,value){
                    keyValue = value.key;
                    if(keyValue.includes(pageName) === true){
                        if(value.value !== "list"){
                            //update the value with "list" 
                            value.value = "list";
                            changed = true;

                        } else if(value.value === "list"){
                            changed = true;
                        } 
                    }
                });
                if(!changed){
                    //add to preferences the new page with the value list
                    currentPref.push({"key": "view_mode_"+pageName, "value": "list"});
                }
                var updatePref = JSON.stringify(currentPref);
                _userPref.updateUserPreferences(updatePref);
            
                preferences.viewMode = "list";
                settings.viewMode = "list";
                repaint();
            }
            if(action === "view-table"){
                
                var username = _settings.readCookie("TaoUserName");
                var id = taoUserProfile.id;
                var pageName = routeTags[0] +'-'+ routeTags[1];
                $.when(_userPref.getUserPreferences(id))
                .done(function(responseProfile){
                    var r = responseProfile.data;
                    currentPref = r.preferences;
                });
                var changed = false;
                $.each(currentPref, function(index,value){
                    keyValue = value.key;
                    if(keyValue.includes(pageName) === true){
                        if(value.value !== "grid"){
                            //update the value with "list" 
                            value.value = "grid";
                            changed = true;
                            
                        } else if(value.value === "grid"){
                            changed = true;
                        } 
                    }
                });
                if(!changed){
                    //add to preferences the new page with the value grid
                    currentPref.push({"key": "view_mode_"+pageName, "value": "grid"});
                }
                var updatePref = JSON.stringify(currentPref);
                _userPref.updateUserPreferences(updatePref);
                
                preferences.viewMode = "grid";
                settings.viewMode = "grid";
                repaint();
            }
            if(action === "refresh-list"){
                getFromEndpoint({ fullList: tagClientSideMode });
            }
            settings.doAction(action);
        })
        .on("click", ".btn-nodeop", function(event){
            event.preventDefault();
            var action = $(this).data("action");
            var $root = $(this).closest(".node-root");
            var dna = $root.data("dna");
            var args = {"dna":dna};
            settings.doAction(action,args);
        })
        .on("click", ".move-button", function(event){
            event.preventDefault();
            var action = $(this).data("action");
            var $root = $(this).closest(".node-root");
            var dna = $root.data("dna");
            var jobID = parseInt($(this).parents(".nodes-item-core-box").children(".queued-jobs-row").find(".val-id").text());
            var args = {"dna":dna, "jobId":jobID};
            settings.doAction(action,args);
        });
    $pagination
        .on('click', "li a", function(e){
            e.preventDefault();
            dataPagination.currentPage = $(this).data("page");
            if (isServerPageMode()) {
                getFromEndpoint({ loading: false, notify: false });
            } else {
                repaint();
            }
        });
    $searchBox
        .on('keyup', function(e){
            refreshSearchUI();
            dataPagination.currentPage = 0;
            if (settings.serverSideFilter && settings.serverSide) {
                getFromEndpoint({ loading: false, notify: false });
            } else {
                repaint();
            }
        });
    $clearSearch
        .on("click", function(e){
            $searchBox.val("");
            refreshSearchUI();
            dataPagination.currentPage = 0;
            if (settings.serverSideFilter && settings.serverSide) {
                getFromEndpoint({ loading: false, notify: false });
            } else {
                repaint();
            }
        });
    $tags
        .on('click', 'span', function (e) {
            $(this).toggleClass("selected");
            readSelectedTags();
            dataPagination.currentPage = 0;
            var cST = $('span.selected',$tags).length;
            $valCountFilters.html(cST);
            if(cST>0){
                $valCountFilters.show("fast");
            }else{
                $valCountFilters.hide("fast");
            }
            if (settings.serverSideFilter && settings.serverSide) {
                getFromEndpoint({ loading: false, notify: false });
            } else if (settings.serverSide) {
                getFromEndpoint({ fullList: cST > 0, loading: false, notify: false });
            } else {
                repaint();
            }
        });
    //call load data
    getFromEndpoint();
	
    return {
        sayChainability: function () {
            $(selectedObjects).each(function () {});
            return selectedObjects;
        },
        getDebug: function () {
            console.log("<<<< all tags >>>>");
            console.log(dataAllTags);
            console.log("<<<< data >>>>");
            console.log(dataFilter);
            console.log("<<<< data >>>>");
            console.log(data);
            console.log("<<<< data full >>>>");
            console.log(dataFull);
            return selectedObjects;
        },
        refreshData: function (url, options) {
            var requestOptions = {};
            if (url && (url instanceof Array)) {
                console.log("change url");
                settings.url = url;
            }
            if (options) {
                var refreshOptions = $.extend({}, options);
                requestOptions.loading = refreshOptions.loading;
                requestOptions.notify = refreshOptions.notify;
                delete refreshOptions.url;
                delete refreshOptions.loading;
                delete refreshOptions.notify;
                $.extend(settings, refreshOptions);
                orderIndex = getDefaultOrderIndex();
                dataPagination.currentPage = 0;
            }
            getFromEndpoint(requestOptions);
            return selectedObjects;
        }
    };
};
