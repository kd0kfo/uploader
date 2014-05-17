


function doView() {
	var filename = loadPageVar("filename");
	window.location="stream.php?" + get_signature_query(filename);
}

function doPostWithJSON(scriptname, json) {
	var result = $.post(scriptname, json);
	result.done(function(data){display_json_message(data);});
	result.fail(function(jqXHR){display_json_message(jqXHR.responseText);});
}

function doPost(scriptname) {
	var filename = loadPageVar("filename");
	var json = {
		'filename': filename,
		'signature': sign_data(filename),
		'username': localStorage['username']
	};
	doPostWithJSON(scriptname, json);
}
	
function cleanPost(scriptname) {
	var filename = loadPageVar("filename");
	var clean_filename = filename.substring(0, filename.lastIndexOf('.'));
	var result = $.post(scriptname, {"filename": clean_filename, "signature": sign_data(clean_filename), "username": localStorage['username']});
	result.done(function(data){display_json_message(data);});
	result.fail(function(jqXHR){display_json_message(jqXHR.responseText);});
}

function unsharePost() {
	var filename = loadPageVar("filename");
	var json = {
		'filename': filename,
		'signature': sign_data(filename),
		'username': localStorage['username'],
		'action': 'remove'
	};
	doPostWithJSON("share.php", json);
}

function sharePost() {
	var filename = loadPageVar("filename");
	var json = {
		'filename': filename,
		'signature': sign_data(filename),
		'username': localStorage['username'],
	};
	var result = $.post("share.php", json);
	result.done(function(data){
		var json = $.parseJSON(data);
		var path = location.href.substring(0,location.href.lastIndexOf('?'));
		path = path.substring(0,path.lastIndexOf('/'));
		var shareurl = path + "/share.php?id=" + json.message;
		$("#result").html("To share this file, use the URL: <a href=\"" + shareurl + "\">" + shareurl + "</a>");
	});
	result.fail(function(jqXHR){display_json_message(jqXHR.responseText);});
}


function doMD5() {
	var filename = loadPageVar("filename");
	var json = {
		'filename': filename,
		'signature': sign_data(filename),
		'username': localStorage['username']
	};
	var result = $.post("md5.php", json);
	result.done(function(data){
		var json = $.parseJSON(data);
		$("#result").text("MD5: " + json.md5);
	});
	result.fail(function(jqXHR){display_json_message(jqXHR.responseText);});
}

function doStat() {
	var filename = loadPageVar("filename");
	var json = {
		'filename': filename,
		'signature': sign_data(filename),
		'username': localStorage['username']
	};
	var result = $.post("stat.php", json);
	result.done(function(data){
		var json = $.parseJSON(data);
		var list = $("<p>").text("In-depth File Information:");
		list.append(json2ul(json));
		$("#result").html(list);
	});
	result.fail(function(jqXHR){display_json_message(jqXHR.responseText);});
}

function doEdit() {
	var filename = loadPageVar("filename");
	var url = 'ace/index.html?filename=' + encodeURIComponent(filename) + "&signature=" + encodeURIComponent(sign_data(filename)) + "&username=" + encodeURIComponent(localStorage['username']);
	window.location=url;
}

function doMove() {
	var filename = loadPageVar("filename");
	var dest = $("#move").val();
	var signature = sign_data(filename+dest);
	var json = {"source": filename, "destination": dest, "username": localStorage['username'], "signature": signature};
	doPostWithJSON("mv.php", json);
}
