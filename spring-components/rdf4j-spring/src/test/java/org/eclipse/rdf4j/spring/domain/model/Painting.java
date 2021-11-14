/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.domain.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.core.ExtendedVariable;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class Painting {
	public static final ExtendedVariable PAINTING_ID = new ExtendedVariable("painting_id");
	public static final ExtendedVariable PAINTING_ARTIST_ID = new ExtendedVariable("painting_artist_id");
	public static final ExtendedVariable PAINTING_TECHNIQUE = new ExtendedVariable("painting_technique");
	public static final ExtendedVariable PAINTING_LABEL = new ExtendedVariable("painting_label");

	private IRI id;
	private String title;
	private String technique;
	private IRI artistId;

	public IRI getId() {
		return id;
	}

	public void setId(IRI id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTechnique() {
		return technique;
	}

	public void setTechnique(String technique) {
		this.technique = technique;
	}

	public IRI getArtistId() {
		return artistId;
	}

	public void setArtistId(IRI artistId) {
		this.artistId = artistId;
	}
}
