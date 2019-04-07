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
5. Make sure you include tests.
6. Make sure the test suite passes after your changes.
7. Commit your changes into the branch. Use meaningful commit messages. Reference the issue number in the commit message (for example "issue #276: added null check").
8. **Sign off** every commit you do, using the `-s` flag (as explained in the [legal stuff](#legal-stuff)).
9. Optionally squash your commits (not necessary, but if you want to clean your commit history a bit, _this_ is the point to do it).
10. Push your changes to your branch in your forked repository.
11. If your contribution is complete, use GitHub to submit a pull request (PR)
	for your contribution back to `master` in the central RDF4J repository.
	Once you have submitted your PR, do not use your branch for any other
	development (unless asked to do so by the reviewers of your PR). 

Once you've put up a PR, we will review your contribution, possibly make some
suggestions for improvements, and once everything is complete it will be merged
into the `master` branch (if it's a bug fix to be included in the next
maintenance release) or into the `develop` branch (if it's a feature or
improvement to be included in the next minor or major release).

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

- Qualified invocations exceeding the maximum line width are reformatted with the first method call on the same line, and each subsequent method call on its own line, indented. 

So, for example:

```
return model.stream().map(st -> st.getObject()).filter(o -> o instanceof Literal).map(l -> (Literal) l).findAny(); 
```

becomes:

```
return model.stream()
    .map(st -> st.getObject())
    .filter(o -> o instanceof Literal)
    .map(l -> (Literal) l)
    .findAny(); 
```

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

## Workflow 

The short version for contributors: start from the `master` branch, and create a new, separate branch for every bugfix, improvement, or new feature. 

For more detailed information on how RDF4J manages git branches, versioning, releases, and planning, see [Developer Workflow and Project Management](http://docs.rdf4j.org/developer/#_developer_workflow_and_project_management).
