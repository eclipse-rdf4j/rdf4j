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
echo "The release script requires several external command line tools:"
echo " - git"
echo " - mvn"
echo " - hub (https://hub.github.com/)"
echo " - xmlllint (http://xmlsoft.org/xmllint.html)"

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

# replace "SNAPSHOT" with ""
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
echo "Pushing release branch to github"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# push release branch and tag
git push -u origin ${BRANCH}
git push origin "${MVN_VERSION_RELEASE}"

echo "";
echo "You need to tell Jenkins to start the release deployment processes, for SDK and maven artifacts"
echo "- SDK deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-sdk/ "
echo "- Maven deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-ossrh/ "
echo "(if you are on linux or windows, remember to use CTRL+SHIFT+C to copy)."
echo "Log in, then choose 'Build with Parameters' and type in ${MVN_VERSION_RELEASE}"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

# Cleanup
mvn clean

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
echo "About to create PR"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";
echo "";

echo "Creating pull request to merge release branch back into master"
hub pull-request -f --message="next development iteration: ${MVN_NEXT_SNAPSHOT_VERSION}" --message="Merge using merge commit rather than rebase"

echo "";
echo "Preparing a merge-branch to merge into develop"
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";


git checkout develop
git pull

MVN_VERSION_DEVELOP=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)

git checkout ${BRANCH}

git checkout -b "merge_master_into_develop_after_release_${MVN_VERSION_RELEASE}"
mvn versions:set -DnewVersion=${MVN_VERSION_DEVELOP}
mvn versions:commit
mvn -P compliance versions:commit
git commit -s -a -m "set correct version"
git push --set-upstream origin "merge_master_into_develop_after_release_${MVN_VERSION_RELEASE}"

echo "Creating pull request to merge the merge-branch into develop"
hub pull-request -f -b develop --message="sync develop branch after release ${MVN_VERSION_RELEASE}" --message="Merge using merge commit rather than rebase"
echo "It's ok to merge this PR later, so wait for the Jenkins tests to finish."
read -n 1 -s -r -p "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

git checkout $MVN_VERSION_RELEASE

echo "DONE!"

# the news file on github should be 302 if the release is 3.0.2, so replace "." twice
NEWS_FILE_NAME=$MVN_VERSION_RELEASE
NEWS_FILE_NAME=${NEWS_FILE_NAME/./}
NEWS_FILE_NAME=${NEWS_FILE_NAME/./}

echo ""
echo "You will now want to inform the community about the new release!"
echo " - Check if all recently closed issues have the correct milestone: https://github.com/eclipse/rdf4j/issues?q=is%3Aissue+is%3Aclosed+"
echo " - Create a new milestone for ${MVN_NEXT_SNAPSHOT_VERSION/-SNAPSHOT/} : https://github.com/eclipse/rdf4j/milestones/new"
echo " - Close the ${MVN_VERSION_RELEASE} milestone: https://github.com/eclipse/rdf4j/milestones"
echo "     - Make sure that all issues in the milestone are closed, or move them to the next milestone"
echo "     - Go to the milestone, click the 'closed' tab and copy the link for later"
echo " - Edit the following file https://github.com/eclipse/rdf4j-doc/blob/master/site/content/release-notes/index.md"
echo " - Edit the following file https://github.com/eclipse/rdf4j-doc/blob/master/site/content/download/_index.md"
echo " - Go to https://github.com/eclipse/rdf4j-doc/tree/master/site/content/news and create rdf4j-${NEWS_FILE_NAME}.md"
echo " - Post to Google Groups: https://groups.google.com/forum/#!forum/rdf4j-users"
echo "     - Good example: https://groups.google.com/forum/#!topic/rdf4j-users/isrC7qdhplY"
echo " - Upload the javadocs by adding them to rdf4j-doc project: site/static/javadoc/${MVN_VERSION_RELEASE}"
echo "     - Aggregated javadoc can be found in target/site/apidocs or in the SDK zip file"
echo "     - Make sure to also replace the site/static/javadoc/latest directory with a copy (don't use a symlink)"

