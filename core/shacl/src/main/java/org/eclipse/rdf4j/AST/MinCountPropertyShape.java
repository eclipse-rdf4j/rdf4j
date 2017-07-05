package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class MinCountPropertyShape extends PathPropertyShape{

    public Integer minCount;

    public MinCountPropertyShape(Resource id, SailRepositoryConnection connection) {
        super(id,connection);
        RepositoryResult<Statement> statement = connection.getStatements(id, SHACL.MIN_COUNT, null, true);
        if(statement.hasNext())
        minCount = Integer.valueOf(statement.next().getObject().stringValue());
    }

    @Override
    public String toString() {
        return "MinCountPropertyShape{" +
                "minCount=" + minCount +
                '}';
    }
}
