# How to contribute

So you want to help out making Eclipse RDF4J better. That's great, we welcome your contributions! 
Before you dive in, here are some things you need to know.

## Terms of Use

This repository is subject to the Terms of Use of the Eclipse Foundation

* http://www.eclipse.org/legal/termsofuse.php

## Eclipse Development Process

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

* https://eclipse.org/projects/dev_process
* https://www.eclipse.org/org/documents/Eclipse_IP_Policy.pdf

## Eclipse Contributor Agreement

In order to be able to contribute to Eclipse Foundation projects you must
electronically sign the Eclipse Contributor Agreement (ECA).

* http://www.eclipse.org/legal/ECA.php

The ECA provides the Eclipse Foundation with a permanent record that you agree
that each of your contributions will comply with the commitments documented in
the Developer Certificate of Origin (DCO). Having an ECA on file associated with
the email address matching the "Author" field of your contribution's Git commits
fulfills the DCO's requirement that you sign-off on your contributions.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

### Signing the Eclipse Contributor Agreement
You must digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php). You can do this as follows:

* If you haven't done so already, [register an Eclipse account](https://accounts.eclipse.org/user/register). **Important**: Use the same email address that you will use on Git commits as the author address.
* Open the [ECA form](https://accounts.eclipse.org/user/eca) and complete it. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info.


## Creating your contribution

Once the legalities are out of the way you can dig in. Here's how:

1. Create an issue in the [issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix. Alternatively, comment on an existing issue to indicate you're keen to help solve it.
2. Fork the repository on GitHub.
3. Create a new branch for your changes starting from the `main` branch. Name your branch like this: `GH-1234-short-description-here` where 1234 is the Github issue number. See [Workflow](#workflow) for details.
4. Make your changes. Apply the [RDF4J code formatting guidelines](#code-formatting) by running `mvn process-resources`.
5. Make sure you include tests. We use JUnit 5 with AssertJ and Mockito. Have a look around for existing tests to get some idea, and of course feel free to ask advice.
6. Make sure the test suite passes after your changes: you can run `mvn verify` to run tests locally.
7. Commit your changes into the branch. Make sure the commit author name and e-mail correspond to what you used to sign the ECA. Use meaningful commit messages. Reference the issue number in the commit message (for example "GH-276: added null check").
8. Push your changes to your branch in your forked repository.
9. Optionally [squash your commits](https://rdf4j.org/documentation/developer/squashing) to clean up the commit history.
10. Use GitHub to submit a pull request (PR) for your contribution back to `main` in the central RDF4J repository.  Once you have submitted your PR, do not use your branch for any other development (unless asked to do so by the reviewers of your PR). 

Once you've put up a PR, we will review your contribution, possibly make some suggestions for improvements, and once everything is complete it will be merged into the `main` branch (if it's a bug fix to be included in the next maintenance release) or into the `develop` branch (if it's a feature or improvement to be included in the next minor or major release).

We are happy to receive "work in progress" pull requests as well: if you're not quite finished, but would like some early feedback on your approach, feel free to publish a PR early and ask us to review. 

## Code formatting

Eclipse RDF4J follows the [Eclipse Coding Conventions for Java](https://wiki.eclipse.org/Coding_Conventions), with a couple of minor modifications:

- We use a line width of 120 characters.
- We use Unix line endings (LF).
- We require curly braces for every control statement body (e.g. if-else), even if it is a single line.
- We use the following header comment on every Java file:

```
/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
 ```

...of course, replace `${year}` with the actual current year (if you use
Eclipse IDE, this will happen automatically). All RDF4J copyright headers
record only the year a file was _first_ contributed, so there is no need
to update the year of existing files if you modify those files.

**NB** for older existing source code, RDF4J sometimes uses a slightly different
copyright header, mentioning 'Aduna' as additional copyright holder . Please
make sure you do not use that different header for any new contributions. 

For import statements, the following conventions hold:

- we do not use wildcard imports or wildcard static imports
- we allow static imports where possible, but do not require their use
- we apply a fixed ordering for import statements, following Eclipse conventions. Import statements are ordered in groups separated by a single empty line, in the following order: static imports, java.\*, javax.\*, org.\*, com.\*, everything else.

There are various ways to apply these conventions to your code, depending on which editor/IDE you use.

We use a single tab as the indentation in all XML files (including the `pom.xml` files). 

### Eclipse IDE users

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Formatter' 
2. Click 'Import...' and find the file `eclipse-settings/eclipse-rdf4j-conventions.xml`, located in the git repository root.
3. The active profile should now say 'Eclipse rdf4j '. Click ok.
4. When making changes to Java code, hit Ctrl+Shift+F before saving to reformat the code. Alternatively, you can configure Eclipse to automatically apply formatting on save. This can be activated by going to 'Preferences' -> 'Java' -> 'Editor' -> 'Save Actions' and making sure the 'Format source code' option checkbox is ticked.

Similarly, to apply templates:

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Code Templates' 
2. Click 'Import...' and find the file `eclipse-settings/codetemplates.xml`, located in the git repository root.
3. Click OK. The templates will be automatically applied when necessary. 

For import organization, the Eclipse defaults should be fine, but you can make sure as follows:

1. go to 'Preferences' -> 'Java' -> 'Code Style' -> 'Organize Imports'. 
2. Make sure the list of ordered groups corresponds to 'java', 'javax', 'org', 'com'.
3. Make sure the numbers of (static) imports needed for wildcard imports are set suitable high (99 or more).

You can apply import organization by hitting Ctrl+Shift+O, or you can configure Eclipse to automtaically organize imports on save. This can be activated by going to 'Preferences' -> 'Java' -> 'Editor' -> 'Save Actions' and making sure the 'Organize imports' option checkbox is ticked.

Finally, for XML formatting, the Eclipse defaults should be fine, but you can make sure as follows:

1. go to 'Preferences' -> 'XML' -> 'XML Files' -> 'Editor'. 
2. Make sure line width is set to 120, 'Indent using tabs' is active, and indentation size is set to 1. 

### Other IDEs / using the Maven formatter and import sorting plugins

If you do not use Eclipse IDE, we still welcome your contributions of course. There are several ways in which you can configure your IDE to format according to our conventions. Some tools will have the Eclipse code conventions built in, or will have options to import Eclipse formatter and import sorting settings. 

In addition, the RDF4J project is configured to use the [formatter maven plugin](https://code.revelc.net/formatter-maven-plugin/), the [import sorting maven plugin](https://code.revelc.net/impsort-maven-plugin/index.html) and [XML Format plugin](https://acegi.github.io/xml-format-maven-plugin/) for validation of all code changes against the coding conventions, and compiling the project using Apache Maven will automatically activate these plugins and fix most formatting/import issues.

The formatters are automatically run by maven during the `process-resources` phase, which means they will activate whenever you compile, verify, package or install the project using maven (see the [maven default lifecycle](https://maven.apache.org/ref/3.6.3/maven-core/lifecycles.html#default_Lifecycle) for more details).

To validate your changes manually, run the following command:

```
mvn formatter:validate impsort:check xml-format:xml-check
```

To reformat your code manually before committing, run:

```
mvn formatter:format impsort:sort xml-format:xml-format
```

or alternatively:

```
mvn process-resources
```

Please note: these maven plugins are meant as a tool to help you and 
us to quickly check the most common formatting problems. They are _not_,
however, intended to completely relieve you of responsibility for correct
formatting, and won't necessarily cover all code conventions we follow. For
example, the guideline that we don't allow wildcard import statements is not
checked by these plugins. 

Having said all that, we appreciate your best effort, but if occassionally something slips through, that's no big deal: code conventions are there to help us as developers, not to make your life miserable :) 
     
## Workflow 

The short version for contributors: start from the `main` branch, and create a new, separate branch for every bugfix, improvement, or new feature. We recommend you use `GH-<issuenumber>-short-description` as the branch name, where `<issuenumber>` is the number of the Github issue you're fixing (without the leading `#`), and `short-description` is a few keywords that describe the issue.

For more detailed information on how RDF4J manages git branches, versioning, releases, and planning, see [Info for RDF4J developers](https://rdf4j.org/documentation/developer/).
