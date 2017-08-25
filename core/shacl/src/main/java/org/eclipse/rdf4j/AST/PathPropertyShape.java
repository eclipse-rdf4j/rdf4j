package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.plan.Select;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

/**
 * Created by heshanjayasinghe on 6/11/17.
 */
public class PathPropertyShape extends PropertyShape implements PlanGenerator {
    public Path path;

    public PathPropertyShape(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
        this.id = id;
        this.connection = connection;

        if(connection.hasStatement(id, SHACL.PATH, null, true)) {
            path = new Path(id, connection);
        }

    }

    public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
        Select select =new Select(shaclSailConnection, null, (IRI) path.path, null);
        return select;
    }
}

