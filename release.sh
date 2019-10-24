#!/bin/bash


increment_version() {
 local v=$1
 if [ -z $2 ]; then
    local rgx='^((?:[0-9]+\.)*)([0-9]+)($)'
 else
    local rgx='^((?:[0-9]+\.){'$(($2-1))'})([0-9]+)(\.|$)'
    for (( p=`grep -o "\."<<<".$v"|wc -l`; p<$2; p++)); do
       v+=.0; done; fi
 val=`echo -e "$v" | perl -pe 's/^.*'$rgx'.*$/$2/'`
 echo "$v" | perl -pe s/$rgx.*$'/${1}'`printf %0${#val}s $(($val+1))`/
}


echo ""
echo "Part of this script connects to the Eclipse build server over sftp to upload the onejar file."
echo "You need to provide your username and password for this to work."
echo "You may need to 'apt-get install expect'."
printf "Username: "
read username
printf "Password: "
read -s password
echo ""
echo ""
echo "Connecting to SFTP server using terminal automation."

./sftp-test.expect $username $password
RET=$?
if [ $RET -eq 91 ]
then
  echo "Could not connect to server!"
  exit 1
fi

if [ $RET -eq 92 ]
then
  echo "Wrong username or password!"
  exit 1
fi

if [ $RET -eq 93 ]
then
  echo "Expected path was not found on this server!"
  exit 1
fi



if [ $RET -eq 0 ]
then
  echo "Username and password are correct"
else
  echo "Unknown error connection to sftp server"
  exit 1
fi


echo ""
echo "This script will stop if an unhandled error occurs";
echo "Do not change any files in this directory while the script is running!"
set -e -o pipefail


read -p "Start the release process (y/n)?" choice
case "${choice}" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exiting"; exit;;
esac


if  !  mvn -v | grep -q "Java version: 1.8."; then
  echo "";
  echo "You need to use Java 8!";
  echo "mvn -v";
  echo "";
  exit 1;
fi


# check that we are on master
if  ! git status --porcelain --branch | grep -q "## master...origin/master"; then
  echo""
  echo "You need to be on master!";
  echo "git checkout master";
  echo "";
  exit 1;
fi

echo "Running git pull to make sure we are up to date"
git pull


# check that we are not ahead or behind
if  ! [[ $(git status --porcelain -u no  --branch) == "## master...origin/master" ]]; then
    echo "";
    echo "There is something wrong with your git. It seems you are not up to date with master. Run git status";
    exit 1;
fi

# check that there are no uncomitted or untracked files
if  ! [[ `git status --porcelain` == "" ]]; then
    echo "";
    echo "There are uncomitted or untracked files! Commit, delete or unstage files. Run git status for more info.";
    exit 1;
fi

echo "Running mvn clean";
mvn clean;

MVN_CURRENT_SNAPSHOT_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

MVN_VERSION_RELEASE="${MVN_CURRENT_SNAPSHOT_VERSION/-SNAPSHOT/}"

MVN_NEXT_SNAPSHOT_VERSION="$(increment_version $MVN_VERSION_RELEASE 3)-SNAPSHOT"

echo "";
echo "Your current maven snapshot version is: ${MVN_CURRENT_SNAPSHOT_VERSION}"
echo "Your maven release version will be: ${MVN_VERSION_RELEASE}"
echo "Your next maven snapshot version will be: ${MVN_NEXT_SNAPSHOT_VERSION}"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# set maven version
mvn versions:set -DnewVersion=${MVN_VERSION_RELEASE}

# set the MVN_VERSION_RELEASE version again just to be on the safe side
MVN_VERSION_RELEASE=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

# find out a way to test that we set the correct version!

#Remove backup files. Finally, commit the version number changes:
mvn versions:commit
mvn -P compliance versions:commit


BRANCH="releases/${MVN_VERSION_RELEASE}"

# delete old release branch if it exits
if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  git branch --delete --force "${BRANCH}" &>/dev/null
fi

# checkout branch for release, commit this maven version and tag commit
git checkout -b ${BRANCH}
git commit -s -a -m "release ${MVN_VERSION_RELEASE}"
git tag "${MVN_VERSION_RELEASE}"

echo "";
read -p "Push tag to github - this will start the Jenkins release (y/n)?" choice
case "${choice}" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exiting"; exit;;
esac

# push tag (only tag, not branch)
git push origin "${MVN_VERSION_RELEASE}"

echo "";
echo "One-jar build takes several minutes"
read -n 1 -s -r -p "Press any key to continue to one-jar build (ctrl+c to cancel)"; printf "\n\n";

# build one jar
mvn -Passembly clean install -DskipTests

echo "Starting utomated upload with sftp. Timeout is set to 1 hour!"

./sftp-onejar-upload.expect $username $password $MVN_VERSION_RELEASE

echo "";
echo "Upload complete";
echo "";

# Cleanup
git checkout master
mvn clean
git branch --delete --force "${BRANCH}" &>/dev/null


# Set a new SNAPSHOT version
echo "";
echo "Setting the next snapshot version to: ${MVN_NEXT_SNAPSHOT_VERSION}"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";


# set maven version
mvn versions:set -DnewVersion=${MVN_NEXT_SNAPSHOT_VERSION}

#Remove backup files. Finally, commit the version number changes:
mvn versions:commit
mvn -P compliance versions:commit

echo "";
echo "Committing the new version to git"
git commit -s -a -m "next development iteration: ${MVN_NEXT_SNAPSHOT_VERSION}"
echo "Pushing the new version to github"
git push

echo "";
echo "Preparing a merge branch to merge into develop"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";


git checkout develop
git pull

MVN_VERSION_DEVELOP=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

git checkout master

git checkout -b "merge_master_into_develop_after_release_${MVN_VERSION_RELEASE}"
mvn versions:set -DnewVersion=${MVN_VERSION_DEVELOP}
mvn versions:commit
mvn -P compliance versions:commit
git commit -s -a -m "set correct version"
git push --set-upstream origin "merge_master_into_develop_after_release_${MVN_VERSION_RELEASE}"


echo "Go got Github and create a new PR"
echo "You want to merge 'merge_master_into_develop_after_release_${MVN_VERSION_RELEASE}' into develop"
echo "When you have created the PR you can press any key to continue. It's ok to merge the PR later, so wait for the Jenkins tests to finish."
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

git checkout master
mvn clean install -DskipTests



echo "DONE!"



