#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_PATH="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/$(basename -- "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"; then
  :
elif REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel 2>/dev/null)"; then
  :
else
  REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
fi
REPO_SCRIPT_DIR="${REPO_ROOT}/scripts"
STATE_DIR="${REPO_ROOT}/.git/release-script"
STATE_FILE="${STATE_DIR}/state.env"
CONTEXT_FILE="${STATE_DIR}/release-context.env"
LOG_FILE="${STATE_DIR}/release.log"

mkdir -p "${STATE_DIR}"
touch "${LOG_FILE}"
exec > >(tee -a "${LOG_FILE}") 2>&1

cd "${REPO_ROOT}"

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

log() {
  printf '\n==> %s\n' "$*"
}

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

save_var() {
  local name="$1"
  local value="${2-}"
  local tmp="${STATE_FILE}.tmp"

  touch "${STATE_FILE}"
  grep -v -E "^${name}=" "${STATE_FILE}" > "${tmp}" || true
  printf '%s=%q\n' "${name}" "${value}" >> "${tmp}"
  mv "${tmp}" "${STATE_FILE}"
  printf -v "${name}" '%s' "${value}"
  export "${name}"
}

load_state() {
  if [[ -f "${STATE_FILE}" ]]; then
    # shellcheck source=/dev/null
    source "${STATE_FILE}"
  fi
}

clear_state() {
  rm -f "${STATE_FILE}" "${CONTEXT_FILE}"
}

print_resume_help() {
  echo "State file: ${STATE_FILE}"
  echo "Log file:   ${LOG_FILE}"
  echo "Resume automatically: ${SCRIPT_PATH}"
  echo "Resume from a specific step: ${SCRIPT_PATH} --from <step>"
  echo "List available steps: ${SCRIPT_PATH} --list-steps"
  echo "Start a fresh run: ${SCRIPT_PATH} --reset"
}

on_err() {
  local rc=$?
  echo
  echo "Unhandled error while running step: ${CURRENT_STEP:-unknown}"
  print_resume_help
  exit "${rc}"
}
trap on_err ERR

open_fix_shell() {
  echo
  echo "Opening a shell in ${REPO_ROOT}. Exit the shell when you want the script to retry."
  (
    cd "${REPO_ROOT}"
    "${SHELL:-/bin/bash}"
  )
}

handle_failure() {
  local description="$1"
  local rc="$2"
  local choice

  while true; do
    echo
    echo "The command failed in step '${CURRENT_STEP:-unknown}' with exit code ${rc}."
    echo "Action: ${description}"
    read -rp "Choose: [r]etry, [s]hell, [a]bort: " choice
    case "${choice}" in
      r|R)
        return 0
        ;;
      s|S)
        open_fix_shell
        ;;
      a|A)
        print_resume_help
        exit "${rc}"
        ;;
      *)
        echo "Unknown choice: ${choice}"
        ;;
    esac
  done
}

run_cmd() {
  local description="$1"
  shift
  local rc

  while true; do
    log "${description}"
    if "$@"; then
      return 0
    else
      rc=$?
      handle_failure "${description}" "${rc}"
    fi
  done
}

capture_cmd() {
  local __resultvar="$1"
  local description="$2"
  shift 2
  local output
  local rc

  while true; do
    log "${description}"
    if output=$("$@" 2>&1); then
      printf '%s\n' "${output}"
      printf -v "${__resultvar}" '%s' "${output}"
      return 0
    else
      rc=$?
      printf '%s\n' "${output}"
      handle_failure "${description}" "${rc}"
    fi
  done
}

require_command() {
  local command_name="$1"
  local help_message="$2"
  command -v "${command_name}" >/dev/null 2>&1 || die "${help_message}"
}

current_branch() {
  git branch --show-current
}

ensure_on_branch() {
  local expected="$1"
  local actual
  actual="$(current_branch)"
  [[ "${actual}" == "${expected}" ]] || die "You need to be on '${expected}', but you are on '${actual:-detached HEAD}'."
}

