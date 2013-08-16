String.prototype.replaceAll = function(str1, str2, ignore)
{
   return this.replace(new RegExp(str1.replace(/([\,\!\\\^\$\{\}\[\]\(\)\.\*\+\?\|\<\>\-\&])/g, function(c){return "\\" + c;}), "g"+(ignore?"i":"")), str2);
};

$(function() {
	var refreshInterval;

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
				$("#submit").prop("disabled", "");
				$("#submit").html("<span>Next &gt;</span>")
				clearInterval(refreshInterval);
			}
		});
	}
	
	$("#submit").click(function() {
		$.ajax({
			type : "POST",
			url : "repositories-clone.gh",
			data : $("#repositories").serialize(),
			success : function() {
				$(".status").each(function() {
					this.style.visibility = "visible";
				});
				refresh();
				refreshInterval = setInterval(refresh, 2000);
			}
		});
		$("#submit").prop("disabled", "disabled");

		return false;
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

$(document).ready(function () {
	$.post('repositories-list.gh', function(data) {
		$("div.loading").attr("style","display: none;");
		$("#submit").prop("disabled", "");
		
		var repos = eval('(' + data + ')');
		
		for (var i=0; i<repos.length; i++) {
			var repoParts = repos[i].split("/");
			var repoTemplate = $("#repo-template").html();
			repoTemplate = repoTemplate.replaceAll("#repo-index#",i);
			repoTemplate = repoTemplate.replaceAll("#repo-name#", repoParts[1]);
			repoTemplate = repoTemplate.replaceAll("#repo-organisation#", repoParts[0]);
			
			$("<li>" + repoTemplate + "</li>").prependTo("ul.repo-sync");
		}
	});
});