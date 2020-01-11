# Welcome to the Eclipse RDF4J repository

This is the main code repository for the Eclipse RDF4J project. 

![](https://github.com/eclipse/rdf4j/workflows/master%20status/badge.svg)
![](https://github.com/eclipse/rdf4j/workflows/develop%20status/badge.svg)

Visit the [project website](https://rdf4j.org/) for news, documentation, and downloadable releases.

## Installation and usage

For installation and usage instructions of the RDF4J Workbench and Server applications, see [RDF4J Server, Workbench, and Console](https://rdf4j.org/documentation/#rdf4j-server-workbench-and-console). 

For installation and usage instructions of the RDF4J Java libaries, see [Programming with RDF4J](https://rdf4j.org/documentation/#programming-with-rdf4j). 

### Building from source

To build the RDF4J project, including onejar and SDK files, from source, run:

     `mvn -Passembly package`

(optionally add the `-Pquick` flag to skip executing tests)

SDK and onejar will be available in `assembly/target`. Individual module jars and wars will be in `target/` in their respective modules. 

## Keen to contribute?

We welcome contributions! Whether you have a new feature you want to add, or a bug you want to fix, or a bit of documentation you want to improve, it's all very welcome. Have a look in our [issue tracker](https://github.com/eclipse/rdf4j/issues) for any open problems, in particular the ones marked as [good first issue](https://github.com/eclipse/rdf4j/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) or as [help wanted](https://github.com/eclipse/rdf4j/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22). Or feel free to add your own new issue if what you have in mind is not there yet.

To get started on your contribution, please first read our [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md).

The short version:

1. Digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php). You can do this by logging into the [Eclipse projects forge](http://www.eclipse.org/contribute/cla); click on "Eclipse Contributor Agreement"; and Complete the form. Be sure to use the same email address when you register for the account that you intend to use on Git commit records. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info. 
2. Create an issue in the [issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix - or if you're picking up an existing issue, comment on that issue that you intend to provide a solution for it.
3. Fork the GitHub repository.
4. Create a new branch (starting from master) for your changes. 
5. Make your changes on this branch. Apply the [rdf4j code formatting guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md#code-formatting). Don't forget to include unit tests.
6. **sign off** every commit (using the `-s` flag).
7. Run `mvn verify` from the project root to make sure all tests succeed (both your own new ones, and existing).
8. Use meaningful commit messages and include the issue number in each commit message.
9. Once your fix is complete, put it up for review by opening a Pull Request against the master branch in the central Github repository. If you have a lot of commits on your PR, make sure to [squash your commits](https://rdf4j.org/documentation/developer/squashing).

These steps are explained in more detail in the [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md).

You can find more detailed information about our development and release processes in the [Developer Workflow and Project Management](https://rdf4j.org/documentation/developer/) documentation.
