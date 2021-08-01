package org.eclipse.rdf4j.sparqlbuilder.constraint;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExtendedExpressionsTest {
	@Test
	public void test_BIND_fromOtherVariable() {
		ExtendedVariable from = new ExtendedVariable("from");
		ExtendedVariable to = new ExtendedVariable("to");
		Assertions.assertEquals(
				"BIND( ?from AS ?to )", ExtendedExpressions.BIND(from, to).getQueryString());
	}

	@Test
	public void test_NOT_IN_twoIris() {
		ExtendedVariable test = new ExtendedVariable("test");
		Assertions.assertEquals(
				"?test NOT IN ( <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, <http://www.w3.org/2000/01/rdf-schema#subClassOf> )",
				ExtendedExpressions.NOT_IN(test, Rdf.iri(RDF.TYPE), Rdf.iri(RDFS.SUBCLASSOF))
						.getQueryString());
	}

	@Test
	public void test_IS_BLANK() {
		ExtendedVariable test = new ExtendedVariable("test");
		Assertions.assertEquals(
				"isBLANK( ?test )", ExtendedExpressions.IS_BLANK(test).getQueryString());
	}
}
