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

package org.eclipse.rdf4j.spring.dao.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.RDF4JSpringTestBase;
import org.eclipse.rdf4j.spring.domain.model.Artist;
import org.eclipse.rdf4j.spring.domain.model.EX;
import org.eclipse.rdf4j.spring.domain.model.Painting;
import org.eclipse.rdf4j.spring.domain.service.ArtService;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.tx.TransactionObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import shaded_package.org.bouncycastle.asn1.tsp.ArchiveTimeStamp;

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

	// TODO
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
			assertTrue(((TransactionObject) ((DefaultTransactionStatus) status).getTransaction()).isRollbackOnly());
			return null;
		});
	}

	@Test
	public void testGetPaintingsOfArtist() {
		transactionTemplate.execute(status -> {
			Set<Painting> paintings = artService.getPaintingsOfArtist(EX.VanGogh);
			assertEquals(3, paintings.size());
			assertTrue(paintings.stream().anyMatch(p -> p.getId().equals(EX.starryNight)));
			assertTrue(paintings.stream().anyMatch(p -> p.getId().equals(EX.potatoEaters)));
			assertTrue(paintings.stream().anyMatch(p -> p.getId().equals(EX.sunflowers)));
			return null;
		});
	}

	@Test
	public void testGetArtistOfPainting() {
		transactionTemplate.execute(status -> {
			Set<Artist> artists = artService.getArtistsOfPainting(EX.guernica);
			assertEquals(1, artists.size());
			assertTrue(artists.stream().anyMatch(p -> p.getId().equals(EX.Picasso)));
			return null;
		});
	}
}
