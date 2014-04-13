#!/bin/sh

set -e

GRADLEFILE=app/build.gradle

if [ "$1" = "" ]; then
	CURRENT_RELEASE_BRANCH=`git branch | grep '^*' | grep release | sed -e 's/.*release\///'`
	if [ "$CURRENT_RELEASE_BRANCH" = "" ]; then
		echo "Usage: $0 <version-number>"
		exit
	fi
	RELEASE=$CURRENT_RELEASE_BRANCH
else
	RELEASE=$1
fi

VERSIONCODE=$(grep versionCode $GRADLEFILE | sed -e 's/versionCode//;s/^[ \t]*//;')
VERSIONCODE=$[VERSIONCODE + 1]

sed -i "" -e "s/versionName.*/versionName \"$RELEASE\"/; s/versionCode.*/versionCode $VERSIONCODE/;" $GRADLEFILE
