#!/bin/sh

echo export CLASSPATH=$PWD/target/webfsconsole-1.0.a.jar:$(mvn -Djsse.enableSNIExtension=false -Dwebfscore.repo.path=/path/to/localrepo dependency:build-classpath|grep -v "^\[") > classpath

echo Wrote the following to $PWD/classpath
cat classpath
