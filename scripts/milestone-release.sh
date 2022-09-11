#!/bin/bash

cd ..

echo ""
echo "The release script requires several external command line tools:"
echo " - git"
echo " - mvn"
echo " - gh (the GitHub CLI, see https://github.com/cli/cli)"
echo " - xmllint (http://xmlsoft.org/xmllint.html)"

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

# check that we are on main or develop
if  ! git status --porcelain --branch | grep -q "## main...origin/main"; then
  if  ! git status --porcelain --branch | grep -q "## develop...origin/develop"; then
    echo""
    echo "You need to be on main or develop!";
    echo "";
    exit 1;
  fi
fi

ORIGINAL_BRANCH=""
if  git status --porcelain --branch | grep -q "## main...origin/main"; then
  ORIGINAL_BRANCH="main";
fi
if  git status --porcelain --branch | grep -q "## develop...origin/develop"; then
  ORIGINAL_BRANCH="develop";
fi

echo "Running git pull to make sure we are up to date"
git checkout develop
git pull

if  ! git status --porcelain --branch | grep -q "## develop...origin/develop"; then
  echo""
  echo "There is something wrong with your git. It seems you are not up to date with develop. Run git status";
  echo "";
  exit 1;
fi

git checkout main
git pull

if  ! git status --porcelain --branch | grep -q "## main...origin/main"; then
  echo""
  echo "There is something wrong with your git. It seems you are not up to date with main. Run git status";
  echo "";
  exit 1;
fi

git checkout "${ORIGINAL_BRANCH}"


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

echo "";
echo "Your current maven snapshot version is: '${MVN_CURRENT_SNAPSHOT_VERSION}'"
echo ""
echo "What is the version you would like to publish?"
read -rp "Version: " MVN_VERSION_RELEASE
echo ""
echo "Your maven release version will be: '${MVN_VERSION_RELEASE}'"
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
echo "Pushing release branch and tag to github, then deleting branch."
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# push release branch and tag
git push -u origin "${BRANCH}"
git push origin "${MVN_VERSION_RELEASE}"

# deleting the branch (local and remote) since we don't intend to merge the branch and it's enough that we leave the git tag
git checkout "${MVN_VERSION_RELEASE}"
git branch -d "${BRANCH}"
git push origin --delete "${BRANCH}"

echo "";
echo "You need to tell Jenkins to start the release deployment processes, for SDK and maven artifacts"
echo "- SDK deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-sdk/ "
echo "- Maven deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-ossrh/ "
echo "(if you are on linux or windows, remember to use CTRL+SHIFT+C to copy)."
echo "Log in, then choose 'Build with Parameters' and type in ${MVN_VERSION_RELEASE}"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

mvn clean

echo "Build javadocs"
read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

git checkout "${MVN_VERSION_RELEASE}"
mvn clean
mvn compile -P quick
mvn install -DskipTests -B
mvn package -Passembly -DskipTests -B

git checkout main
RELEASE_NOTES_BRANCH="${MVN_VERSION_RELEASE}-release-notes"
git checkout -b "${RELEASE_NOTES_BRANCH}"

tar -cvzf "site/static/javadoc/${MVN_VERSION_RELEASE}.tgz" -C target/site/apidocs .
git add --all
git commit -s -a -m "javadocs for ${MVN_VERSION_RELEASE}"
git push --set-upstream origin "${RELEASE_NOTES_BRANCH}"
gh pr create -B main --title "${RELEASE_NOTES_BRANCH}" --body "Javadocs, release-notes and news item for ${MVN_VERSION_RELEASE}"

echo "Javadocs are in git branch ${RELEASE_NOTES_BRANCH}"

git checkout "${ORIGINAL_BRANCH}"
mvn clean

cd scripts

echo ""
echo "DONE!"

echo ""
echo "You will now want to inform the community about the new milestone build!"
echo " - Check if all recently completed issues have the correct milestone: https://github.com/eclipse/rdf4j/projects/19"
echo " - For issues closed in the current milestone, those issues need to be tagged with the RDF4J milestone number (use Github labels M1, M2 or M3)"
echo "Remember that milestone builds are not releases!"

echo ""
echo "To generate the news item and release-notes you will want to run the following command:"
echo "./release-notes.sh ${MVN_VERSION_RELEASE} empty.md milestone-news-item.md ${RELEASE_NOTES_BRANCH}"
