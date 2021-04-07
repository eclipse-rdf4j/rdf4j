---
title: "RDF4J merge strategy"
toc: true
---

RDF4J values a clean but accurate commit history on our main branches, where
commits are meaningfully described and linked back to the issue that they
address. To achieve this, we merge everything using merge-commits, and we may
ask you to [squash](/documentation/developer/squashing) your pull request branch before we
merge it.

<!--more-->

We use merge-commits exclusively to merge pull requests into our main branches
as this preserves the history of changes and who made those changes accurately.
You as a contributor are completely free to use rebasing, squashing or merging
on your own branches as you see fit, of course - as long as you make sure that
history of your branch is clean when it's time to merge your PR.

Note: we previously experimented with using 'squash-and-merge' as our Pull
Request strategy. The advantage of this was that it kept the main branch
history nicely linear, and automatically squashes all changes in a Pull Request
into a single commit. However, squash-and-merge sometimes overwrites the Author
field of a commit, which introduces problems in terms of IP provenance. For
that reason, we have decided to switch back to a simpler 'merge-commit'
strategy. See also [Github issue 2011](https://github.com/eclipse/rdf4j/issues/2011).

## Self-contained changes, pull requests, and commits

We define a *self-contained change* as a change that addresses a single issue,
addresses it completely, and does not also address other issues.

We expect every pull request to be a self-contained change. Note that that does
not mean that a pull request can only contain a single commit: it can have
several commits that together form a self-contained change.

We do, however, prefer fewer commits before merging, so if you have created a
long list of commits on our branch, we may ask you to [squash](/documentation/developer/squashing) it first.

### Commit messages

We prefer every commit message to be descriptive: it should start with the
github issue number to which it relates, then have a short one line description
that details the specific change.

Examples of good commit messages:

- "GH-1234 else condition no longer hits NPE in MyFancyClass"
- "GH-666 added test for corner case where user inputs negative number"

Examples of poor commit messages:

- "typo"
- "GH-666 typo"
- "GH-1234 fixed the problem"

We prefer meaningful commits because:

- it makes reviewing the pull request easier;
- after your PR has been merged, your commit messages become part of the main branch's history, and having each commit linked to an issue and meaningfully described makes it easier to figure what got changed where and why.

That said, if occassionally a less "perfect" commit message slips through, that's
fine. We're all human.
