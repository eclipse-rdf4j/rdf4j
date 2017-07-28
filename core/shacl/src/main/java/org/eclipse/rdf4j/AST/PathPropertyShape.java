package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
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


    @Override
    public Select getPlan(ShaclSailConnection shaclSailConnection,Shape shape) {
//        List<PropertyShape> propertyShapes = shape.propertyShapes;
//        Tuple tuple = new Tuple();
//        Boolean hastuple =tuple.line.addAll(shape.propertyShapes);
//        if(hastuple){
//            return new Select( tuple.getlist());
//        }
        return null;
    }
}

