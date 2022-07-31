/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.readonly;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.domain.dao.ArtistDao;
import org.eclipse.rdf4j.spring.domain.model.Artist;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestHelperService {
	ArtistDao artistDao;

	public TestHelperService(ArtistDao artistDao) {
		this.artistDao = artistDao;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public IRI createArtist() {
		Artist artist = new Artist();
		artist.setFirstName("Leonardo");
		artist.setLastName("Da Vinci");
		Artist created = artistDao.save(artist);
		assertNotNull(created.getId());
		return created.getId();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public IRI createProjectInReadonlyTransaction() {
		Artist artist = new Artist();
		artist.setFirstName("Leonardo");
		artist.setLastName("Da Vinci");
		Artist created = artistDao.save(artist);
		assertNotNull(created.getId());
		return created.getId();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Optional<Artist> loadProject(IRI id) {
		return artistDao.getByIdOptional(id);
	}
}
