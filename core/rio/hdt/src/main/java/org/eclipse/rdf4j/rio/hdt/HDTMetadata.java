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
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * HDT Metadata helper class
 * 
 * This is used to pass metadata from/to the HDT file. More specifically, 
 * into the {@link org.eclipse.rdf4j.rio.hdt.HDTHeader HDTHeader}
 * 
 * This is actually an N-Triples file embedded into the binary HDT file.
 * 
 * @author Bart Hanssens
 */
public class HDTMetadata  {
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
	 * Set the IRI (typically: file location) or blank node to be used for the metadata root.
	 * If the base is null, an unnamed blank node will be used. 
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
	 * Write the metadata part to the output stream. Not using the n-triples writer.
	 * 
	 * @param os
	 * @throws IOException 
	 */
	protected void write(OutputStream os) throws IOException {
		String root = base.toString();
		os.write(buildTriple(root, RDF.TYPE.toString(), ""));
	}
	
	private byte[] buildTriple(String s, String p, String o) {
		String t = "<" + s + "> <" + p + "> <" + o + "> .\n";
		return t.getBytes(StandardCharsets.UTF_8);
	}
}
