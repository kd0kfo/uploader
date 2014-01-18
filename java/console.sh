#!/bin/sh

CLASSPATH=""

for lib in lib/*.jar;do
	CLASSPATH=$lib:$CLASSPATH
done
CLASSPATH=$CLASSPATH:uploader-1.0.b.jar

java -cp $CLASSPATH com.davecoss.uploader.utils.UploaderConsole $@
