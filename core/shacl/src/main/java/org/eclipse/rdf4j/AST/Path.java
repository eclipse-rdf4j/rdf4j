/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Path {
    Resource path;
    Resource id;
    SailRepositoryConnection connection;

    public Path(Resource next, SailRepositoryConnection connection) {
        super();
        this.id = id;
        this.connection = connection;

        ValueFactory vf = connection.getValueFactory();
        if(connection.hasStatement(id, SHACL.PATH, null, true)) {
            path = (Resource) connection.getStatements(next, SHACL.PATH, null, true).next().getObject();
        }
    }

    @Override
    public String toString() {
        return "Path{" +
                "path=" + path +
                '}';
    }


}
