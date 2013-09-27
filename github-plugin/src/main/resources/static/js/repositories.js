$(function() {
	var refreshInterval;

	var completed = false;
	var running = false;

	var refresh = function() {
		$.post('repositories-clone-status.gh', function(data) {
			var repos = eval('(' + data + ')');
			var allCompleted = false;
			if(repos.length == 0) {
				$("#submit").prop("disabled", "disabled");
			} else {
				allCompleted = true;
				for (var i=0; i<repos.length; i++) {
					var id = repos[i].index;
					var value = repos[i].value;
					var status = repos[i].status.toLowerCase();
					$("#status_" + id).attr("class", "status " + status);
					$("#repo_" + id).text(value);
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
	
	$("select#organisation").change(function() {
		completed = false;
		running = false;
		loadRepositories();
		$("input#filter").val("");
	});
	
	$("select#numitems").change(function() {
		filterRepositories();
	})
	
	var filterTimeout;
	$("#filter").keyup(function() {
		if (filterTimeout) {
			clearTimeout(filterTimeout);
		}
		filterTimeout = setTimeout(function () {
			filterRepositories();
		},500);
	});
	
	var filterRepositories = function() {
		var filter = $("input#filter").val().toLowerCase();
		var numRepos = 0;
		$(".repo-sync li").each(function() {
			var repoName = $(this).find("input.name").val();
			var matched = repoName.toLowerCase().indexOf(filter)>=0;
			var maxItems = $("select#numitems").val();
			var checkbox = $(this).find("input.keycheckbox");
			if(matched && numRepos < maxItems) {
				$(this).attr("style","display: visible;");
				$(this).find("input").removeAttr("disabled");
				checkbox.prop("checked", true);
				numRepos++;
			} else {
				$(this).attr("style","display: none;");
				$(this).find("input").attr("disabled","disabled");
				checkbox.prop("checked", false);
			}
		});
	}
	
	$("#submit").click(function() {
		var destination;

		if(completed || $("ul.repo-sync li").length <= 0) {
			$('#repositories').submit();
			return true;
		} else {
			$.ajax({
				type : "POST",
				url : "repositories-clone.gh",
				data : $("#repositories").serialize(),
				success : function() {
					$(".status").each(function() {
						this.style.visibility = "visible";
					});
					$("ul.repo-sync .keycheckbox").each(function() {
						if($(this).prop("checked")) {
							$(this).prop("checked", false);
							this.style.visibility = "hidden";
						}
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
				url : "repositories-clone-cancel.gh",
				success: function() {
					refresh();
				}
			});
		} else {
			window.location = "/";
		}
	});
	
	$("input#filter").focusin(function() {
		$("input#filter").attr("placeholder","");
	});
	
	$("input#selectallcheckbox").change(function() {
		var checked = $("input#selectallcheckbox").prop("checked");
		$("ul.repo-sync .keycheckbox").each(function() {
			if(this.style.display != "none") {
				$(this).prop("checked", checked);
			}
		});
	});
});

var loadRepositories = function () {
	$("div.loading").attr("style","display: visible;");
	$("ul.repo-sync").empty();
	$("div.filter").attr("style","display: none;");
	$("#submit").prop("disabled", "disabled");
	
	var organisation = $("select#organisation option:selected").val();
	var filter = $("input#filter").val();
	$.post('repositories-list.gh', 
		{ "organisation": organisation, "filter": filter },
		function(data) {
		$("div.loading").attr("style","display: none;");
		
		var repos = eval('(' + data + ')');
		var maxItems = $("select#numitems").val();
		
		for (var i=0; i<repos.length; i++) {
			var repo = repos[i];
			var repoLine = $(_.template($("#repo-sync-template").html(),
					{ "index": i, 
				      "repo": repo }));
			if(i >= maxItems) {
				repoLine.attr("style","display:none;");
				repoLine.find("input").attr("disabled","disabled");
				repoLine.find("input.keycheckbox").prop("checked", false);
			} else {
				repoLine.find("input.keycheckbox").prop("checked", true);
			}
			repoLine.appendTo("ul.repo-sync");
		}
		
		if(repos.length > 0) {
			$("#submit").html("<span class=\"button green\"><span>Import &gt;</span></span>")
		} else {
			$("#submit").html("<span class=\"button green\"><span>Next &gt;</span></span>")
		}
		$("#submit").prop("disabled", "");
		$("div.filter").attr("style","display: visible;");
	});
};


$(document).ready(function () {
	loadRepositories();
});