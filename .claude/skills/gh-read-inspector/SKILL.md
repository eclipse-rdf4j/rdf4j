---
name: gh-read-inspector
description: Retrieve GitHub issues, pull requests, and milestones with read-only, whitelisted `gh` commands only. Use when you need complete issue or PR context, need to resolve a PR from commit ID/PR ID/issue ID, fetch milestone metadata, or list all issues in a milestone (labels, status, assignees, and related fields).
---

# gh-read-inspector

Use this skill for GitHub metadata lookup only. No write operations.

## Guardrails

- Use only `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py ...`.
- Do not run ad-hoc `gh` commands from this skill.
- If `gh` is missing, stop and ask the user to install GitHub CLI (`https://cli.github.com/`).

## Commands

- Issue details:
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py issue --repo owner/repo --issue 123`
- PR details by PR number:
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py pr --repo owner/repo --pr 123`
- PR details by issue number:
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py pr --repo owner/repo --issue 123`
- PR details by commit SHA:
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py pr --repo owner/repo --commit <sha>`
- Milestone details (number or exact title):
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py milestone --repo owner/repo --milestone 7`
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py milestone --repo owner/repo --milestone "Release 4.3"`
- Milestone issues:
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py milestone-issues --repo owner/repo --milestone 7 --state all`
- Show exact whitelist:
  - `python3 .codex/skills/gh-read-inspector/scripts/gh_read_inspector.py whitelist`

## Whitelist

The script can execute only these command templates:

- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/issues/{issue}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/issues/{issue}/comments?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/issues/{issue}/events?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json,application/vnd.github.mockingbird-preview+json" /repos/{repo}/issues/{issue}/timeline?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/pulls/{pr}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/issues/{pr}/comments?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/pulls/{pr}/reviews?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/pulls/{pr}/comments?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/issues/{pr}/events?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/pulls/{pr}/commits?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/pulls/{pr}/files?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/commits/{commit}/pulls?per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/commits/{commit}/check-runs`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/commits/{commit}/status`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/milestones/{milestone}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/milestones?state=all&per_page={per_page}&page={page}`
- `gh api -H "Accept: application/vnd.github+json" /repos/{repo}/issues?milestone={milestone}&state={state}&per_page={per_page}&page={page}`
