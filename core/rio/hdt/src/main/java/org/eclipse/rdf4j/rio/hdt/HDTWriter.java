/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.output.CountingOutputStream;

import org.eclipse.rdf4j.model.Statement;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;

import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;

/**
 * <strong>Experimental<strong> RDF writer for HDT v1.0 files. This writer is not thread-safe, therefore its public methods are synchronized.
 * 
 * Unfortunately the draft specification is not entirely clear and probably slightly out of date, since the open source
 * reference implementation HDT-It seems to implement a slightly different version. This parser tries to be compatible
 * with HDT-It 1.0.
 * 
 * The most important parts are the Dictionaries containing the actual values (S, P, O part of a triple), and the
 * Triples containing the numeric references to construct the triples.
 * 
 * Since objects in one triple are often subjects in another triple, these "shared" parts are stored in a shared
 * Dictionary, which may significantly reduce the file size.
 * 
 * File structure:
 * 
 * <pre>
 * +---------------------+
 * | Global              |
 * | Header              |
 * | Dictionary (Shared) |
 * | Dictionary (S)      |
 * | Dictionary (P)      |
 * | Dictionary (O)      |    
 * | Triples             |
 * +---------------------+
 * </pre>
 * 
 * @author Bart Hanssens
 * 
 * @see <a href="http://www.rdfhdt.org/hdt-binary-format/">HDT draft (2015)</a>
 * @see <a href="https://www.w3.org/Submission/2011/03/">W3C Member Submission (2011)</a>
 */
public class HDTWriter extends AbstractRDFWriter {
	private final OutputStream out;

	// various counters
	private long triples;

	private final TreeMap<byte[],Integer> shared = new TreeMap<>();
	private final TreeMap<byte[],Integer> s = new TreeMap<>();
	private final TreeMap<byte[],Integer> p = new TreeMap<>();
	private final TreeMap<byte[],Integer> o = new TreeMap<>();

	/**
	 * Creates a new HDTWriter.
	 */
	public HDTWriter(OutputStream out) {
		this.out = out;
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.HDT;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>();
		return result;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		// write everything at the end
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		// not using try-with-resources, since the counter is needed in the catch clause (JDK8)
		CountingOutputStream bos = new CountingOutputStream(out);
		try {
			HDTGlobal global = new HDTGlobal();
			global.write(out);
			
			HDTHeader header = new HDTHeader();
			header.write(out);
		} catch (IOException ioe) {
			throw new RDFHandlerException("At byte: " + bos.getCount(), ioe);
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {
				//
			}
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		byte[] s = st.getSubject().stringValue().getBytes(StandardCharsets.UTF_8);
		byte[] p = st.getObject().stringValue().getBytes(StandardCharsets.UTF_8);
		byte[] o = st.getPredicate().stringValue().getBytes(StandardCharsets.UTF_8);
		
		triples++;
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		// ignore
	}
}
