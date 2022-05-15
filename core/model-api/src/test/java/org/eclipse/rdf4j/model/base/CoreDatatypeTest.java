/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class CoreDatatypeTest {

	@Test
	public void testOrderOfXSD() {
		ArrayList<CoreDatatype.XSD> datatypes = getXSDDatatypesShuffled();

		List<String> datatypeIRIs = getXSDDatatypesShuffled().stream()
				.map(CoreDatatype::getIri)
				.map(Object::toString)
				.collect(Collectors.toList());

		Collections.sort(datatypes);
		Collections.sort(datatypeIRIs);

		List<String> datatypeIRIsSortedByEnum = datatypes.stream()
				.map(CoreDatatype::getIri)
				.map(Object::toString)
				.collect(Collectors.toList());
		Assert.assertEquals(datatypeIRIs, datatypeIRIsSortedByEnum);

	}

	@Test
	public void testOrderOfRDF() {
		ArrayList<CoreDatatype.RDF> datatypes = getRDFDatatypesShuffled();

		List<String> datatypeIRIs = getRDFDatatypesShuffled().stream()
				.map(CoreDatatype::getIri)
				.map(Object::toString)
				.collect(Collectors.toList());

		Collections.sort(datatypes);
		Collections.sort(datatypeIRIs);

		List<String> datatypeIRIsSortedByEnum = datatypes.stream()
				.map(CoreDatatype::getIri)
				.map(Object::toString)
				.collect(Collectors.toList());
		Assert.assertEquals(datatypeIRIs, datatypeIRIsSortedByEnum);

	}

	private ArrayList<CoreDatatype.XSD> getXSDDatatypesShuffled() {

		ArrayList<CoreDatatype.XSD> datatypes = new ArrayList<>(Arrays.asList(CoreDatatype.XSD.values()));

		Collections.shuffle(datatypes, new Random(42353245));
		return datatypes;
	}

	private ArrayList<CoreDatatype.RDF> getRDFDatatypesShuffled() {

		ArrayList<CoreDatatype.RDF> datatypes = new ArrayList<>(Arrays.asList(CoreDatatype.RDF.values()));

		Collections.shuffle(datatypes, new Random(42353245));
		return datatypes;
	}

	@Test
	public void testToString() {
		for (CoreDatatype value : CoreDatatypeHelper.getReverseLookup().values()) {
			assertSame(value.toString(), value.getIri().toString());
		}

	}

	@Test
	public void testUnknownDatatype() {
		assertEquals(CoreDatatype.NONE, CoreDatatype.from(new AbstractIRI.GenericIRI("http://example.com")));
	}

}
