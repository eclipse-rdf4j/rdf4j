package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.spring.support.Rdf4JTemplate;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.eclipse.rdf4j.spring.util.TypeMappingUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BasicTests extends Rdf4JTestBase {
	@Autowired
	Rdf4JTemplate rdf4JTemplate;

	@Test
	public void testIsTemplateWired() {
		Assertions.assertNotNull(rdf4JTemplate);
	}

	@Test
	void testTripleCount() {
		int count = rdf4JTemplate
				.tupleQuery("SELECT (count(?a) as ?cnt) WHERE { ?a ?b ?c}")
				.evaluateAndConvert()
				.toSingleton(bs -> TypeMappingUtils.toInt(
						QueryResultUtils.getValue(bs, "cnt")));
		if (count != 26) {
			Model model = rdf4JTemplate.graphQuery("CONSTRUCT { ?a ?b ?c } WHERE { ?a ?b ?c }")
					.evaluateAndConvert()
					.toModel();
			Rio.write(model, System.out, RDFFormat.TURTLE);
		}
		Assertions.assertEquals(26, count);
	}

}
