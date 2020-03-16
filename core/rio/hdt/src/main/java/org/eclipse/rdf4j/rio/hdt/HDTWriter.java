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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.output.CountingOutputStream;

import org.eclipse.rdf4j.model.Statement;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;

import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.HDTWriterSettings;

/**
 * <strong>Experimental<strong> RDF writer for HDT v1.0 files. Currently only suitable for input that can fit into
 * memory.
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
	private long cnt;

	// TODO: rewrite this to cater for larger input
	// create dictionaries and triples, with some size estimations
	private static int SIZE = 1_048_576;
	private Map<String, Integer> dictShared = new HashMap<>(SIZE / 4);
	private Map<String, Integer> dictS = new HashMap<>(SIZE / 8);
	private Map<String, Integer> dictP = new HashMap<>(SIZE / 1024);
	private Map<String, Integer> dictO = new HashMap<>(SIZE / 2);
	private List<int[]> t = new ArrayList<>(SIZE);

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
		Set<RioSetting<?>> result = Collections.singleton(HDTWriterSettings.ORIGINAL_FILE);
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
			global.write(bos);

			HDTHeader header = new HDTHeader();
			header.setHeaderData(getMetadata());
			header.write(bos);

			HDTDictionary dict = new HDTDictionary();
			dict.write(bos);

			long dpos = bos.getByteCount();
			HDTDictionarySection shared = HDTDictionarySectionFactory.write(bos, "S+O", dpos,
					HDTDictionarySection.Type.FRONT);
			dictShared = sortMap(dictShared);
			shared.setSize(dictShared.size());
			shared.set(dictShared.keySet().iterator());
			shared.write(bos);

			dpos = bos.getByteCount();
			HDTDictionarySection subjects = HDTDictionarySectionFactory.write(bos, "S", dpos,
					HDTDictionarySection.Type.FRONT);
			dictS = sortMap(dictS);
			subjects.setSize(dictS.size());
			subjects.set(dictS.keySet().iterator());
			subjects.write(bos);

			dpos = bos.getByteCount();
			HDTDictionarySection predicates = HDTDictionarySectionFactory.write(bos, "P", dpos,
					HDTDictionarySection.Type.FRONT);
			dictP = sortMap(dictP);
			predicates.setSize(dictP.size());
			predicates.set(dictP.keySet().iterator());
			predicates.write(bos);

			dpos = bos.getByteCount();
			HDTDictionarySection objects = HDTDictionarySectionFactory.write(bos, "O", dpos,
					HDTDictionarySection.Type.FRONT);
			dictO = sortMap(dictO);
			objects.setSize(dictO.size());
			objects.set(dictO.keySet().iterator());
			objects.write(bos);

			dpos = bos.getByteCount();
			System.err.println("pos" + dpos);

			getLookup(dictShared);
			getLookup(dictS);
			getLookup(dictP);
			getLookup(dictO);
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
		String bs = st.getSubject().stringValue();
		String bp = st.getPredicate().stringValue();
		String bo = st.getObject().stringValue();

		int s = putSO(bs, dictShared, dictS, dictO);
		int p = putX(bp, dictP);
		int o = putSO(bo, dictShared, dictO, dictS);

		t.add(new int[] { s, p, o });
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
	private int putSO(String part, Map<String, Integer> shared, Map<String, Integer> dict, Map<String, Integer> other) {
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
	private int putX(String part, Map<String, Integer> dict) {
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

	/**
	 * Get metadata for the HDT Header part
	 * 
	 * @return byte array
	 */
	private byte[] getMetadata() {
		String file = getWriterConfig().get(HDTWriterSettings.ORIGINAL_FILE);

		Path path = Paths.get(file);
		long len = -1;
		try {
			len = Files.size(path);
		} catch (IOException ioe) {
			//
		}

		HDTMetadata meta = new HDTMetadata();
		meta.setBase(file);
		meta.setDistinctSubj(dictS.size());
		meta.setProperties(dictP.size());
		meta.setTriples(t.size());
		meta.setDistinctObj(dictO.size());
		meta.setDistinctShared(dictShared.size());
		meta.setMapping(HDTArray.Type.LOG64.getValue());
		meta.setBlockSize(16);

		if (len > 0) {
			meta.setInitialSize(len);
		}
		// meta.setHDTSize(-1);
		// meta.setSizeStrings(-1);

		return meta.get();
	}

	/**
	 * Move key,values from a map to a sorted map (i.e. it removes entries from the unsorted while ordering to save
	 * memory)
	 * 
	 * @param map
	 * @return sorted map
	 */
	private static SortedMap<String, Integer> sortMap(Map<String, Integer> unsorted) {
		TreeMap<String, Integer> sorted = new TreeMap<>();

		Iterator<Entry<String, Integer>> iter = unsorted.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Integer> e = iter.next();
			sorted.put(e.getKey(), e.getValue());
			iter.remove();
		}
		return sorted;
	}

	/**
	 * Create lookup table containing the new position at the index of the old position
	 * 
	 * @param map map
	 * @return array of
	 */
	private static int[] getLookup(Map<String, Integer> map) {
		// positions in HDT are counted from 1, leave 0-th element empty to avoid minus/plus 1
		int[] swap = new int[map.size() + 1];

		int newpos = 1;
		for (int oldpos : map.values()) {
			swap[oldpos] = newpos++;
		}

		return swap;
	}
}
