#!/bin/bash

IFS=$'\n'

rm -f changeLog.md
LASTUPDATE=$(git log -100 | grep -B 4 "Version update: Release" | grep "commit" -m 1 | cut -d " " -f 2)
COMMITS=$(git rev-list $LASTUPDATE..HEAD)
for COMMIT in $COMMITS
do
        FULLCOMMIT=$(git show $COMMIT | grep -zoP "CHANGELOG: .+" | tr -d '\0')
        for LINE in $FULLCOMMIT
        do
                 echo "- "${LINE##*CHANGELOG: } >> changeLog.md
        done
done
