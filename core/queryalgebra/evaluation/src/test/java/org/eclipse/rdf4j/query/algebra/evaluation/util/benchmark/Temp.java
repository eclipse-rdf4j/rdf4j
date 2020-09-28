package org.eclipse.rdf4j.query.algebra.evaluation.util.benchmark;

import org.eclipse.rdf4j.model.datatypes.XmlDatatype;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;

public class Temp {
	public static void main(String[] args) {

		for (XmlDatatype value : XmlDatatype.values()) {
			for (XmlDatatype xmlDatatype : XmlDatatype.values()) {

				System.out.println(value + ".valueComparatorLookup.put(" + xmlDatatype + ", "
						+ new ValueComparator().compare(value.getIri(), xmlDatatype.getIri()) + ");");

			}
			System.out.println();
		}

	}

}
