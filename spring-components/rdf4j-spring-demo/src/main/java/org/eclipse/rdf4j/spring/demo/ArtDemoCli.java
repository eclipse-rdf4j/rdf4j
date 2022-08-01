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

package org.eclipse.rdf4j.spring.demo;

import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.demo.model.Artist;
import org.eclipse.rdf4j.spring.demo.model.Painting;
import org.eclipse.rdf4j.spring.demo.service.ArtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Command line interface for the demo. Takes no parameters. It outputs the content of the demo repository, then adds
 * some data and outputs the content of the repository again.
 *
 * Accessing the repository is done via the {@link ArtService} class, which just encapsulates accesses to the
 * {@link org.eclipse.rdf4j.spring.demo.dao.PaintingDao} and {@link org.eclipse.rdf4j.spring.demo.dao.ArtistDao}
 * classes.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@SpringBootApplication
public class ArtDemoCli implements CommandLineRunner {
	@Autowired
	ArtService artService;

	public static void main(String[] args) {
		SpringApplication.run(ArtDemoCli.class, args).close();
	}

	@Override
	public void run(String... args) {
		System.out.println("\nData read from 'artists.ttl':");
		Map<Artist, Set<Painting>> paintingsMap = artService.getPaintingsGroupedByArtist();
		listPaintingsByArtist(paintingsMap);
		System.out.println("\nNow adding some data...");
		addPaintingWithArtist();
		System.out.println("\nReloaded data:");
		paintingsMap = artService.getPaintingsGroupedByArtist();
		listPaintingsByArtist(paintingsMap);
		System.out.println("\n");
		listArtistsWithoutPaintings();
		System.out.println("\n");
	}

	private void addPaintingWithArtist() {
		Artist a = new Artist();
		a.setFirstName("Jan");
		a.setLastName("Vermeer");
		IRI artistId = artService.addArtist(a);
		Painting p = new Painting();
		p.setTitle("View of Delft");
		p.setTechnique("oil on canvas");
		p.setArtistId(artistId);
		artService.addPainting(p);
	}

	private void listPaintingsByArtist(Map<Artist, Set<Painting>> paintingsMap) {
		for (Artist a : paintingsMap.keySet()) {
			System.out.println(String.format("%s %s", a.getFirstName(), a.getLastName()));
			for (Painting p : paintingsMap.get(a)) {
				System.out.println(String.format("\t%s (%s)", p.getTitle(), p.getTechnique()));
			}
		}
	}

	private void listArtistsWithoutPaintings() {
		System.out.println("Artists without paintings:");
		Set<Artist> a = artService.getArtistsWithoutPaintings();
		if (a.isEmpty()) {
			System.out.println("\t[none]");
		} else {
			for (Artist artist : a) {
				System.out.println(String.format("%s %s", artist.getFirstName(), artist.getLastName()));
			}
		}
	}
}
