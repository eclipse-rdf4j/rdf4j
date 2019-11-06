package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.Test;

public class ElasticsearchStoreTest {

	@Test
	public void testInstantiate() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore();
	}

	@Test
	public void testGetConneciton() {
		ElasticsearchStore elasticsearchStore = new ElasticsearchStore();
		try (NotifyingSailConnection connection = elasticsearchStore.getConnection()) {
		}

	}

	@Test
	public void testSailRepository() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore());
	}

	@Test
	public void testGetSailRepositoryConneciton() {
		SailRepository elasticsearchStore = new SailRepository(new ElasticsearchStore());
		try (SailRepositoryConnection connection = elasticsearchStore.getConnection()) {
		}
	}

}
