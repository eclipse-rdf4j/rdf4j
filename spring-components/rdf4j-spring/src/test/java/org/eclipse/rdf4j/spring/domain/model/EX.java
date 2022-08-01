/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.domain.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class EX {
	private static final String base = "http://example.org/";
	public static final IRI Artist = SimpleValueFactory.getInstance().createIRI(base, "Artist");
	public static final IRI Gallery = SimpleValueFactory.getInstance().createIRI(base, "Gallery");
	public static final IRI Painting = SimpleValueFactory.getInstance().createIRI(base, "Painting");
	public static final IRI Picasso = SimpleValueFactory.getInstance().createIRI(base, "Picasso");
	public static final IRI VanGogh = SimpleValueFactory.getInstance().createIRI(base, "VanGogh");
	public static final IRI street = SimpleValueFactory.getInstance().createIRI(base, "street");
	public static final IRI city = SimpleValueFactory.getInstance().createIRI(base, "city");
	public static final IRI country = SimpleValueFactory.getInstance().createIRI(base, "country");
	public static final IRI creatorOf = SimpleValueFactory.getInstance().createIRI(base, "creatorOf");
	public static final IRI technique = SimpleValueFactory.getInstance().createIRI(base, "technique");
	public static final IRI starryNight = SimpleValueFactory.getInstance().createIRI(base, "starryNight");
	public static final IRI sunflowers = SimpleValueFactory.getInstance().createIRI(base, "sunflowers");
	public static final IRI potatoEaters = SimpleValueFactory.getInstance().createIRI(base, "potatoEaters");
	public static final IRI guernica = SimpleValueFactory.getInstance().createIRI(base, "guernica");

	public static IRI of(String localName) {
		return SimpleValueFactory.getInstance().createIRI(base, localName);
	}
}
