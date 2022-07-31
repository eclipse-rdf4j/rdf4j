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

import java.util.Set;

import org.eclipse.rdf4j.spring.demo.TestConfig;
import org.eclipse.rdf4j.spring.demo.model.Artist;
import org.eclipse.rdf4j.spring.demo.model.EX;
import org.eclipse.rdf4j.spring.support.DataInserter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource("classpath:application.properties")
@TestPropertySource(
		properties = {
				"rdf4j.spring.repository.inmemory.enabled=true",
				"rdf4j.spring.repository.inmemory.use-shacl-sail=true",
				"rdf4j.spring.tx.enabled=true",
				"rdf4j.spring.resultcache.enabled=false",
				"rdf4j.spring.operationcache.enabled=false",
				"rdf4j.spring.pool.enabled=true",
				"rdf4j.spring.pool.max-connections=2"
		})
@DirtiesContext
public class ArtistDaoTests {

	@Autowired
	private ArtistDao artistDao;

	@BeforeAll
	public static void insertTestData(
			@Autowired DataInserter dataInserter,
			@Value("classpath:artists.ttl") Resource dataFile) {
		dataInserter.insertData(dataFile);
	}

	@Test
	public void testReadArtist() {
		Artist a = artistDao.getById(EX.Picasso);
		Assertions.assertEquals("Picasso", a.getLastName());
		Assertions.assertEquals("Pablo", a.getFirstName());
	}

	@Test
	public void testWriteArtist() {
		Artist a = new Artist();
		a.setFirstName("Salvador");
		a.setLastName("Dalí");
		Artist savedDali = artistDao.save(a);
		Assertions.assertNotNull(savedDali.getId());
		Artist reloadedDali = artistDao.getById(savedDali.getId());
		Assertions.assertEquals(savedDali, reloadedDali);
	}

	@Test
	public void testReadArtistWithoutPaintings() {
		Set<Artist> withoutPaintings = artistDao.getArtistsWithoutPaintings();
		Assertions.assertEquals(1, withoutPaintings.size());
		Artist a = artistDao.getById(EX.Rembrandt);
		Assertions.assertTrue(withoutPaintings.contains(a));
	}

}
