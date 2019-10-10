#!/bin/sh

echo "Enabling fail on error";
set -e -o pipefail


read -p "Start the release process (y/n)?" choice
case "$choice" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exitting"; exit;;
esac


 if  ! git status --porcelain --branch | grep -q "## master...origin/master"; then
         echo "You need to be on master!";
         echo "git checkout master";
         echo "";
        exit 1;
 fi

echo "Running git pull to make sure we are up to date"
git pull


if  ! [[ `git status --porcelain -u no  --branch` == "## master...origin/master" ]]; then
    echo "There is something wrong with your git. It seems you are not up to date with master. Run git status";
    exit 1;
fi

if  ! [[ `git status --porcelain` == "" ]]; then
    echo "There are uncomitted or untracked files! Commit, delete or unstage files. Run git status for more info.";
    exit 1;
fi

# set maven version, user will be prompted
mvn versions:set

MVN_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

echo "Your maven version is: ${MVN_VERSION}"

#Remove backup files. Finally, commit the version number changes:
mvn versions:commit

git checkout -b releases/${MVN_VERSION}
git commit -s -a -m "release ${MVN_VERSION}"

git tag ${MVN_VERSION}

read -p "Push tag (y/n)?" choice
case "$choice" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exitting"; exit;;
esac

git push origin ${MVN_VERSION}








