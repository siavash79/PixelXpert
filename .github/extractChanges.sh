#!/bin/bash

  # reset the file - most likely not needd
  rm -f changeLog.md
  touch changeLog.md

  echo $(pwd)

  #find the last time we made a changelog
  LASTUPDATE=$(git log -100 | grep -B 4 "Version update: Release" | grep "commit" -m 1 | cut -d " " -f 2)
  echo $LASTUPDATE
  #find commits since - starting with the magic phrase
  COMMITS=$(git rev-list $LASTUPDATE..HEAD --grep "^CHANGELOG: ")
  echo $COMMITS
  #separator is newline
  IFS=$'\n'
  for COMMIT in $COMMITS
  do
    COMMITMSGS=$(git show $COMMIT --pretty=format:"%s" | grep "^CHANGELOG: " | tr -d '\0')
      for LINE in $COMMITMSGS
      do
        echo "////"$LINE"////"
        #save in the temp file to be used by next script
        echo "- "${LINE##*CHANGELOG: } >> changeLog.md
        echo "----"${LINE##*CHANGELOG: }"----"
        cat changeLog.md

      done
  done