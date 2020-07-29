---
title: "Release management"
toc: true
---

This document outlines how to create a new release of RDF4J.
<!--more-->

# The simple way: using the release script

In the project root, the script `release.sh` is a shell-script that (almost) fully automates the handling of releases. It creates branches, sets correct version numbers, builds and uploads artifacts, etc. It gives you several prompts along the way to guide you through the process. 

The release script should always be run from the `master` branch.

If for whatever reason, you wish to manually create a release instead, the following sections detail the manual process.

# Patch releases

Patch releases are created by branching the `master` branch into a release branch, and
when complete, tagging this release branch with the version number before
release deployment. Once release deployment is complete, the release branch
is deleted.

IMPORTANT: the `master` branch is always in a release ready state (build
passes, no new features, docs are up to date), so a patch release from the
`master` branch can be done in an ad-hoc fashion, without the need for a formal
review.

Plans to do a patch release are announced by the project lead on the
[rdf4j-dev@eclipse.org mailinglist](https://dev.eclipse.org/mailman/listinfo/rdf4j-dev),
usually about a week in advance, with an open invitation for contributors to
propose additional fixes to include, which are done as Pull Requests to the
`master` branch.

## Creating a patch release branch

Any fixes to be included in a patch release must be merged into the `master`
branch first.  A patch release branch should differ from the `master` branch,
at the time of release, only by the version number - a patch release branch has
a patch number version, while the `master` branch has a SNAPSHOT version.  To
create a patch release branch, follow these steps:

1. Check out the `master` branch.
   E.g. if we're preparing a release 2.2.1, the `master` branch will have the version 2.2.1-SNAPSHOT:

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

4. Tag the version and push tag and branch upstream:

    `git tag 2.2.1`<br>
    `git push -u origin releases/2.2.1`
    `git push origin 2.2.1`

5. Prepare the release branch for the next iteration:

   `mvn versions:set` and enter `2.2.2-SNAPSHOT` as the new snapshot number
   `mvn versions:commit`
   `git commit -s -a -m "next development iteration"`
   `git push`

6. Finally, create a pull request to merge the release branch back into master. Like branch sync PRs, this PR will be merged by means of a merge-commit, rather than the default 'squash and merge', so as not to lose the version-tagged commit.

# Hotfix releases

Hotfix release are patch releases that target a prior minor version (not the
latest stable release). These are needed when a critical bug was found in a
production deployment using an earlier version.

A hotfix release use a preceding release as its basis. This means we need to create a release branch not by simply branching from the current `master` branch, but by branching from a specific release tag. To create a patch release branch, follow these steps:

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

Bug fixes are typically added to a hotfix release branch by [cherry-picking](https://git-scm.com/docs/git-cherry-pick) the relevant commits from the `master` branch.

This works as follows:

1. Check out the patch release branch.

2. In the git commit history, identify the commit for the fix you wish to add to the release. You can usually easily find this by looking at git commit message, which should start with the issue nmber.

3. Add this fix to the release branch by executing the following command:

    `git cherry-pick <commit-SHA>`<br>

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

Once the release is complete, the hotfix branch can to be deleted. Although this can of course be done from the command line, it is cumbersome, and we recommend using a decent Git client (like SourceTree) that can do this for you.

Note that, although the branch is deleted, the release tag is still in place, for future use of further hotfix releases.

# Release distribution deployment

RDF4J has two separate distributions:

1. the SDK and onejar archives, downloadable via http://www.rdf4j.org/download .
2. the Maven artifacts, available via [The Central Repository](http://search.maven.org).

RDF4J has four separate announcements:

1. the github [releases tab](https://github.com/eclipse/rdf4j/releases),
2. the [RDF4J download page](http://rdf4j.org/download/),
3. the [RDF4J.org website](http://rdf4j.org/wp-admin/edit.php), and
4. the [rdf4j-user mailing list](https://groups.google.com/forum/#!forum/rdf4j-users).

## Building and uploading the release

We use the [Eclipse RDF4J Jenkins CI instance](https://ci.eclipse.org/rdf4j) to build and deploy new releases to the Central Repository, as well as building and deploying the SDK archive and the onejar to the Eclipse download mirrors.

To do this, log in to Jenkins, and start the job named `rdf4j-deploy-release-sdk`.
The job will ask for the github release tag as an input parameters, e.g. '2.2.1'. It will automatically check out the release tag, build the project, and upload the SDK zip file and the onejar to the Eclipse download mirrors. 

After successful completion, it will kick off a second job:
`rdf4j-deploy-release-ossrh`. This job will build all Maven artifacts and
upload them to [OSS Sonatype](https://oss.sonatype.org/).  After successful
upload, it will also automatically invoke synchronization with the Central
Repository.  Note that after successful completion, the artifacts may not be
available on the Central Repository for several hours.

# Minor and Major releases

Minor and major releases require a formal [release review](https://www.eclipse.org/projects/handbook/#release-review), and because this is the case, they need to be planned well in advance, and the project lead needs to manage what can go into each release, and prepare necessary documentation (both technical and legal) for review.

We plan each release about 8 weeks in advance. At this stage, the final feature set is not etched in stone but a number of priority features/improvements is identified (via discussion on the mailinglist and/or via issue tracker comments and PRs) and scheduled. A first draft of a release plan is created by the project lead on the [Eclipse RDF4J project site](https://projects.eclipse.org/projects/technology.rdf4j), and the necessary milestones are created in the [issue tracker](https://github.com/eclipse/rdf4j/issues).

## Review planning and application

A release can only be done once its review is successfully concluded. Eclipse release review are announced in regular cycles, and always complete on the first or third Wednesday of each month. For this reason, we schedule our releases to happen on a first or third Thursday.

A release review runs for a week. Although mostly a formality, it does need some careful preparation and planning. It needs to be formally applied for, and this application in turn requires that several pieces of documentation are in order:

1. The project's [IP log](https://www.eclipse.org/projects/handbook/#ip-iplog-generator) needs to be filed and approved by the Eclipse legal team;<br>
   The IP log can be automatically generated and submitted to the legal team. Obtaining approval may require several days, so it's good practice to submit this at least two weeks before the planned release date.
2. The project's [review documentation](https://projects.eclipse.org/projects/technology.rdf4j), part of the application, needs to be in order.

Typical review documentation can be a simple reiteration of the most important new features, a link to the issue tracker/release notes and documentation, and a remark about compatibility (if applicable). Once the review documentation is up, a mail needs to be sent to `technology-pmc@eclipse.org` to ask for approval. Here's an example of such a message, which was to get approval for the RDF4J 2.2 release:

    Dear PMC members,

    Can I get your approval for RDF4J release 2.2, scheduled for February 2.

    Release review info: https://projects.eclipse.org/projects/technology.rdf4j/reviews/2.2-release-review

    Issue tracking the release: https://bugs.eclipse.org/bugs/show_bug.cgi?id=510577

    Kind regards,

    Jeen Broekstra

When IP log approval and review approval have been given, the review can be scheduled. To do this, emo@eclipse.org needs to be notified. This can happen through the [eclipse project governance page](https://projects.eclipse.org/projects/technology.rdf4j/) (accessible through the project site), which will show a link at the top of the page for the planned release.

For more detailed information about the release review process, see the [Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/).

## Branching minor releases

Prior to a minor release, the `develop` branch is merged into the `master` branch
(along with the `develop` branch's version) to facilitate release review.
This will increment the `master` version to the latest major/minor SNAPSHOT version.
After the review is complete the steps to create a minor release are the same as the patch release steps.

IMPORTANT: It is important that only features and fixes that have already been scheduled
for release (via PR milestone labels) be merged into the `develop` branch, so
that there is no confusion as to what will be included in the next minor release.

Once a minor release is published the `develop` minor version should be incremented to the next SNAPSHOT
version and any approved features that are scheduled for this next minor
version should be merged into `develop` branch.

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
Check the RDF4J version in System / Information.

Log in on `hub.docker.com`, and push the image:

    docker login
    docker push eclipse/rdf4j-workbench:amd64-2.5.0-testing

