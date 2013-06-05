$(document).ready(
		function() {
			var button = $('<a  href="#" class="green_button">Add Diagram</a>')
					.appendTo($('#more'));
			button.click(function() {
				$.ajax({
					headers : {
						Accept : "application/json; charset=utf-8",
					},
					type : 'GET',
					url : '/c3po/properties',
					timeout : 5000,
					async : false,
					success : function(oData) {
						showPopup(oData);

					}
				});
			});

			$('<span> </span>').appendTo($('#more'));

			var addBubbleChartButton = $('<a href="#" class="green_button">Add Bubble Chart</a>')
					.appendTo($('#more'));
			addBubbleChartButton.click(function() {
				$.ajax({
					headers : {
						Accept : "application/json; charset=utf-8",
					},
					type : 'GET',
					url : '/c3po/properties',
					timeout : 5000,
					async : false,
					success : function(oData) {
						showBubbleChartPopup(oData);
					}
				});
			});
		});

function showPopup(properties) {
	$("#overlay").addClass('activeoverlay');

	var popup = $('#filterpopup');
	popup.children('.popupreason').text('Please select a property');
	var config = popup.children('.popupconfig');

	var sel = $('<select>').appendTo($(config));
	$(sel).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel).append($('<option>').text(value).attr('value', value));
	});

	popup.css({
		'display' : 'block',
		'z-index' : 11
	});

	$('.popupconfig select').change(function() {
		$.ajax({
			type : 'GET',
			url : '/c3po/property?name=' + $(this).val(),
			timeout : 5000,
			success : function(oData) {
				showOptions(oData.type);
			}
		});
	});
};

function showBubbleChartPopup(properties) {
	$("#overlay").addClass('activeoverlay');

	var popup = $('#filterpopup');
	popup.children('.popupreason').text('Please select two properties');
	var config = popup.children('.popupconfig');

	var fieldset1 = $('<fieldset><legend>First Property</legend></fieldset>').appendTo(config);
	var fieldset2 = $('<fieldset><legend>Second Property</legend></fieldset>').appendTo(config);

	var sel1 = $('<select>').appendTo(fieldset1);
	$(sel1).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel1).append($('<option>').text(value).attr('value', value));
	});

	var sel2 = $('<select>').appendTo(fieldset2);
	$(sel2).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel2).append($('<option>').text(value).attr('value', value));
	});

	popup.css({
		'display' : 'block',
		'z-index' : 11
	});

	$('.popupconfig select').change(function(event) {
		$.ajax({
			type : 'GET',
			url : '/c3po/property?name=' + $(this).val(),
			timeout : 5000,
			success : function(oData) {
				showBubbleOptions(event.target, oData.type);
			}
		});
	});
};

function showOptions(type) {
	if (type == "STRING" || type == "BOOL" || type == "DATE") {
		var property = $('.popupconfig select').val();
		hidePopupDialog();
		startSpinner();
		$.ajax({
			type : 'GET',
			url : '/c3po/overview/graph?property=' + property,
			timeout : 5000,
			success : function(oData) {
				stopSpinner();
				var hist = [];
				$.each(oData.keys, function(i, k) {
					hist.push([ oData.keys[i], parseInt(oData.values[i]) ]);
				});
				var id = oData.property;
				var data = {};
				data[id] = hist;
				drawGraphs(data);
				//scroll to bottom of page.

			}
		});

	} else {
		showIntegerPropertyDialog('applyIntegerHistogramSelection()');
	}
}

function showBubbleOptions(select, type) {
	if (type != "STRING" && type != "BOOL" && type != "DATE") {
		showIntegerPropertyBubbleChartInput(select);
	} else {
		// remove unused inputs and selects
		var fieldset = $(select).closest('fieldset');
		fieldset.find('select[class="algorithm"]').remove();
		fieldset.find('input').remove();

		// try to draw bubble chart
		applyBubbleChartSelection();
	}
}

function showIntegerPropertyBubbleChartInput(select) {
	var fieldset = $(select).closest('fieldset');
	var algorithmSelection = $('<select class="algorithm"><option/><option value="fixed">fixed</option><option value="sturge">Sturge\'s</option><option value="sqrt">Square-root choice</option></select>');
	fieldset.append(algorithmSelection);
	
	algorithmSelection.change(function(event) {
		var fieldset = $(event.target).closest('fieldset');

		// remove old inputs and selects
		fieldset.find('input').remove();

		var val = $(this).val();
		if (val == "") {
			$(this).effect("highlight", {color:'#FF1400'} , "slow");
		} else { 
			if (val == "fixed") {
				var binInput = $('<input type="text" placeholder="bin width" />');
				fieldset.append(binInput);
				binInput.change(function() {
					// try to draw bubble chart
					applyBubbleChartSelection();
				});
			}

			// try to draw bubble chart
			applyBubbleChartSelection();
		}
	});
}

