---
title: "Developer workflow and project management"
layout: "doc"
toc: true
autonumbering: true
---

In this document the Eclipse RDF4J project workflow and developer best practices are explained. It contains information on how to create branches, tag releases, manage pull requests, create and schedule issues, and so on.
<!--more-->
Some of this information is targeted specifically at the project lead(s), other information is relevant to every committer.

## Semantic Versioning

RDF4J strives to apply [Semantic Versioning](http://www.semver.org/) principles to its development:

1. We use a `MAJOR.MINOR.PATCH` versioning template.
2. A *PATCH* release (2.2.1, 2.2.2, etc.) is a release that contains only bug fixes that are binary compatible and source compatible.
3. A *MINOR* release (2.0.0, 2.1.0, 2.2.0, etc.) is a release that can contain improvements and new features but makes no binary-incompatible changes to existing functionality.
4. A *MAJOR* release (1.0.0, 2.0.0, 3.0.0, etc) is a release that can contain changes to the public API that are not compatible.

We allow changes to public or protected methods/classes/interfaces in *minor* releases under the following conditions:

1. any renamed _interface_ is declared an extension of the old interface. The old interface is marked deprecated with Javadoc containing a reference to the new name;
2. any renamed _class_ is declared a superclass of the old class. The old class is marked deprecated with Javadoc containing a reference to the new name;
3. any renamed _member_ is added next to the old member name. The old member is declared deprecated with Javadoc containing a reference to the new name.
4. any class, interface or method that is annotated with `@Experimental` or `@InternalUseOnly` is not considered part of the public API, and may be changed in a minor release.

For patch releases we never allow changes in the public API, unless the change is specifically to fix a bug that aligns the actual behavior of the code with the publicly documented behavior.

The main branches (`main` and `develop`) use a SNAPSHOT version number to indicate that they are snapshots on the road to the next version. The `main` version always has the same major and minor number as the latest release, with the patch version incremented by one: for example if the latest release was 3.1.0, the `main` version will be 3.1.1-SNAPSHOT. The `develop` version uses the next expected major/minor release number, for example 3.2.0-SNAPSHOT.

## Workflow

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
the next (minor or major) release happens. The `main` branch is reserved for
small bug fixes (to be released in patch/service releases) only.

Once a issue is complete and tested, a *Pull Request* (PR) should be created
for peer review. Like the feature branch to which it corresponds, a Pull
Request should be a self-contained change, that is it fixes a single issue.
Don't be tempted to fix several unrelated issues in a single PR please.

The Pull Request description should start with a link to the
issue that is addressed by the PR. If the issue is a new feature or improvment,
the PR should target the `develop` branch. If the issue is a bug fix, the PR
should be branched from (and target) the `main` branch.

Tip: when starting work on an issue, and you are unsure if it will be a new
feature or "just" a bug fix, start by branching from the `main` branch. It
will always be possible to merge your issue branch into `develop` later if
necessary. However, if you start from `develop`, merging into `main` will not
be possible, and you're therefore committed to the next minor/major release.

RDF4J uses 'merge-commits' as its pull request merge strategy. We aim to
achieve a clean but accurate history. Read more about our strategy and the
motivation for it in this article: [RDF4J merge
strategy](/documentation/developer/merge-strategy/).

For step-by-step instructions on how to create contributions, see the [contributor guidelines](https://github.com/eclipse/rdf4j/blob/main/CONTRIBUTING.md).

## Further reading

Some generic sources of information about projects hosted by Eclipse:

* [The Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/)
* [The Eclipse Development Process](https://eclipse.org/projects/dev_process/index-quick.php)
* [Committer Cheat Sheet](https://wiki.eclipse.org/Development_Resources/Committer_Cheat_Sheet)
