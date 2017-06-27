package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 6/11/17.
 */
public class PathPropertyShape extends PropertyShape {
    public Path path;

    public PathPropertyShape(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
        this.id = id;
        this.connection = connection;

        if(connection.hasStatement(id, SHACL.PATH, null, true)) {
            path = new Path(id, connection);
        }

    }


}

