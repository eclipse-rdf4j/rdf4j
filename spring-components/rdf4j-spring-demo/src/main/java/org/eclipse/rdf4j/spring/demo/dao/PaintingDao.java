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

package org.eclipse.rdf4j.spring.demo.dao;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.spring.demo.model.Painting.PAINTING_ARTIST_ID;
import static org.eclipse.rdf4j.spring.demo.model.Painting.PAINTING_ID;
import static org.eclipse.rdf4j.spring.demo.model.Painting.PAINTING_LABEL;
import static org.eclipse.rdf4j.spring.demo.model.Painting.PAINTING_TECHNIQUE;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.spring.dao.RDF4JDao;
import org.eclipse.rdf4j.spring.dao.SimpleRDF4JCRUDDao;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.MutableBindings;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.demo.model.EX;
import org.eclipse.rdf4j.spring.demo.model.Painting;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.springframework.stereotype.Component;

/**
 * Class responsible for repository access for managing {@link Painting} entities.
 *
 * The class extends the {@link SimpleRDF4JCRUDDao}, providing capabilities for inserting and reading entities.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Component
public class PaintingDao extends SimpleRDF4JCRUDDao<Painting, IRI> {

	public PaintingDao(RDF4JTemplate rdf4JTemplate) {
		super(rdf4JTemplate);
	}

	@Override
	protected void populateIdBindings(MutableBindings bindingsBuilder, IRI iri) {
		bindingsBuilder.add(PAINTING_ID, iri);
	}

	@Override
	protected RDF4JDao.NamedSparqlSupplierPreparer prepareNamedSparqlSuppliers(NamedSparqlSupplierPreparer preparer) {
		return null;
	}

	@Override
	protected Painting mapSolution(BindingSet querySolution) {
		Painting painting = new Painting();
		painting.setId(QueryResultUtils.getIRI(querySolution, PAINTING_ID));
		painting.setTechnique(QueryResultUtils.getString(querySolution, PAINTING_TECHNIQUE));
		painting.setTitle(QueryResultUtils.getString(querySolution, PAINTING_LABEL));
		painting.setArtistId(QueryResultUtils.getIRI(querySolution, PAINTING_ARTIST_ID));
		return painting;
	}

	@Override
	protected String getReadQuery() {
		return Queries.SELECT(PAINTING_ID, PAINTING_LABEL, PAINTING_TECHNIQUE, PAINTING_ARTIST_ID)
				.where(
						PAINTING_ID.isA(iri(EX.Painting))
								.andHas(iri(EX.technique), PAINTING_TECHNIQUE)
								.andHas(iri(RDFS.LABEL), PAINTING_LABEL),
						PAINTING_ARTIST_ID.has(iri(EX.creatorOf), PAINTING_ID))
				.getQueryString();
	}

	@Override
	protected NamedSparqlSupplier getInsertSparql(Painting painting) {
		return NamedSparqlSupplier.of("insert", () -> Queries.INSERT(
				PAINTING_ID.isA(iri(EX.Painting))
						.andHas(iri(EX.technique), PAINTING_TECHNIQUE)
						.andHas(iri(RDFS.LABEL), PAINTING_LABEL),
				PAINTING_ARTIST_ID.has(iri(EX.creatorOf), PAINTING_ID))
				.getQueryString());
	}

	@Override
	protected void populateBindingsForUpdate(MutableBindings bindingsBuilder, Painting painting) {
		bindingsBuilder
				.add(PAINTING_LABEL, painting.getTitle())
				.add(PAINTING_TECHNIQUE, painting.getTechnique())
				.add(PAINTING_ARTIST_ID, painting.getArtistId());
	}

	@Override
	protected IRI getInputId(Painting painting) {
		if (painting.getId() == null) {
			return getRdf4JTemplate().getNewUUID();
		}
		return painting.getId();
	}
}
