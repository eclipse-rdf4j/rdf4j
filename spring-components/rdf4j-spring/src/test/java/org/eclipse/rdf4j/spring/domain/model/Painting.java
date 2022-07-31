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
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class Painting {
	public static final Variable PAINTING_ID = SparqlBuilder.var("painting_id");
	public static final Variable PAINTING_ARTIST_ID = SparqlBuilder.var("painting_artist_id");
	public static final Variable PAINTING_TECHNIQUE = SparqlBuilder.var("painting_technique");
	public static final Variable PAINTING_LABEL = SparqlBuilder.var("painting_label");

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
