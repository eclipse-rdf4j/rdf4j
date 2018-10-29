# Welcome to the RDF4J Storage repository

[![Build Status](https://travis-ci.org/eclipse/rdf4j-storage.svg?branch=master)](https://travis-ci.org/eclipse/rdf4j-storage)

This is the storage code repository for the Eclipse RDF4J project. More code can be found in
* [rdf4j-client](https://github.com/eclipse/rdf4j) containing parsers and client libraries
* [rdf4j-tools](https://github.com/eclipse/rdf4j-tools) containing server and console
* [rdf4j-testsuite](https://github.com/eclipse/rdf4j-testsuite) containing common tests and benchmarks

Please see [RDF4J.org](http://rdf4j.org) for detailed information about RDF4J, including
user documentation and [downloads of the latest release](http://rdf4j.org/download).

[![Visit our IRC channel](https://kiwiirc.com/buttons/irc.freenode.net/rdf4j.png)](https://kiwiirc.com/client/irc.freenode.net/?nick=rdf4j-user|?#rdf4j)

## Keen to contribute?

We welcome contributions! To get started, please first read our [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md).

The short version:

1. Digitally sign the [Eclipse Contributor Agreement (ECA)](https://www.eclipse.org/legal/ECA.php). You can do this by logging into the [Eclipse projects forge](http://www.eclipse.org/contribute/cla); click on "Eclipse Contributor Agreement"; and Complete the form. Be sure to use the same email address when you register for the account that you intend to use on Git commit records. See the [ECA FAQ](https://www.eclipse.org/legal/ecafaq.php) for more info. 
2. Create an issue in the [RDF4J GitHub issue tracker](https://github.com/eclipse/rdf4j/issues) that describes your improvement, new feature, or bug fix.
3. Fork the GitHub repository.
4. Create a new branch (starting from master) for your issue. 
5. Make your changes on this branch. Apply the [RDF4J code formatting guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md#code-formatting). Don't forget to include unit tests.
7. Run `mvn verify` from the project root to make sure all tests succeed (both your own new ones, and existing).
8. Use meaningful commit messages and include the issue number in each commit message.
9. **sign off** every commit (using the `-s` flag).
10. Once your fix is complete, put it up for review by opening a Pull Request against the master branch in the central RDF4J repository.

These steps are explained in more detail in the [Contributor
guidelines](https://github.com/eclipse/rdf4j/blob/master/.github/CONTRIBUTING.md).
