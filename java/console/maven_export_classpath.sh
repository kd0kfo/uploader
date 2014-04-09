#!/bin/sh

echo export CLASSPATH=$(mvn -Djsse.enableSNIExtension=false -Dwebfscore.repo.path=/path/to/localrepo dependency:build-classpath|grep -v "^\[") > classpath

echo Wrote the following to $PWD/classpath
cat classpath
