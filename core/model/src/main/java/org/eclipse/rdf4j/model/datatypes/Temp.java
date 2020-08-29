package org.eclipse.rdf4j.model.datatypes;

public class Temp {
	public static void main(String[] args) {

		for (XmlDatatypeEnum value : XmlDatatypeEnum.values()) {
			System.out.println(value.name() + "(XSD." + value.name() + ", " +
					XMLDatatypeUtil.isPrimitiveDatatype(value.iri) + " ," +
					XMLDatatypeUtil.isDurationDatatype(value.iri) + " ," +
					XMLDatatypeUtil.isIntegerDatatype(value.iri) + " ," +
					XMLDatatypeUtil.isDerivedDatatype(value.iri) + " ," +
					XMLDatatypeUtil.isDecimalDatatype(value.iri) + " ," +
					XMLDatatypeUtil.isFloatingPointDatatype(value.iri) + " ," +
					XMLDatatypeUtil.isCalendarDatatype(value.iri) + " " +
					") ,"
			);

		}
		;

	}

}
