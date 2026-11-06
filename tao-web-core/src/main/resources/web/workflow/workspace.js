//create case insensitive search jQuery selector:
jQuery.expr[':'].ci_search = function(a, i, m) {
    return jQuery(a).text().toUpperCase().indexOf(m[3].toUpperCase()) >= 0;
};

//geometry function for svg
function polarToCartesian(centerX, centerY, radius, angleInDegrees) {
    var angleInRadians = (angleInDegrees - 90) * Math.PI / 180.0;
    return {
        x: centerX + (radius * Math.cos(angleInRadians)),
        y: centerY + (radius * Math.sin(angleInRadians))
    };
}

// get enums based on enum name
function getTaoEnums(name) {
	var retVal = [];
	$.each(wfTools.taoEnums, function (index, enm) {
		if (index.toLowerCase().indexOf(name.toLowerCase()) >= 0) {
			$.each(enm, function (idx, value) {
				retVal.push(value);
			});
		}
	});
	return retVal;
}

function describeArc(x, y, radius, startAngle, endAngle){
    var start = polarToCartesian(x, y, radius, endAngle);
    var end = polarToCartesian(x, y, radius, startAngle);
    var arcSweep = endAngle - startAngle <= 180 ? "0" : "1";
    return d = [
        "M", start.x, start.y,
        "A", radius, radius, 0, arcSweep, 0, end.x, end.y,
        //"L", x, y, "Z"
    ].join(" ");
}



    var preferences = {};
	var componentCategories;

    var toolboxTopHeader = {
        elTBH: $("#toolboxTopHeader"),
        init: function () {
            var a = this;
            a.elTBH.on("click", ".toolbox-top-header-close", function(e){
                e.stopPropagation();
                a.close();
            });
            //intercept and cancell scroll events in order to stop propagate to parents
            $(a.elTBH).on( 'mousewheel DOMMouseScroll', function (e) {
                e.preventDefault();
            });
            return this;
        },
        populate: function(header){
            $(".val-title",this.elTBH).html(header.name);
            $(".val-meta",this.elTBH).html(header.status +",&nbsp;"+ header.visibility);
            return this;
        },
        open: function () {
            this.elTBH.removeClass("outside");
            return this;
        },
        close: function () {
            this.elTBH.addClass("outside");
            return this;
        }
    };
    toolboxTopHeader.init();

    var toolboxComponents = {
    	elTBS: $("#toolboxComponents"),
        init: function () {
            var a = this;
            componentCategories = getTaoEnums("ComponentCategory");
			$.each(componentCategories, function(index, category) {
				var $componentHeader = $("h3","#componentTemplate").clone().append(category.value).css('textTransform', 'capitalize');
				var $componentContent = $("#componentTemplateBody","#componentTemplate").clone().addClass("thin-scrollbar onhover").attr("id", category.key);
				a.elTBS.append($componentHeader).append($componentContent);
			});
			
			//init accordion
            a.elTBS.accordion({
                heightStyle: "fill",
                collapsible: true,
                active: false,
                animate: 500
            });
            
			//add resize handler
            $(window).resize(function () {
                a.elTBS.accordion("refresh");
            });
			
            $( ".toolbox-search .fa-search", a.elTBS).show();
            $( ".toolbox-search .fa-times", a.elTBS).hide().on("click",function(){
                $(this).parent().find("input").val("").trigger("keyup");
            });
			
            a.elTBS.on('keyup','.toolbox-search>input', function(e){
                var q = $(this).val();
                var $s = $(this).parent();
                var $l = $s.next();
                if(q && q.length>0){
                    $( ".fa-search", $s).hide();
                    $( ".fa-times", $s).show();
                    $( "sortable_item", $l).hide();
                    $( ".item-label:ci_search('"+q+"')", $l).closest("sortable_item").show();
                }else{
                    $( ".fa-search", $s).show();
                    $( ".fa-times", $s).hide();
                    $( "sortable_item", $l).show();
                }
            });
        },
		refresh: function() {
			this.elTBS.trigger("ready");
			this.elTBS.accordion("refresh");
		}
    };

	function setWFConnectorsToStateMachine(){
		window.preferences.workFlowConnectors = "S";
		_settings.createCookie("preferences_workFlowConnectors","S");
	}
	function setWFConnectorsToFlowchart(){
		window.preferences.workFlowConnectors = "F";
		_settings.createCookie("preferences_workFlowConnectors","F");
	}

    function wf_renderComponentsToolBox(){
        var $elDatasurcesList = $("#datasourceslist");
        var $elUDatasurcesList = $("#udatasourceslist");		
        $elDatasurcesList.empty();
        $elUDatasurcesList.empty();
		$.each(componentCategories, function (index, tag) {
			var $elComponentsList = $("#moduleslist", "#" + tag.key);
			
			$elComponentsList.empty();
				
			$.each(wfTools.toolboxnodes.pc, function(i, item) {
				var prettyLabel = (item.label || "").replace(/([A-Z])([a-z])/g, ' $1$2').trim();
				if ($elComponentsList.length !== 0 && (jQuery.inArray(tag.key, item.dna.tags) !== -1 || (typeof item.dna.category !== "undefined" && tag.key === item.dna.category))) {
					item.image = "./media/"+item.dna.containerId+".png";
					if(item.dna.category === "CONDITION"){
						item.image = "./media/cond-comp.png";
                        var html ='<sortable_item class="item selected" id="'+item.id+'" data-componentid="'+item.dna.id+'" data-componenttype="cc" data-toolboxid="'+item.id+'">\n' +
                        '\t\t\t\t\t<div class="item-info">\n' +
                        '\t\t\t\t\t  <div class="item-label text-left"> \n' +
                        '\t\t\t\t\t\t<img src="'+item.image+'">\n' +'<span>'+prettyLabel+'</span>'+
                        '\t\t\t\t\t</div>\n' +
                        '\t\t\t\t\t</div>\n' +
                        '\t\t\t\t</sortable_item>';
                    $elComponentsList.append(html);
                    }else{
                        var html ='<sortable_item class="item selected" id="'+item.id+'" data-componentid="'+item.dna.id+'" data-componenttype="pc" data-toolboxid="'+item.id+'">\n' +
                            '\t\t\t\t\t<div class="item-info">\n' +
                            '\t\t\t\t\t  <div class="item-label text-left"> \n' +
                            '\t\t\t\t\t\t<img src="'+item.image+'">\n' +'<span>'+prettyLabel+'</span>'+
                            '\t\t\t\t\t</div>\n' +
                            '\t\t\t\t\t</div>\n' +
                            '\t\t\t\t</sortable_item>';
                        $elComponentsList.append(html);
                    }
				}
				/*else if (typeof item.dna.category === "undefined" || jQuery.inArray(item.dna.category.toLowerCase(),componentCategories) < 0) {
					item.image = "./media/"+item.dna.containerId+".png";
					var html ='<sortable_item class="item selected" id="'+item.id+'" data-componentid="'+item.dna.id+'" data-componenttype="pc" data-toolboxid="'+item.id+'">\n' +
						'\t\t\t\t\t<div class="item-info">\n' +
						'\t\t\t\t\t  <div class="item-label text-left"> \n' +
						'\t\t\t\t\t\t<img src="'+item.image+'">\n' +'<span>'+prettyLabel+'</span>'+
						'\t\t\t\t\t</div>\n' +
						'\t\t\t\t\t</div>\n' +
						'\t\t\t\t</sortable_item>';
					$("#moduleslist", "#other").append(html); //default tag for all components marked as Other or have no tags from the predefined toolboxTagsList or no category defined
				}*/
			});
			if ($elComponentsList.children().length > 0) {
				$("h3[aria-controls='" + tag.key + "']").removeClass("hidden");
			}
		});
        $.each(wfTools.toolboxnodes.uds, function(i, item) {
            var prettyLabel = (item.label || "").replace(/([A-Z])([a-z])/g, ' $1$2').trim();
			item.image = "./media/module-ds.png";
            var html ='<sortable_item class="item selected" id = "'+item.id+'" data-componentid="'+item.dna.id+'" data-componenttype="uds" data-toolboxid="'+item.id+'">\n' +
                '\t\t\t\t\t<div class="item-info">\n' +
                '\t\t\t\t\t  <div class="item-label text-left"> \n' +
                '\t\t\t\t\t\t<img src="'+item.image+'">\n' +'<span>'+prettyLabel+'</span>'+
                '\t\t\t\t\t</div>\n' +
                '\t\t\t\t\t</div>\n' +
                '\t\t\t\t</sortable_item>';
            $elUDatasurcesList.append(html);
        });
        $.each(wfTools.toolboxnodes.uq, function(i, item) {
            var prettyLabel = (item.label || "").replace(/([A-Z])([a-z])/g, ' $1$2').trim();
			item.image = "./media/module-ds.png";
            var html ='<sortable_item class="item selected" id = "'+item.id+'" data-componentid="'+item.dna.componentId+'" data-componenttype="uq" data-toolboxid="'+item.id+'">\n' +
                '\t\t\t\t\t<div class="item-info">\n' +
                '\t\t\t\t\t  <div class="item-label text-left"> \n' +
                '\t\t\t\t\t\t<img src="'+item.image+'">\n' +'<span>'+prettyLabel+'</span>'+
                '\t\t\t\t\t</div>\n' +
                '\t\t\t\t\t</div>\n' +
                '\t\t\t\t</sortable_item>';
            $elUDatasurcesList.append(html);
        });

        //make toolbox items draggable & droppable
        $("#moduleslist .item, #datasourceslist .item, #udatasourceslist .item").draggable({
            revert: "invalid",
            helper: "clone",
            start: function(e, ui)
            {
                $(ui.helper).css('opacity', '0.5');
            }
        });
		$elCanvas.droppable({
			accept: "#moduleslist .item, #datasourceslist .item, #udatasourceslist .item",
			drop: function (event, ui) {
				if (event.which !== 1 || event.originalEvent.isTrigger) {
					console.log("node creation canceled by user");
					return;
				}
				var btop = (this.offsetHeight - this.scrollHeight) / 2 * wfZoom;
				var bleft = (this.offsetWidth - this.scrollWidth) / 2 * wfZoom;
				var top = (ui.offset.top - $(this).offset().top - btop) / wfZoom;
				var left = (ui.offset.left - $(this).offset().left - bleft) / wfZoom;

				//snap to grid (TODO: get defined grid size?)
				var xGrid = 20;//horizontal grid size
				var yGrid = 20;//vertical grid size
				var leftGridSpaces = Math.floor(left / xGrid);
				if (left % xGrid <= Math.floor(xGrid/2)) {
					left = leftGridSpaces * xGrid;
				}
				else {
					left = (leftGridSpaces + 1) * xGrid;
                }
				var topGridSpaces = Math.floor(top / 20);
				if (top % xGrid <= Math.floor(yGrid / 2)) {
					top = topGridSpaces * yGrid;
				}
				else {
					top = (topGridSpaces + 1) * yGrid;
				}			

				var toolboxId = $(ui.helper).data("toolboxid");
				var compType = $(ui.helper).data("componenttype");
				var nodeInformation = wfTools.toolboxnodes[compType][toolboxId]
				var initialName = nodeInformation.label;
				var nodeData = {
						"ntype": compType,
						"ntemplateid": $(ui.helper).data("componentid"),
						"mtype":"",
						"mlabel":"No Name",
						"fullData": {
								componentId: $(ui.helper).data("componentid"),
								componentType: (function(c){if(c==="ds") return "DATASOURCE"; if(c==="uds") return "DATASOURCE"; if(c==="uq") return "DATASOURCE"; if(c==="pc") return "PROCESSING"; if(c==="cc") return "CONDITION"}(compType)),
								created: arrayFromDatetime(),
								customValues:[],
								//id:0, !!! do not send id!!!
								incomingLinks:[],
								level:0,
								name:initialName,
								xCoord:left,
								yCoord:top,
								behavior: "FAIL_ON_ERROR"
						}
				};
				// if(nodeInformation.dna.template){
				// 	nodeData.fullData.customValues.push('templateContents',nodeInformation.dna.template.contents) 
				// }
				//adjust drop coordinates to conform pan&zoom
				//call tao_dropNewNode, it will save and render new node
				jsPlumb.fire("tao_dropNewNode", [nodeData, left, top]);
//				  $(this).append($(ui.helper).clone().draggable({
//					  containment: "parent"
//				  }));
            }
        });
    }

      function tao_ZoomFitAllNodes() {
        pz.panzoom('reset', { animate: false });
        wfZoom = 1;
			console.log("f:fitCanvasToWindow");
			var minLeft			= 0;
			var minTop			= 0;
			var maxBottom		= 0;
			var maxRight		= 0;
			var spanY			= 0;
			var spanX			= 0;
			var nodes			= [];
			var itemOffset		= {};
			var theWindow = {};
			var canvasW = 10000;
			var canvasH = 10000;
			
			theWindow.w = window.innerWidth;
			theWindow.h = window.innerHeight;
				
			
			$(".w", $elCanvas).each(function(index) {
				itemOffset = {};
				itemOffset = $(this).offset();
				itemOffset.w = $(this).outerWidth()*wfZoom;
				itemOffset.h = $(this).outerHeight()*wfZoom;
				itemOffset.right = $(this).offset().left + itemOffset.w;
				itemOffset.bottom = $(this).offset().top + itemOffset.h;
//				if($(this).hasClass("selected")){		}
				
				if(index === 0){
					minLeft = itemOffset.left;
					minTop = itemOffset.top;
					maxBottom = itemOffset.bottom;
					maxRight = itemOffset.right;
				} 
				minLeft = Math.min(minLeft,itemOffset.left);
				minTop = Math.min(minTop,itemOffset.top);
				maxBottom = Math.max(maxBottom,itemOffset.bottom);
				maxRight = Math.max(maxRight,itemOffset.right);
				nodes.push(itemOffset);
            });
			
			spanX = maxRight-minLeft;
			spanY = maxBottom-minTop;
			
			console.log("minTop:"+minTop);
	
			wfZoom = Math.min(theWindow.w/spanX, theWindow.h/spanY);
			rezidualSpace = {
				left: (theWindow.w-spanX*wfZoom)/2,
				top: (theWindow.h-spanY*wfZoom)/2
			};
			//set it in motion


//var fitTop = parseInt(-minTop*wfZoom - (canvasH - canvasH*wfZoom)) - rezidualSpace.top/wfZoom;
        var fitTop = parseInt(-minTop*wfZoom - (canvasH - canvasH*wfZoom)/2) + rezidualSpace.top/wfZoom;
        var fitLeft = parseInt(-minLeft*wfZoom - (canvasW - canvasW*wfZoom)/2) + rezidualSpace.left/wfZoom;
//var fitLeft = -(1)*parseInt(minLeft*wfZoom + (canvasW - canvasW*wfZoom)/2) + rezidualSpace.left/wfZoom;
			
			
			var matrix = [wfZoom, 0, 0, wfZoom, fitLeft, fitTop];
			pz.panzoom("setMatrix", matrix);
        	jsPlumb.fire("jsPlumbSetZoom", wfZoom);
			makeWFPreview();
    }
	
	//preview window
	function makeWFPreview() {
			console.log("f:makeWFPreview");
            var $wfPreview		= $("#img-wf-preview");
			var wfPreviewSizeX	= $wfPreview.width();
			var wfPreviewSizeY	= $wfPreview.height();
			var minLeft			= 0;
			var minTop			= 0;
			var maxBottom		= 0;
			var maxRight		= 0;
			var spanY			= 0;
			var spanX			= 0;
			var nodes			= [];
			var itemOffset		= {};
			
			$wfPreview.closest(".ui-widget-content").css({ "width": wfPreviewSizeX + "px" });
			
			//fake node. viewport.
			itemOffset.left = 0;
			itemOffset.top = 0;
			itemOffset.bottom = itemOffset.top + window.innerHeight;
			itemOffset.right = itemOffset.left + window.innerWidth;
			itemOffset.w = window.innerWidth;
			itemOffset.h = window.innerHeight;
			itemOffset.selected = -1;
			nodes.push(itemOffset);
				
			minLeft = itemOffset.left;
			minTop = itemOffset.top;
			maxBottom = itemOffset.bottom;
			maxRight = itemOffset.right;
			
			$(".w", $elCanvas).each(function(index) {
				itemOffset = {};
				itemOffset = $(this).offset();
                itemOffset.selected = 0;
				itemOffset.w = $(this).outerWidth()*wfZoom;
				itemOffset.h = $(this).outerHeight()*wfZoom;
				itemOffset.right = $(this).offset().left + itemOffset.w;
				itemOffset.bottom = $(this).offset().top + itemOffset.h;
				if($(this).hasClass("selected")){
					itemOffset.selected = 1;
				}
                if($(this).hasClass("g")){
                    itemOffset.selected = -2;
				}
				minLeft = Math.min(minLeft,itemOffset.left);
				minTop = Math.min(minTop,itemOffset.top);
				maxBottom = Math.max(maxBottom,itemOffset.bottom);
				maxRight = Math.max(maxRight,itemOffset.right);
				nodes.push(itemOffset);
            });
			
			spanX = maxRight-minLeft;
			spanY = maxBottom-minTop;
			
			previewScale = Math.min(wfPreviewSizeX/spanX, wfPreviewSizeY/spanY);
			rezidualSpace = {
				left: (wfPreviewSizeX-spanX*previewScale)/2,
				top: (wfPreviewSizeY-spanY*previewScale)/2
			};
			
			$wfPreview.empty();
        	var pHtml = '';
			$.each( nodes, function( key, itemMatrix ) {
				if(itemMatrix.selected === 1){
					pHtml += "<div class=\"preview-node selected\" style=\"top:"+(rezidualSpace.top+(itemMatrix.top-minTop)*previewScale)+"px;left:"+(rezidualSpace.left+(itemMatrix.left-minLeft)*previewScale)+"px;width:"+itemMatrix.w*previewScale+"px;height:"+itemMatrix.h*previewScale+"px\"></div>";
				}
				if(itemMatrix.selected === 0){
					pHtml += "<div class=\"preview-node\" style=\"top:"+(rezidualSpace.top+(itemMatrix.top-minTop)*previewScale)+"px;left:"+(rezidualSpace.left+(itemMatrix.left-minLeft)*previewScale)+"px;width:"+itemMatrix.w*previewScale+"px;height:"+itemMatrix.h*previewScale+"px\"></div>";
				}
                if(itemMatrix.selected === -2){
                    pHtml += "<div class=\"preview-node group\" style=\"top:"+(rezidualSpace.top+(itemMatrix.top-minTop)*previewScale)+"px;left:"+(rezidualSpace.left+(itemMatrix.left-minLeft)*previewScale)+"px;width:"+itemMatrix.w*previewScale+"px;height:"+itemMatrix.h*previewScale+"px\"></div>";
                }
				
				//add Viewfinder
				if(itemMatrix.selected === -1){
					pHtml += "<div class=\"preview-node viewfinder\" style=\"top:"+(rezidualSpace.top+(itemMatrix.top-minTop)*previewScale)+"px;left:"+(rezidualSpace.left+(itemMatrix.left-minLeft)*previewScale)+"px;width:"+itemMatrix.w*previewScale+"px;height:"+itemMatrix.h*previewScale+"px\"></div>";
				}
			});
        	$wfPreview.append(pHtml);
    }
	
	//prevent browser zooming on mouse scroll only.
	$(window).bind('mousewheel DOMMouseScroll', function (event) {
			if (event.ctrlKey === true) {
    			event.preventDefault();
			}
	});
	//prevent browser zooming on keyboard shortcuts.
	$(document).keydown(function(event) {
			//Ctrl + +
	   	    if (event.ctrlKey === true && (event.which === '61' || event.which === '107' || event.which === '187') ) {
				event.preventDefault();
				doWFZoom("zoom-plus");
			}
	   	    //Ctrl + -
			if (event.ctrlKey === true && (event.which === '173' || event.which === '109' || event.which === '189') ) {
				event.preventDefault();
				doWFZoom("zoom-minus");
			}
			//Ctrl + 1
			if (event.ctrlKey === true && (event.which === '49' || event.which === '35' || event.which === '97') ){
				event.preventDefault();
				doWFZoom("zoom-1to1");
			}
			if (event.ctrlKey === true && (event.which === '37' || event.which === '100') ){
				event.preventDefault();
				doWFPan("pan-right");
			}
			if (event.ctrlKey === true && (event.which === '39' || event.which === '102') ){
				event.preventDefault();
				doWFPan("pan-left");
			}
			if (event.ctrlKey === true && (event.which === '40' || event.which === '98') ){
				event.preventDefault();
				doWFPan("pan-up");
			}
			if (event.ctrlKey === true && (event.which === '38' || event.which === '104') ){
				event.preventDefault();
				doWFPan("pan-down");
			}
	});

