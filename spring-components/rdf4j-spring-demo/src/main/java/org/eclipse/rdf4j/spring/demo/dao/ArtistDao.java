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

import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.bound;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.not;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.spring.demo.model.Artist.ARTIST_FIRST_NAME;
import static org.eclipse.rdf4j.spring.demo.model.Artist.ARTIST_ID;
import static org.eclipse.rdf4j.spring.demo.model.Artist.ARTIST_LAST_NAME;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.spring.dao.SimpleRDF4JCRUDDao;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.MutableBindings;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.demo.model.Artist;
import org.eclipse.rdf4j.spring.demo.model.EX;
import org.eclipse.rdf4j.spring.demo.model.Painting;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.springframework.stereotype.Component;

/**
 * Class responsible for repository access for managing {@link Artist} entities.
 *
 * The class extends the {@link SimpleRDF4JCRUDDao}, providing capabilities for inserting and reading entities.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Component
public class ArtistDao extends SimpleRDF4JCRUDDao<Artist, IRI> {

	public ArtistDao(RDF4JTemplate rdf4JTemplate) {
		super(rdf4JTemplate);
	}

	@Override
	protected void populateIdBindings(MutableBindings bindingsBuilder, IRI iri) {
		bindingsBuilder.add(ARTIST_ID, iri);
	}

	@Override
	protected void populateBindingsForUpdate(MutableBindings bindingsBuilder, Artist artist) {
		bindingsBuilder
				.add(ARTIST_FIRST_NAME, artist.getFirstName())
				.add(ARTIST_LAST_NAME, artist.getLastName());
	}

	@Override
	protected Artist mapSolution(BindingSet querySolution) {
		Artist artist = new Artist();
		artist.setId(QueryResultUtils.getIRI(querySolution, ARTIST_ID));
		artist.setFirstName(QueryResultUtils.getString(querySolution, ARTIST_FIRST_NAME));
		artist.setLastName(QueryResultUtils.getString(querySolution, ARTIST_LAST_NAME));
		return artist;
	}

	@Override
	protected String getReadQuery() {
		return "prefix foaf: <http://xmlns.com/foaf/0.1/> "
				+ "prefix ex: <http://example.org/> "
				+ "SELECT ?artist_id ?artist_firstName ?artist_lastName where {"
				+ "?artist_id a ex:Artist; "
				+ "    foaf:firstName ?artist_firstName; "
				+ "    foaf:surname ?artist_lastName ."
				+ " } ";
	}

	@Override
	protected NamedSparqlSupplier getInsertSparql(Artist artist) {
		return NamedSparqlSupplier.of("insert", () -> Queries.INSERT(ARTIST_ID.isA(iri(EX.Artist))
				.andHas(iri(FOAF.FIRST_NAME), ARTIST_FIRST_NAME)
				.andHas(iri(FOAF.SURNAME), ARTIST_LAST_NAME))
				.getQueryString());
	}

	@Override
	protected IRI getInputId(Artist artist) {
		if (artist.getId() == null) {
			return getRdf4JTemplate().getNewUUID();
		}
		return artist.getId();
	}

	static abstract class QUERY_KEYS {
		public static final String ARTISTS_WITHOUT_PAINTINGS = "artists-without-paintings";
	}

	@Override
	protected NamedSparqlSupplierPreparer prepareNamedSparqlSuppliers(NamedSparqlSupplierPreparer preparer) {
		return preparer.forKey(QUERY_KEYS.ARTISTS_WITHOUT_PAINTINGS)
				.supplySparql(Queries.SELECT(
						ARTIST_ID)
						.where(
								ARTIST_ID.isA(iri(EX.Artist))
										.and(ARTIST_ID.has(iri(EX.creatorOf), Painting.PAINTING_ID).optional())
										.filter(not(bound(Painting.PAINTING_ID))))
						.getQueryString()
				);
	}

	public Set<Artist> getArtistsWithoutPaintings() {
		return getNamedTupleQuery(QUERY_KEYS.ARTISTS_WITHOUT_PAINTINGS)
				.evaluateAndConvert()
				.toStream()
				.map(bs -> QueryResultUtils.getIRI(bs, ARTIST_ID))
				.map(this::getById)
				.collect(Collectors.toSet());
	}

}
