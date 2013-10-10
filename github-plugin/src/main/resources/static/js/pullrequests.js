$(function() {
	$("select#organisation").change(function() {
		loadPullRequests();
		$("input#filter").val("");
	});
	
	var completed = false;
	var running = false;
	
	var refresh = function() {
		$.post('pull-request-import-status.gh', function(data) {
			var pullRequests = eval('(' + data + ')');
			var allCompleted = false;
			if(pullRequests.length == 0) {
				$("#submit").prop("disabled", "disabled");
			} else {
				allCompleted = true;
				for (var i=0; i<pullRequests.length; i++) {
					var id = pullRequests[i].index;
					var description = pullRequests[i].value;
					var status = pullRequests[i].status.toLowerCase();
					var shortDescription = pullRequests[i].shortDescription;
					$("#status_" + id).attr("class", "status " + status);
					$("#status_description_" + id).text(shortDescription);
					$("#status_description_" + id).attr("title", description);
					if(status == 'sync') {
						allCompleted = false;
					}
				}
			}
			
			if(allCompleted || repos.length <= 0) {
				completed = true;
				$("#submit").prop("disabled", "");
				$("#submit").html("<span class=\"button green\"><span>Next &gt;</span></span>")
				clearInterval(refreshInterval);
				running = false;
			}
		});
	}
	
	$("#submit").click(function() {
		var destination;

		if(completed || $("input#checkall:checked").length <= 1) {
			$('#pullrequests').submit();
			return true;
		} else {
			
			$("tr").each(function() {
				var importCheckbox = $(this).find("td input:checkbox");
				if(importCheckbox.prop("checked")) {
					var importTypeSelect = $(this).find("#importtype");
					$(importTypeSelect).attr("style","display: none;");
				}
			});
			
			$.ajax({
				type : "POST",
				url : "pull-request-import.gh",
				data : $("#pullrequests").serialize(),
				success : function() {
					$(".status").each(function() {
						$(this).attr("display","block");
					});
					refresh();
					refreshInterval = setInterval(refresh, 2000);
				}
			});
			running = true;
			$("#submit").prop("disabled", "disabled");
			return false;
		}
	});
	
	$("#cancel").click(function() {
		if(running) {
			$.ajax({
				type : "POST",
				url : "pull-request-import-cancel.gh",
				success: function() {
					refresh();
				}
			});
		} else {
			window.location = "/";
		}
	});
});	

// Table sort - DataTables
var table = $('#pull-requests'),
    tableStyled = false;

table.dataTable({
    'aoColumnDefs': [
        { 'bSortable': false, 'aTargets': [ 0, 4 ] }
    ],
    'oLanguage': {
        'sLengthMenu': '_MENU_ Rows',
        'sSearch':'Search pull request'
    },
    'sPaginationType': 'full_numbers',
    'sDom': '<"dataTables_header"fpl>t',
    'bAutoWidth': true,
    'fnDrawCallback': function( oSettings )
    {
        // Only run once
    }
});

$("table thead input#checkall").change(function() {
	var checked = $(this).prop("checked");
	$("table tbody td input#checkall").each(function() {
		$(this).prop("checked", checked);
	});
});

$("table thead select#importtype").change(function() {
	var importType = $("table thead select#importtype option:selected").val();
	$("table tbody select#importtype option").filter(function() {
	    return $(this).text() == importType; 
	}).prop('selected', true);
});

