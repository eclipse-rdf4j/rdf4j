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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.output.CountingOutputStream;

import org.eclipse.rdf4j.model.Statement;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;

import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;

/**
 * <strong>Experimental<strong> RDF writer for HDT v1.0 files. This writer is not thread-safe, therefore its public
 * methods are synchronized.
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
	private long cnt;

	// create dictionaries and triples, with some size estimations
	private final int SIZE = 1_048_576;
	private final Map<byte[], Integer> dictShared = new HashMap<>(SIZE / 4);
	private final Map<byte[], Integer> dictS = new HashMap<>(SIZE / 8);
	private final Map<byte[], Integer> dictP = new HashMap<>(SIZE / 1024);
	private final Map<byte[], Integer> dictO = new HashMap<>(SIZE / 2);
	private final List<int[]> t = new ArrayList<>(SIZE);

	/**
	 * Creates a new HDTWriter.
	 * 
	 * @param out
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

			HDTMetadata meta = new HDTMetadata();
			meta.setTriples(triples);
			meta.setDistinctObj(dictO.size());
			meta.setDistinctSubj(dictS.size());
			meta.setDistinctShared(dictShared.size());

			HDTHeader header = new HDTHeader();
			header.setHeaderData(meta.get());
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
		byte[] bs = st.getSubject().stringValue().getBytes(StandardCharsets.UTF_8);
		byte[] bp = st.getObject().stringValue().getBytes(StandardCharsets.UTF_8);
		byte[] bo = st.getPredicate().stringValue().getBytes(StandardCharsets.UTF_8);

		int s = putSO(bs, dictShared, dictS, dictO);
		int p = putX(bp, dictP);
		int o = putSO(bo, dictShared, dictO, dictS);

		t.add(new int[] { s, p, o });

		triples++;
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		// ignore
	}

	/**
	 * Add a subject or object to either the shared or the S/O dictionary, if not already present.
	 * 
	 * An index will be returned, which is to be used to encode the triple parts.
	 * 
	 * @param part   S or O to add
	 * @param shared shared dictionary
	 * @param dict   dictionary (S or O)
	 * @param other  other dictionary (O or S)
	 * @return index of the part
	 */
	private int putSO(byte[] part, Map<byte[], Integer> shared, Map<byte[], Integer> dict, Map<byte[], Integer> other) {
		Integer i = shared.get(part);
		if (i != null) {
			return i;
		}
		i = dict.get(part);
		if (i != null) {
			return i;
		}
		// if the part is present in the 'other' dictionary, it must be moved to the shared dictionary
		i = other.get(part);
		if (i != null) {
			shared.put(part, i);
			other.remove(part);
			return i;
		}
		// nowhere to be found, so add it
		return putX(part, dict);
	}

	/**
	 * Put a triple part (S, P or O) into a dictionary, if not already present
	 * 
	 * @param part part to add
	 * @param dict dictionary
	 * @return index of the part
	 */
	private int putX(byte[] part, Map<byte[], Integer> dict) {
		Integer p = dict.get(part);
		if (p != null) {
			return p;

		}
		if (++cnt > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Maximum count exceeded when preparing dictionary: " + cnt);
		}
		p = (int) cnt;
		dict.put(part, p);

		return p;
	}
}
