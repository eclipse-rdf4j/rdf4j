# How to contribute

So you want to help out making RDF4J better. That's great, we welcome all contributions! 
Before you dive in, here are some things you need to know.

**Table of Contents**  

- [Legal stuff](#legal-stuff)
- [Creating your contribution](#creating-your-contribution)
- [Workflow](#workflow) 
 - [Releases](#releases)
  - [Release branches](#release-branches)
- [Code formatting](#code-formatting)
	
## Legal stuff

RDF4J is a project governed by the [Eclipse Foundation](http://www.eclipse.org/), which has strict [policies and guidelines](https://wiki.eclipse.org/Development_Resources#Policies_and_Guidelines) regarding contributions.

In order for any contributions to RDF4J to be accepted, you MUST do the following things:

1. Digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php). You can do this as follows: 

  * If you haven't done so already, [register an Eclipse account](https://dev.eclipse.org/site_login/createaccount.php). Use the same email address when you register for the account that you intend to use on Git commit records. 
  * Log into the [Eclipse projects forge](http://www.eclipse.org/contribute/cla); click on the "Eclipse Contributor Agreement" tab; and complete the form. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info. 

2. Add your github username in your [Eclipse account settings](https://dev.eclipse.org/site_login/#open_tab_accountsettings).

3. "Sign-off" your commits
Every commit you make in your patch or pull request MUST be "signed off".
You do this by adding the `-s` flag when you make the commit(s).

## Creating your contribution

Once the legalities are out of the way you can dig in. Here's how:

1. Create an issue in the [RDF4J GitHub issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix. Alternatively, comment on an existing issue to indicate you're keen to help solve it.
2. Fork the repository on GitHub.
3. Create a new branch for your changes starting from the `master` branch. See [Workflow](#workflow) for details.
4. Make your changes. Apply [RDF4J code formatting guidelines](#code-formatting)
5. Make sure you include tests.
6. Make sure the test suite passes after your changes.
7. Commit your changes into the branch. Use meaningful commit messages. Reference the issue number in the commit message (for example "issue #276: added null check").
8. **Sign off** every commit you do, using the `-s` flag (as explained in the [legal stuff](#legal-stuff).
9. Optionally squash your commits (not necessary, but if you want to clean your commit history a bit, _this_ is the point to do it).
10. Push your changes to your branch in your forked repository.
11. If your contribution is complete, use GitHub to submit a pull request (PR)
	for your contribution back to `master` in the central RDF4J repository.
	Once you have submitted your PR, do not use your branch for any other
	development (unless asked to do so by the reviewers of your PR). 

Once you've put up a PR, we will review your contribution, possibly make some
suggestions for improvements, and once everything is complete it will be merged
into the `master` branch, to be included in the next minor or major release. If
your contribution is a bug fix in an existing release, we will also schedule it
for inclusion in a patch release.

## Workflow

The short-short version for contributors: work from the `master` branch. 

The more complete version: RDF4J uses a git branching model where feature
development takes place on branches from the `master` branch. This is where all
development for the next (minor or major) release happens.

Every issue, no matter how small, gets its own branch while under development.
Issue branch names are always prefixed with `issues/`, followed by the issue
number in the [GitHub issue tracker](https://github.com/eclipse/rdf4j/issues),
followed by one or two dash-separated keywords for the issue. 

For example: `issues/#276-rdfxml-sax` is the branch for a fix for
GitHub issue #276, which has to do with RDF/XML and the SAX parser.

Once a feature/fix is complete, tested, and reviewed (via a Pull Request), it
is merged into master, and the issue branch is closed.

### Releases

RDF4J is on an 8-week release cycle for minor and major releases. See the [Milestone overview](https://github.com/eclipse/rdf4j/milestones) in the [issue tracker](https://github.com/eclipse/rdf4j/issues) for an overview of planned releases.

In order to guarantee stability of the release and to ensure we have enough
time to complete release review, we use a feature cutoff date. This date is
typically three weeks before the release date. The feature cutoff date does not
mean that each feature needs to be complete, but it does mean that we ask every
contributor to make a commitment to have their feature complete and stable in
time for final review and release. If a contributor cannot make that
commitment, the feature is not included in the release and is simply
"postponed" to the next release cycle.

We occasionally also schedule *patch releases*, which only include (backward
compatible) bug fixes. These releases are typically planned on short notice and
can be done without much in the way of formal review.

#### Release branches

A few days before release of a minor or major version, we create a *release
branch* from the current head of the master branch. The purpose of this release
branch is to perform last-minute tweaks and little fixes. Once we are ready to
release, the latest commit of the release branch is tagged with the version number, and
the release branch is merged back to master, then closed.

## Code formatting

RDF4J uses custom code formatting settings and a few code templates as well, and we expect every contributor to apply these settings before submitting a contribution.

The simplest way to do apply code formatting is as follows:

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Formatter' 
2. Click 'Import...' and find the file `eclipse-settings/rdf4j-formatting.xml`, located in the git repository root.
3. The active profile should now say 'RDF4J formatting'. Click ok.
4. When making changes to Java code, hit Ctrl+Shift+F before saving to reformat the code. Alternatively, you can configure Eclipse to automatically apply formatting on save. This can be activated by going to 'Preferences' -> 'Java' -> 'Editor' -> 'Save Actions' and making sure the 'Format source code' option checkbox is ticked.

Similarly, to apply templates:

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Code Templates' 
2. Click 'Import...' and find the file `eclipse-settings/codetemplates.xml`, located in the git repository root.
3. Click OK. The templates will be automatically applied when necessary. 

By the way: if you do not use Eclipse IDE, we still welcome your contributions
of course. We appreciate it if you make an effort to format your code to at least these
principles:

1. Use tabs only for indentation
2. Use unix newlines consistently
3. Use UTF-8 file encoding
4. Set line width to 110 characters 
5. Make sure every new source file starts with the correct Eclipse copyright license header. The license header to use is:
```
/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 ```
 **NB** for older existing source code, RDF4J uses a slightly different copyright header, mentioning 'Aduna' as additional copyright holder . Please make sure you do not use that different header for any new contributions. 
