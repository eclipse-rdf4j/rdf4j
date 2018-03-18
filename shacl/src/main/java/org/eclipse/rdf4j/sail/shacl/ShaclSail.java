/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.shacl.AST.Shape;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Heshan Jayasinghe
 */
public class ShaclSail extends NotifyingSailWrapper {

	public List<Shape> shapes;

	ShaclSailConfig config = new ShaclSailConfig();

	public ShaclSail(NotifyingSail baseSail, SailRepository shaclSail) {
		super(baseSail);
		try (SailRepositoryConnection shaclSailConnection = shaclSail.getConnection()) {
			shapes = Shape.Factory.getShapes(shaclSailConnection);
		}
	}

	@Override
	public NotifyingSailConnection getConnection()
		throws SailException {
		return new ShaclSailConnection(this, super.getConnection(), super.getConnection());
	}

	public void disableValidation() {
		config.validationEnabled = false;
	}

	public void enableValidation() {
		config.validationEnabled = true;
	}

}

class ShaclSailConfig {

	boolean validationEnabled = true;

}
