# Welcome to the Eclipse RDF4J repository

![RDF4J](https://github.com/eclipse/rdf4j/blob/main/site/static/images/rdf4j-logo-orange-114.png)

This is the main code repository for the Eclipse RDF4J project. 

[![main status](https://github.com/eclipse/rdf4j/workflows/main%20status/badge.svg)](https://github.com/eclipse/rdf4j/actions?query=workflow%3A%22main+status%22)
[![develop status](https://github.com/eclipse/rdf4j/workflows/develop%20status/badge.svg)](https://github.com/eclipse/rdf4j/actions?query=workflow%3A%22develop+status%22) [![Join the chat at https://gitter.im/eclipse/rdf4j](https://badges.gitter.im/eclipse/rdf4j.svg)](https://gitter.im/eclipse/rdf4j?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Visit the [project website](https://rdf4j.org/) for news, documentation, and downloadable releases. For support questions, comments, and any ideas for improvements you'd like to discuss, please use our [discussion forum](https://github.com/eclipse/rdf4j/discussions). If you have found a bug or have a very specific feature/improvement request, you can also use our [issue tracker](https://github.com/eclipse/rdf4j/issues) to report it.

## Installation and usage

For installation and usage instructions of the RDF4J Workbench and Server applications, see [RDF4J Server and Workbench](https://rdf4j.org/documentation/tools/server-workbench). 

For installation and usage instructions of the RDF4J Java libaries, see [Programming with RDF4J](https://rdf4j.org/documentation/programming). 

### Building from source

RDF4J is a multi-module [maven](https://maven.apache.org/index.html) project. It can be compiled, tested, and installed with the [usual maven lifecycle phases](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) from the command line, for example:

- `mvn verify` - compiles and runs all tests
- `mvn package` - compiles, tests, and packages all modules
- `mvn install` - compiles, tests, packages, and installs all artifacts in the local maven repository
- `mvn -Pquick install` - compiles, packages and installs everything (skipping test execution)

These commands can be run from the project root to execute on the entire project or (if you're only interested in working with a particular module) from any module's subdirectory. 

To build the full RDF4J project, including onejar and SDK files and full aggregated javadoc, from source, run:

     mvn -Passembly package

The SDK and onejar will be available in `assembly/target`. Individual module jars and wars will be in `target/` in their respective modules. 

Modern IDEs like Eclipse, IntelliJ IDEA, or Netbeans can of course also be used to build, test, and run (parts of) the project. 

## Keen to contribute?

We welcome contributions! Whether you have a new feature you want to add, or a bug you want to fix, or a bit of documentation you want to improve, it's all very welcome. Have a look in our [issue tracker](https://github.com/eclipse/rdf4j/issues) for any open problems, in particular the ones marked as [good first issue](https://github.com/eclipse/rdf4j/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) or as [help wanted](https://github.com/eclipse/rdf4j/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22). Or feel free to add your own new issue if what you have in mind is not there yet.

To get started on your contribution, please first read our [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/main/CONTRIBUTING.md).

The short version:

1. Digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php), as follows: 
     * [Register an Eclipse account](https://accounts.eclipse.org/user/register). **Important**: Use the same email address that you will use on Git commits as the author address. 
     * Open the [ECA form](https://accounts.eclipse.org/user/eca) and complete it. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info. 
2. Create an issue in the [issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix - or if you're picking up an existing issue, comment on that issue that you intend to provide a solution for it.
3. Fork the GitHub repository.
4. Create a new branch (starting from main) for your changes. Name your branch like this: `GH-1234-short-description-here` where 1234 is the Github issue number.
5. Make your changes on this branch. Apply the [RDF4J code formatting guidelines](https://github.com/eclipse/rdf4j/blob/main/CONTRIBUTING.md#code-formatting). Don't forget to include unit tests.
7. Run `mvn verify` from the project root to make sure all tests succeed (both your own new ones, and existing).
8. Commit your changes into the branch. Make sure the commit author name and e-mail correspond to what you used to sign the ECA. Use meaningful commit messages. Reference the issue number in each commit message (for example "GH-276: added null check").
9. Once your fix is complete, put it up for review by opening a Pull Request against the main branch in the central Github repository. If you have a lot of commits on your PR, make sure to [squash your commits](https://rdf4j.org/documentation/developer/squashing).

These steps are explained in more detail in the [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/main/CONTRIBUTING.md).

You can find more detailed information about our development and release processes in the [Developer Workflow and Project Management](https://rdf4j.org/documentation/developer/) documentation.
