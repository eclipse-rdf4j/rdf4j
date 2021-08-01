package org.eclipse.rdf4j.spring.uuidsource.noveltychecking;

import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.support.Rdf4JTemplate;
import org.eclipse.rdf4j.spring.support.UUIDSource;
import org.springframework.beans.factory.annotation.Autowired;

public class NoveltyCheckingUUIDSource implements UUIDSource {
	@Autowired
	private Rdf4JTemplate rdf4JTemplate;

	@Override
	public IRI nextUUID() {
		return rdf4JTemplate.applyToConnection(
				con -> {
					IRI newId;
					do {
						newId = toURNUUID(UUID.randomUUID().toString());
					} while (con.hasStatement(newId, null, null, true));
					return newId;
				});
	}
}
