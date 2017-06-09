package org.eclipse.rdf4j.shape;

/**
 * Created by heshanjayasinghe on 6/8/17.
 */
public class ShaclResourceImpl implements ResourceFactory.InterfaceResource{

    @Override
    public Property createProperty(String uriref) {
        return new PropertyImpl(uriref);
    }
}
