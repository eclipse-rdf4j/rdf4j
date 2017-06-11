package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class TargetClass extends Shape {
    Resource targetclass;

    public TargetClass(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
    }
}
