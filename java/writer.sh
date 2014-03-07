#!/bin/sh

CLASSPATH="build/jar/uploader-1.0.a.jar"

for lib in lib/*.jar;do
	CLASSPATH=$lib:$CLASSPATH
done
CLASSPATH=${CLASSPATH}

java -cp $CLASSPATH com.davecoss.uploader.utils.UploadWriter $@
