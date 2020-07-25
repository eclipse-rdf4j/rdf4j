---
title: "FedX joins RDF4J"
date: 2019-10-15
layout: "single"
categories: ["news"]
---
We are happy to announce the integration of FedX as federation engine into RDF4J.
 
FedX provides transparent federation of multiple SPARQL endpoints under a
single virtual endpoint. As an example, a knowledge graph such as Wikidata can
be queried in a federation with endpoints that are linked to Wikidata as an
integration hub. In a federated SPARQL query in FedX, one no longer needs to
explicitly address specific endpoints using SERVICE clauses. Instead, FedX
automatically selects relevant sources, sends statement patterns to these
sources for evaluation, and joins the individual results. FedX seamlessly
integrates into RDF4J using the Repository API and can be used as a drop-in
component in existing applications including the RDF4J Workbench.
 
 <!--more-->
The project was initially developed at fluid Operations as part of research
work in 2011 [^1] and further evolved as open source project in recent years.
Veritas Technologies acquired the company in 2017 and recently contributed FedX
to the Eclipse Foundation.
 
Andreas Schwarte – the original author of FedX and long-time RDF4J contributor
– recently joined the semantic technologies company [metaphacts](https://www.metaphacts.com/) where he will
continue to work on the project. With the integration into RDF4J we hope to
reach out to a broader community and further strengthen the query processing
capabilities of the framework for data federation and integration use cases.
 
We expect FedX to be included in the next RDF4J release, 3.1.0 (release date TBD).

[^1]: Schwarte, A., Haase, P., Hose, K., Schenkel, R., & Schmidt, M. (2011). _FedX: Optimization Techniques for Federated Query Processing on Linked Data._ International Semantic Web Conference. https://link.springer.com/chapter/10.1007/978-3-642-21064-8_39
