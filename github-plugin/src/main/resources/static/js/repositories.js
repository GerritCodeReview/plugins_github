String.prototype.replaceAll = function(str1, str2, ignore)
{
   return this.replace(new RegExp(str1.replace(/([\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g, function(c){return "\\" + c;}), "g"+(ignore?"i":"")), str2);
};

$(function() {
	var refreshInterval;

	var completed = false;

	var refresh = function() {
		$.post('repositories-clone-status.gh', function(data) {
			var repos = eval('(' + data + ')');
			var allCompleted = true;
			if(repos.length == 0) {
				return;
			}
			
			for (var i=0; i<repos.length; i++) {
				var id = repos[i].index;
				var value = repos[i].value;
				var status = repos[i].status;
				$("#status_" + id).attr("class", "status " + status);
				$("#repo_" + id).text(value);
				if(status == 'sync') {
					allCompleted = false;
				}
			}
			
			if(allCompleted && repos.length > 0) {
				completed = true;
				$("#submit").prop("disabled", "");
				$("#submit").html("<span class=\"button\"><span>Next &gt;</span></span>")
				clearInterval(refreshInterval);
			}
		});
	}
	
	$("select#organisation").change(function() {
		loadRepositories();
		$("input#filter").val("");
		completed = false;
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
			if(matched && numRepos < maxItems) {
				$(this).attr("style","display: block;");
				$(this).find("input.keycheckbox").attr("checked", true);
				numRepos++;
			} else {
				$(this).find("input.keycheckbox").attr("checked", false);
				$(this).attr("style","display: none;");
			}
		});
	}
	
	$("button#search").click(function() {
		loadRepositories();
		completed = false;
		return false;
	});
	
	$("#submit").click(function() {
		var destination;

		if(completed) {
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
						this.style.display = "none";
					});
					refresh();
					refreshInterval = setInterval(refresh, 2000);
				}
			});
			$("#submit").prop("disabled", "disabled");
			return false;
		}
	});
	
	$("#cancel").click(function() {
		$.ajax({
			type : "POST",
			url : "repositories-clone-cancel.gh",
			success: function() {
				refresh();
			}
		});
	});
});

var loadRepositories = function () {
	$("div.loading").attr("style","display: visible;");
	$("ul.repo-sync").empty();
	
	var organisation = $("select#organisation option:selected").val();
	var filter = $("input#filter").val();
	$.post('repositories-list.gh', 
		{ "organisation": organisation, "filter": filter },
		function(data) {
		$("div.loading").attr("style","display: none;");
		$("#submit").prop("disabled", "");
		
		var repos = eval('(' + data + ')');
		var maxItems = $("select#numitems").val();
		
		for (var i=0; i<repos.length; i++) {
			var repo = repos[i];
			var repoTemplate = $("#repo-template").html();
			repoTemplate = repoTemplate.replaceAll("#repo-index#",i);
			repoTemplate = repoTemplate.replaceAll("#repo-name#", repo.name);
			repoTemplate = repoTemplate.replaceAll("#repo-description#", repo.description);
			repoTemplate = repoTemplate.replaceAll("#repo-organisation#", repo.organisation);
			var repoLine = $("<li>" + repoTemplate + "</li>");
			if(i >= maxItems) {
				repoLine.attr("style","display:none;");
				repoLine.find("input.keycheckbox").attr("checked", false);
			}
			repoLine.appendTo("ul.repo-sync");
		}
		$("#submit").html("<span class=\"button\"><span>Import &gt;</span></span>")
	});
};


$(document).ready(function () {
	loadRepositories();
});