package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.spring.pool.PoolConfig;
import org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryConfig;
import org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryConfig;
import org.eclipse.rdf4j.spring.resultcache.ResultCacheConfig;
import org.eclipse.rdf4j.spring.support.DataInserter;
import org.eclipse.rdf4j.spring.support.Rdf4JTemplate;
import org.eclipse.rdf4j.spring.tx.TxConfig;
import org.eclipse.rdf4j.spring.util.QueryResultUtils;
import org.eclipse.rdf4j.spring.util.TypeMappingUtils;
import org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSourceConfig;
import org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequenceConfig;
import org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSourceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.Commit;
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
				"rdf4j.spring.tx.enabled=true"
		})
public class BasicTests extends Rdf4JTestBase {
	@Autowired
	Rdf4JTemplate rdf4JTemplate;

	@Test
	public void testIsTemplateWired() {
        Assertions.assertNotNull(rdf4JTemplate);
	}

	@Test
	void testTripleCount() {
		int count = rdf4JTemplate
				.tupleQuery("SELECT (count(?a) as ?cnt) WHERE { ?a ?b ?c}")
				.evaluateAndConvert()
				.toSingleton(bs -> TypeMappingUtils.toInt(
						QueryResultUtils.getValue(bs, "cnt")));
		Assertions.assertEquals(26, count);
	}

}
