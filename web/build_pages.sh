#!/bin/bash

HTML_FILES="file.html index.html logon.html logout.html ls.html mkdir.html upload.html"
PHP_FILES="auth.php base64.php checkin.php checkout.php chmod.php chown.php classes.php clean.php dbase.php delete.php googleauth.php info.php logon.php logout.php ls.php md5.php merge.php mkdir.php mv.php postdata.php share.php stat.php stream.php upload.php"
INCLUDE_FILES="includes"
JS_FILES="js"
ACE_FILES="ace"
CSS_FILES="css"
FONTS_FILES="fonts"
IMAGES_FILES="images"
SITE_FILES="parameters.yaml site_db.inc site.inc site.js"

case $1 in 
build)
	PARAMS=""
	if [[ -f parameters.yaml ]];then
		PARAMS="-p parameters.yaml"
	fi
	for HTML_FILE in $HTML_FILES;do
		./jinjacli $PARAMS -o $HTML_FILE templates/$HTML_FILE 
	done
	;;
clean)
	rm -f $HTML_FILES
	;;
deploy)
	if [[ -z $2 ]];then
		echo Missing destination directory >&2
		exit 1
	fi
	DEST=$2
	cp -v -R $HTML_FILES $PHP_FILES $INCLUDE_FILES $JS_FILES $ACE_FILES $CSS_FILES $FONTS_FILES $IMAGES_FILE $DEST/
	for SITE_FILE in $SITE_FILES;do
		if [[ ! -f $SITE_FILE ]];then
			echo Missing $SITE_FILE, copying default.
			cp $SITE_FILE.example $DEST/
		else
			cp $SITE_FILE $DEST/
		fi
	done
	;;
*)
	echo Unknown command: $1 >&2
	exit 1
	;;
esac

