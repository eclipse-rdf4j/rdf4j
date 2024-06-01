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

package org.eclipse.rdf4j.spring.domain.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.domain.dao.ArtistDao;
import org.eclipse.rdf4j.spring.domain.dao.PaintingDao;
import org.eclipse.rdf4j.spring.domain.model.Artist;
import org.eclipse.rdf4j.spring.domain.model.Painting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ArtService {
	@Autowired
	private ArtistDao artistDao;

	@Autowired
	private PaintingDao paintingDao;

	@Transactional(propagation = Propagation.REQUIRED)
	public Artist createArtist(String firstName, String lastName) {
		Artist artist = new Artist();
		artist.setFirstName(firstName);
		artist.setLastName(lastName);
		return artistDao.save(artist);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Painting createPainting(String title, String technique, IRI artist) {
		Painting painting = new Painting();
		painting.setTitle(title);
		painting.setTechnique(technique);
		painting.setArtistId(artist);
		return paintingDao.save(painting);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Set<Painting> getPaintingsOfArtist(IRI artistId) {
		Set<IRI> paintingIds = artistDao.getPaintingsIdsOfArtist(artistId);
		return paintingIds.stream().map(paintingDao::getById).collect(Collectors.toSet());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Set<Artist> getArtistsOfPainting(IRI paintingId) {
		Set<IRI> artistIds = paintingDao.getArtistIdsOfPainting(paintingId);
		return artistIds.stream().map(artistDao::getById).collect(Collectors.toSet());
	}
}