assert_clean_worktree() {
  if [[ -n "$(git status --porcelain)" ]]; then
    git status --short
    die "There are uncommitted or untracked files. Clean them up before continuing."
  fi
}

assert_branch_in_sync() {
  local branch="$1"
  local counts
  local ahead
  local behind

  counts="$(git rev-list --left-right --count "${branch}...origin/${branch}")"
  ahead="${counts%%[[:space:]]*}"
  behind="${counts##*[[:space:]]}"

  if [[ "${ahead}" != "0" || "${behind}" != "0" ]]; then
    die "Branch '${branch}' is not in sync with origin/${branch} (ahead=${ahead}, behind=${behind})."
  fi
}

assert_push_access() {
  if ! git push --dry-run >/dev/null 2>&1; then
    die "Could not push to the repository. Check that you have sufficient access rights."
  fi
}

local_branch_exists() {
  git show-ref --verify --quiet "refs/heads/$1"
}

local_tag_exists() {
  git show-ref --verify --quiet "refs/tags/$1"
}

remote_tag_exists() {
  git ls-remote --exit-code --tags origin "refs/tags/$1" >/dev/null 2>&1
}

ensure_local_tag() {
  local tag="$1"
  if local_tag_exists "${tag}"; then
    return 0
  fi
  if remote_tag_exists "${tag}"; then
    run_cmd "Fetch tag ${tag}" git fetch origin "refs/tags/${tag}:refs/tags/${tag}"
  fi
}

checkout_existing_or_create_from_current() {
  local branch="$1"

  if [[ "$(current_branch)" == "${branch}" ]]; then
    return 0
  fi

  if local_branch_exists "${branch}"; then
    run_cmd "Checkout existing branch ${branch}" git checkout "${branch}"
  else
    run_cmd "Create branch ${branch}" git checkout -b "${branch}"
  fi
}

read_maven_version() {
  xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml
}

write_context_file() {
  cat > "${CONTEXT_FILE}" <<EOF_CTX
MVN_CURRENT_SNAPSHOT_VERSION=${MVN_CURRENT_SNAPSHOT_VERSION}
MVN_VERSION_RELEASE=${MVN_VERSION_RELEASE}
MVN_NEXT_SNAPSHOT_VERSION=${MVN_NEXT_SNAPSHOT_VERSION}
BRANCH=${BRANCH}
RELEASE_NOTES_BRANCH=${RELEASE_NOTES_BRANCH}
MERGE_BRANCH=${MERGE_BRANCH}
MVN_VERSION_DEVELOP=${MVN_VERSION_DEVELOP}
EOF_CTX
}

maven_clean_all() {
  run_cmd "mvn clean (pass 1)" mvn clean -q -Dmaven.clean.failOnError=false
  run_cmd "mvn clean (pass 2)" mvn clean -q -Dmaven.clean.failOnError=false
  run_cmd "mvn clean" mvn clean
}

commit_tracked_if_needed() {
  local message="$1"

  if git diff --quiet && git diff --cached --quiet; then
    echo "No tracked changes to commit for '${message}'."
    return 0
  fi

  run_cmd "Commit tracked changes: ${message}" git commit -s -a -m "${message}"
}

stage_all_and_commit_if_needed() {
  local message="$1"
  run_cmd "Stage all changes for commit: ${message}" git add --all
  if git diff --cached --quiet; then
    echo "No staged changes to commit for '${message}'."
    return 0
  fi
  run_cmd "Commit: ${message}" git commit -s -m "${message}"
}

create_pr_if_missing() {
  local base="$1"
  local head="$2"
  local title="$3"
  local body="$4"
  local pr_number

  capture_cmd pr_number "Check for an existing PR (${head} -> ${base})" \
    gh pr list --state all --head "${head}" --base "${base}" --json number --jq '.[0].number // ""'

  if [[ -n "${pr_number}" ]]; then
    echo "PR already exists for ${head} -> ${base}: #${pr_number}"
    return 0
  fi

  run_cmd "Create PR (${head} -> ${base})" \
    gh pr create --base "${base}" --head "${head}" --title "${title}" --body "${body}"
}

