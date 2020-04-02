---
title: "Developer workflow and project management"
layout: "doc"
hide_page_title: "true"
---

# Developer Workflow

In this document the Eclipse RDF4J project workflow and developer best practices are explained. It contains information on how to create branches, tag releases, manage pull requests, create and schedule issues, and so on. Some of this information is targeted specifically at the project lead(s), other information is relevant to every committer.

# Semantic Versioning

RDF4J strives to apply [Semantic Versioning](http://www.semver.org/) principles to its development:

1. We use a `MAJOR.MINOR.PATCH` versioning template.
2. A *PATCH* release (2.2.1, 2.2.2, etc.) is a release that contains only bug fixes that are backwards compatible.
3. A *MINOR* release (2.0.0, 2.1.0, 2.2.0, etc.) is a release that can contain improvements and new features but makes no backward-incompatible changes to existing functionality.
4. A *MAJOR* release (1.0.0, 2.0.0, 3.0.0, etc) is a release that can contain changes to the public API that are not backward compatible.

It is currently not fully specified what the boundaries of the RDF4J public API are. Until this is resolved (see [issue #619](https://github.com/eclipse/rdf4j/issues/619)), we allow changes to public or protected methods/classes/interfaces in *minor* releases under the following conditions:

1. any renamed _interface_ is declared an extension of the old interface. The old interface is marked deprecated with Javadoc containing a reference to the new name;
2. any renamed _class_ is declared a superclass of the old class. The old class is marked deprecated with Javadoc containing a reference to the new name;
3. any renamed _member_ is added next to the old member name. The old member is declared deprecated with Javadoc containing a reference to the new name.

These conditions are to ensure that existing user code will continue to work when upgrading to the new release. If there is any doubt about a change being backwards-compatible, it can not be made part of a minor release.

For patch releases we never allow changes in the public API, unless the change is specifically to fix a bug that aligns the actual behavior of the code with the publicly documented behavior.

The main branches (`master` and `develop`) use a SNAPSHOT version number to indicate that they are snapshots on the road to the next version. The `master` version always has the same major and minor number as the latest release, with the patch version incremented by one: for example if the latest release was 3.1.0, the master version will be 3.1.1-SNAPSHOT. The `develop` version uses the next expected major/minor release number, for example 3.2.0-SNAPSHOT.

# Workflow

Every issue, no matter how small, gets its own [issue
ticket](https://github.com/eclipse/rdf4j/issues), and its own branch while
under development. The milestone label of the issue is set to the *planned*
release version for the issue, but that could change by the time a PR is
merged. Issue branch names are always prefixed with `GH-<issuenumber>-`,
followed by one or two dash-separated keywords for the issue.

For example: `GH-1664-transformation-servlet` is the branch for a fix for issue
`GH-1664`, which has to do with the transformation servlet.

RDF4J uses a git branching model where collaborative feature development takes
place on branches from the `develop` branch. This is where all development for
the next (minor or major) release happens. The `master` branch is reserved for
small bug fixes (to be released in patch/service releases) only. 

Once a issue is complete and tested, a *Pull Request* (PR) should be created
for peer review. Like the feature branch to which it corresponds, a Pull
Request should be a self-contained change, that is it fixes a single issue.
Don't be tempted to fix several unrelated issues in a single PR please.

The Pull Request description should start with a link to the
issue that is addressed by the PR. If the issue is a new feature or improvment,
the PR should target the `develop` branch. If the issue is a bug fix, the PR
should be branched from (and target) the `master` branch.

Tip: when starting work on an issue, and you are unsure if it will be a new
feature or "just" a bug fix, start by branching from the `master` branch. It
will always be possible to merge your issue branch into `develop` later if
necessary. However, if you start from `develop`, merging into `master` will not
be possible, and you're therefore committed to the next minor/major release.

RDF4J uses 'squash and merge' as its pull request merge strategy, to preserve a
clean history. Read more about our strategy and the motivation for in this
article: [RDF4J merge strategy](/documentation/developer/merge-strategy/).

## Patch Requests

If the change is a bug fix, contains no new features, and does not change any public or protected APIs:

1. Create an issue in our [issue tracker](https://github.com/eclipse/rdf4j/issues) if it doesn't exist yet.
1. Create an issue branch by branching off from the `master` branch, using `GH-<issuenumber>-short-description` as the branch name convention.
2. Make the necessary changes and verify the codebase.
3. Optionally [squash your commits](../squashing) to clean up your branch.
3. Create a Pull Request that targets the `master` branch.
4. Peers and project committers now have a chance to review the PR and make suggestions.
5. Any modifications can be made to the _issue_ branch as recommended.
6. Once any necessary changes have been made, project committers can mark the PR as approved.
7. Project committers should then determine what patch release this fix will be included in by updating the milestone label of both the PR and the issue.
8. Once a Pull Request is approved and scheduled, it can be merged into the `master` branch, using 'squash and merge'.
9. After a PR has been merged into the `master` branch, the `master` branch should
then be merged into the `develop` branch by the project committer that merged the PR,
any conflicts (such as due to new features) should be resolved. This merge should happen using a merge-commit.

## Feature Requests

Pull Requests that add a self-contained new feature to the public API follow
the same steps as a Patch Request but should target the `develop` branch.
Only PRs that have been scheduled for the next minor release
should be merged into the `develop` branch.

Project committers that are contributing to a branch should periodically
pull changes from the `develop` branch (by either rebasing or merging) to minimize conflicts later on.
Once a feature is complete a PR should be created using the feature branch and target the `develop` branch.
Then follow similar steps to a patch request to schedule and merge into `develop`, using 'squash and merge'.

Minor and major releases require a formal [release
review](https://www.eclipse.org/projects/handbook/#release-review), and because
this is the case, they need to be planned well in advance, and the project lead
needs to manage what can go into each release, and prepare necessary
documentation (both technical and legal) for review. For this reason approved
Pull Requests may stay open (not scheduled or merged) for some time until a
release plan that incorporates these changes and any required documentation is
in place.  The comment section in the PR can be used to keep everyone informed
of the progress.

# Further reading

Some generic sources of information about projects hosted by Eclipse:

* [The Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/)
* [The Eclipse Development Process](https://eclipse.org/projects/dev_process/index-quick.php)
* [Committer Cheat Sheet](https://wiki.eclipse.org/Development_Resources/Committer_Cheat_Sheet)
