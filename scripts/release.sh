#!/bin/bash

cd ..

increment_version() {
 local v=$1
 if [ -z $2 ]; then
    local rgx='^((?:[0-9]+\.)*)([0-9]+)($)'
 else
    local rgx='^((?:[0-9]+\.){'$(($2-1))'})([0-9]+)(\.|$)'
    for (( p=$(grep -o "\."<<<".$v"|wc -l); p<$2; p++)); do
       v+=.0; done; fi
 val=$(echo -e "$v" | perl -pe 's/^.*'$rgx'.*$/$2/')
 echo "$v" | perl -pe s/$rgx.*$'/${1}'$(printf %0${#val}s $(($val+1)))/
}

echo ""
echo "The release script requires several external command line tools:"
echo " - git"
echo " - mvn"
echo " - gh (the GitHub CLI, see https://github.com/cli/cli)"
echo " - xmlllint (http://xmlsoft.org/xmllint.html)"

echo ""
echo "This script will stop if an unhandled error occurs";
echo "Do not change any files in this directory while the script is running!"
set -e -o pipefail


read -rp "Start the release process (y/n)?" choice
case "${choice}" in
  y|Y ) echo "";;
  n|N ) exit;;
  * ) echo "unknown response, exiting"; exit;;
esac

# verify required tools are installed
if ! command -v git &> /dev/null; then
    echo "";
    echo "git command not found!";
    echo "";
    exit 1;
fi

if ! command -v mvn &> /dev/null; then
    echo "";
    echo "mvn command not found!";
    echo  "See https://maven.apache.org/";
    echo "";
    exit 1;
fi

if ! command -v gh &> /dev/null; then
    echo "";
    echo "gh command not found!";
    echo  "See https://github.com/cli/cli";
    echo "";
    exit 1;
fi

if ! command -v xmllint &> /dev/null; then
    echo "";
    echo "xmllint command not found!";
    echo "See http://xmlsoft.org/xmllint.html"
    echo "";
    exit 1;
fi

# check Java version
if  !  mvn -v | grep -q "Java version: 1.8."; then
  echo "";
  echo "Java 1.8 expected but not detected";
  read -rp "Continue (y/n)?" choice
  case "${choice}" in
      y|Y ) echo "";;
      n|N ) exit;;
      * ) echo "unknown response, exiting"; exit;;
  esac
fi

# check that we are on main
if  ! git status --porcelain --branch | grep -q "## main...origin/main"; then
  echo""
  echo "You need to be on main!";
  echo "git checkout main";
  echo "";
  exit 1;
fi

echo "Running git pull to make sure we are up to date"
git pull

# check that we are not ahead or behind
if  ! [[ $(git status --porcelain -u no  --branch) == "## main...origin/main" ]]; then
    echo "";
    echo "There is something wrong with your git. It seems you are not up to date with main. Run git status";
    exit 1;
fi

# check that there are no uncomitted or untracked files
if  ! [[ $(git status --porcelain) == "" ]]; then
    echo "";
    echo "There are uncomitted or untracked files! Commit, delete or unstage files. Run git status for more info.";
    exit 1;
fi

# check that we have push access
if ! git push --dry-run > /dev/null 2>&1; then
    echo "";
    echo "Could not push to the repository! Check that you have sufficient access rights.";
    echo "";
    exit 1;
fi

echo "Running mvn clean";
mvn clean;

MVN_CURRENT_SNAPSHOT_VERSION=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

# replace "SNAPSHOT" with ""
MVN_VERSION_RELEASE="${MVN_CURRENT_SNAPSHOT_VERSION/-SNAPSHOT/}"

MVN_NEXT_SNAPSHOT_VERSION="$(increment_version "$MVN_VERSION_RELEASE" 3)-SNAPSHOT"

echo "";
echo "Your current maven snapshot version is: '${MVN_CURRENT_SNAPSHOT_VERSION}'"
echo "Your maven release version will be: '${MVN_VERSION_RELEASE}'"
echo "Your next maven snapshot version will be: '${MVN_NEXT_SNAPSHOT_VERSION}'"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# set maven version
mvn versions:set -DnewVersion="${MVN_VERSION_RELEASE}"

# set the MVN_VERSION_RELEASE version again just to be on the safe side
MVN_VERSION_RELEASE=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

# find out a way to test that we set the correct version!

#Remove backup files. Finally, commit the version number changes:
mvn versions:commit


BRANCH="releases/${MVN_VERSION_RELEASE}"

