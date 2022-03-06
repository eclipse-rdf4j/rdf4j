#!/bin/bash

MVN_VERSION_RELEASE_RAW=$1
RELEASE_NOTES_TEMPLATE=$2
NEWS_ITEM_TEMPLATE=$3
BRANCH=$4

# We handle both "1.2.3" and "1.2.3-M1"
IFS='-' read -ra MVN_VERSION_RELEASE_ARRAY <<< "${MVN_VERSION_RELEASE_RAW}"

export MVN_VERSION_RELEASE=${MVN_VERSION_RELEASE_ARRAY[0]}

IFS='M' read -ra M_ARRAY <<< "${MVN_VERSION_RELEASE_ARRAY[1]}"

# $M is the milestone build number, eg. 4 in 1.2.3-M4
export M=${M_ARRAY[1]}

if ! [[ ${BRANCH} == "" ]]; then
  git checkout "${BRANCH}"
  git pull
fi

echo ""
echo "The script requires several external command line tools:"
echo " - git"
echo " - mvn"
echo " - gh (the GitHub CLI, see https://github.com/cli/cli)"
echo " - jq (https://stedolan.github.io/jq/)"

echo ""
echo "This script will stop if an unhandled error occurs";
echo "Do not change any files in this directory while the script is running!"
echo ""
set -e -o pipefail

if [ ! -f templates/"${RELEASE_NOTES_TEMPLATE}" ]; then
    echo "File not found!"
    echo "templates/${RELEASE_NOTES_TEMPLATE}"
    exit 1;
fi


if [ ! -f templates/"${NEWS_ITEM_TEMPLATE}" ]; then
    echo "File not found!"
    echo "templates/${NEWS_ITEM_TEMPLATE}"
    exit 1;
fi

# All builds except milestone builds need to have their milestones closed on Github before we continue.
if [[ ${M} == "" ]]; then

  echo "Please make sure that you have cleaned up and closed the milestone connected to ${MVN_VERSION_RELEASE}";
  read -n 1 -srp "Press any key to continue (ctrl+c to cancel)"; printf "\n\n";

  SHOULD_BE_NULL_IF_MILESTONE_IS_CLOSED=$(curl  -s -H "Accept: application/vnd.github.v3+json"  https://api.github.com/repos/eclipse/rdf4j/milestones?state=open | jq '.[] | select(.title == "'"${MVN_VERSION_RELEASE}"'") | .number')
  echo "${SHOULD_BE_NULL_IF_MILESTONE_IS_CLOSED}"
  if  ! [[ ${SHOULD_BE_NULL_IF_MILESTONE_IS_CLOSED} == "" ]]; then
      echo "";
      echo "Milestone not closed!";
      echo "https://github.com/eclipse/rdf4j/milestone/${SHOULD_BE_NULL_IF_MILESTONE_IS_CLOSED}";
      exit 1;
  fi
fi

echo "Version: ${MVN_VERSION_RELEASE}";

# first try to get the GITHUB_MILESTONE number from the closed milestones
export GITHUB_MILESTONE
GITHUB_MILESTONE=$(curl  -s -H "Accept: application/vnd.github.v3+json"  https://api.github.com/repos/eclipse/rdf4j/milestones?state=closed\&direction=desc\&sort=title | jq '.[] | select(.title == "'"${MVN_VERSION_RELEASE}"'") | .number')

# then try to get the GITHUB_MILESTONE number from the open milestones (this should only be relevant for RDF4J Milestone builds).
if  [[ ${GITHUB_MILESTONE} == "" ]]; then
    GITHUB_MILESTONE=$(curl  -s -H "Accept: application/vnd.github.v3+json"  https://api.github.com/repos/eclipse/rdf4j/milestones | jq '.[] | select(.title == "'"${MVN_VERSION_RELEASE}"'") | .number')
fi

if  [[ ${GITHUB_MILESTONE} == "" ]]; then
    echo "";
    echo "Milestone not found matching '${MVN_VERSION_RELEASE}'";
    exit 1;
fi

export NUMBER_OF_CLOSED_ISSUES
NUMBER_OF_CLOSED_ISSUES=$(curl  -s -H "Accept: application/vnd.github.v3+json"  https://api.github.com/repos/eclipse/rdf4j/milestones/${GITHUB_MILESTONE}  | jq '.closed_issues')

echo "Milestone: https://github.com/eclipse/rdf4j/milestone/${GITHUB_MILESTONE}"
echo "Number of closed issues: ${NUMBER_OF_CLOSED_ISSUES}"

export DATETIME
DATETIME=$(date +"%Y-%m-%dT%H:%M:%S%z")

echo "Datetime: ${DATETIME}"

echo ""
echo "Using envsubst to generate content from templates."
RELEASE_NOTES=$(cat templates/"${RELEASE_NOTES_TEMPLATE}" | envsubst)
NEWS_ITEM=$(cat templates/"${NEWS_ITEM_TEMPLATE}" | envsubst)


NEWS_FILENAME=${MVN_VERSION_RELEASE_RAW}
NEWS_FILENAME=${NEWS_FILENAME/./}
NEWS_FILENAME=${NEWS_FILENAME/./}
NEWS_FILENAME="rdf4j-${NEWS_FILENAME}.md"

RELEASE_NOTES_FILENAME="${MVN_VERSION_RELEASE_RAW}.md"

# We do not create release-notes for Milestone builds
if [[ ${M} == "" ]]; then
  echo "Writing release notes to ../site/content/release-notes/${RELEASE_NOTES_FILENAME}"
  echo "${RELEASE_NOTES}" > "../site/content/release-notes/${RELEASE_NOTES_FILENAME}"
fi

echo "Writing news item to ../site/content/news/${NEWS_FILENAME}"
echo "${NEWS_ITEM}" > "../site/content/news/${NEWS_FILENAME}"

if ! [[ ${BRANCH} == "" ]]; then
  git add --all
  git commit -s -a -m "news item and release-notes if relevant for ${MVN_VERSION_RELEASE_RAW}"
  git push
fi

echo ""
echo "DONE!"

echo ""
echo "You will need to:"
echo " - manually edit site/content/download.md"
echo " - check site/content/news/${NEWS_FILENAME}"
echo " - check site/content/release-notes/${RELEASE_NOTES_FILENAME} (if it exists)"
echo ""
echo "The current branch can be used for that"
