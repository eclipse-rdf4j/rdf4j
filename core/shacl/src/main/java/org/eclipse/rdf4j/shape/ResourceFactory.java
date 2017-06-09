package org.eclipse.rdf4j.shape;

/**
 * Created by heshanjayasinghe on 6/8/17.
 */
public class ResourceFactory {
    protected static InterfaceResource instance = new ShaclResourceImpl();

    public static Property createProperty(String uriref) {
        return instance.createProperty(uriref);
    }


    public interface InterfaceResource {

        Property createProperty(String var1);
    }

}