$( function() {
	if(_settings.readCookie("preferences_workFlowConnectors")){
		if(_settings.readCookie("preferences_workFlowConnectors") === "F"){
			setWFConnectorsToFlowchart();
		}
		if(_settings.readCookie("preferences_workFlowConnectors") === "S"){
			setWFConnectorsToStateMachine();
		}
	}else{
		setWFConnectorsToStateMachine();
	}

		toolboxModules.rescanBuild();
		$( "#draggable-toolbox-preview" ).draggable({ handle: "p" });
		$( "#draggable-toolbox-modules" ).draggable({ handle: "p" });
		$( "#selectable" ).selectable({
											selected: function( event, ui ) {
												console.log("selected????");
												var dna = $(ui.selected).data("dna");
												$("#"+dna.blockid).addClass("selected");
											},
											start: function( event, ui ) {
												console.log("select start");
												$(".w").each(function() {
																	$(this).removeClass("selected");
																});
											},
											stop: function( event, ui ) {
												console.log("select stop");
												toolboxModules.rescanSelected();
											}
									});
        $(document)
        .on('keyup', function(e){
            if(e.which === 46) {
                toolboxModules.rmSelected();
			}
			if (e.ctrlKey && (e.which == 37 || e.which == 100)) {//CTRL + left arrow
				toolboxModules.leftAlignSelected();
			}
			if (e.ctrlKey && (e.which == 38 || e.which == 104)) {//CTRL + up arrow
				toolboxModules.topAlignSelected();
			}
			if (e.ctrlKey && (e.which == 39 || e.which == 102)) {//CTRL + right arrow
				toolboxModules.rightAlignSelected();
			}
			if (e.ctrlKey && (e.which == 40 || e.which == 98)) {//CTRL + down arrow
				toolboxModules.bottomAlignSelected();
			}
        })
        .on('keyup keydown', function(e){
            keyboardShifted = e.shiftKey;
            shifted = e.shiftKey; ////???? xxx
        })
        .on('keydown', function(e){
            if (e.ctrlKey) {
                if (e.which === 65 || e.which === 97) { // 'A' or 'a'
                    e.preventDefault();
                    console.log("CTRL + A");
                    $(".w", $elCanvas).addClass("selected");
                    toolboxModules.rescanSelected();
                }
            }
        });

    //warn user before leaving page, nothing fancy for the moment, consider dirty flag for unsaved work
    window.addEventListener("beforeunload", function (e) {
        var confirmationMessage = "\o/";
        (e || window.event).returnValue = confirmationMessage;     //Gecko + IE
        return confirmationMessage;                                //Webkit, Safari, Chrome etc.
    });
});
