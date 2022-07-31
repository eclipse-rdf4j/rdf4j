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

package org.eclipse.rdf4j.spring.support;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.UpdateExecutionBuilder;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.domain.model.EX;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
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
		Assertions.assertEquals(EX.Artist.toString(), type.toString());
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
		Assertions.assertEquals(EX.Artist.toString(), type.toString());
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
		Assertions.assertEquals(EX.Artist.toString(), type.toString());
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
		Assertions.assertEquals(EX.Artist.toString(), type.toString());

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
		Assertions.assertEquals(EX.Artist.toString(), type.toString());
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
		Assertions.assertEquals(EX.Artist.toString(), type.toString());
	}

	@Test
	public void testTupleQuery() {
		Set<IRI> artists = rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
				+ "SELECT distinct ?artist "
				+ "WHERE { ?artist a ex:Artist }")
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
	}

	@Test
	public void testTupleQueryParametrized() {
		Set<IRI> artists = rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
				+ "SELECT distinct ?artist "
				+ "WHERE { ?artist a ?type }")
				.withBinding("type", EX.Artist)
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
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
		Set<IRI> artists = rdf4JTemplate.tupleQuery(getClass(), "readArtists",
				() -> "PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?artist "
						+ "WHERE { ?artist a ex:Artist }")
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
	}

	@Test
	public void testTupleQueryFromResource() {
		Set<IRI> artists = rdf4JTemplate.tupleQueryFromResource(getClass(), "classpath:sparql/get-artists.rq")
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
	}

	@Test
	public void testTupleQuery2() {
		Set<IRI> artists = rdf4JTemplate.tupleQuery(getClass(),
				NamedSparqlSupplier.of("getArtists", () -> "PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?artist "
						+ "WHERE { ?artist a ex:Artist }"))
				.evaluateAndConvert()
				.toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
		Assertions.assertEquals(2, artists.size());
		Assertions.assertTrue(artists.contains(EX.Picasso));
		Assertions.assertTrue(artists.contains(EX.VanGogh));
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
						EX.Picasso,
						FOAF.SURNAME,
						SimpleValueFactory.getInstance().createLiteral("Picasso")));
		Assertions.assertTrue(
				model.contains(
						EX.Picasso,
						FOAF.FIRST_NAME,
						SimpleValueFactory.getInstance().createLiteral("Pablo")));
		Assertions.assertTrue(
				model.contains(
						EX.VanGogh,
						FOAF.FIRST_NAME,
						SimpleValueFactory.getInstance().createLiteral("Vincent")));
		Assertions.assertTrue(
				model.contains(
						EX.VanGogh,
						EX.creatorOf,
						EX.starryNight));
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

	@Test
	public void testDeleteTriplesWithSubject() {
		rdf4JTemplate.deleteTriplesWithSubject(EX.guernica);
		Assertions.assertTrue(
				rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?a "
						+ "WHERE { ?a a ex:Painting . FILTER (?a = ex:guernica) }")
						.evaluateAndConvert()
						.toList(bs -> bs.getValue("a"))
						.isEmpty());
		Assertions.assertFalse(
				rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?a "
						+ "WHERE { ?a ?p ?o . FILTER (?o = ex:guernica) }")
						.evaluateAndConvert()
						.toList(bs -> bs.getValue("a"))
						.isEmpty());
	}

	@Test
	public void testDelete() {
		rdf4JTemplate.delete(EX.guernica);
		Assertions.assertTrue(
				rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?a "
						+ "WHERE { ?a a ex:Painting . FILTER (?a = ex:guernica) }")
						.evaluateAndConvert()
						.toList(bs -> bs.getValue("a"))
						.isEmpty());
		Assertions.assertTrue(
				rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?a "
						+ "WHERE { ?a ?p ?o . FILTER (?o = ex:guernica) }")
						.evaluateAndConvert()
						.toList(bs -> bs.getValue("a"))
						.isEmpty());
	}

	@Test
	public void testDelete2() {
		Assertions.assertFalse(
				rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?a "
						+ "WHERE { ?a ?b ?c . "
						+ "  FILTER (?a = ex:guernica "
						+ "  || ?c = ex:guernica) "
						+ "}")
						.evaluateAndConvert()
						.toList(bs -> bs.getValue("a"))
						.isEmpty());
		rdf4JTemplate.delete(EX.Picasso,
				List.of(
						PropertyPathBuilder
								.of(Rdf.iri(EX.creatorOf))
								.build()
				));
		Assertions.assertTrue(
				rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
						+ "SELECT distinct ?a "
						+ "WHERE { ?a ?b ?c . "
						+ "  FILTER (?a = ex:guernica "
						+ "  || ?a = ex:Picasso"
						+ "  || ?c = ex:guernica"
						+ "  || ?c = ex:Picasso) "
						+ "}")
						.evaluateAndConvert()
						.toList(bs -> bs.getValue("a"))
						.isEmpty());
		Assertions.assertFalse(rdf4JTemplate.tupleQuery("PREFIX ex: <http://example.org/>"
				+ "SELECT distinct ?a "
				+ "WHERE { ?a ?b ?c . "
				+ "  FILTER (?a = ex:starryNight "
				+ "  || ?a = ex:VanGogh"
				+ "  || ?c = ex:starryNight"
				+ "  || ?c = ex:VanGogh) "
				+ "}")
				.evaluateAndConvert()
				.toList(bs -> bs.getValue("a"))
				.isEmpty());

	}

	@Test
	public void testAssociate_deleteIncoming() {
		IRI me = EX.of("me");
		rdf4JTemplate.updateWithBuilder()
				.subject(me)
				.add(RDF.TYPE, EX.Artist)
				.execute();

		// let's forge some data
		rdf4JTemplate.associate(
				me,
				EX.creatorOf,
				Set.of(EX.guernica, EX.starryNight, EX.potatoEaters),
				false, true);
		Assertions.assertTrue(
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", EX.Picasso)
						.evaluateAndConvert()
						.toList(b -> b)
						.isEmpty());
		Assertions.assertEquals(1,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", EX.VanGogh)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());
		Assertions.assertEquals(3,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", me)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());

	}

	@Test
	public void testAssociate_deleteOutgoing() {
		rdf4JTemplate.associate(
				EX.Picasso,
				EX.creatorOf,
				Set.of(EX.starryNight, EX.potatoEaters),
				true, false);
		Assertions.assertEquals(2,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", EX.Picasso)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());
		Assertions.assertEquals(3,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", EX.VanGogh)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());

	}

	@Test
	public void testAssociate() {
		IRI me = EX.of("me");
		rdf4JTemplate.updateWithBuilder()
				.subject(me)
				.add(RDF.TYPE, EX.Artist)
				.execute();

		// let's forge some data
		rdf4JTemplate.associate(
				me,
				EX.creatorOf,
				Set.of(EX.guernica, EX.starryNight, EX.potatoEaters),
				false, false);
		Assertions.assertEquals(1,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", EX.Picasso)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());
		Assertions.assertEquals(3,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", EX.VanGogh)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());
		Assertions.assertEquals(3,
				rdf4JTemplate.tupleQueryFromResource(getClass(),
						"classpath:sparql/get-paintings-of-artist.rq")
						.withBinding("artist", me)
						.evaluateAndConvert()
						.toList(b -> b)
						.size());

	}
}
