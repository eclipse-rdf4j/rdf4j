/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.trig.TriGParserFactory;
import org.eclipse.rdf4j.rio.trig.TriGWriterFactory;
import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public class TriGPrettyWriterTest extends RDFWriterTest {

	public TriGPrettyWriterTest() {
		super(new TriGWriterFactory(), new TriGParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, true);
	}

	@Test
	public void testPrettyWriteSingleStatementNoBNodesNoContext()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri1, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1)));
	}

	@Test
	public void testPrettyWriteSingleStatementNoBNodesSingleContextIRI()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri1, uri1, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1, uri1)));
	}

	@Test
	public void testPrettyWriteSingleStatementNoBNodesSingleContextBnode()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(uri1, uri1, uri1, bnode));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteSingleStatementSubjectBNodeNoContext()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteSingleStatementSubjectBNodeSingleContextIRI()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteSingleStatementSubjectBNodeSingleContextBNode()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteTwoStatementsSubjectBNodeSinglePredicateNoContext()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextIRI()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextBNode()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, bnode));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.contexts().size());
		assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}
	
	@Test
	public void testPrettyWriteSingleStatementNoBNodesNoContextWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1)));
	}

	@Test
	public void testPrettyWriteSingleStatementNoBNodesSingleContextIRIWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertTrue(parsedOutput.contains(vf.createStatement(uri1, uri1, uri1, uri1)));
	}

	@Test
	public void testPrettyWriteSingleStatementNoBNodesSingleContextBnodeWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(uri1, uri1, uri1, bnode));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteSingleStatementSubjectBNodeNoContextWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteSingleStatementSubjectBNodeSingleContextIRIWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteSingleStatementSubjectBNodeSingleContextBNodeWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(1, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.contexts().size());
		assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteTwoStatementsSubjectBNodeSinglePredicateNoContextWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextIRIWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, uri1));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, uri1));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2, uri1).size());
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}

	@Test
	public void testPrettyWriteTwoStatementsSubjectBNodeSinglePredicateSingleContextBNodeWithNamespace()
		throws Exception
	{
		Model input = new LinkedHashModel();
		input.setNamespace("ex", exNs);
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri1, bnode));
		input.add(vf.createStatement(bnodeSingleUseSubject, uri1, uri2, bnode));
		StringWriter outputWriter = new StringWriter();
		write(input, outputWriter);
		String outputString = outputWriter.toString();
		System.out.println(outputString);
		StringReader inputReader = new StringReader(outputString);
		Model parsedOutput = parse(inputReader, "");
		assertEquals(2, parsedOutput.size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri1).size());
		assertEquals(1, parsedOutput.filter(null, uri1, uri2).size());
		assertEquals(1, parsedOutput.contexts().size());
		assertTrue(parsedOutput.contexts().iterator().next() instanceof BNode);
		assertEquals(1, parsedOutput.subjects().size());
		assertTrue(parsedOutput.subjects().iterator().next() instanceof BNode);
	}	
}
