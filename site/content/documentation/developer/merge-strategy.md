---
title: "RDF4J merge strategy"
layout: "doc"
---

RDF4J values a clean, linear commit history on our main branches. To achieve this, we default to using [Squash and merge](https://help.github.com/en/github/administering-a-repository/about-merge-methods-on-github#squashing-your-merge-commits) as our merge strategy for all new features, improvements, or bug fixes. 

See also: [developer workflow](/documentation/developer/workflow/)

# Self-contained changes, pull requests, and commits

We define a *self-contained change* as a change that addresses a single issue,
addresses it completely, and does not also address other issues. 

We expect every pull request to be a self-contained change. Note that that does
not mean that a pull request can only contain a single commit: it can have
several commits that together form a self-contained change.

If a pull request is properly self-contained, merging it using squash and merge
will result in a single, self-contained commit on the main branch that
completely addresses a single issue.

## Commit messages

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

You may ask why we bother with this when we're going to squash everything into
a single commit anyway. We still prefer meaningful commits because:

- it makes reviewing the pull request easier;
- in a squash and merge, each individual commit message is added as a bullet
  list point in the description, so it is helpful if it is meaningful even
  after squashing.

That said, if occassionally a less "perfect" commit message slips through, that's
fine. We're all human.

And oh yeah: don't forget to [sign off your commits](/documentation/developer/workflow/#patch-requests)! 

# Motivation 

We use squash and merge because we value a clean, linear history over a more
detailed, accurate history for our main branches. There are several reasons we
value this:

1. it makes the history tree easier to read, with no "branch spaghetti"
2. it makes it more obvious what feature a particular change relates to when
   using blame or bisect tools. Because changes are self-contained, every
   commit on the main branch relates to a single feature or fix, and the
   context of any particular line changed as part of that is immediately
   obvious.

A common objection is that using squash and merge, you lose the (potentially
valuable) information the individual commits gave you. This is not quite true:
Github preserves the original commit history on the (closed) pull request page.
Since the squash and merge commit message refers back to this pull request
with a number, it can be easily found back even years later.

For an excellent in-depth discussion of the advantages of using squash and
merge, we recommend reading this blog article: [Two Years of squash
merge](https://blog.dnsimple.com/2019/01/two-years-of-squash-merge/).

# Exceptions

There is one standard exception to the rule: pull requests that involve
bringing our main branches (`master` and `develop`) in sync with each other use
a merge commit. The main reason for this is that here, it is more important to
track progression through time accurately, and we do want to preserve
individual commits.

In very rare cases, by exception, we allow a feature pull request to be merged
using a merge commit. This will only be done if the following conditions are
all met:

1. the author explicitly comments on the pull request that this is necessary (and why), and;
2. the author can show that the PR has been rebased (not merged!) so that the result of the merge will be near-linear, and;
3. the project lead has given explicit approval of the intent to use a merge commit.

