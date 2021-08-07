/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.domain.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.MultiIRI;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class EX {
	private static final String base = "http://example.org/";
	public static final MultiIRI Artist = new MultiIRI(base, "Artist");
	public static final MultiIRI Gallery = new MultiIRI(base, "Gallery");
	public static final MultiIRI Painting = new MultiIRI(base, "Painting");
	public static final MultiIRI Picasso = new MultiIRI(base, "Picasso");
	public static final MultiIRI VanGogh = new MultiIRI(base, "VanGogh");
	public static final MultiIRI street = new MultiIRI(base, "street");
	public static final MultiIRI city = new MultiIRI(base, "city");
	public static final MultiIRI country = new MultiIRI(base, "country");
	public static final MultiIRI creatorOf = new MultiIRI(base, "creatorOf");
	public static final MultiIRI technique = new MultiIRI(base, "technique");
	public static final MultiIRI starryNight = new MultiIRI(base, "starryNight");
	public static final MultiIRI sunflowers = new MultiIRI(base, "sunflowers");
	public static final MultiIRI potatoEaters = new MultiIRI(base, "potatoEaters");
	public static final MultiIRI guernica = new MultiIRI(base, "guernica");

	public static IRI of(String localName) {
		return new MultiIRI(base, localName);
	}
}
