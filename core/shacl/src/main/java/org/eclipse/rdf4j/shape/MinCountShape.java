package org.eclipse.rdf4j.shape;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.List;

/**
 * Created by heshanjayasinghe on 5/4/17.
 */
public class MinCountShape {

    Integer minCount;

    public MinCountShape(Resource next, SailRepositoryConnection connection) {

        ValueFactory vf = connection.getValueFactory();
        minCount = Integer.parseInt(connection.getStatements(next, vf.createIRI(Main.SH, "minCount"), null, true).next().getObject().stringValue());


    }

    public boolean validate(List<Statement> addedStatement) {
        return false;
    }
}
