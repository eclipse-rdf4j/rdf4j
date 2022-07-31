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

package org.eclipse.rdf4j.spring.dao;

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.domain.dao.ArtistDao;
import org.eclipse.rdf4j.spring.domain.model.Artist;
import org.eclipse.rdf4j.spring.domain.model.EX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class RDF4JCrudDaoTests extends RDF4JSpringTestBase {
	@Autowired
	private ArtistDao artistDao;

	@ParameterizedTest
	@MethodSource
	public void testRead(Artist artist) {
		Artist artistFromDb = artistDao.getById(artist.getId());
		Assertions.assertEquals(artist.getFirstName(), artistFromDb.getFirstName());
		Assertions.assertEquals(artist.getLastName(), artistFromDb.getLastName());
	}

	public static Stream<Artist> testRead() {
		Artist picasso = new Artist();
		picasso.setFirstName("Pablo");
		picasso.setLastName("Picasso");
		picasso.setId(SimpleValueFactory.getInstance().createIRI("http://example.org/Picasso"));
		Artist vanGogh = new Artist();
		vanGogh.setFirstName("Vincent");
		vanGogh.setLastName("van Gogh");
		vanGogh.setId(SimpleValueFactory.getInstance().createIRI("http://example.org/VanGogh"));
		return Stream.of(picasso, vanGogh);
	}

	@Test
	public void testInsertThenRead() {
		IRI id = EX.of("Vermeer");
		Artist a = new Artist();
		a.setId(id);
		a.setLastName("Vermeer");
		a.setFirstName("Jan");
		artistDao.save(a);
		Artist artistfromDb = artistDao.getById(id);
		Assertions.assertEquals(a.getLastName(), artistfromDb.getLastName());
		Assertions.assertEquals(a.getFirstName(), artistfromDb.getFirstName());
		Assertions.assertEquals(a.getId(), artistfromDb.getId());
	}

	@Test
	public void testModify() {
		Artist a = artistDao.getById(EX.of("Picasso"));
		a.setFirstName("Pablo Ruiz");
		artistDao.save(a);
		Artist artistFromDb = artistDao.getById(EX.of("Picasso"));
		Assertions.assertEquals(a.getLastName(), artistFromDb.getLastName());
	}

	@Test
	public void testDelete() {
		artistDao.delete(EX.of("Picasso"));
		Optional<Artist> a = artistDao.getByIdOptional(EX.of("Picasso"));
		Assertions.assertTrue(a.isEmpty());
	}

	@Test
	public void testInsertWithUUID() {
		Artist a = new Artist();
		a.setFirstName("Munch");
		a.setLastName("Edvard");
		a = artistDao.save(a);
		Assertions.assertNotNull(a.getId());
		Assertions.assertTrue(a.getId().toString().startsWith("urn:uuid:"));
	}

}
