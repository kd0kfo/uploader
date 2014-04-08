#!/bin/sh

PREFIX=$1
USERNAME=$2
find . -type f | while read line;do
	FILEPATH=$PREFIX$(echo $line|sed 's/^\.//g')
	echo "insert or ignore into filemetadata (id, filepath) values (NULL, '$FILEPATH');"
	echo "insert into fileacls select NULL, filemetadata.id, '$USERNAME', 6 from filemetadata where filemetadata.filepath = '$FILEPATH';"
done