# delete old release branch if it exits
if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  git branch --delete --force "${BRANCH}" &>/dev/null
fi

# checkout branch for release, commit this maven version and tag commit
git checkout -b "${BRANCH}"
git commit -s -a -m "release ${MVN_VERSION_RELEASE}"
git tag "${MVN_VERSION_RELEASE}"

echo "";
echo "Pushing release branch to github"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# push release branch and tag
git push -u origin "${BRANCH}"
git push origin "${MVN_VERSION_RELEASE}"

echo "";
echo "You need to tell Jenkins to start the release deployment processes, for SDK and maven artifacts"
echo "- SDK deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-sdk/ "
echo "- Maven deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-ossrh/ "
echo "(if you are on linux or windows, remember to use CTRL+SHIFT+C to copy)."
echo "Log in, then choose 'Build with Parameters' and type in ${MVN_VERSION_RELEASE}"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# Cleanup
mvn clean

# Set a new SNAPSHOT version
echo "";
echo "Setting the next snapshot version to: ${MVN_NEXT_SNAPSHOT_VERSION}"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";


# set maven version
mvn versions:set -DnewVersion="${MVN_NEXT_SNAPSHOT_VERSION}"

#Remove backup files. Finally, commit the version number changes:
mvn versions:commit

echo "";
echo "Committing the new version to git"
git commit -s -a -m "next development iteration: ${MVN_NEXT_SNAPSHOT_VERSION}"
echo "Pushing the new version to github"
git push

echo "";
echo "About to create PR"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";
echo "";

echo "Creating pull request to merge release branch back into main"
gh pr create --title "next development iteration: ${MVN_NEXT_SNAPSHOT_VERSION}" --body "Merge using merge commit rather than rebase"

echo "";
echo "Preparing a merge-branch to merge into develop"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";


git checkout develop
git pull

MVN_VERSION_DEVELOP=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

git checkout "${BRANCH}"

git checkout -b "merge_main_into_develop_after_release_${MVN_VERSION_RELEASE}"
mvn versions:set -DnewVersion="${MVN_VERSION_DEVELOP}"
mvn versions:commit
git commit -s -a -m "set correct version"
git push --set-upstream origin "merge_main_into_develop_after_release_${MVN_VERSION_RELEASE}"

echo "Creating pull request to merge the merge-branch into develop"
gh pr create -B develop --title "sync develop branch after release ${MVN_VERSION_RELEASE}" --body "Merge using merge commit rather than rebase"
echo "It's ok to merge this PR later, so wait for the Jenkins tests to finish."
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

mvn clean

echo "Build javadocs"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

git checkout "${MVN_VERSION_RELEASE}"
mvn clean
mvn package -Passembly -DskipTests

git checkout main
RELEASE_NOTES_BRANCH="${MVN_VERSION_RELEASE}-release-notes"
git checkout -b "${RELEASE_NOTES_BRANCH}"

tar -cvzf "site/static/javadoc/${MVN_VERSION_RELEASE}.tgz" -C target/site/apidocs .
cp -f "site/static/javadoc/${MVN_VERSION_RELEASE}.tgz" "site/static/javadoc/latest.tgz"
git add --all
git commit -s -a -m "javadocs for ${MVN_VERSION_RELEASE}"
git push --set-upstream origin "${RELEASE_NOTES_BRANCH}"
gh pr create -B main --title "${RELEASE_NOTES_BRANCH}" --body "Javadocs, release-notes and news item for ${MVN_VERSION_RELEASE}"

echo "Javadocs are in git branch ${RELEASE_NOTES_BRANCH}"

git checkout main
mvn clean

cd scripts

echo ""
echo "DONE!"



echo ""
echo "You will now want to inform the community about the new release!"
echo " - Check if all recently completed issues have the correct milestone: https://github.com/eclipse/rdf4j/projects/19"
echo " - Create a new milestone for ${MVN_NEXT_SNAPSHOT_VERSION/-SNAPSHOT/} : https://github.com/eclipse/rdf4j/milestones/new"
echo " - Close the ${MVN_VERSION_RELEASE} milestone: https://github.com/eclipse/rdf4j/milestones"
echo "     - Make sure that all issues in the milestone are closed, or move them to the next milestone"

echo ""
echo "To generate the news item and release-notes you will want to run the following command:"
echo "./release-notes.sh ${MVN_VERSION_RELEASE} patch-release-notes.md patch-news-item.md ${RELEASE_NOTES_BRANCH}"
