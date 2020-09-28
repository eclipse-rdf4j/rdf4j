package org.eclipse.rdf4j.query.algebra.evaluation.util.benchmark;

import org.eclipse.rdf4j.model.datatypes.XmlDatatype;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;

public class main {

	public static void main(String[] args) {

		ValueComparator valueComparator = new ValueComparator();

		System.out.println("switch (datatype1){");

		for (XmlDatatype d1 : XmlDatatype.values()) {

			System.out.println("\t case " + d1 + ":");

			System.out.println("\t\tswitch (datatype2){");

			for (XmlDatatype d2 : XmlDatatype.values()) {
				System.out.println(
						"\t\t\t case " + d2 + ": return " + valueComparator.compare(d1.getIri(), d2.getIri()) + ";");

			}

			System.out.println("\t\t}");

		}

		System.out.println("}");

//		XmlDatatype datatype1 = XmlDatatype.ANYURI;
//		XmlDatatype datatype2 = XmlDatatype.ANYURI;
//
//		switch (datatype1){
//			case ID:
//				switch (datatype2){
//					case ID: return 1;
//				}
//		}

	}
}
