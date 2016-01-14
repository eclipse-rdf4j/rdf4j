/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link SimpleLiteral}.
 *
 * @author Peter Ansell
 */
public class SimpleLiteralTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#hashCode()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testHashCode()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String)}.
	 */
	@Test
	public final void testStringNull()
		throws Exception
	{
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(null);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String)}.
	 */
	@Test
	public final void testStringEmpty()
		throws Exception
	{
		Literal test = new SimpleLiteral("");
		assertEquals("", test.getLabel());
		assertFalse(test.getLanguage().isPresent());
		assertEquals(XMLSchema.STRING, test.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String)}.
	 */
	@Test
	public final void testStringLong()
		throws Exception
	{
		StringBuilder testBuilder = new StringBuilder(1000000);
		for (int i = 0; i < 1000000; i++) {
			testBuilder.append(Integer.toHexString(i % 16));
		}

		Literal test = new SimpleLiteral(testBuilder.toString());
		assertEquals(testBuilder.toString(), test.getLabel());
		assertFalse(test.getLanguage().isPresent());
		assertEquals(XMLSchema.STRING, test.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testStringStringNullNull()
		throws Exception
	{
		String label = null;
		String language = null;
		
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(label, language);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testStringStringEmptyNull()
		throws Exception
	{
		String label = "";
		String language = null;
		
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(label, language);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testStringStringNullEmpty()
		throws Exception
	{
		String label = null;
		String language = "";
		
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(label, language);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Test
	public final void testStringIRINullNull()
		throws Exception
	{
		String label = null;
		IRI datatype = null;
		
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(label, datatype);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Test
	public final void testStringIRINullString()
		throws Exception
	{
		String label = null;
		IRI datatype = XMLSchema.STRING;
		
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(label, datatype);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Test
	public final void testStringIRINullLangString()
		throws Exception
	{
		String label = null;
		IRI datatype = RDF.LANGSTRING;
		
		thrown.expect(NullPointerException.class);
		new SimpleLiteral(label, datatype);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Test
	public final void testStringIRIEmptyNull()
		throws Exception
	{
		String label = "";
		IRI datatype = null;
		
		Literal test = new SimpleLiteral(label, datatype);
		assertEquals("", test.getLabel());
		assertFalse(test.getLanguage().isPresent());
		assertEquals(XMLSchema.STRING, test.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#SimpleLiteral(java.lang.String, org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Test
	public final void testStringIRIEmptyLangString()
		throws Exception
	{
		String label = "";
		IRI datatype = RDF.LANGSTRING;
		
		thrown.expect(IllegalArgumentException.class);
		new SimpleLiteral(label, datatype);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#setLabel(java.lang.String)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testSetLabel()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#getLabel()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testGetLabel()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#setLanguage(java.lang.String)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testSetLanguage()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#getLanguage()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testGetLanguage()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#setDatatype(org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testSetDatatype()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#getDatatype()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testGetDatatype()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#equals(java.lang.Object)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testEqualsObject()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#toString()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testToString()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#stringValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testStringValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#booleanValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testBooleanValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#byteValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testByteValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#shortValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testShortValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#intValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testIntValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#longValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testLongValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#floatValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testFloatValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#doubleValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testDoubleValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#integerValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testIntegerValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#decimalValue()}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testDecimalValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.model.impl.SimpleLiteral#calendarValue()}
	 * .
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testCalendarValue()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.lang.Object#equals(java.lang.Object)}.
	 */
	@Ignore("TODO: Implement me!")
	@Test
	public final void testEqualsObject1()
		throws Exception
	{
		fail("Not yet implemented"); // TODO
	}

}
