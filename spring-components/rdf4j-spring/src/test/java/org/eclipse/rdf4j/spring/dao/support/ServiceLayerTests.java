/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.dao.support;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.domain.model.Artist;
import org.eclipse.rdf4j.spring.domain.model.Painting;
import org.eclipse.rdf4j.spring.domain.service.ArtService;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Florian Kleedorfer
 * @since 4.0.0
 */
public class ServiceLayerTests extends RDF4JSpringTestBase {
	@Autowired
	private ArtService artService;
	@Autowired
	private RDF4JTemplate rdf4JTemplate;
	@Autowired
	private PlatformTransactionManager transactionManager;
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Test
	public void testCreateArtist() {
		Artist artist = artService.createArtist("Jan", "Vermeer");
		assertNotNull(artist.getId());
		assertTrue(artist.getId().toString().startsWith("urn:uuid"));
	}

	@Test
	public void testCreatePainting() {
		Artist artist = artService.createArtist("Jan", "Vermeer");
		Painting painting = artService.createPainting("Girl with a pearl earring", "oil on canvas", artist.getId());
		assertNotNull(painting.getId());
		assertTrue(painting.getId().toString().startsWith("urn:uuid"));
	}

	@Test
	public void testCreatePaintingWithoutArtist() {
		assertThrows(NullPointerException.class, () -> artService.createPainting(
				"Girl with a pearl earring",
				"oil on canvas",
				null));
	}

	@Test
	public void testRollbackOnException() {
		transactionTemplate.execute(status -> {
			Artist artist = artService.createArtist("Jan", "Vermeer");
			// make sure we can query vermeer from the db
			assertEquals(1,
					rdf4JTemplate.tupleQueryFromResource(
							getClass(),
							"classpath:sparql/get-artists.rq")
							.withBinding("artist", artist.getId())
							.evaluateAndConvert()
							.toSet(BindingSetMapper.identity())
							.size());
			// now ascertain that the transaction will commit eventually
			assertFalse(status.isRollbackOnly());
			// now insert a painting without artist (throws exception)
			assertThrows(NullPointerException.class, () -> artService.createPainting(
					"Girl with a pearl earring",
					"oil on canvas",
					null));
			// now ascertain that the transaction will not commit because of the exception
			assertTrue(status.isRollbackOnly());
			return null;
		});
	}
}
