package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by heshanjayasinghe on 6/10/17.
 */
public class PropertyShape {

    public PropertyShape(Resource next,SailRepositoryConnection connection) {
    }

    static class Factory{

        List< PropertyShape > getShapes(Resource propertyShapeId, SailRepositoryConnection connection){
            List<PropertyShape> ret = new ArrayList<>();

            if(hasMinCount(propertyShapeId, connection)){
                ret.add(new MinCountPropertyShape(propertyShapeId, connection));
            }

            if(hasMaxCount(propertyShapeId, connection)){
                ret.add(new MaxCountPropertyShape(propertyShapeId, connection));
            }

            return ret;

        }

        private boolean hasMaxCount(Resource propertyShapeId,SailRepositoryConnection connection) {
            return true;
        }

        private boolean hasMinCount(Resource propertyShapeId, SailRepositoryConnection connection) {
            return true;
        }

    }
}
