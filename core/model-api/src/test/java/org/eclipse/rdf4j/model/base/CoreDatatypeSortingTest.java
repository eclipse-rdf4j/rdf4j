/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class CoreDatatypeSortingTest {

	@Test
	public void testXSD() {
		ArrayList<CoreDatatype.XSD> datatypes = getDatatypesShuffled();

		List<String> datatypeIRIs = getDatatypesShuffled().stream()
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

	private ArrayList<CoreDatatype.XSD> getDatatypesShuffled() {
		Random random = new Random(42353245);

		ArrayList<CoreDatatype.XSD> xsds = new ArrayList<>(Arrays.asList(CoreDatatype.XSD.values()));

		Collections.shuffle(xsds);
		return xsds;
	}

}
