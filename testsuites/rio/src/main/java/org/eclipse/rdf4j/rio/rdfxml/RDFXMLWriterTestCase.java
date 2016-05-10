/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;

public abstract class RDFXMLWriterTestCase extends RDFWriterTest {

	protected RDFXMLWriterTestCase(RDFWriterFactory writerF, RDFParserFactory parserF) {
		super(writerF, parserF);
	}

	// TODO temporarily disabled since legal status of CIA factbook test files is not clear. Test
	// should be modified to use different test data that we own ourselves. 
	//	public void testWrite()
	//		throws RepositoryException, RDFParseException, IOException, RDFHandlerException
	//	{
	//		Repository rep1 = new SailRepository(new MemoryStore());
	//		rep1.initialize();
	//
	//		RepositoryConnection con1 = rep1.getConnection();
	//
	//		InputStream ciaScheme = this.getClass().getResourceAsStream("/cia-factbook/CIA-onto-enhanced.rdf");
	//		InputStream ciaFacts = this.getClass().getResourceAsStream("/cia-factbook/CIA-facts-enhanced.rdf");
	//
	//		con1.add(ciaScheme, "urn:cia-factbook/CIA-onto-enhanced.rdf", RDFFormat.RDFXML);
	//		con1.add(ciaFacts, "urn:cia-factbook/CIA-facts-enhanced.rdf", RDFFormat.RDFXML);
	//
	//		StringWriter writer = new StringWriter();
	//		RDFWriter rdfWriter = rdfWriterFactory.getWriter(writer);
	//		con1.export(rdfWriter);
	//
	//		con1.close();
	//
	//		Repository rep2 = new SailRepository(new MemoryStore());
	//		rep2.initialize();
	//
	//		RepositoryConnection con2 = rep2.getConnection();
	//
	//		con2.add(new StringReader(writer.toString()), "foo:bar", RDFFormat.RDFXML);
	//		con2.close();
	//
	//		Assert.assertTrue("result of serialization and re-upload should be equal to original",
	//				RepositoryUtil.equals(rep1, rep2));
	//	}

}
