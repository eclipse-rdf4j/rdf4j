---
title: "Developer workflow and project management"
layout: "doc"
hide_page_title: "true"
---

# Developer Workflow and Project Management

In this document the Eclipse rdf4j project workflow and developer best practices are explained. It contains information on how to create branches, tag releases, manage pull requests, create and schedule issues, and so on. Some of this information is targeted specifically at the project lead(s), other information is relevant to every committer.

# Semantic Versioning

Rdf4j strives to apply [Semantic Versioning](http://www.semver.org/) principles to its development:

1. We use a `MAJOR.MINOR.PATCH` versioning template.
2. A *PATCH* release (2.2.1, 2.2.2, etc.) is a release that contains only bug fixes that are backwards compatible.
3. A *MINOR* release (2.0, 2.1, 2.2, etc.) is a release that can contain improvements and new features but makes no backward-incompatible changes to existing functionality.
4. A *MAJOR* release (1.0, 2.0, 3.0, etc) is a release that can contain changes to the public API that are not backward compatible.

It is currently not fully specified what the boundaries of the rdf4j public API are. Until this is resolved (see [issue #619](https://github.com/eclipse/rdf4j/issues/619)), we allow changes to public or protected methods/classes/interfaces in *minor* releases under the following conditions:

1. any renamed _interface_ is declared an extension of the old interface. The old interface is marked deprecated with Javadoc containing a reference to the new name;
2. any renamed _class_ is declared a superclass of the old class. The old class is marked deprecated with Javadoc containing a reference to the new name;
3. any renamed _member_ is added next to the old member name. The old member is declared deprecated with Javadoc containing a reference to the new name.

These conditions are to ensure that existing user code will continue to work when upgrading to the new release. If there is any doubt about a change being backwards-compatible, it can not be made part of a minor release.

For patch releases we never allow changes in the public API, unless the change is specifically to fix a bug that aligns the actual behavior of the code with the publicly documented behavior.

Only release branches (more on those later) include the full major.minor.patch
version numbers included with releases. The major and minor version of the _master_ and
_develop_ branches do not include a patch number and always have a SNAPSHOT tag.
The _master_ and _develop_ versions always matches the pattern *MAJOR*.*MINOR*-SNAPSHOT.

The _master_ version always has the same major and minor number as the latest
release while the _develop_ version uses the next expected major/minor
release number, such as 2.1-SNAPSHOT and 2.2-SNAPSHOT respectively
(after a 2.1.x release, but before any 2.2.x releases are made public).

# Subprojects

Rdf4j is split between five github projects. Those are:

* https://github.com/eclipse/rdf4j
* https://github.com/eclipse/rdf4j-storage
* https://github.com/eclipse/rdf4j-tools
* https://github.com/eclipse/rdf4j-testsuite
* https://github.com/eclipse/rdf4j-doc

# Workflow

Every issue, no matter how small, gets its own branch while under development.
The milestone label of the issue is set to the *planned* release version for the
issue, but that could change by the time a PR is merged.
Issue branch names are always prefixed with `issues/`, followed by one or two dash-separated keywords for the issue.

For example: `issues/rdfxml-sax` is the branch for a fix that has to do with RDF/XML and the SAX parser.

Rdf4j uses a git branching model where collaborative feature development takes
place on branches from the _develop_ branch. This is where all development for
the next (minor or major) release happens.

Once a issue is complete and tested, a *Pull Request* (PR) should be created for
peer review. The Pull Request description should start with a link to the issue that is addressed by the PR.
If the issue is part of a larger feature, the PR should be branched from
(and target) the corresponding `issues/` branch.
If the issue is a new feature in and of itself, the PR should be branched from
(and target) the _develop_ branch.
If the issue is a bug fix, the PR should be branched from
(and target) the _master_ branch.

Tip: when starting work on an issue, and you are unsure if it will be a new feature or "just" a bug fix, start by branching from the 
`master` branch. It will always be possible to merge your issue branch into `develop` later if necessary. However, if you start from `develop`, merging into `master` will not be possible, and you're therefore committed to the next minor/major release.

## Patch Requests

If the change is a bug fix, contains no new features, and
does not change any public or protected APIs then

1. Create an _issue_ branch by forking the _master_ branch.
2. Make the necessary changes and verify the codebase.
3. Create a Pull Request that targets the _master_ branch.
4. Peers and project committers now have a chance to review the PR and make suggestions.
5. Any modifications can be made to the _issue_ branch as recommended.
6. Once any necessary changes have been made, project committers can mark the PR as approved.
7. Project committers should then determine what patch release this fix will be included in by updating the milestone label of both the PR and the issue.
8. Only when a Pull Request is approved *and* scheduled for the next patch release it should be merged
into the _master_ branch then
9. the issue branch can be deleted (if desired).
10. After a PR has been merged into the _master_ branch, the _master_ branch should
then be merged into the _develop_ branch by the project committer that merged the PR,
any conflicts (such as due to new features) should be resolved.
This can be done from the command line from a clean checkout as follows:

    git checkout develop
    git pull origin master
    git push origin develop

## Feature Requests

Pull Requests that add a self contained new features to the public API follow
the same steps as a Patch Request but should fork and target the _develop_ branch.
However, Pull Requests that make changes to a feature that has not been
scheduled for the next minor release, should fork and target the corresponding
`issues/` branch.
Only PRs that have been scheduled for the next minor release
should be merged into the _develop_ branch.

Project committers that are contributing to a `issues/` branch should periodically
pull changes from the _develop_ branch to minimize conflicts later on.
Once a features is complete another PR should be created using the `issues/`
branch and target the _develop_ branch.
Then follow similar steps to a patch request to schedule and merge into _develop_.

Minor and major releases require a formal [release review](https://www.eclipse.org/projects/handbook/#release-review), and because this is the case, they need to be planned well in advance, and the project lead needs to manage what can go into each release, and prepare necessary documentation (both technical and legal) for review.
For this reason approved Pull Requests may stay open (not scheduled or merged)
for some time until a release plan that incorporates these changes and any
required documentation is in place.
The comment section in the PR can be used to keep everyone informed of the progress.

# Patch releases

Patch releases are created by branching the _master_ branch into a release branch, and
when complete, tagging this release branch with the version number before
release deployment. Once release deployment is complete, the release branch
is deleted.

IMPORTANT: It is important that the _master_ branch is always in a release ready state
(build passes, no new features, docs are upto date), so a patch release from the
_master_ branch can be done in an ad-hoc fashion, without the need for a formal review.

Plans to do a patch release are announced by the project lead on the
[rdf4j-dev@eclipse.org mailinglist](https://dev.eclipse.org/mailman/listinfo/rdf4j-dev),
usually about a week in advance, with an open invitation for contributors to
propose additional fixes to include, which are done as Pull Requests to the
_master_ branch.

## Creating a patch release branch

Any fixes to be included in the a patch release must be merged into the _master_
branch first.
A patch release branch should differ from the _master_ branch, at the time of
release, only by the version number - a patch release branch has a patch number
version, while the _master_ branch has a SNAPSHOT version.
To create a patch release branch, follow these steps:

1. Check out the _master_ branch.
   E.g. if we're preparing a release 2.2.1, the _master_ branch will have the version 2.2-SNAPSHOT:

    `git checkout master`

2. Create a new release branch, named `releases/<version>`:

    `git checkout -b releases/2.2.1`

3. Fix maven version numbers in the release branch. We need to set the project version to `2.2.1` by using:

    `mvn versions:set`<br>
    <br>
 This will ask for the new version number. Enter `2.2.1` to indicate that this is the 2.2.1 release.
 After this is done, execute

    `mvn versions:commit`<br>
    <br>
 which will remove backup files.
 Finally, commit the version number changes:

    `git commit -s -a -m "release 2.2.1"`

4. Tag the version and push the tag upstream:

    `git tag 2.2.1`<br>
    `git push origin 2.2.1`

NOTE: The release branch itself is usually not pushed upstream - you only use it locally to prepare the commit that sets the pom version numbers for the release. Also, the branch is not merged back into the master branch (because we want to keep the SNAPSHOT version number on the master branch). Once you pushed the tag, you can delete your local branch.

# Hotfix releases

Hotfix release are patch releases that target an prior minor version (not the
latest stable release). These are needed when a critical bug was found in a
production deployment using an earlier version.

A Hotfix release use a preceding release as its basis. This means we need to create a release branch not by simply branching from the current _master_ branch, but by branching from a specific release tag. To create a patch release branch, follow these steps:

1. Check out the tag of the previous release. E.g. if we're preparing a release 2.1.6, the preceding release is 2.1.5, so we do:

    `git checkout 2.1.5`

2. Create a new release branch, named `releases/<version>`:

    `git branch releases/2.1.6`

3. Fix maven version numbers in the release branch. We need to set the project version to `2.1.6-SNAPSHOT` by using:
    
    `mvn versions:set`<br>
    <br>
    This will ask for the new version number. Enter `2.1.6-SNAPSHOT` to indicate that this is the development branch for the upcoming 2.1.6 release. After this is done, execute

    `mvn versions:commit`<br>
    <br>
    which will remove backup files. Finally, commit the version number changes:

    `git commit -s -a -m "release branch for 2.1.6"`

4. Push the newly created branch, as follows:

    `git push origin releases/2.1.6`

Bug fixes are typically added to a hotfix release branch by [cherry-picking](https://git-scm.com/docs/git-cherry-pick) the relevant commits from the _master_ branch.

This works as follows:

1. Check out the patch release branch.

2. In the git commit history, identify the commit for the fix you wish to add to the release. You can usually easily find this by looking at the original Pull Request for the (normally the PR can be found by looking through the issue coments on GitHub). You're looking for a message in the PR about the merge, usually at the end, that looks like this:

    `jeenbroekstra merged commit 5d13554 into eclipse:master`<br>
    <br>
    The commit number (5d13554) is what you're after.

3. Add this fix to the release branch by executing the following command:

    `git cherry-pick -m 1 5d13554`<br>
    <br>
    The `-m 1` flag is necessary because this is a merge-commit, which has two parents: we need inform git which parent commit it needs to use as the base (we select 1, which is the _master_ branch, to ensure that _only_ changes introduced by this fix are included).

Once all fixes are applied to the release branch, and the build is stable (NB verify by executing `mvn clean verify`), we can tag and finalize the release:

1. Set the maven pom version numbers. We need to set the project version to from `2.1.6-SNAPSHOT` to `2.1.6` by using:

    `mvn versions:set`<br>
    <br>
    This will ask for the new version number. Enter `2.1.6` to indicate that this is the actual code for 2.1.6 release. After this is done, execute

    `mvn versions:commit`<br>
    <br>
    which will remove backup files. Finally, commit the changes and push:
    
    `git commit -s -a -m "patch release 2.1.6"`
    `git push`

2. Tag the version and push the tag upstream:

    `git tag 2.1.6`
    `git push origin 2.1.6`

Once the release is complete, the hotfix branch needs to be deleted. Although this can of course be done from the command line, it is cumbersome, and we recommend using a decent Git client (like SourceTree) that can do this for you.

Note that, although the branch is deleted, the release tag is still in place, for future use of further hotfix releases.

# Release distribution deployment

Rdf4j has two separate distributions:

1. the SDK and onejar archives, downloadable via http://www.rdf4j.org/download .
2. the Maven artifacts, available via [The Central Repository](http://search.maven.org).

RDF4j has four separate announcements:

1. the github [releases tab](https://github.com/eclipse/rdf4j/releases),
2. the [rdf4j download page](http://rdf4j.org/download/),
3. the [rdf4j.org website](http://rdf4j.org/wp-admin/edit.php), and
4. the [rdf4j-user mailling list](https://groups.google.com/forum/#!forum/rdf4j-users).

## Building and uploading Maven artifacts

We use the [Eclipse rdf4j Jenkins CI instance](https://ci.eclipse.org/rdf4j) to build and deploy new releases to the Central Repository.
To do this, log in to Jenkins, and start the job named `rdf4j-client-deploy-release-ossrh-central`.
The job will ask for the github release tag as an input parameters, e.g. '2.2.1'. It will automatically start deployment jobs for the `rdf4j-storage`, `rdf4j-tools`, and `rdf4j-testsuite` subprojects, in the correct sequence.

These jobs will automatically check out the release tag, build the project, and upload all artifacts to [OSS Sonatype](https://oss.sonatype.org/).
After successful upload, it will also automatically invoke synchronization with the Central Repository.
Note that after successful completion, the artifacts may not be available on the Central Repository for several hours.

## Building and uploading SDK and onejar

The SDK and onejar archives are hosted on https://www.eclipse.org/downloads/ . The archives need to be built locally, and uploaded manually, via secure FTP.

## Building the SDK and onejar

1. Check out the release tag of [rdf4j](https://github.com/eclipse/rdf4j)
2. From the root directory, execute the following:

   `mvn -Passembly clean install -DskipTests`

3. Repeat the above steps for [rdf4j-storage](https://github.com/eclipse/rdf4j-storage) and [rdf4j-tools](https://github.com/eclipse/rdf4j-tools).
4. Run the above command a fourth time from the assembly directory of the tools project.
5. Once this completes, the SDK and onejar can be found in `assembly/target` of the rdf4j-tools subproject.
6. Verify that the SDK is complete by inspecting its contents (in particular, check that javadoc is included).

## Uploading the SDK and onejar

1. SFTP to `build.eclipse.org`. You will need to provide your eclipse username and password.
2. Go to remote directory `/home/data/httpd/download.eclipse.org/rdf4j`.
3. Upload the SDK and onejar archives to this directory (NB we currently only distribute the SDK zip file, not the tar.gz file)

# Minor and Major releases

Minor and major releases require a formal [release review](https://www.eclipse.org/projects/handbook/#release-review), and because this is the case, they need to be planned well in advance, and the project lead needs to manage what can go into each release, and prepare necessary documentation (both technical and legal) for review.

We plan each release about 8 weeks in advance. At this stage, the final feature set is not etched in stone but a number of priority features/improvements is identified (via discussion on the mailinglist and/or via issue tracker comments and PRs) and scheduled. A first draft of a release plan is created by the project lead on the https://projects.eclipse.org/projects/technology.rdf4j[Eclipse rdf4j project site], and the necessary milestones are created in the https://github.com/eclipse/rdf4j/issues[rdf4j issue tracker].
In addition, the [rdf4j Pull Requests](https://github.com/eclipse/rdf4j/pulls) milestones are updated as the code is ready for inclusion.

## Review planning and application

A release can only be done once its review is successfully concluded. Eclipse release review are announced in regular cycles, and always complete on the first or third Wednesday of each month. For this reason, we schedule our releases to happen on a first or third Thursday.

A release review runs for a week. Although mostly a formality, it does need some careful preparation and planning. It needs to be formally applied for, and this application in turn requires that several pieces of documentation are in order:

1. The project's [IP log](https://www.eclipse.org/projects/handbook/#ip-iplog-generator) needs to be filed and approved by the Eclipse legal team;<br>
   The IP log can be automatically generated and submitted to the legal team. Obtaining approval may require several days, so it's good practice to submit this at least two weeks before the planned release date.
2. The project's [review documentation](https://projects.eclipse.org/projects/technology.rdf4j), part of the application, needs to be in order.

Typical review documentation can be a simple reiteration of the most important new features, a link to the issue tracker/release notes and documentation, and a remark about compatibility (if applicable). Once the review documentation is up, a mail needs to be sent to `technology-pmc@eclipse.org` to ask for approval. Here's an example of such a message, which was to get approval for the rdf4j 2.2 release:

    Dear PMC members,

    Can I get your approval for rdf4j release 2.2, scheduled for February 2.

    Release review info: https://projects.eclipse.org/projects/technology.rdf4j/reviews/2.2-release-review

    Issue tracking the release: https://bugs.eclipse.org/bugs/show_bug.cgi?id=510577

    Kind regards,

    Jeen Broekstra

When IP log approval and review approval have been given, the review can be scheduled. To do this, emo@eclipse.org needs to be notified. This can happen through the [eclipse project governance page](https://projects.eclipse.org/projects/technology.rdf4j/) (accessible through the project site), which will show a link at the top of the page for the planned release.

For more detailed information about the release review process, see the [Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/).

## Branching minor releases

Prior to a minor release, the _develop_ branch is merged into the _master_ branch
(along with the _develop_ branch's version) to facilitate release review.
This will increment the _master_ version to the latest major/minor SNAPSHOT version.
After the review is complete the steps to create a minor release are the same as the patch release steps.

IMPORTANT: It is important that only features and fixes that have already been scheduled
for release (via PR milestone labels) be merged into the _develop_ branch, so
that there is no confusion as to what will be included in the next minor release.

When the release has been approved, a release branch from the _master_ is created and
tagged with the version number before release deployment.
Once release deployment is complete, the release branch is deleted.

Once a minor release is published the _develop_ minor version should be incremented to the next SNAPSHOT
version and any approved features that are scheduled for this next minor
version should be merged into _develop_ branch.

# Optional: publishing docker images

Occasionally a docker server/workbench image could be built 
and pushed to `https:/hub.docker.com/eclipse/rdf4j-workbench`,
which is part of the Eclipse organizational account.
Since this account is managed separately by the Eclipse Foundation,
only a limited number of committers will be granted access by the EMO.
 
The Dockerfiles are stored in `rdf4j-tools/assembly/src/main/dist/docker`,
and currently there are two supported architectures: `amd64` (Intel/AMD x86-64) and `arm64v8`.

Note that docker does not support building cross-architecture out of the box,
so either two systems or some additional software (e.g. QEMU) will be needed. See also
https://www.ecliptik.com/Cross-Building-and-Running-Multi-Arch-Docker-Images.


Go to the previously mentioned docker directory and build the image(s):

    docker build --build-arg VERSION=2.5.0 --file Dockerfile.amd64 \
                 --tag eclipse/rdf4j-workbench:amd64-2.5.0-testing .

Verify the image by running it:

    docker run -p 8080:8080 eclipse/rdf4j-workbench:amd64-2.5.0-testing

After a fews seconds, the workbench should be avaible on `http://localhost:8080/rdf4j-workbench`.
Check the rdf4j version in System / Information.

Log in on `hub.docker.com`, and push the image:

    docker login
    docker push eclipse/rdf4j-workbench:amd64-2.5.0-testing

# Further reading

Some generic sources of information about projects hosted by Eclipse:

* [The Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/)
* [The Eclipse Development Process](https://eclipse.org/projects/dev_process/index-quick.php)
* [Committer Cheat Sheet](https://wiki.eclipse.org/Development_Resources/Committer_Cheat_Sheet)