function applyBubbleChartSelection() {
	// are all inputs and selects chosen/filled?
	var elements = $('.popupconfig select, .popupconfig input');
	var foundEmptyValue = false;
	for (var i = 0; i < elements.length; i++) {
		if ($(elements[i]).val() == '') {
			foundEmptyValue = true;
			break;
		}
	}

	// all values filled, draw chart
	if (!foundEmptyValue) {
		// gather data
		var fieldsets = $('.popupconfig fieldset');
		var fieldset1 = $(fieldsets[0]);
		var fieldset2 = $(fieldsets[1]);

		// first fieldset/property
		var selects1 = fieldset1.find('select');
		var inputs1 = fieldset1.find('input');

		var property1 = $(selects1[0]).val();
		var alg1 = $(selects1[1]).val();
		var width1 = $(inputs1[1]).val();

		// second fieldset/property
		var selects2 = fieldset2.find('select');
		var inputs2 = fieldset2.find('input');

		var property2 = $(selects2[0]).val();
		var alg2 = $(selects2[1]).val();
		var width2 = $(inputs2[1]).val();

		// create URL
		var url = '/c3po/overview/graph?property=&property1=' + property1 + '&property2=' + property2;
		if (alg1 != undefined) {
			url += '&alg1=' + alg1;

			if (width1 != undefined) {
				url += '&width1=' + width1;
			}
		}

		if (alg2 != undefined) {
			url += '&alg2=' + alg2;

			if (width2 != undefined) {
				url += '&width2=' + width2;
			}
		}

		hidePopupDialog();
		startSpinner();
		$.ajax({
			type : 'GET',
			url : url,
			timeout : 5000,
			success : function(oData) {
				var id = oData.property;
				var data = {};
				data[id] = oData.values;
				$('#' + id).remove(); // remove the old graph if exist
				drawGraphs(data, oData.options);
				stopSpinner();
				//scroll to bottom of page.
			}
		});
	}
}

function applyIntegerHistogramSelection() {
	var selects = $('.popupconfig').children('select');
	var property = $('.popupconfig').children('select:first').val();
	var alg = $('.popupconfig').children('select:last').val();
	var width = -1;
	if (alg == "fixed") {
		width = $('.popupconfig input:first').val();
	}

	hidePopupDialog();
	startSpinner();
	$.ajax({
		type : 'GET',
		url : '/c3po/overview/graph?property=' + property + "&alg=" + alg
				+ "&width=" + width,
		timeout : 5000,
		success : function(oData) {
			var hist = [];
			$.each(oData.keys, function(i, k) {
				hist.push([ oData.keys[i], parseInt(oData.values[i]) ]);
			});
			var id = oData.property;
			var data = {};
			data[id] = hist;
			$('#' + id).remove(); // remove the old graph if exist
			drawGraphs(data, oData.options);
			stopSpinner();
			//scroll to bottom of page.
		}
	});
};

function getBarChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults : {
			renderer : $.jqplot.BarRenderer,
			// Show point labels to the right ('e'ast) of each bar.
			// edgeTolerance of -15 allows labels flow outside the grid
			// up to 15 pixels. If they flow out more than that, they
			// will be hidden.
			pointLabels : {
				show : true,
				location : 'n',
				edgeTolerance : -15
			},
			// Rotate the bar shadow as if bar is lit from top right.
			shadowAngle : 70,
			// Here's where we tell the chart it is oriented horizontally.
			rendererOptions : {
				barDirection : 'vertical',
				barWidth : '12'
			},
			color : '#639B00'
		},
		axesDefaults : {
			tickRenderer : $.jqplot.CanvasAxisTickRenderer,
			tickOptions : {
				angle : -30,
				fontSize : '8pt'
			}
		},
		axes : {
			// Use a category axis on the x axis and use our custom ticks.
			xaxis : {
				renderer : $.jqplot.CategoryAxisRenderer,
				tickOptions : {
					formatter : function(format, val) {
						if (val.length > 30) {
							val = val.substring(0, 25) + '...';
						}

						// val = (val.replace(/\.0/g, ""));
						return val;
					}
				}
			},
			// Pad the y axis just a little so bars can get close to, but
			// not touch, the grid boundaries. 1.2 is the default padding.
			yaxis : {
				pad : 1.05,
				tickOptions : {
					formatString : '%d',
				}
			}
		},
		highlighter : {
			show : true,
			tooltipLocation : 'n',
			showTooltip : true,
			useAxesFormatters : true,
			sizeAdjust : 0.5,
			tooltipAxes : 'y',
			bringSeriesToFront : true,
			tooltipOffset : 30,
		},
		cursor : {
			style : 'pointer', // A CSS spec for the cursor type to change the
								// cursor to when over plot.
			show : true,
			showTooltip : false, // show a tooltip showing cursor position.
			useAxesFormatters : true, // wether to use the same formatter and
										// formatStrings
		// as used by the axes, or to use the formatString
		// specified on the cursor with sprintf.
		}

	};

	return options;
};

function getPieChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults : {
			renderer : $.jqplot.PieRenderer,
			rendererOptions : {
				showDataLabels : true
			}
		},
		legend : {
			show : true,
			location : 'e'
		}
	};

	return options;
};

function getBubbleChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults: {
			renderer: $.jqplot.BubbleRenderer,
			rendererOptions: {
				showLabels: false
			}
		},
		axes: {
			xaxis: {
				label: ttl.substring(0, ttl.indexOf('Versus') - 1),
				labelRenderer: $.jqplot.CanvasAxisLabelRenderer
			},
			yaxis: {
				label: ttl.substring(ttl.indexOf('Versus') + 7),
				labelRenderer: $.jqplot.CanvasAxisLabelRenderer
			}
		}
	};

	return options;
}

function prettifyTitle(title) {
	title = title.replace(/_/g, " ");
	return title.replace(/\w\S*/g, function(txt) {
		return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
	});
};

function drawGraphs(data, options) {
	var idx = 0;
	var graphsdiv = $('#graphs');
	$.each(data, function(i, d) {
		var container;
		var clazz;
		if (idx % 2 == 0) {
			container = $('<div class="span-24">').appendTo(graphsdiv);
			clazz = "dia_left";
		} else if (idx % 2 == 1) {
			container = graphsdiv.children('.span-24:last');
			clazz = "dia_right";
		}

		if (d.length > 30) {
			container = $('<div class="span-24">').appendTo(graphsdiv);
			clazz = "dia_full";
			idx++; // if full length skip to next row left
		}

		container.append('<div id="' + i + '" class="' + clazz + '">');
		$('#' + i).bind(
				'jqplotDataClick',
				function(ev, seriesIndex, pointIndex, data) {
					startSpinner();

					var versusLocation = i.indexOf('_versus_');
					if (versusLocation != -1) {
						// it's a bubble chart we have two filters
						var url1 = '/c3po/filter?filter='+ i.substring(0, versusLocation) +'&value='+ data[0] +'&type=graph';
						// TODO: append options

						// fix value if there are missing bubbles for the property
						var fixedValue = data[1];
						for (var j = 0; j < data[1]; j++) {
							if (seriesIndex - j < 0) {
								fixedValue--;
								continue;
							}

							if (d[seriesIndex - j][0] != data[0]) {
								// different property
								fixedValue--;
							}
						}
						var url2 = '/c3po/filter?filter='+ i.substring(versusLocation + 8) +'&value='+ fixedValue +'&type=graph';
						// TODO: append options

						$.post(url1, function(data) {
							$.post(url2, function(data) {
								window.location = '/c3po/overview';
							});
						});
					} else {
						var url = '/c3po/filter?filter=' + i + '&value='
								+ pointIndex + '&type=graph';
						if (options) {
							var type = options['type'];
							var alg = options['alg'];
							var width = options['width'];

							if (type == 'INTEGER') {
								url += '&alg=' + alg;

								if (width) {
									url += '&width=' + width;
								}
							}
						}
						$.post(url, function(data) {
							window.location = '/c3po/overview';
						});
					}
				});
		if (options != undefined && options['diagramType'] == 'Bubble') {
			var plot = $.jqplot(i, [ d ], getBubbleChart(prettifyTitle(i)));

			// create tooltip-bindings for bubble chart
			if ($('#tooltip'+ i).size() == 0) {
				$('<div style="position:absolute;z-index:99;font-size:12px;color:rgb(15%, 15%, 15%);padding:2px;background-color:rgba(95%,95%,95%,0.8)" id="tooltip'+ i +'"></div>').insertAfter('#'+ i);
			}

			$('#'+ i).bind('jqplotDataHighlight',
				function (ev, seriesIndex, pointIndex, data, radius) {
					var chart_left = $('#'+ i).offset().left,
						chart_top = $('#'+ i).offset().top,
						x = plot.axes.xaxis.u2p(data[0]),  // convert x axis unita to pixels
						y = plot.axes.yaxis.u2p(data[1]);  // convert y axis units to pixels
					var color = 'rgb(50%,50%,100%)';
					$('#tooltip'+ i).css({left:chart_left+x+radius+5, top:chart_top+y});
					$('#tooltip'+ i).html('<span style="font-size:14px;font-weight:bold;color:' + color + ';">' + data[3] + '</span><br />' + data[2] + ' files');
					$('#tooltip'+ i).show();
				});

			// Bind a function to the unhighlight event to clean up after highlighting.
			$('#'+ i).bind('jqplotDataUnhighlight',
				function (ev, seriesIndex, pointIndex, data) {
					$('#tooltip'+ i).empty();
					$('#tooltip'+ i).hide();
				});
		} else {
			// bar chart is the default chart
			$.jqplot(i, [ d ], getBarChart(prettifyTitle(i)));
		}

		if (idx == 0) {
			idx++; // if first row skip the right and go to next row...
		}
		idx++;
	})
};