require_release_context() {
  [[ -n "${MVN_CURRENT_SNAPSHOT_VERSION:-}" ]] || die "Missing MVN_CURRENT_SNAPSHOT_VERSION. Resume from compute_versions or use --reset."
  [[ -n "${MVN_VERSION_RELEASE:-}" ]] || die "Missing MVN_VERSION_RELEASE. Resume from compute_versions or use --reset."
  [[ -n "${MVN_NEXT_SNAPSHOT_VERSION:-}" ]] || die "Missing MVN_NEXT_SNAPSHOT_VERSION. Resume from compute_versions or use --reset."
  [[ -n "${BRANCH:-}" ]] || die "Missing BRANCH. Resume from compute_versions or use --reset."
  [[ -n "${RELEASE_NOTES_BRANCH:-}" ]] || die "Missing RELEASE_NOTES_BRANCH. Resume from compute_versions or use --reset."
  [[ -n "${MERGE_BRANCH:-}" ]] || die "Missing MERGE_BRANCH. Resume from compute_versions or use --reset."
  [[ -n "${MVN_VERSION_DEVELOP:-}" ]] || die "Missing MVN_VERSION_DEVELOP. Resume from compute_versions or use --reset."
}

step_confirm_start() {
  echo
  echo "The release script requires several external command line tools:"
  echo " - git"
  echo " - mvn"
  echo " - gh (the GitHub CLI, see https://github.com/cli/cli)"
  echo " - xmllint (http://xmlsoft.org/xmllint.html)"
  echo
  echo "This script checkpoints progress so you can rerun it after a failure."
  echo "After the initial confirmation it will run unattended unless a step fails."
  echo "If something fails, you can retry, open a shell to fix it, or abort and resume later."
  print_resume_help
  echo
  echo "Do not change unrelated files in the repository while the script is running."

  if [[ -n "${RESUME_FROM_STATE:-}" ]]; then
    read -rp "Resume the release process from step '${START_FROM}' (y/n)? " choice
  else
    read -rp "Start the release process (y/n)? " choice
  fi

  case "${choice}" in
    y|Y)
      echo
      ;;
    n|N)
      exit 0
      ;;
    *)
      die "Unknown response '${choice}'."
      ;;
  esac
}

step_verify_prereqs() {
  require_command git "git command not found"
  require_command mvn "mvn command not found. See https://maven.apache.org/"
  require_command gh "gh command not found. See https://github.com/cli/cli"
  require_command xmllint "xmllint command not found. See http://xmlsoft.org/xmllint.html"
  [[ -f pom.xml ]] || die "pom.xml not found in ${REPO_ROOT}"
  run_cmd "Check GitHub CLI authentication" gh auth status
}

step_sync_main() {
  ensure_on_branch main
  assert_clean_worktree
  run_cmd "Fetch origin/main" git fetch origin main
  run_cmd "Fast-forward main" git pull --ff-only origin main
  assert_branch_in_sync main
  assert_clean_worktree
  assert_push_access
  maven_clean_all
}

step_sync_develop() {
  run_cmd "Checkout develop" git checkout develop
  assert_clean_worktree
  run_cmd "Fetch origin/develop" git fetch origin develop
  run_cmd "Fast-forward develop" git pull --ff-only origin develop
  assert_branch_in_sync develop
  maven_clean_all
  run_cmd "Checkout main" git checkout main
  maven_clean_all
}

