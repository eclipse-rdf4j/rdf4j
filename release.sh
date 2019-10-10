#!/bin/bash

echo "This script will stop if and unhandled error occurs";
set -e -o pipefail


read -p "Start the release process (y/n)?" choice
case "${choice}" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exitting"; exit;;
esac


if  !  mvn -v | grep -q "Java version: 1.8."; then
         echo "You need to use Java 8!";
         echo "mvn -v";
         echo "";
        exit 1;
fi


# check that we are on master
if  ! git status --porcelain --branch | grep -q "## master...origin/master"; then
         echo "You need to be on master!";
         echo "git checkout master";
         echo "";
        exit 1;
fi

echo "Running git pull to make sure we are up to date"
git pull


# check that we are not ahead or behind
if  ! [[ `git status --porcelain -u no  --branch` == "## master...origin/master" ]]; then
    echo "There is something wrong with your git. It seems you are not up to date with master. Run git status";
    exit 1;
fi

# check that there are not uncomitted or untracked files
if  ! [[ `git status --porcelain` == "" ]]; then
    echo "There are uncomitted or untracked files! Commit, delete or unstage files. Run git status for more info.";
    exit 1;
fi

# set maven version, user will be prompted
mvn versions:set

# find the maven version of the project from the root pom.xml
MVN_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

echo "Your maven version is: ${MVN_VERSION}"

read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

#Remove backup files. Finally, commit the version number changes:
mvn versions:commit


BRANCH="releases/${MVN_VERSION}"

# delete old release branch if it exits
if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  git branch --delete --force ${BRANCH} &>/dev/null
fi

# checkout branch for release, commit this maven version and tag commit
git checkout -b bran${BRANCH}
git commit -s -a -m "release ${MVN_VERSION}"
git tag ${MVN_VERSION}

read -p "Push tag (y/n)?" choice
case "${choice}" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exitting"; exit;;
esac

# push tag (only tag, not branch)
git push origin ${MVN_VERSION}

read -n 1 -s -r -p "Press any key to continue to one-jar build (ctrl+c to cancel)"; printf "\n\n";

# build one jar
mvn -Passembly clean install -DskipTests

# todo upload to SFTP (also check sftp credentials at beginning of this script)


# Cleanup
git checkout master
mvn clean install -DskipTests
git branch --delete --force ${BRANCH} &>/dev/null


