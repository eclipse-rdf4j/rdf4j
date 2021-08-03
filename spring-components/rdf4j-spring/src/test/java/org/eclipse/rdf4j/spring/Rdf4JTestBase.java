package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.spring.pool.PoolConfig;
import org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryConfig;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryConfig;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheConfig;
import org.eclipse.rdf4j.spring.support.DataInserter;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSourceConfig;
import org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequenceConfig;
import org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSourceConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration(
		classes = {
				Rdf4JConfig.class,
				TestConfig.class,
				InMemoryRepositoryConfig.class,
				RemoteRepositoryConfig.class,
				PoolConfig.class,
				ResultCacheConfig.class,
				TxConfig.class,
				UUIDSequenceConfig.class,
				NoveltyCheckingUUIDSourceConfig.class,
				SimpleRepositoryUUIDSourceConfig.class
		})
@TestPropertySource("classpath:application.properties")
@TestPropertySource(
		properties = {
				"rdf4j.spring.repository.inmemory.enabled=true",
				"rdf4j.spring.repository.inmemory.use-shacl-sail=true",
				"rdf4j.spring.tx.enabled=true",
				"rdf4j.spring.pool.enabled=false",
				"rdf4j.spring.resultcache.enabled=false"
		})
@DirtiesContext
public class Rdf4JTestBase {
	@BeforeAll
	public static void insertTestData(
			@Autowired DataInserter dataInserter,
			@Value("classpath:/data/example-data-artists-copy.ttl") Resource dataFile) {
		dataInserter.insertData(dataFile);
	}
}
