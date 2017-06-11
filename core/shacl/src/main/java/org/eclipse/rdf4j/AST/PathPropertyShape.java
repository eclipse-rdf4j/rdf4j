package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 6/11/17.
 */
public class PathPropertyShape extends PropertyShape {
    Path path;

    public PathPropertyShape(Resource next, SailRepositoryConnection connection) {
        super(next, connection);
    }
}

