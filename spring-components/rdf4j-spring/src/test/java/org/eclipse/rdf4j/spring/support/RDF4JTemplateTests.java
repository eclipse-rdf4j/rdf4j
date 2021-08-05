/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.support;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.dao.support.UpdateWithModelBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.GraphQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.TupleQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.UpdateExecutionBuilder;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.domain.model.EX;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public class RDF4JTemplateTests extends RDF4JSpringTestBase {

	@Autowired
	private RDF4JTemplate rdf4JTemplate;

	@Test
	public void testUpdate1() {
		UpdateExecutionBuilder updateBuilder = rdf4JTemplate.update(
				String.format("INSERT { <%s> a <%s> } WHERE {} ", EX.of("Vermeer"), EX.Artist));
		updateBuilder.execute();
		Value type = rdf4JTemplate.tupleQuery(
				String.format("SELECT ?type WHERE { <%s> a ?type }",
						EX.of("Vermeer")))
				.evaluateAndConvert()
				.toSingleton(bs -> bs.getBinding("type").getValue());
		Assertions.assertTrue(type.isIRI());
		Assertions.assertTrue(type.toString().equals(EX.Artist.toString()));
	}

	@Test
	public void testUpdate1RepeatUpdate() {
		testUpdate1();
		testUpdate1();
		testUpdate1();
		testUpdate1();
		testUpdate1();
	}

	@Test
	public void testUpdate3() {
		UpdateExecutionBuilder updateBuilder = rdf4JTemplate.update(getClass(), "createVermeer",
				() -> String.format("INSERT { <%s> a <%s> } WHERE {} ", EX.of("Vermeer"), EX.Artist));
		updateBuilder.execute();
		Value type = rdf4JTemplate.tupleQuery(
				String.format("SELECT ?type WHERE { <%s> a ?type }",
						EX.of("Vermeer")))
				.evaluateAndConvert()
				.toSingleton(bs -> bs.getBinding("type").getValue());
		Assertions.assertTrue(type.isIRI());
		Assertions.assertTrue(type.toString().equals(EX.Artist.toString()));
	}

	@Test
	public void testUpdateFromResource() {
		UpdateExecutionBuilder updateBuilder = rdf4JTemplate.updateFromResource(getClass(),
				"classpath:sparql/insert-vermeer.rq");
		updateBuilder.execute();
		Value type = rdf4JTemplate.tupleQuery(
				String.format("SELECT ?type WHERE { <%s> a ?type }",
						EX.of("Vermeer")))
				.evaluateAndConvert()
				.toSingleton(bs -> bs.getBinding("type").getValue());
		Assertions.assertTrue(type.isIRI());
		Assertions.assertTrue(type.toString().equals(EX.Artist.toString()));
	}

	@Test
	public void testUpdate2() {
		UpdateExecutionBuilder updateBuilder = rdf4JTemplate.update(getClass(),
				NamedSparqlSupplier.of("addVermeer",
						() -> String.format("INSERT { <%s> a <%s> } WHERE {} ", EX.of("Vermeer"), EX.Artist)));
		updateBuilder.execute();
		Value type = rdf4JTemplate.tupleQuery(
				String.format("SELECT ?type "
						+ "WHERE { <%s> a ?type }",
						EX.of("Vermeer")))
				.evaluateAndConvert()
				.toSingleton(bs -> bs.getBinding("type").getValue());
		Assertions.assertTrue(type.isIRI());
		Assertions.assertTrue(type.toString().equals(EX.Artist.toString()));

	}

	@Test
	public void testUpdateWithoutCachingStatement() {
		UpdateExecutionBuilder updateBuilder = rdf4JTemplate.updateWithoutCachingStatement(
				String.format("INSERT { <%s> a <%s> } "
						+ "WHERE {} ", EX.of("Vermeer"), EX.Artist));
		updateBuilder.execute();
		Value type = rdf4JTemplate.tupleQuery(
				String.format("SELECT ?type "
						+ "WHERE { <%s> a ?type }",
						EX.of("Vermeer")))
				.evaluateAndConvert()
				.toSingleton(bs -> bs.getBinding("type").getValue());
		Assertions.assertTrue(type.isIRI());
		Assertions.assertTrue(type.toString().equals(EX.Artist.toString()));
	}

	@Test
	public void testUpdateWithBuilder() {
		rdf4JTemplate.updateWithBuilder()
				.subject(EX.of("Vermeer"))
				.add(RDF.TYPE, EX.Artist)
				.execute();
		Value type = rdf4JTemplate.tupleQuery(
				String.format("SELECT ?type WHERE { <%s> a ?type }",
						EX.of("Vermeer")))
				.evaluateAndConvert()
				.toSingleton(bs -> bs.getBinding("type").getValue());
		Assertions.assertTrue(type.isIRI());
		Assertions.assertTrue(type.toString().equals(EX.Artist.toString()));
	}

	@Test
	public void testTupleQuery() {
		Set artists = rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
				+ "SELECT distinct ?artist "
				+ "WHERE { ?artist a ex:Artist }")
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.of("Picasso")));
		Assertions.assertTrue(artists.contains(EX.of("VanGogh")));
	}

	@Test
	public void testTupleQueryParametrized() {
		Set artists = rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
				+ "SELECT distinct ?artist "
				+ "WHERE { ?artist a ?type }")
				.withBinding("type", EX.Artist)
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.of("Picasso")));
		Assertions.assertTrue(artists.contains(EX.of("VanGogh")));
	}

	@Test
	public void testTupleQueryRepeatQuery() {
		testTupleQuery();
		testTupleQuery();
		testTupleQuery();
		testTupleQuery();
		testTupleQuery();
	}

	@Test
	public void tupleQuery3() {
		Set artists = rdf4JTemplate.tupleQuery(getClass(), "readArtists",
				() -> "PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?artist "
						+ "WHERE { ?artist a ex:Artist }")
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.of("Picasso")));
		Assertions.assertTrue(artists.contains(EX.of("VanGogh")));
	}

	@Test
	public void testTupleQueryFromResource() {
		Set artists = rdf4JTemplate.tupleQueryFromResource(getClass(), "classpath:sparql/get-artists.rq")
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.of("Picasso")));
		Assertions.assertTrue(artists.contains(EX.of("VanGogh")));
	}

	@Test
	public void testTupleQuery2() {
		Set artists = rdf4JTemplate.tupleQuery(getClass(),
				NamedSparqlSupplier.of("getArtists", () -> "PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?artist "
						+ "WHERE { ?artist a ex:Artist }"))
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.of("Picasso")));
		Assertions.assertTrue(artists.contains(EX.of("VanGogh")));
	}

	@Test
	public void testGraphQuery() {
		Model model = rdf4JTemplate.graphQuery("PREFIX ex: <http://example.org/>"
				+ "CONSTRUCT { ?a ?p ?o } "
				+ "WHERE { ?a a ex:Artist; ?p ?o }")
				.evaluateAndConvert()
				.toModel();
		checkArtistModel(model);
	}

	@Test
	public void graphQueryRepeatedly() {
		for (int i = 0; i < 20; i++) {
			testGraphQuery();
		}
	}

	protected void checkArtistModel(Model model) {
		Assertions.assertTrue(
				model.contains(
						EX.of("Picasso"),
						FOAF.SURNAME,
						SimpleValueFactory.getInstance().createLiteral("Picasso")));
		Assertions.assertTrue(
				model.contains(
						EX.of("Picasso"),
						FOAF.FIRST_NAME,
						SimpleValueFactory.getInstance().createLiteral("Pablo")));
		Assertions.assertTrue(
				model.contains(
						EX.of("VanGogh"),
						FOAF.FIRST_NAME,
						SimpleValueFactory.getInstance().createLiteral("Vincent")));
		Assertions.assertTrue(
				model.contains(
						EX.of("VanGogh"),
						EX.of("creatorOf"),
						EX.of("starryNight")));
	}

	@Test
	public void testGraphQuery3() {
		Model model = rdf4JTemplate.graphQuery(
				getClass(),
				"getArtistStarshapedGraphs",
				() -> "PREFIX ex: <http://example.org/>"
						+ "CONSTRUCT { ?a ?p ?o } "
						+ "WHERE { ?a a ex:Artist; ?p ?o }")
				.evaluateAndConvert()
				.toModel();
		checkArtistModel(model);
	}

	@Test
	public void testGraphQueryFromResource() {
		Model model = rdf4JTemplate.graphQueryFromResource(getClass(), "classpath:sparql/construct-artists.rq")
				.evaluateAndConvert()
				.toModel();
		checkArtistModel(model);
	}

	@Test
	public void testGraphQuery2() {
		Model model = rdf4JTemplate.graphQuery(
				getClass(),
				NamedSparqlSupplier.of("getArtistStarshapedGraphs",
						() -> "PREFIX ex: <http://example.org/>"
								+ "CONSTRUCT { ?a ?p ?o } "
								+ "WHERE { ?a a ex:Artist; ?p ?o }"))
				.evaluateAndConvert()
				.toModel();
		checkArtistModel(model);
	}

	/**
	 *
	 * STILL TODO
	 *
	 *
	 * public void deleteTriplesWithSubject(IRI id) { rdf4JTemplate.deleteTriplesWithSubject(id); }
	 * 
	 * public void delete(IRI id) { rdf4JTemplate.delete(id); }
	 * 
	 * public void delete(IRI start, List<PropertyPath> propertyPaths) { rdf4JTemplate.delete(start, propertyPaths); }
	 * 
	 * public void associate(IRI fromResource, IRI property, Collection<IRI> toResources, boolean deleteOtherOutgoing,
	 * boolean deleteOtherIcoming) { rdf4JTemplate.associate(fromResource, property, toResources, deleteOtherOutgoing,
	 * deleteOtherIcoming); }
	 * 
	 * public Supplier<String> getStringSupplierFromResourceContent(String resourceName) { return
	 * rdf4JTemplate.getStringSupplierFromResourceContent(resourceName); }
	 * 
	 * public IRI getNewUUID() { return rdf4JTemplate.getNewUUID(); }
	 **/
}