step_compute_versions() {
  ensure_on_branch main
  MVN_CURRENT_SNAPSHOT_VERSION="$(read_maven_version)"
  [[ "${MVN_CURRENT_SNAPSHOT_VERSION}" == *-SNAPSHOT ]] || die "Expected main pom.xml version to end with -SNAPSHOT, got '${MVN_CURRENT_SNAPSHOT_VERSION}'."

  MVN_VERSION_RELEASE="${MVN_CURRENT_SNAPSHOT_VERSION/-SNAPSHOT/}"
  MVN_NEXT_SNAPSHOT_VERSION="$(increment_version "${MVN_VERSION_RELEASE}" 3)-SNAPSHOT"
  BRANCH="releases/${MVN_VERSION_RELEASE}"
  RELEASE_NOTES_BRANCH="${MVN_VERSION_RELEASE}-release-notes"
  MERGE_BRANCH="merge_main_into_develop_after_release_${MVN_VERSION_RELEASE}"

  run_cmd "Checkout develop to read its pom version" git checkout develop
  MVN_VERSION_DEVELOP="$(read_maven_version)"
  run_cmd "Checkout main" git checkout main

  save_var MVN_CURRENT_SNAPSHOT_VERSION "${MVN_CURRENT_SNAPSHOT_VERSION}"
  save_var MVN_VERSION_RELEASE "${MVN_VERSION_RELEASE}"
  save_var MVN_NEXT_SNAPSHOT_VERSION "${MVN_NEXT_SNAPSHOT_VERSION}"
  save_var BRANCH "${BRANCH}"
  save_var RELEASE_NOTES_BRANCH "${RELEASE_NOTES_BRANCH}"
  save_var MERGE_BRANCH "${MERGE_BRANCH}"
  save_var MVN_VERSION_DEVELOP "${MVN_VERSION_DEVELOP}"

  write_context_file

  echo
  echo "Your current maven snapshot version is: '${MVN_CURRENT_SNAPSHOT_VERSION}'"
  echo "Your maven release version will be: '${MVN_VERSION_RELEASE}'"
  echo "Your next maven snapshot version will be: '${MVN_NEXT_SNAPSHOT_VERSION}'"
  echo "Your develop branch version is: '${MVN_VERSION_DEVELOP}'"
  echo
  echo "Release context written to ${CONTEXT_FILE}"
  echo "Continuing automatically. The script will only stop for input if a step fails."
}

step_build_main_snapshot() {
  require_release_context
  ensure_on_branch main
  echo "Running maven clean and install -DskipTests"
  maven_clean_all
  run_cmd "mvn install -DskipTests" mvn install -DskipTests
}

step_set_release_version_on_worktree() {
  require_release_context
  ensure_on_branch main
  run_cmd "Set maven version to ${MVN_VERSION_RELEASE}" mvn versions:set -DnewVersion="${MVN_VERSION_RELEASE}"
  run_cmd "Commit maven version metadata" mvn versions:commit
}

step_create_or_checkout_release_branch() {
  require_release_context
  checkout_existing_or_create_from_current "${BRANCH}"
}

step_commit_release_version() {
  require_release_context
  ensure_on_branch "${BRANCH}"
  commit_tracked_if_needed "release ${MVN_VERSION_RELEASE}"
}

step_tag_release_commit() {
  require_release_context
  ensure_on_branch "${BRANCH}"
  ensure_local_tag "${MVN_VERSION_RELEASE}"
  if local_tag_exists "${MVN_VERSION_RELEASE}"; then
    echo "Tag ${MVN_VERSION_RELEASE} already exists locally."
    return 0
  fi
  run_cmd "Create tag ${MVN_VERSION_RELEASE}" git tag "${MVN_VERSION_RELEASE}"
}

step_push_release_branch() {
  require_release_context
  ensure_on_branch "${BRANCH}"
  echo
  echo "Pushing release branch to GitHub"
  run_cmd "Push branch ${BRANCH}" git push -u origin "${BRANCH}"
}

step_push_release_tag() {
  require_release_context
  ensure_local_tag "${MVN_VERSION_RELEASE}"
  if remote_tag_exists "${MVN_VERSION_RELEASE}"; then
    echo "Tag ${MVN_VERSION_RELEASE} already exists on origin."
    return 0
  fi
  run_cmd "Push tag ${MVN_VERSION_RELEASE}" git push origin "${MVN_VERSION_RELEASE}"
}

