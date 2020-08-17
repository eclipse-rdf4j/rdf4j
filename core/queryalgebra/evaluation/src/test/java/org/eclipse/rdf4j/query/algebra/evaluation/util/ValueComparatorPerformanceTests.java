package org.eclipse.rdf4j.query.algebra.evaluation.util;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.LexicalValueComparator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ValueComparatorPerformanceTests {

	public static void main(String[] args) throws IOException {

		System.out.println("Parsing");
		Model parse = Rio.parse(ValueComparatorPerformanceTests.class.getClassLoader().getResourceAsStream("datagovbe-valid.ttl"), "", RDFFormat.TURTLE);

		ValueComparator valueComparator = new ValueComparator();

		System.out.println("Sorting");
		Set<Value> objects = parse.objects().stream().collect(Collectors.toSet());

		for(int i = 0; i<1; i++){
			int comp = 0;
			for (Value v1 : objects) {
				int counter = 0;
				for (Value v2 : objects) {
					if(counter++ > 1000) break;
					comp += valueComparator.compare(v1, v2);
				}
			}
			System.out.println(comp);

		}


	}

}
