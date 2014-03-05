function loadPageVar (sVar) {
  return decodeURI(window.location.search.replace(new RegExp("^(?:.*[&\\?]" + encodeURI(sVar).replace(/[\.\+\*]/g, "\\$&") + "(?:\\=([^&]*))?)?.*$", "i"), "$1"));
}

function hoverin() {
    $(this).css("font-style", "italic").css("color", "red");
}

function hoverout() {
    $(this).css("font-style", "normal").css("color", "black");
}

function get_server_info() {
	var server_info = $.ajax({ 
        url: 'info.php',
        async: false
     }).responseText;
  return $.parseJSON(server_info);
}

function updateDir(thedirname, username, sessionkey) {
    window.document.title = thedirname;
    $("#heading").text("Contents of " + thedirname);
    var signature = hash(thedirname, sessionkey);
    $.getJSON("ls.php", {"filename": thedirname, "username": username, "signature": signature},
    	function(data) {
	    $("#contents").text("");
	    if(data['status'] && data['status'] != 0) {
	    	$("#contents").text("ERROR: " + data['message']);
	    }
	    else if(data['type'] == "f") {
	    	$("#contents").text("File: " + data['name']);
	    }
	    else if(data['type'] == "d") {
			if(thedirname != "/") {
			    var parentdir = $("<p/>", {class: "d"});
			    parentdir.text("Up to " + data["parent"]);
			    parentdir.click(function() {updateDir(data["parent"], username, sessionkey);});
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
				    dirent.click(function() {updateDir($(this).text().trim(), username, sessionkey);});
				} else {
				    dirent.click(function() {window.open(localStorage['contentdir'] + text, thedirname);});
				}
				dirent.hover(hoverin, hoverout);
				$("#contents").append(dirent);
			});
	    }
	});
}

function hash(data, key) {
	var hash = CryptoJS.HmacSHA256(data, key);
	return CryptoJS.enc.Base64.stringify(hash);
}

