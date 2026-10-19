#!/usr/bin/env python3
"""Read-only GitHub metadata retrieval through a strict gh command whitelist."""

from __future__ import annotations

import argparse
import json
import re
import shlex
import shutil
import subprocess
import sys
from typing import Any, Dict, List, Tuple


READ_HEADER = "Accept: application/vnd.github+json"
TIMELINE_HEADER = "Accept: application/vnd.github+json,application/vnd.github.mockingbird-preview+json"
PER_PAGE = 100
MAX_PAGES = 200

ALLOWED_COMMANDS: Dict[str, List[str]] = {
    "issue_view": ["gh", "api", "-H", READ_HEADER, "/repos/{repo}/issues/{issue}"],
    "issue_comments_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/issues/{issue}/comments?per_page={per_page}&page={page}",
    ],
    "issue_events_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/issues/{issue}/events?per_page={per_page}&page={page}",
    ],
    "issue_timeline_page": [
        "gh",
        "api",
        "-H",
        TIMELINE_HEADER,
        "/repos/{repo}/issues/{issue}/timeline?per_page={per_page}&page={page}",
    ],
    "pr_view": ["gh", "api", "-H", READ_HEADER, "/repos/{repo}/pulls/{pr}"],
    "pr_issue_comments_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/issues/{pr}/comments?per_page={per_page}&page={page}",
    ],
    "pr_reviews_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/pulls/{pr}/reviews?per_page={per_page}&page={page}",
    ],
    "pr_review_comments_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/pulls/{pr}/comments?per_page={per_page}&page={page}",
    ],
    "pr_events_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/issues/{pr}/events?per_page={per_page}&page={page}",
    ],
    "pr_commits_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/pulls/{pr}/commits?per_page={per_page}&page={page}",
    ],
    "pr_files_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/pulls/{pr}/files?per_page={per_page}&page={page}",
    ],
    "commit_pulls_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/commits/{commit}/pulls?per_page={per_page}&page={page}",
    ],
    "commit_check_runs": ["gh", "api", "-H", READ_HEADER, "/repos/{repo}/commits/{commit}/check-runs"],
    "commit_status": ["gh", "api", "-H", READ_HEADER, "/repos/{repo}/commits/{commit}/status"],
    "milestone_view": ["gh", "api", "-H", READ_HEADER, "/repos/{repo}/milestones/{milestone}"],
    "milestones_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/milestones?state=all&per_page={per_page}&page={page}",
    ],
    "milestone_issues_page": [
        "gh",
        "api",
        "-H",
        READ_HEADER,
        "/repos/{repo}/issues?milestone={milestone}&state={state}&per_page={per_page}&page={page}",
    ],
}


class SkillError(RuntimeError):
    """User-facing errors for the skill wrapper."""


def quote_command(parts: List[str]) -> str:
    return " ".join(shlex.quote(part) for part in parts)


def ensure_gh_installed() -> None:
    if shutil.which("gh") is None:
        raise SkillError(
            "`gh` is not installed. Install GitHub CLI from https://cli.github.com/ and run `gh auth login`."
        )


