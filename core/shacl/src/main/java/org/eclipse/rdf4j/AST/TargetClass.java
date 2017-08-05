package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.plan.Select;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class TargetClass extends Shape implements PlanGenerator{
    Resource id;
    SailRepositoryConnection connection;
    Resource targetClass;

    public TargetClass(Resource id, SailRepositoryConnection connection) {
        super(id, connection);
        this.id = id;
        this.connection = connection;

        if(connection.hasStatement(id, SHACL.TARGET_CLASS, null, true)){
            targetClass = (Resource) connection.getStatements(id, SHACL.TARGET_CLASS, null).next().getObject();
        }
    }

    @Override
    public Select getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {
      //  return null;
        Select select =new Select(shaclSailConnection,null, RDF.TYPE,targetClass);
//        if (select.iterator().hasNext()){
//            select.iterator().next();
//        }
      //  return new Select(shape.generatePlans(shaclSailConnection,shape));
        return select;
    }
}
