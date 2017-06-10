package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.vocabulary.SH;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class Name {
    String name;

    Name(Resource next, SailRepositoryConnection connection){
        ValueFactory vf = connection.getValueFactory();
        name = connection.getStatements(next, vf.createIRI(SH.BASE_URI, "name"), null, true).next().getObject().stringValue();
    }

}
