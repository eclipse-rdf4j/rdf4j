package org.eclipse.rdf4j.model;

import java.io.IOException;

import org.eclipse.rdf4j.model.util.LexicalValueComparator;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Ignore;
import org.junit.Test;

public class SlowIsomorphicTest {

	@Ignore
	@Test
	public void testSlowFile() throws IOException {
		Model m1 = Rio.parse(SlowIsomorphicTest.class.getClassLoader().getResourceAsStream("slowIsomorphic.ttl"),
				RDFFormat.TURTLE);
		Model m2 = Rio.parse(SlowIsomorphicTest.class.getClassLoader().getResourceAsStream("slowIsomorphic.ttl"),
				RDFFormat.TURTLE);

		LexicalValueComparator lexicalValueComparator = new LexicalValueComparator();

		m1 = m1.stream()
				.sorted((a, b) -> lexicalValueComparator.compare(a.getObject(), b.getObject()))
				.collect(ModelCollector.toModel());

		System.out.println("Sorting done");

		boolean isomorphic = Models.isomorphic(m1, m2);
		System.out.println(isomorphic);

	}

}
