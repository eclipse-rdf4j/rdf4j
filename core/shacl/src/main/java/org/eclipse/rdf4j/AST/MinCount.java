package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class MinCount {

    Integer minCount;

    public MinCount(Resource next, SailRepositoryConnection connection) {

        ValueFactory vf = connection.getValueFactory();
        minCount = Integer.parseInt(connection.getStatements(next, vf.createIRI(SH.BASE_URI, "minCount"), null, true).next().getObject().stringValue());

    }
}
