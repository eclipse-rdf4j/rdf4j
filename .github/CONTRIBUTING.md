# How to contribute

So you want to help out making RDF4J better. Great! Here's some things you need
to know and do before you dive in.

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

## Developer Guidelines

RDF4J feature development takes place off of the `master` branch. In addition,
each minor release has its own release branch (e.g. `releases/1.0.x`, and
`releases/2.0.x`), on which hotfixes/patches are done.

Every issue, no matter how small, gets its own feature branch. New features are usually
developed on branches from the master branch. Patches or hotfixes for existing
releases are developed on a branch split off from the releveant release branch.
Issue branch names are always prefixed with `issues/`, followed by the issue
number in the [GitHub issue tracker](https://github.com/eclipse/rdf4j/issues),
followed by one or two dash-separated keywords for the issue. 

For example (fictional): `issues/#6-sparql-npe` is the branch for a fix for
GitHub issue #6, which has to do with SPARQL and a NullPointerException.

If you're unsure on which branch a contribution should be made, please ask!

### Code formatting

In the directory `eclipse-settings` in our main repository you will find
Eclipse code formatting setting configurations. Please import these into your
Eclipse workspace and make sure they are applied to your code contributions.

## Creating your contribution

Once the legalities are out of the way and you're up-to-date on developer guidelines, you can 
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

## Submitting the changes

Once you are satisfied your changes are in good shape, you can use GitHub to
submit a pull request (PR) for your contribution back to the central RDF4J
repository.
 
Once you have submitted your PR, do not use your branch for any other
development (unless asked to do so by the reviewers of your PR). If you do, any
further changes that you make will be visible in the PR.

# Credit

This document was copied and adapted from the one available at the ICE project:

 https://github.com/eclipse/ice/blob/master/CONTRIBUTING.md

