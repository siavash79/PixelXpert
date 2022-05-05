#!/bin/bash

  s=$1
	pat='"message": *"\[changelog\]([^"]*)" *,'

    while [[ $s =~ $pat ]]; do
			MSG=${BASH_REMATCH[1]}
			if [ ${#MSG} -gt 0 ]; then
			  echo "- $MSG" >> ./workflowFiles/futureChangelog.md
			fi
			s=${s#*"${BASH_REMATCH[1]}"}
	done