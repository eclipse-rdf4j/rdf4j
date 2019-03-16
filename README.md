# Welcome to the Eclipse RDF4J client repository

This is the client code repository for the Eclipse RDF4J project. It contains the main client APIs (such as the Repository and RDF Model API) and the various Rio RDF parser and writer modules. 

Other parts of the RDF4J project can be found in
* [rdf4j-storage](https://github.com/eclipse/rdf4j-storage) containing the SAIL API, database implementations, indexers, and reasoners.
* [rdf4j-tools](https://github.com/eclipse/rdf4j-tools) containing RDF4J Server, Workbench and Console
* [rdf4j-testsuite](https://github.com/eclipse/rdf4j-testsuite) containing common tests and benchmarks

Please see [rdf4j.org](http://rdf4j.org) for detailed information about RDF4J, including
user documentation and [downloads of the latest release](http://rdf4j.org/download).

## Keen to contribute?

We welcome contributions! To get started, please first read our [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md).

The short version:

1. Digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php). You can do this by logging into the [Eclipse projects forge](http://www.eclipse.org/contribute/cla); click on "Eclipse Contributor Agreement"; and Complete the form. Be sure to use the same email address when you register for the account that you intend to use on Git commit records. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info. 
2. Create an issue in the [issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix.
3. Fork the GitHub repository.
4. Create a new branch (starting from master) for your changes. 
5. Make your changes on this branch. Apply the [RDF4J code formatting guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md#code-formatting). Don't forget to include unit tests.
6. **sign off** every commit (using the `-s` flag).
7. Run `mvn verify` from the project root to make sure all tests succeed (both your own new ones, and existing).
8. Use meaningful commit messages and include the issue number in each commit message.
9. Once your fix is complete, put it up for review by opening a Pull Request against the master branch in the central Github repository.

These steps are explained in more detail in the [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md).
