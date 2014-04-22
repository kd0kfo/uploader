#!/bin/bash

HTML_FILES="file.html index.html logon.html logout.html ls.html mkdir.html upload.html writer.html"

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
*)
	echo Unknown command: $1 >&2
	exit 1
	;;
esac
