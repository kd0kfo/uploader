function loadPageVar (sVar) {
  return decodeURI(window.location.search.replace(new RegExp("^(?:.*[&\\?]" + encodeURI(sVar).replace(/[\.\+\*]/g, "\\$&") + "(?:\\=([^&]*))?)?.*$", "i"), "$1"));
}

function hoverin() {
    $(this).css("font-style", "italic").css("color", "red");
}

function hoverout() {
    $(this).css("font-style", "normal").css("color", "black");
}

function updateDir(thedirname) {
    window.document.title = thedirname;
    $("#heading").text("Contents of " + thedirname);
    $.getJSON("ls.php", {"dir": thedirname}, function(data) {
	    $("#contents").text("");
	    if(data['error']) {
	    	$("#contents").text("ERROR: " + data['error']);
	    }
	    else if(data['type'] == "f") {
	    	$("#contents").text("File: " + data['name']);
	    }
	    else if(data['type'] == "d") {
			if(thedirname != "/") {
			    var parentdir = $("<p/>", {class: "d"});
			    parentdir.text("Up to " + data["parent"]);
			    parentdir.click(function() {updateDir(data["parent"]);});
			    parentdir.hover(hoverin,
					    hoverout);
			    $("#contents").append(parentdir);
			}
			var debug = $("#debug");
			$.each(data['dirents'], function(key, val) {
				if(val["name"] == thedirname) {
				    return 1;
				}
				var text = thedirname;
				if(thedirname != "/") {
				    text = text + "/";
				}
				text = text + val["name"];
				var dirent = $("<p/>", {class: val["type"]});
				dirent.text(text);
				if(val["type"] == "d") {
				    dirent.click(function() {updateDir($(this).text().trim());});
				} else {
				    dirent.click(function() {window.open(localStorage['contentdir'] + text, thedirname);});
				}
				dirent.hover(hoverin, hoverout);
	
				$("#contents").append(dirent);
			});
	    }
	});
}
