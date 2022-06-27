/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.readonly;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.TestConfig;
import org.eclipse.rdf4j.spring.domain.dao.ArtistDao;
import org.eclipse.rdf4j.spring.domain.model.Artist;
import org.eclipse.rdf4j.spring.operationlog.OperationLogConfig;
import org.eclipse.rdf4j.spring.operationlog.log.jmx.OperationLogJmxConfig;
import org.eclipse.rdf4j.spring.pool.PoolConfig;
import org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryConfig;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryConfig;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheConfig;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.eclipse.rdf4j.spring.tx.exception.WriteDeniedException;
import org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSourceConfig;
import org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequenceConfig;
import org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSourceConfig;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = {
				TestConfig.class,
				InMemoryRepositoryConfig.class,
				RemoteRepositoryConfig.class,
				PoolConfig.class,
				ResultCacheConfig.class,
				TxConfig.class,
				OperationLogConfig.class,
				OperationLogJmxConfig.class,
				UUIDSequenceConfig.class,
				NoveltyCheckingUUIDSourceConfig.class,
				SimpleRepositoryUUIDSourceConfig.class,
				ReadonlyTests.Config.class
		})
@ComponentScan(
		value = {
				"at.researchstudio.sat.merkmalservice.readonly",
				"at.researchstudio.sat.merkmalservice.service"
		})
@TestPropertySource("classpath:application.properties")
@TestPropertySource(
		properties = {
				"rdf4j.spring.repository.inmemory.enabled=true",
				"rdf4j.spring.repository.inmemory.use-shacl-sail=true",
				"rdf4j.spring.tx.enabled=true"
		})
public class ReadonlyTests {

	@Configuration
	public static class Config {
		@Bean
		public TestHelperService getTestHelperService(@Autowired ArtistDao artistDao) {
			return new TestHelperService(artistDao);
		}
	}

	@Autowired
	TestHelperService testHelperService;

	private static IRI projectId = null;

	@Test
	@Order(1)
	public void testReadonlyTransactionBehaviour() {
		projectId = testHelperService.createArtist();
		assertNotNull(projectId);
	}

	@Test
	@Order(2)
	public void testReadonlyTransactionBehaviour2() {
		Optional<Artist> artist = testHelperService.loadProject(projectId);
		assertTrue(artist.isPresent());
	}

	@Test
	@Order(3)
	public void test3() {
		assertThrows(
				WriteDeniedException.class,
				() -> testHelperService.createProjectInReadonlyTransaction());
	}
}
