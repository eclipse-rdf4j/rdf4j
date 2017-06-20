package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class PropertyShape{
    Resource id;
    SailRepositoryConnection connection;


    public PropertyShape(Resource id,SailRepositoryConnection connection) {
        this.id = id;
        this.connection = connection;
        }


    public static class Factory{
        List<PropertyShape> ret;
        List< PropertyShape > getProprtyShapes(Resource propertyShapeId, SailRepositoryConnection connection){
            ret = new ArrayList<>();

            if(!hasMinCount(propertyShapeId, connection)){
                ret.add(new MinCountPropertyShape(propertyShapeId, connection));
            }

            if(!hasMaxCount(propertyShapeId, connection)){
                ret.add(new MaxCountPropertyShape(propertyShapeId, connection));
            }

//            if(!haspath(propertyShapeId, connection)){
//                ret.add((Path)new PathPropertyShape(propertyShapeId, connection).getPaths());
//            }

            return ret;
        }

        private boolean hasMaxCount(Resource propertyShapeId,SailRepositoryConnection connection) {
            for (PropertyShape propertyShape:ret) {
                if(propertyShape instanceof MaxCountPropertyShape)
                    return true;
            }
            return false;
        }

        private boolean hasMinCount(Resource propertyShapeId, SailRepositoryConnection connection) {
            for (PropertyShape propertyShape:ret) {
                if(propertyShape instanceof MinCountPropertyShape)
                    return true;
            }
            return false;
        }

    }


}
