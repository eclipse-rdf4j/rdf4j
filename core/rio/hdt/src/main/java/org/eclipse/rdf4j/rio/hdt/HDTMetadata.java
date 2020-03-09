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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.io.UncloseableOutputStream;
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
	private Resource base;
	private long triples;
	private long properties;
	private long dinstinctSubjects;
	private long dinstinctObjects;
	private byte[] triplesOrder;
	private long initialSize;
	private long hdtSize;
	private Date issued;

	/**
	 * Set the IRI (typically: file location) or blank node to be used for the metadata root. If the base is null, an
	 * unnamed blank node will be used.
	 * 
	 * @param base
	 */
	public void setBase(Resource base) {
		this.base = (base != null) ? base : SimpleValueFactory.getInstance().createBNode();
	}

	protected void parse(InputStream is, int len) throws IOException {
		byte[] b = new byte[len];
		is.read(b);
	}

	/**
	 * Write the metadata part to the output stream. Currently not using the n-triples writer to avoid dragging this
	 * runtime dependency.
	 * 
	 * @param os
	 * @throws IOException
	 */
	protected void write(OutputStream os) throws IOException {
		String root = base.toString();
		String dictionary = "_:dictionary";
		String triples = "_:triples";

		os.write(buildTriple(root, RDF.TYPE, HDT.DATASET));
		os.write(buildTriple(root, RDF.TYPE, VOID.DATASET));
		os.write(buildTriple(root, VOID.TRIPLES, ""));
		os.write(buildTriple(root, VOID.PROPERTIES, ""));
		os.write(buildTriple(root, VOID.DISTINCT_SUBJECTS, ""));
		os.write(buildTriple(root, VOID.DISTINCT_OBJECTS, ""));
		os.write(buildTriple(root, HDT.STATISTICAL_INFORMATION, "_:statistics"));
		os.write(buildTriple(root, HDT.PUBLICATION_INFORMATION, "_:publicationInformation"));
		os.write(buildTriple(root, HDT.FORMAT_INFORMATION, "_:format"));
		os.write(buildTriple("_:format", HDT.DICTIONARY, dictionary));
		os.write(buildTriple("_:format", HDT.TRIPLES, triples));
		os.write(buildTriple(dictionary, DCTERMS.FORMAT, HDT.DICTIONARY_FOUR));
		os.write(buildTriple(dictionary, HDT.DICTIONARY_NUMSHARED, ""));
		os.write(buildTriple(dictionary, HDT.DICTIONARY_MAPPING, ""));
		os.write(buildTriple(dictionary, HDT.DICTIONARY_SIZE_STRINGS, ""));
		os.write(buildTriple(dictionary, HDT.DICTIONARY_BLOCK_SIZE, ""));
		os.write(buildTriple(triples, DCTERMS.FORMAT, HDT.TRIPLES_BITMAP));
		os.write(buildTriple(triples, HDT.TRIPLES_NUMTRIPLES, ""));
		os.write(buildTriple(triples, HDT.TRIPLES_ORDER, ""));
	}

	/**
	 * Build triple into a byte array
	 * 
	 * @param s subject string
	 * @param p predicate IRI
	 * @param o object string
	 * @return byte array
	 */
	private byte[] buildTriple(String s, IRI p, String o) {
		String t = s.startsWith("_") ? s : "<" + s + ">";
		t += " <" + p.stringValue() + "> " + o + ".\n";
		return t.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Build triple into a byte array
	 * 
	 * @param s subject string
	 * @param p predicate IRI
	 * @param o object value
	 * @return byte array
	 */
	private byte[] buildTriple(String s, IRI p, Value o) {
		String t = "<" + s + "> <" + p.toString() + "> <" + o.stringValue() + "> .\n";
		return t.getBytes(StandardCharsets.UTF_8);
	}
}
