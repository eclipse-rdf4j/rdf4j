package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MainRepositorySail {

	public static void main(String[] args) {

		SailRepository repository = new SailRepository(new MemoryStore());
//		repository.initialize();
		
//		RepositorySail sail = new RepositorySail(repository);

		Sail sail = repository.getSail();
		
		System.out.println("#### START");
		sail.initialize();

		System.out.println("#### STOP");
		sail.shutDown();

	}

}
