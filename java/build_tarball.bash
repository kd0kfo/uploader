#!/bin/bash

set -e

cd core
mvn deploy

cd ../console
mvn package

CLASSPATH=$PWD/target/webfsconsole-1.0.a.jar:$(mvn -Djsse.enableSNIExtension=false -Dwebfscore.repo.path=$PWD/../corerepo dependency:build-classpath|grep -v "^\[")

cd ..
TMPDIR=$(mktemp -d)
ORIGDIR=$PWD
cd $TMPDIR
cp $(echo $CLASSPATH|tr : " ") .
tar czvf $ORIGDIR/webfs-1.0.a.tgz *

cd $ORIGDIR
rm -rf $TMPDIR
