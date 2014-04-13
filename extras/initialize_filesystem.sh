#!/bin/sh
#
# Produces a SQL script that nitializes the filesystem metadata database.
# Arguments: <Filesystem prefix> [Default File Owner [Output file]]
#
# Default File Owner: root
# Default Output File: init.sql
#
# Note: Previous metadata will be removed!
#

PREFIX=$1
USERNAME=$2
OUTPUT=$3

if [[ -z $USERNAME ]];then
    USERNAME=root
fi

if [[ -z $OUTPUT ]];then
    OUTPUT=init.sql
fi

NOW=$(date)
echo "--Metadata Initialization for $PREFIX run on $NOW"  > $OUTPUT
echo "--Remove existing metadata" >> $OUTPUT
for TABLE in filerevisions filemetadata fileacls filecheckouts fileshares;do
    echo "delete from $TABLE;" >> $OUTPUT
done

echo "" >> $OUTPUT
echo "--File Metadata" >> $OUTPUT
find . | while read line;do
	FILEPATH=$PREFIX$(echo $line|sed 's/^\.//g')
	echo "insert into filemetadata (id, filepath, owner) values (NULL, '$FILEPATH', '$USERNAME');" >> $OUTPUT
	echo "insert into fileacls select NULL, filemetadata.id, '$USERNAME', 6 from filemetadata where filemetadata.filepath = '$FILEPATH';" >> $OUTPUT
done