def validate_repo(repo: str) -> str:
    if not re.match(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", repo):
        raise SkillError("`--repo` must match `owner/repo`.")
    return repo


def build_command(command_key: str, **params: Any) -> List[str]:
    template = ALLOWED_COMMANDS.get(command_key)
    if template is None:
        raise SkillError(f"Attempted non-whitelisted command key: {command_key}")
    try:
        return [token.format(**params) for token in template]
    except KeyError as exc:
        raise SkillError(f"Missing template parameter `{exc.args[0]}` for `{command_key}`.") from exc


def run_json(command_key: str, **params: Any) -> Any:
    command = build_command(command_key, **params)
    completed = subprocess.run(command, capture_output=True, text=True, check=False)
    if completed.returncode != 0:
        message = completed.stderr.strip() or completed.stdout.strip() or f"exit code {completed.returncode}"
        raise SkillError(f"Command failed: {quote_command(command)}\n{message}")
    output = completed.stdout.strip()
    if not output:
        return None
    try:
        return json.loads(output)
    except json.JSONDecodeError as exc:
        raise SkillError(
            f"Command returned non-JSON output: {quote_command(command)}\n{output[:500]}"
        ) from exc


def fetch_pages(command_key: str, **params: Any) -> List[Any]:
    items: List[Any] = []
    for page in range(1, MAX_PAGES + 1):
        payload = run_json(command_key, page=page, per_page=PER_PAGE, **params)
        if payload is None:
            break
        if not isinstance(payload, list):
            raise SkillError(f"Expected list payload from `{command_key}`, got {type(payload).__name__}.")
        if not payload:
            break
        items.extend(payload)
        if len(payload) < PER_PAGE:
            break
    else:
        raise SkillError(f"Pagination guard hit for `{command_key}` after {MAX_PAGES} pages.")
    return items


def resolve_pr_numbers(repo: str, pr: int | None, issue: int | None, commit: str | None) -> Tuple[List[int], Dict[str, Any]]:
    if pr is not None:
        return [pr], {"mode": "pr", "pr": pr}

    if issue is not None:
        issue_payload = run_json("issue_view", repo=repo, issue=issue)
        if not isinstance(issue_payload, dict) or "pull_request" not in issue_payload:
            raise SkillError(f"Issue #{issue} in {repo} is not a pull request.")
        pr_number = issue_payload.get("number")
        if not isinstance(pr_number, int):
            raise SkillError(f"Failed resolving PR number from issue #{issue} in {repo}.")
        return [pr_number], {"mode": "issue", "issue": issue}

    if commit is not None:
        pull_payloads = fetch_pages("commit_pulls_page", repo=repo, commit=commit)
        pr_numbers = sorted(
            {
                payload.get("number")
                for payload in pull_payloads
                if isinstance(payload, dict) and isinstance(payload.get("number"), int)
            }
        )
        if not pr_numbers:
            raise SkillError(f"No pull requests found for commit `{commit}` in {repo}.")
        return pr_numbers, {"mode": "commit", "commit": commit}

    raise SkillError("Provide one of `--pr`, `--issue`, or `--commit`.")


def resolve_milestone_number(repo: str, milestone_value: str) -> int:
    if milestone_value.isdigit():
        return int(milestone_value)

    milestones = fetch_pages("milestones_page", repo=repo)
    for milestone in milestones:
        if not isinstance(milestone, dict):
            continue
        title = milestone.get("title")
        number = milestone.get("number")
        if isinstance(title, str) and isinstance(number, int) and title == milestone_value:
            return number
    raise SkillError(f"Milestone `{milestone_value}` not found in {repo}. Use exact title or numeric milestone.")


def build_issue_payload(repo: str, issue_number: int) -> Dict[str, Any]:
    return {
        "repo": repo,
        "issue_number": issue_number,
        "issue": run_json("issue_view", repo=repo, issue=issue_number),
        "comments": fetch_pages("issue_comments_page", repo=repo, issue=issue_number),
        "events": fetch_pages("issue_events_page", repo=repo, issue=issue_number),
        "timeline": fetch_pages("issue_timeline_page", repo=repo, issue=issue_number),
    }


def build_single_pr_payload(repo: str, pr_number: int) -> Dict[str, Any]:
    pr_payload = run_json("pr_view", repo=repo, pr=pr_number)
    head_sha = None
    if isinstance(pr_payload, dict):
        head = pr_payload.get("head")
        if isinstance(head, dict):
            head_sha = head.get("sha")
    checks = run_json("commit_check_runs", repo=repo, commit=head_sha) if isinstance(head_sha, str) else None
    status = run_json("commit_status", repo=repo, commit=head_sha) if isinstance(head_sha, str) else None
    return {
        "pr_number": pr_number,
        "pull_request": pr_payload,
        "issue_comments": fetch_pages("pr_issue_comments_page", repo=repo, pr=pr_number),
        "reviews": fetch_pages("pr_reviews_page", repo=repo, pr=pr_number),
        "review_comments": fetch_pages("pr_review_comments_page", repo=repo, pr=pr_number),
        "events": fetch_pages("pr_events_page", repo=repo, pr=pr_number),
        "commits": fetch_pages("pr_commits_page", repo=repo, pr=pr_number),
        "files": fetch_pages("pr_files_page", repo=repo, pr=pr_number),
        "head_commit_check_runs": checks,
        "head_commit_status": status,
    }


def build_pr_payload(repo: str, pr: int | None, issue: int | None, commit: str | None) -> Dict[str, Any]:
    pr_numbers, resolution = resolve_pr_numbers(repo=repo, pr=pr, issue=issue, commit=commit)
    pull_requests = [build_single_pr_payload(repo=repo, pr_number=number) for number in pr_numbers]
    return {
        "repo": repo,
        "resolution": resolution,
        "resolved_pr_numbers": pr_numbers,
        "pull_requests": pull_requests,
    }


def build_milestone_payload(repo: str, milestone_value: str) -> Dict[str, Any]:
    milestone_number = resolve_milestone_number(repo=repo, milestone_value=milestone_value)
    milestone_payload = run_json("milestone_view", repo=repo, milestone=milestone_number)
    return {
        "repo": repo,
        "milestone_input": milestone_value,
        "milestone_number": milestone_number,
        "milestone": milestone_payload,
    }


def build_milestone_issues_payload(repo: str, milestone_value: str, state: str) -> Dict[str, Any]:
    milestone_number = resolve_milestone_number(repo=repo, milestone_value=milestone_value)
    milestone_payload = run_json("milestone_view", repo=repo, milestone=milestone_number)
    issues_payload = fetch_pages(
        "milestone_issues_page",
        repo=repo,
        milestone=milestone_number,
        state=state,
    )
    return {
        "repo": repo,
        "milestone_input": milestone_value,
        "milestone_number": milestone_number,
        "state": state,
        "milestone": milestone_payload,
        "issues": issues_payload,
    }


def print_whitelist() -> None:
    payload = {name: parts for name, parts in ALLOWED_COMMANDS.items()}
    print(json.dumps(payload, indent=2, sort_keys=True))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Read-only GitHub issue/PR/milestone retrieval with a strict gh whitelist.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    issue_parser = subparsers.add_parser("issue", help="Fetch issue details with comments/events/timeline.")
    issue_parser.add_argument("--repo", required=True, help="GitHub repository in owner/repo format.")
    issue_parser.add_argument("--issue", required=True, type=int, help="Issue number.")

    pr_parser = subparsers.add_parser("pr", help="Fetch PR details by PR number, issue number, or commit SHA.")
    pr_parser.add_argument("--repo", required=True, help="GitHub repository in owner/repo format.")
    source_group = pr_parser.add_mutually_exclusive_group(required=True)
    source_group.add_argument("--pr", type=int, help="Pull request number.")
    source_group.add_argument("--issue", type=int, help="Issue number that corresponds to a pull request.")
    source_group.add_argument("--commit", help="Commit SHA to resolve pull request(s).")

    milestone_parser = subparsers.add_parser("milestone", help="Fetch milestone metadata.")
    milestone_parser.add_argument("--repo", required=True, help="GitHub repository in owner/repo format.")
    milestone_parser.add_argument("--milestone", required=True, help="Milestone number or exact title.")

    milestone_issues_parser = subparsers.add_parser(
        "milestone-issues", help="Fetch all issues assigned to a milestone."
    )
    milestone_issues_parser.add_argument("--repo", required=True, help="GitHub repository in owner/repo format.")
    milestone_issues_parser.add_argument("--milestone", required=True, help="Milestone number or exact title.")
    milestone_issues_parser.add_argument(
        "--state",
        default="all",
        choices=["open", "closed", "all"],
        help="Issue state filter. Defaults to all.",
    )

    subparsers.add_parser("whitelist", help="Print the exact internal gh command whitelist.")

    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        if args.command == "whitelist":
            print_whitelist()
            return 0

        ensure_gh_installed()

        if args.command == "issue":
            payload = build_issue_payload(repo=validate_repo(args.repo), issue_number=args.issue)
        elif args.command == "pr":
            payload = build_pr_payload(
                repo=validate_repo(args.repo),
                pr=args.pr,
                issue=args.issue,
                commit=args.commit,
            )
        elif args.command == "milestone":
            payload = build_milestone_payload(repo=validate_repo(args.repo), milestone_value=args.milestone)
        elif args.command == "milestone-issues":
            payload = build_milestone_issues_payload(
                repo=validate_repo(args.repo),
                milestone_value=args.milestone,
                state=args.state,
            )
        else:
            raise SkillError(f"Unsupported command: {args.command}")
    except SkillError as exc:
        print(str(exc), file=sys.stderr)
        return 2

    print(json.dumps(payload, indent=2, sort_keys=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
