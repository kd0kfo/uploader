#!/bin/sh

if [[ -z $UPLOADER_HOME ]];then
	UPLOADER_HOME=$PWD
fi

CLASSPATH="$UPLOADER_HOME/build/jar/webfsdesktop-1.0.a.jar"

for lib in $UPLOADER_HOME/lib/*.jar;do
	CLASSPATH=$lib:$CLASSPATH
done
CLASSPATH=${CLASSPATH}

java -cp $CLASSPATH com.davecoss.uploader.utils.UploadWriter $@
