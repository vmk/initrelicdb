#!/bin/bash
# $1 = project_id
# $2 = dir to list
# $3 = srm base url
S=0
ls "$2" > filelist.tmp
echo id,filename,localfs,srmurl > "$1"_relic.list.csv
cat filelist.tmp | while read line
do
        if [ -f $2$line ]
        then
                echo "$1"_"$line","$line","$2""$line","$3""$line" >> "$1"_relic.list.csv
        fi
done
rm filelist.tmp
./initrelicdb.sh -r "$1"_relic.list.csv