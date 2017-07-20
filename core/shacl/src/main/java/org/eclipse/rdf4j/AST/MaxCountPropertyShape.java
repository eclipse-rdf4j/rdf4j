package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class MaxCountPropertyShape extends PathPropertyShape{

    Integer maxCount;

    public MaxCountPropertyShape(Resource next, SailRepositoryConnection connection) {
        super(next,connection);
        ValueFactory vf = connection.getValueFactory();
     //   maxCount = Integer.parseInt(connection.getStatements(next, vf.createIRI(SH.BASE_URI, "maxCount"), null, true).next().getObject().stringValue());

    }

    @Override
    public String toString() {
        return "MaxCountPropertyShape{" +
                "maxCount=" + maxCount +
                '}';
    }
}
