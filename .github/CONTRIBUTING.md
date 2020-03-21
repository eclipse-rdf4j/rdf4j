# How to contribute

So you want to help out making Eclipse RDF4J better. That's great, we welcome your contributions! 
Before you dive in, here are some things you need to know.

**Table of Contents**  

- [Legal stuff](#legal-stuff)
- [Creating your contribution](#creating-your-contribution)
- [Code formatting](#code-formatting)
- [Workflow](#workflow) 
	
## Legal stuff

Eclipse RDF4J is a project governed by the [Eclipse Foundation](http://www.eclipse.org/), which has strict [policies and guidelines](https://wiki.eclipse.org/Development_Resources#Policies_and_Guidelines) regarding contributions and intellectual property rights.

In order for any contributions to RDF4J to be accepted, you MUST do the following things:

### Sign the Eclipse Contributor Agreement
You must digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php). You can do this as follows: 

* If you haven't done so already, [register an Eclipse account](https://dev.eclipse.org/site_login/createaccount.php). Use the same email address when you register for the account that you intend to use on Git commit records. 
* Log into the [Eclipse projects forge](http://www.eclipse.org/contribute/cla); click on the "Eclipse Contributor Agreement" tab; and complete the form. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info. 

### Link your Github and Eclipse accounts

Add your github username in your [Eclipse account settings](https://dev.eclipse.org/site_login/#open_tab_accountsettings).

### Sign-off every commit

Every commit you make in your patch or pull request MUST be "signed off". You do this by adding the `-s` flag when you make the commit(s).

## Creating your contribution

Once the legalities are out of the way you can dig in. Here's how:

1. Create an issue in the [issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix. Alternatively, comment on an existing issue to indicate you're keen to help solve it.
2. Fork the repository on GitHub.
3. Create a new branch for your changes starting from the `master` branch. See [Workflow](#workflow) for details.
4. Make your changes. Apply the [RDF4J code formatting guidelines](#code-formatting)
5. Make sure you include tests. We use JUnit 4 with AssertJ and Mockito. Have a look around for existing tests to get some idea, and of course feel free to ask advice.
6. Make sure the test suite passes after your changes: you can run `mvn verify` to run tests locally.
7. Commit your changes into the branch. Use meaningful commit messages. Reference the issue number in the commit message (for example "GH-276: added null check").
8. **Sign off** every commit you do, using the `-s` flag (as explained in the [legal stuff](#legal-stuff)).
9. Push your changes to your branch in your forked repository.
10. Once you're ready, [squash your commits](https://rdf4j.org/documentation/developer/squashing) into one or two meaningful commits.
11. Use GitHub to submit a pull request (PR) for your contribution back to `master` in the central RDF4J repository.  Once you have submitted your PR, do not use your branch for any other development (unless asked to do so by the reviewers of your PR). 

Once you've put up a PR, we will review your contribution, possibly make some suggestions for improvements, and once everything is complete it will be merged into the `master` branch (if it's a bug fix to be included in the next maintenance release) or into the `develop` branch (if it's a feature or improvement to be included in the next minor or major release).

We are happy to receive "work in progress" pull requests as well: if you're not quite finished, but would like some early feedback on your approach, feel free to publish a PR early and ask us to review. 

## Code formatting

Eclipse RDF4J follows the [Eclipse Coding Conventions for Java](https://wiki.eclipse.org/Coding_Conventions), with a couple of minor modifications:

- We use a line width of 120 characters.
- We use Unix line endings (LF).
- We use the following header comment on every Java file:

```
/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 ```

**NB** for older existing source code, RDF4J uses a slightly different
copyright header, mentioning 'Aduna' as additional copyright holder . Please
make sure you do not use that different header for any new contributions. 

There are various ways to apply these conventions to your code, depending on which editor/IDE you use.

### Eclipse IDE users

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Formatter' 
2. Click 'Import...' and find the file `eclipse-settings/eclipse-rdf4j-conventions.xml`, located in the git repository root.
3. The active profile should now say 'Eclipse rdf4j '. Click ok.
4. When making changes to Java code, hit Ctrl+Shift+F before saving to reformat the code. Alternatively, you can configure Eclipse to automatically apply formatting on save. This can be activated by going to 'Preferences' -> 'Java' -> 'Editor' -> 'Save Actions' and making sure the 'Format source code' option checkbox is ticked.

Similarly, to apply templates:

1. In Eclipse, open 'Preferences' -> 'Java' -> 'Code Style' -> 'Code Templates' 
2. Click 'Import...' and find the file `eclipse-settings/codetemplates.xml`, located in the git repository root.
3. Click OK. The templates will be automatically applied when necessary. 

### Other IDEs / using the Maven formatter plugin

If you do not use Eclipse IDE, we still welcome your contributions of course. There are several ways in which you can configure your IDE to format according to our conventions. Some tools will have the Eclipse code conventions built in, or will have options to import Eclipse formatter settings. 

In addition, the RDF4J project is configured to use the [formatter maven plugin](https://code.revelc.net/formatter-maven-plugin/) for validation of all code changes against the coding conventions. 

To validate your changes manually, run the following command:

```
mvn formatter:validate
```

To reformat your code before committing, run:


```
mvn formatter:format
```

### XML indentation

We use a single tab as the indentation in all XML files (including the `pom.xml` files). This is not enforced by the build process, but we ask you to adjust your editors accordingly. If you wish, you can use the following command to check your xml files:

     mvn xml-format:xml-check
     
To reformat xml files in bulk, run:

     mvn xml-format:xml-format
     
## Workflow 

The short version for contributors: start from the `master` branch, and create a new, separate branch for every bugfix, improvement, or new feature. We recommend you use `GH-<issuenumber>-short-description` as the branch name, where `<issuenumber>` is the number of the Github issue you're fixing (without the leading `#`), and `short-description` is a few keywords that describe the issue.

For more detailed information on how RDF4J manages git branches, versioning, releases, and planning, see [Info for RDF4J developers](https://rdf4j.org/documentation/developer/).
