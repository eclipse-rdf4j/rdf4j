# Change Log

## [1.0M2](https://github.com/eclipse/rdf4j/tree/1.0M2) (2016-07-07)
[Full Changelog](https://github.com/eclipse/rdf4j/compare/1.0M1...1.0M2)

## [2.0M3](https://github.com/eclipse/rdf4j/tree/2.0M3) (2016-07-06)
[Full Changelog](https://github.com/eclipse/rdf4j/compare/2.0M2...2.0M3)

**Fixed bugs:**

- Lucene wrapped in SailRepository with multithreading gives error [\#192](https://github.com/eclipse/rdf4j/issues/192)
- create.xsl has wrong value attribute for In Memory Store with RDFS+SPIN support [\#196](https://github.com/eclipse/rdf4j/issues/196)
- "Multiple statements for pattern" error when attempting to load RDF file w/ SPIN constraints [\#195](https://github.com/eclipse/rdf4j/issues/195)
- rdf4j-rio-jsonld is not listed in rdf4j-bom [\#189](https://github.com/eclipse/rdf4j/issues/189)
- Remote add statements ignores the context when transaction is started  [\#188](https://github.com/eclipse/rdf4j/issues/188)
- Statement controller doesn't take into account max execution time [\#187](https://github.com/eclipse/rdf4j/issues/187)
- FederationJoinOptimizer.getUnionArgs [\#180](https://github.com/eclipse/rdf4j/issues/180)
- Unhandled java.lang.NoClassDefFoundError: Could not initialize class: org.apache.http.impl.conn.ManagedHttpClientConnectionFactory [\#177](https://github.com/eclipse/rdf4j/issues/177)

**Closed issues:**

- SPIN Construct Rule Fails To Run Upon Instantiation when Same SPARQL Query Runs in Workbench [\#213](https://github.com/eclipse/rdf4j/issues/213)
- Additional vocabularies [\#211](https://github.com/eclipse/rdf4j/issues/211)
- Add support for RDF-Patch format [\#194](https://github.com/eclipse/rdf4j/issues/194)
- How many triples can we put into this triple store ? [\#191](https://github.com/eclipse/rdf4j/issues/191)
- Upgrade to servlet-api 3.0 [\#206](https://github.com/eclipse/rdf4j/issues/206)
- fix maven config to execute all tests when running on dev machine [\#173](https://github.com/eclipse/rdf4j/issues/173)
- Update JSONLD-Java dependency to 0.8.3 [\#138](https://github.com/eclipse/rdf4j/issues/138)

## [1.0M1](https://github.com/eclipse/rdf4j/tree/1.0M1) (2016-06-02)
[Full Changelog](https://github.com/eclipse/rdf4j/compare/2.0M2...1.0M1)

**Closed issues:**

- Support parallel Java 7-compatible release [\#38](https://github.com/eclipse/rdf4j/issues/38)

## [2.0M2](https://github.com/eclipse/rdf4j/tree/2.0M2) (2016-05-31)
[Full Changelog](https://github.com/eclipse/rdf4j/compare/2.0M1...2.0M2)

**Fixed bugs:**

- Federation does not handle subselects [\#134](https://github.com/eclipse/rdf4j/issues/134)
- regex filter for Russian is case sensitive [\#161](https://github.com/eclipse/rdf4j/issues/161)
- OSGi bundle misses most classes [\#142](https://github.com/eclipse/rdf4j/issues/142)
- FederationSail fails on SPARQL 1.1 query test subquery/sq14 [\#29](https://github.com/eclipse/rdf4j/issues/29)

**Closed issues:**

- fix version numbers in master branch [\#154](https://github.com/eclipse/rdf4j/issues/154)
- Docs link broken in "Installing RDF4J Server and RDF4J Workbench" [\#140](https://github.com/eclipse/rdf4j/issues/140)
- Should package names be named based on major version [\#139](https://github.com/eclipse/rdf4j/issues/139)
- Use IRI class in all vocabularies [\#171](https://github.com/eclipse/rdf4j/issues/171)
- ProtocolTest.testContentTypeForGraphQuery3\_GET needs to check for all possible formats [\#167](https://github.com/eclipse/rdf4j/issues/167)
- Fix package names in logback.xml [\#151](https://github.com/eclipse/rdf4j/issues/151)
- Remove rdbms remaining files [\#150](https://github.com/eclipse/rdf4j/issues/150)
- Make sure about and license files are included in SDK distro [\#135](https://github.com/eclipse/rdf4j/issues/135)

## [2.0M1](https://github.com/eclipse/rdf4j/tree/2.0M1) (2016-05-16)
**Implemented enhancements:**

- SparqlQueryRenderer does not support SPARQL 1.1 [\#107](https://github.com/eclipse/rdf4j/issues/107)
- Provide utility methods for IRI validation [\#105](https://github.com/eclipse/rdf4j/issues/105)
- Provide support for SPARQL 1.1 entailment regimes in inferencing stores [\#102](https://github.com/eclipse/rdf4j/issues/102)
- Allow SAIL to inspect/process unparsed query at prepareQuery stage [\#99](https://github.com/eclipse/rdf4j/issues/99)

**Fixed bugs:**

- QueryJoinOptimizer breaks ordering of dependent statements [\#119](https://github.com/eclipse/rdf4j/issues/119)
- Exception in NativeStore under parallel transaction handling [\#118](https://github.com/eclipse/rdf4j/issues/118)
- RDF4J crash when loading ntriples file [\#116](https://github.com/eclipse/rdf4j/issues/116)
- BIND with type errors result leads to cross joins instead of empty result [\#115](https://github.com/eclipse/rdf4j/issues/115)
- Parser produces non-canonical integer when parsing math '+' expression [\#113](https://github.com/eclipse/rdf4j/issues/113)
- Query results download: Provide query results page with a hidden form for sending long query text [\#112](https://github.com/eclipse/rdf4j/issues/112)
- XMLDatatypeUtil.isValidValue\(\) doesn't validate xsd:anyURI [\#110](https://github.com/eclipse/rdf4j/issues/110)
- Loss of bound values for variables assigned in a SERVICE graphPattern [\#108](https://github.com/eclipse/rdf4j/issues/108)
- Results of aggregations in service calls are not included in the inner query projections [\#104](https://github.com/eclipse/rdf4j/issues/104)
- File format autodetect does not work in 'Add RDF' screen [\#103](https://github.com/eclipse/rdf4j/issues/103)
- Statement's context is not justified by SPARQLConnection\#add [\#101](https://github.com/eclipse/rdf4j/issues/101)
- RDF4J POMs do not play nicely with gradle [\#100](https://github.com/eclipse/rdf4j/issues/100)

**Closed issues:**

- Document known JAXP entity expansion limit and workaround [\#117](https://github.com/eclipse/rdf4j/issues/117)
- MapDB-based SAIL [\#111](https://github.com/eclipse/rdf4j/issues/111)
- Add configurable size limit on request payload [\#109](https://github.com/eclipse/rdf4j/issues/109)
- Should we rewrite @since tags? [\#55](https://github.com/eclipse/rdf4j/issues/55)
- Transfer open issues from JIRA to Github [\#53](https://github.com/eclipse/rdf4j/issues/53)
- Clean up maven project config where necessary [\#40](https://github.com/eclipse/rdf4j/issues/40)
- Add templates for pull request opening [\#16](https://github.com/eclipse/rdf4j/issues/16)
- Provide compatibility/transitional modules for old package hierarchy [\#14](https://github.com/eclipse/rdf4j/issues/14)
- Add a CONTRIBUTING.md file to display the guidelines when opening issues or pull requests [\#4](https://github.com/eclipse/rdf4j/issues/4)
- SPIN compliance tests are slow and unstable [\#58](https://github.com/eclipse/rdf4j/issues/58)
- Run code formatting over entire master branch [\#57](https://github.com/eclipse/rdf4j/issues/57)
- fix assembly setup and distribution file naming [\#50](https://github.com/eclipse/rdf4j/issues/50)
- Review and edit Javadoc [\#46](https://github.com/eclipse/rdf4j/issues/46)
- stabilize build [\#43](https://github.com/eclipse/rdf4j/issues/43)
- design new logo [\#35](https://github.com/eclipse/rdf4j/issues/35)
- Rename datadirs  [\#33](https://github.com/eclipse/rdf4j/issues/33)
- Fix test failures [\#27](https://github.com/eclipse/rdf4j/issues/27)
- update SPI services entries [\#22](https://github.com/eclipse/rdf4j/issues/22)
- reintegrate test case data from W3C [\#20](https://github.com/eclipse/rdf4j/issues/20)
- get approval for ElasticSearch [\#19](https://github.com/eclipse/rdf4j/issues/19)
- get approval for Solr inclusion [\#18](https://github.com/eclipse/rdf4j/issues/18)
- Update project documentation and website [\#15](https://github.com/eclipse/rdf4j/issues/15)
- Decide on version number for initial release [\#13](https://github.com/eclipse/rdf4j/issues/13)
- Set up CI server [\#12](https://github.com/eclipse/rdf4j/issues/12)
- Reintegrate YASQE and CodeMirror [\#11](https://github.com/eclipse/rdf4j/issues/11)
- Remove legacy lucene versions [\#10](https://github.com/eclipse/rdf4j/issues/10)
- Update design and naming for Server, Console and Workbench apps [\#9](https://github.com/eclipse/rdf4j/issues/9)
- rename OpenRDFException [\#6](https://github.com/eclipse/rdf4j/issues/6)
- Set up upload of artifacts to maven central [\#3](https://github.com/eclipse/rdf4j/issues/3)
- Verify versions of 3rd party libraries used [\#2](https://github.com/eclipse/rdf4j/issues/2)



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