step_set_next_snapshot_on_release_branch() {
  require_release_context
  ensure_on_branch "${BRANCH}"
  maven_clean_all
  echo
  echo "Setting the next snapshot version to: ${MVN_NEXT_SNAPSHOT_VERSION}"
  run_cmd "Set maven version to ${MVN_NEXT_SNAPSHOT_VERSION}" mvn versions:set -DnewVersion="${MVN_NEXT_SNAPSHOT_VERSION}"
  run_cmd "Commit maven version metadata" mvn versions:commit
}

step_commit_next_snapshot() {
  require_release_context
  ensure_on_branch "${BRANCH}"
  commit_tracked_if_needed "next development iteration: ${MVN_NEXT_SNAPSHOT_VERSION}"
}

step_push_next_snapshot() {
  require_release_context
  ensure_on_branch "${BRANCH}"
  echo
  echo "Pushing the next snapshot version to GitHub"
  run_cmd "Push branch ${BRANCH}" git push -u origin "${BRANCH}"
}

step_create_main_pr() {
  require_release_context
  echo
  echo "Creating pull request to merge release branch back into main"
  create_pr_if_missing \
    main \
    "${BRANCH}" \
    "next development iteration: ${MVN_NEXT_SNAPSHOT_VERSION}" \
    "Merge using merge commit rather than rebase"
}

step_create_or_checkout_merge_branch() {
  require_release_context
  run_cmd "Checkout ${BRANCH}" git checkout "${BRANCH}"
  checkout_existing_or_create_from_current "${MERGE_BRANCH}"
}

step_set_develop_version_on_merge_branch() {
  require_release_context
  ensure_on_branch "${MERGE_BRANCH}"
  run_cmd "Set maven version to ${MVN_VERSION_DEVELOP}" mvn versions:set -DnewVersion="${MVN_VERSION_DEVELOP}"
  run_cmd "Commit maven version metadata" mvn versions:commit
}

step_commit_develop_sync_branch() {
  require_release_context
  ensure_on_branch "${MERGE_BRANCH}"
  commit_tracked_if_needed "set correct version"
}

step_push_develop_sync_branch() {
  require_release_context
  ensure_on_branch "${MERGE_BRANCH}"
  run_cmd "Push branch ${MERGE_BRANCH}" git push -u origin "${MERGE_BRANCH}"
}

step_create_develop_pr() {
  require_release_context
  echo "Creating pull request to merge the merge branch into develop"
  create_pr_if_missing \
    develop \
    "${MERGE_BRANCH}" \
    "sync develop branch after release ${MVN_VERSION_RELEASE}" \
    "Merge using merge commit rather than rebase"
  echo "It's ok to merge this PR later, so wait for the CI tests to finish."
}

step_cleanup_before_javadocs() {
  require_release_context
  maven_clean_all
  run_cmd "Checkout develop" git checkout develop
  maven_clean_all
  run_cmd "Checkout main" git checkout main
  maven_clean_all
}

step_checkout_release_tag() {
  require_release_context
  ensure_local_tag "${MVN_VERSION_RELEASE}"
  echo "Build javadocs"
  run_cmd "Checkout tag ${MVN_VERSION_RELEASE}" git checkout "${MVN_VERSION_RELEASE}"
}

step_build_release_artifacts() {
  require_release_context
  run_cmd "mvn clean" mvn clean
  run_cmd "mvn install -DskipTests -Djapicmp.skip" mvn install -DskipTests -Djapicmp.skip
  run_cmd "mvn package -Passembly -DskipTests -Djapicmp.skip" mvn package -Passembly -DskipTests -Djapicmp.skip
}

step_build_javadoc_archive() {
  require_release_context
  run_cmd "Build javadoc archive" env JAVADOC_SKIP_INSTALL=true "${REPO_SCRIPT_DIR}/build-javadoc-archive.sh"
}

