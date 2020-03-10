/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;

/**
 * HDT Metadata helper class
 * 
 * This is used to pass metadata from/to the HDT file. More specifically, into the
 * {@link org.eclipse.rdf4j.rio.hdt.HDTHeader HDTHeader}
 * 
 * This is actually an N-Triples file embedded into the binary HDT file.
 * 
 * @author Bart Hanssens
 */
public class HDTMetadata {
	private final String DICTIONARY = "_:dictionary";
	private final String TRIPLES = "_:triples";

	private Resource base = SimpleValueFactory.getInstance().createBNode("dataset");
	private long triples;
	private int properties;
	private int distinctSubjects;
	private int distinctObjects;
	private int distinctShared;
	private byte[] triplesOrder;
	private long initialSize;
	private long hdtSize;
	private Date issued;

	/**
	 * Set the IRI (typically: file location) or blank node to be used for the metadata root.
	 * 
	 * @param base
	 */
	protected void setBase(Resource base) {
		this.base = base;
	}

	/**
	 * Set the number of triples
	 * 
	 * @param triples
	 */
	protected void setTriples(long triples) {
		this.triples = triples;
	}

	/**
	 * Set the number of distinct objects
	 * 
	 * @param distinctObjects
	 */
	protected void setDistinctObj(int distinctObjects) {
		this.distinctObjects = distinctObjects;
	}

	/**
	 * Set the number of distinct subjects
	 * 
	 * @param distinctSubjects
	 */
	protected void setDistinctSubj(int distinctSubjects) {
		this.distinctSubjects = distinctSubjects;
	}

	/**
	 * Set the number of distinct shared parts
	 * 
	 * @param distinctShared
	 */
	protected void setDistinctShared(int distinctShared) {
		this.distinctShared = distinctShared;
	}

	/**
	 * Set the number of distinct properties
	 * 
	 * @param properties
	 */
	protected void setProperties(int properties) {
		this.properties = properties;
	}

	protected void parse(InputStream is, int len) throws IOException {
		byte[] b = new byte[len];
		is.read(b);
	}

	/**
	 * Write the metadata part to the output stream. Currently not using the n-triples writer to avoid dragging this
	 * runtime dependency.
	 * 
	 * @return byte array
	 * @throws IOException
	 */
	protected byte[] get() throws IOException {
		StringBuilder sb = new StringBuilder(4096);
		String root = base.toString();

		addTriple(sb, root, RDF.TYPE, HDT.DATASET);
		addTriple(sb, root, RDF.TYPE, VOID.DATASET);
		addTriple(sb, root, VOID.TRIPLES, String.valueOf(triples));
		addTriple(sb, root, VOID.PROPERTIES, String.valueOf(properties));
		addTriple(sb, root, VOID.DISTINCT_SUBJECTS, String.valueOf(distinctSubjects));
		addTriple(sb, root, VOID.DISTINCT_OBJECTS, String.valueOf(distinctObjects));
		addTriple(sb, root, HDT.STATISTICAL_INFORMATION, "_:statistics");
		addTriple(sb, root, HDT.PUBLICATION_INFORMATION, "_:publicationInformation");
		addTriple(sb, root, HDT.FORMAT_INFORMATION, "_:format");
		addTriple(sb, "_:format", HDT.DICTIONARY, DICTIONARY);
		addTriple(sb, "_:format", HDT.TRIPLES, TRIPLES);
		addTriple(sb, DICTIONARY, DCTERMS.FORMAT, HDT.DICTIONARY_FOUR);
		addTriple(sb, DICTIONARY, HDT.DICTIONARY_NUMSHARED, String.valueOf(distinctShared));
		addTriple(sb, DICTIONARY, HDT.DICTIONARY_MAPPING, "1");
		addTriple(sb, DICTIONARY, HDT.DICTIONARY_SIZE_STRINGS, "");
		addTriple(sb, DICTIONARY, HDT.DICTIONARY_BLOCK_SIZE, "");
		addTriple(sb, TRIPLES, DCTERMS.FORMAT, HDT.TRIPLES_BITMAP);
		addTriple(sb, TRIPLES, HDT.TRIPLES_NUMTRIPLES, String.valueOf(triples));
		addTriple(sb, TRIPLES, HDT.TRIPLES_ORDER, "SPO");

		return sb.toString().getBytes(StandardCharsets.US_ASCII);
	}

	/**
	 * Build triple into a byte array
	 * 
	 * @param sb string builder
	 * @param s  subject string
	 * @param p  predicate IRI
	 * @param o  object string
	 */
	private void addTriple(StringBuilder sb, String s, IRI p, Object obj) {
		if (s.startsWith("_:")) {
			sb.append(s);
		} else {
			sb.append('<').append(s).append('>');
		}

		sb.append(" <").append(p.stringValue()).append("> ");

		if (obj instanceof String) {
			String o = (String) obj;
			if (o.startsWith("_:")) {
				sb.append(o);
			} else {
				sb.append('"').append(o).append('"');
			}
		} else {
			sb.append('<').append(((Resource) obj).stringValue()).append('>');
		}
		sb.append(" .\n");
	}
}
