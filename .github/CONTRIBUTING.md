# How to contribute

So you want to help out making RDF4J better. That's great! Here's some things you need
to know and do before you dive in.

**Table of Contents**  

- [Legal stuff](#legal-stuff)
- [Developer Workflow: Which Branch?](#developer-workflow-which-branch)
 - [A note on the 1.0.x release branch and Java 7 compatibility](#a-note-on-the-10x-release-branch-and-java-7-compatibility)
- [Code formatting](#code-formatting)
- [Creating your contribution](#creating-your-contribution)
- [Submitting your changes](#submitting-your-changes)
	
## Legal stuff

RDF4J is a project governed by the [Eclipse Foundation](http://www.eclipse.org/), which has strict [policies and guidelines](https://wiki.eclipse.org/Development_Resources#Policies_and_Guidelines) regarding contributions.

In order for any contributions to RDF4J to be accepted, you MUST do the following things:

1. Digitally sign the [Eclipse Foundation Contributor License Agreement](http://www.eclipse.org/legal/CLA.php).
To sign the Eclipse CLA you need to:

  * Obtain an Eclipse Foundation userid. If you already use Eclipse Bugzilla or Gerrit you already have one of those. If you don’t, you need to
[register](https://dev.eclipse.org/site_login/createaccount.php).

  * Login into the [projects portal](https://projects.eclipse.org/), select “My Account”, and then the “Contributor License Agreement” tab. Read through, then sign.

2. Add your github username in your Eclipse Foundation account settings. Log in it to Eclipse and go to account settings.

2. "Sign-off" your commits

Every commit you make in your patch or pull request MUST be "signed off".
You do this by adding the `-s` flag when you make the commit(s).

## Developer Workflow: Which Branch?

RDF4J feature development takes place against the `master` branch. In addition,
each minor release has its own release branch (e.g. `releases/1.0.x`, and
`releases/2.0.x`), on which hotfixes/patches are done.

Every issue, no matter how small, gets its own branch. New features and enhancements are usually
developed on branches from the master branch. Patches or hotfixes for bugs in existing
releases are developed on a branch split off from the releveant release branch.
Issue branch names are always prefixed with `issues/`, followed by the issue
number in the [GitHub issue tracker](https://github.com/eclipse/rdf4j/issues),
followed by one or two dash-separated keywords for the issue. 

For example (fictional): `issues/#6-sparql-npe` is the branch for a fix for
GitHub issue #6, which has to do with SPARQL and a NullPointerException.

If you're unsure on which branch a contribution should be made, please ask!

### A note on the 1.0.x release branch and Java 7 compatibility ###

RDF4J 1.0 is a **backport** of RDF4J 2.0, to be executable on a Java 7 Runtime Environment. This means that we do not accept any fixes or feature development directly against the `releases/1.0.x` branch, and we never merge this branch back into either `master`, or the `releases/2.0.x` branch. 

Instead, new features are developed against the `master` branch, and these are never merged into either release branch (they are instead scheduled for inclusion in the next minor or major release: 2.1, or 3.0). Bug fixes and minor improvements (which are sufficiently minor that they can be considered part of a patch release) are developed against the `releases/2.0.x` branch, which can then be merged back into `releases/1.0.x` branch. 

Since `releases/1.0.x` is to be kept compatible with Java 7, it is inevitable that it will occassionally become unstable when merging the `releases/2.0.x` branch into it: the build process verifies that the branch is Java 7-compatible, so it will fail with a compilation error when this happens. These compilation failures can be fixed by branching a hotfix branch directly directly off the `releases/1.0.x` branch.

## Code formatting

RDF4J uses custom code formatting settings and a few code templates as well, and we expect _every_ contributor to apply these settings before submitting a contribution.

The simplest way to do apply code formatting is as follows:

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Formatter' 
2. Click 'Import...' and find the file `eclipse-settings/rdf4j-formatting.xml`, located in the git repository root.
3. The active profile should now say 'RDF4J formatting'. Click ok.
4. When making changes to Java code, hit Ctrl+Shift+F before saving to reformat the code. Alternatively, you can configure Eclipse to automatically apply formatting on save. This can be activated by going to 'Preferences' -> 'Java' -> 'Editor' -> 'Save Actions' and making sure the 'Format source code' option checkbox is ticked.

Similarly, to apply templates:

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Code Templates' 
2. Click 'Import...' and find the file `eclipse-settings/codetemplates.xml`, located in the git repository root.
3. Click OK. The templates will be automatically applied when necessary. 

## Creating your contribution

Once the legalities are out of the way and you're up-to-date on our workflow and code formatting, you can 
start coding. Here's how:

1. Create an issue in the [RDF4J GitHub issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix. Alternatively, comment on an existing issue to indicate you're keen to help solve it.
2. Fork the repository on GitHub
3. Create a new branch for your changes (see the Developer Guidelines above for details - please make sure you pick the correct and up-to-date starting branch!)
4. Make your changes
5. Make sure you include tests
6. Make sure the test suite passes after your changes
7. Commit your changes into the branch. Please use descriptive and meaningful commit messages. Reference the issue number in the commit message (for example " #6 added null check")
8. Sign off every commit you do, as explained above.
9. Optionally squash your commits (not necessary, but if you want to clean your commit history a bit, _this_ is the point to do it).
10. Push your changes to your branch in your forked repository

## Submitting your changes

Once you are satisfied your changes are in good shape, you can use GitHub to
submit a pull request (PR) for your contribution back to the central RDF4J
repository. Again, please make sure you submit your PR against the correct branch (the appropriate release branch if you fixed a bug, or the master branch if you developed a new feature). 
 
Once you have submitted your PR, do not use your branch for any other
development (unless asked to do so by the reviewers of your PR). If you do, any
further changes that you make will be visible in the PR.