step_create_or_checkout_release_notes_branch() {
  require_release_context
  run_cmd "Checkout main" git checkout main
  checkout_existing_or_create_from_current "${RELEASE_NOTES_BRANCH}"
}

step_commit_release_notes() {
  require_release_context
  ensure_on_branch "${RELEASE_NOTES_BRANCH}"
  stage_all_and_commit_if_needed "javadocs for ${MVN_VERSION_RELEASE}"
}

step_push_release_notes_branch() {
  require_release_context
  ensure_on_branch "${RELEASE_NOTES_BRANCH}"
  run_cmd "Push branch ${RELEASE_NOTES_BRANCH}" git push -u origin "${RELEASE_NOTES_BRANCH}"
}

step_create_release_notes_pr() {
  require_release_context
  local body_content

  body_content=$(cat <<EOF_BODY
Javadocs, release-notes and news item for ${MVN_VERSION_RELEASE}.

 - [ ] check that downloads.md is updated
 - [ ] check that the release notes file is updated
 - [ ] check that the news item file is updated
 - [ ] check that [Jenkins](https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-ossrh/) finished publishing the release
 - [ ] check that [Jenkins](https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-sdk/) finished publishing the sdk
 - [ ] remember to also [add the release here on GitHub](https://github.com/eclipse-rdf4j/rdf4j/releases/new?tag=${MVN_VERSION_RELEASE}&title=RDF4J%20${MVN_VERSION_RELEASE}) (include announcement)
EOF_BODY
)

  create_pr_if_missing \
    main \
    "${RELEASE_NOTES_BRANCH}" \
    "${MVN_VERSION_RELEASE} release notes" \
    "${body_content}"

  echo "Javadocs are in git branch ${RELEASE_NOTES_BRANCH}"
}

step_final_cleanup() {
  require_release_context
  run_cmd "Checkout main" git checkout main
  maven_clean_all
  save_var NEXT_STEP done
  cd "${REPO_SCRIPT_DIR}"

  echo
  echo "DONE!"
  echo
  echo "Next manual actions after the script finishes:"
  echo "- Tell Jenkins to start the release deployment processes, for SDK and maven artifacts"
  echo "- SDK deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-sdk/"
  echo "- Maven deployment: https://ci.eclipse.org/rdf4j/job/rdf4j-deploy-release-ossrh/"
  echo "- Log in, then choose 'Build with Parameters' and type in ${MVN_VERSION_RELEASE}"
  echo
  echo "You will now want to inform the community about the new release!"
  echo " - Check if all recently completed issues have the correct milestone: https://github.com/eclipse/rdf4j/issues?q=is%3Aissue+no%3Amilestone+-label%3A%22cannot+reproduce%22+-label%3A%22%F0%9F%94%A7+internal+task%22+-label%3Awontfix+-label%3Astale+-label%3Aduplicate+sort%3Aupdated-desc+is%3Aclosed"
  echo " - Create a new milestone for ${MVN_NEXT_SNAPSHOT_VERSION/-SNAPSHOT/}: https://github.com/eclipse/rdf4j/milestones/new"
  echo " - Close the ${MVN_VERSION_RELEASE} milestone: https://github.com/eclipse/rdf4j/milestones"
  echo "     - Make sure that all issues in the milestone are closed, or move them to the next milestone"
  echo
  echo "To generate the news item and release-notes you will want to run the following command:"
  echo "./release-notes.sh ${MVN_VERSION_RELEASE} patch-release-notes.md patch-news-item.md ${RELEASE_NOTES_BRANCH}"
  echo
  echo "Release state and logs are stored in ${STATE_DIR}"
}

STEPS=(
  confirm_start
  verify_prereqs
  sync_main
  sync_develop
  compute_versions
  build_main_snapshot
  set_release_version_on_worktree
  create_or_checkout_release_branch
  commit_release_version
  tag_release_commit
  push_release_branch
  push_release_tag
  set_next_snapshot_on_release_branch
  commit_next_snapshot
  push_next_snapshot
  create_main_pr
  create_or_checkout_merge_branch
  set_develop_version_on_merge_branch
  commit_develop_sync_branch
  push_develop_sync_branch
  create_develop_pr
  cleanup_before_javadocs
  checkout_release_tag
  build_release_artifacts
  build_javadoc_archive
  create_or_checkout_release_notes_branch
  commit_release_notes
  push_release_notes_branch
  create_release_notes_pr
  final_cleanup
)

is_valid_step() {
  local candidate="$1"
  local step
  for step in "${STEPS[@]}"; do
    [[ "${step}" == "${candidate}" ]] && return 0
  done
  return 1
}

list_steps() {
  local step
  printf 'Available steps:\n'
  for step in "${STEPS[@]}"; do
    printf ' - %s\n' "${step}"
  done
}

usage() {
  cat <<EOF_USAGE
Usage: ${SCRIPT_PATH} [--reset] [--from <step>] [--list-steps]

  --reset        Remove the saved checkpoint and start from the beginning.
  --from <step>  Start from a specific step.
  --list-steps   Print all step names.
EOF_USAGE
}

RESET_STATE=0
FROM_STEP=""
LIST_ONLY=0

while (( $# > 0 )); do
  case "$1" in
    --reset)
      RESET_STATE=1
      shift
      ;;
    --from)
      [[ $# -ge 2 ]] || die "Missing value for --from"
      FROM_STEP="$2"
      shift 2
      ;;
    --list-steps)
      LIST_ONLY=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown option: $1"
      ;;
  esac
done

if (( LIST_ONLY )); then
  list_steps
  exit 0
fi

if (( RESET_STATE )); then
  clear_state
fi

load_state

if [[ -n "${FROM_STEP}" ]]; then
  is_valid_step "${FROM_STEP}" || die "Unknown step '${FROM_STEP}'. Use --list-steps to see valid names."
  START_FROM="${FROM_STEP}"
  RESUME_FROM_STATE=""
else
  START_FROM="${NEXT_STEP:-confirm_start}"
  if [[ -n "${NEXT_STEP:-}" && "${NEXT_STEP}" != "confirm_start" && "${NEXT_STEP}" != "done" ]]; then
    RESUME_FROM_STATE=1
  else
    RESUME_FROM_STATE=""
  fi
fi

if [[ "${START_FROM}" == "done" ]]; then
  echo "The last recorded run already completed."
  print_resume_help
  exit 0
fi

is_valid_step "${START_FROM}" || die "Unknown starting step '${START_FROM}'. Use --list-steps to see valid names."

if [[ -n "${FROM_STEP}" ]]; then
  echo "Starting from explicitly requested step: ${START_FROM}"
elif [[ -n "${RESUME_FROM_STATE:-}" ]]; then
  echo "Resuming from saved step: ${START_FROM}"
  print_resume_help
  echo
fi

RUN_ENABLED=0
run_step() {
  local step="$1"
  local next_step="$2"
  local fn="step_${step}"

  if [[ "${START_FROM}" == "${step}" ]]; then
    RUN_ENABLED=1
  fi

  if (( RUN_ENABLED == 0 )); then
    return 0
  fi

  save_var CURRENT_STEP "${step}"
  save_var NEXT_STEP "${step}"
  log "Running step: ${step}"
  "${fn}"
  save_var LAST_COMPLETED_STEP "${step}"
  save_var NEXT_STEP "${next_step}"
}

for (( i = 0; i < ${#STEPS[@]}; i++ )); do
  current_step_name="${STEPS[$i]}"
  if (( i + 1 < ${#STEPS[@]} )); then
    next_step_name="${STEPS[$((i + 1))]}"
  else
    next_step_name="done"
  fi
  run_step "${current_step_name}" "${next_step_name}"
done