var loadPullRequests = function (repository) {
	$('#pull-requests').attr("style","display: none;");
	$('.dataTables_header').attr("style","display: none;");
	$("div.loading").attr("style","display: visible;");
	if (repository == undefined) {
		$("ul.repo-list").empty();
	}
	$("table#pull-requests tbody").empty();
	$("#submit").prop("disabled", "disabled");

	var organisation = $("select#organisation option:selected").val();
	$.post('pull-request-list.gh', 
		{ 
		"organisation": organisation,
		"repository": repository
		},
		function(data) {
		
		var prs = eval('(' + data + ')');
		var numItems = 0;
		table.fnClearTable();
		var idx = 0;
		for (var i=0; i<prs.length; i++) {
			var pr = prs[i];

			if(repository == undefined) {
				repoLi = $("ul.repo-list li#" + pr.repository);
				if(repoLi.length == 0) {
					$('<li id="' + pr.repository + '"><a href="#">' + pr.repository + '</a>' + 
							(pr.pullrequests == undefined ? '':'<p>' + pr.pullrequests.length + '</p>') + 
							'</li>').appendTo('ul.repo-list');
				} 
			}
			
			if(pr.pullrequests != undefined) {
				for(var j=0; j<pr.pullrequests.length; j++) {
					var req = pr.pullrequests[j];
					var paramPrefix = "pr." + idx;
					table.fnAddData( [ 
		                        '<input type="checkbox" name="' + paramPrefix + '.selected" id="checkall" checked="checked">',
		                        '#' + req.id + 
		                        '<input type="hidden" name="' + paramPrefix + '.idx" value="' + idx + '"/>' +
		                        '<input type="hidden" name="' + paramPrefix + '.id" value="' + req.id + '"/>' +
		                        '<input type="hidden" name="' + paramPrefix + '.repo" value="' + pr.repository + '"/>',
		                        '<p class="repository">' + pr.repository + '</p> | ' +
		                        '<p class="title">' + req.title + '</p>' +
		                        '<p class="body">' + req.body + '</p>' +
		                        '<p class="author">by ' + req.author + '</p>',
		                        '<p class="timestamp">' + req.date + '</p>',
		                        '<select name="' + paramPrefix + '.type" id="importtype"><option>Commits</option> </select>' +
		                        '<span id="status_' +  idx + '" class="status" ></span>' +
		                        '<label id="status_description_' + idx + '" class="synch-status">'
			                  ] );
					idx = idx + 1;
				}
				numItems = numItems + pr.pullrequests.length;
			}
		}
		
		if(idx == 0) {
			$("#submit").html("<span class=\"button green\"><span>Next &gt;</span></span>")
		}

		if(repository == undefined) {
			var repoSort = function sortAlpha(a,b){  
				var cmpA = ($(a).find("p") == undefined ? "0":$(a).find("p").text()) + $(a).find("a").text();
				var cmpB = ($(b).find("p") == undefined ? "0":$(b).find("p").text()) + $(b).find("a").text();
				return cmpA > cmpB ? 1 : -1;  
			};  
			  
			$('ul.repo-list li').sort(repoSort).appendTo('ul.repo-list');
			$('<li class="all selected"><a href="#">All repositories</a><p>' + numItems + "</p></li>").prependTo('ul.repo-list');
			$('ul.repo-list li a').click(function() {
				var prevClass = $("ul.repo-list li.selected").attr("class");
				$("ul.repo-list li.selected").attr("class", prevClass == "all selected" ? "all":"");

				var currClass = $(this).parent().attr("class");
				$(this).parent().attr("class", currClass + " selected");
				
				var repository = $(this).text();
				if($(this).parent().attr("class").indexOf("all") < 0) {
					loadPullRequests($(this).text());
				} else {
					loadPullRequests();
				}
			});
		}
		
		$("#submit").prop("disabled", "");
		$(".filter").attr("style","display: visible;");		
		$("div.loading").attr("style","display: none;");
		$('#pull-requests').attr("style","display: visible;");
		$('.dataTables_header').attr("style","display: visible;");
	});
};

var filterTimeout;
$("input#repo-filter").keyup(function() {
	if (filterTimeout) {
		clearTimeout(filterTimeout);
	}
	filterTimeout = setTimeout(function () {
		filterRepositories();
	},500);
});

var filterRepositories = function() {
	var filter = $("input#repo-filter").val().toLowerCase();
	var numRepos = 0;
	$("ul.repo-list li").each(function() {
		var repoName = $(this).find("a").text();
		var matched = repoName.toLowerCase().indexOf(filter)>=0;
		if(matched) {
			$(this).attr("style","display: visible;");
		} else {
			$(this).attr("style","display: none;");
		}
	});
}

$(document).ready(function () {
	loadPullRequests();
});

