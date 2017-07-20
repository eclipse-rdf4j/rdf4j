package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.plan.Select;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class PropertyShape implements PlanGenerator{
    Resource id;
    SailRepositoryConnection connection;


    public PropertyShape(Resource id, SailRepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
    }

    @Override
    public Select getPlan() {
        throw  new IllegalStateException("Should never get here!!!");
    }


    public static class Factory {
        static List<PropertyShape> ret;

        static List<PropertyShape> getProprtyShapes(Resource ShapeId, SailRepositoryConnection connection) {
            ret = new ArrayList<>();
            System.out.println(ret.size());
            RepositoryResult<Statement> propertyShapeIds = connection.getStatements(ShapeId,SHACL.PROPERTY,null);
            while(propertyShapeIds.hasNext()) {
                Resource propertyShapeId = (Resource) propertyShapeIds.next().getObject();
                if (hasMinCount(propertyShapeId, connection)) {
                    ret.add(new MinCountPropertyShape(propertyShapeId, connection));
                    System.out.println("okkkkkk");
                }

                System.out.println(ret.size());
//            if (!hasMaxCount(propertyShapeId, connection, ret)) {
//                ret.add(new MaxCountPropertyShape(propertyShapeId, connection));
//            }

//            if(!haspath(propertyShapeId, connection)){
//                ret.add((Path)new PathPropertyShape(propertyShapeId, connection).getPaths());
//            }
            }
                return ret;

        }

        private static boolean hasMaxCount(Resource propertyShapeId, SailRepositoryConnection connection, List<PropertyShape> ret) {
            for (PropertyShape propertyShape : ret) {
                if (propertyShape instanceof MaxCountPropertyShape)
                    return true;
            }
            return false;
        }

        private static boolean hasMinCount(Resource id, SailRepositoryConnection connection) {
            if (connection.hasStatement(id, SHACL.MIN_COUNT, null, true)) {
                System.out.println("Has statement");
                return true;
            } else {
                return false;
            }

           /* RepositoryResult<Statement> result = connection.getStatements(null, null, null);
            boolean hasMinCount = false;
            while (result.hasNext()) {
                Statement st = result.next();
                if(st.getPredicate().getLocalName().equals("minCount")){
                    hasMinCount = true;
                }
                //System.out.println("db contains: " + st + " : " + st.getPredicate().getLocalName());
            }*/
//            for (PropertyShape propertyShape:ret) {
//                if(propertyShape instanceof MinCountPropertyShape)
//                    return true;
//            }
//            return false;
            // }
           // return hasMinCount;
        }
    }
}



