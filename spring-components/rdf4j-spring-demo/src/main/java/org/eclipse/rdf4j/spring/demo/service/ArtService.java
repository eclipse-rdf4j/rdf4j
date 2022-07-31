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

package org.eclipse.rdf4j.spring.demo.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.demo.dao.ArtistDao;
import org.eclipse.rdf4j.spring.demo.dao.PaintingDao;
import org.eclipse.rdf4j.spring.demo.model.Artist;
import org.eclipse.rdf4j.spring.demo.model.Painting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uses {@link ArtistDao} and {@link PaintingDao} to query and manipulate the repository.
 *
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Component
public class ArtService {
	@Autowired
	private ArtistDao artistDao;

	@Autowired
	private PaintingDao paintingDao;

	@Transactional
	public Artist createArtist(String firstName, String lastName) {
		Artist artist = new Artist();
		artist.setFirstName(firstName);
		artist.setLastName(lastName);
		return artistDao.save(artist);
	}

	@Transactional
	public Painting createPainting(String title, String technique, IRI artist) {
		Painting painting = new Painting();
		painting.setTitle(title);
		painting.setTechnique(technique);
		painting.setArtistId(artist);
		return paintingDao.save(painting);
	}

	@Transactional
	public List<Painting> getPaintings() {
		return paintingDao.list();
	}

	@Transactional
	public List<Artist> getArtists() {
		return artistDao.list();
	}

	@Transactional
	public Set<Artist> getArtistsWithoutPaintings() {
		return artistDao.getArtistsWithoutPaintings();
	}

	@Transactional
	public Map<Artist, Set<Painting>> getPaintingsGroupedByArtist() {
		List<Painting> paintings = paintingDao.list();
		return paintings
				.stream()
				.collect(groupingBy(
						p -> artistDao.getById(p.getArtistId()),
						toSet()));
	}

	@Transactional
	public IRI addArtist(Artist artist) {
		return artistDao.saveAndReturnId(artist);
	}

	@Transactional
	public IRI addPainting(Painting painting) {
		return paintingDao.saveAndReturnId(painting);
	}

}
