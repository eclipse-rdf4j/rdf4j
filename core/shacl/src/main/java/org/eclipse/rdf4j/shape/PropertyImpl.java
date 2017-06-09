package org.eclipse.rdf4j.shape;

import org.eclipse.rdf4j.model.Model;

/**
 * Created by heshanjayasinghe on 6/8/17.
 */
public class PropertyImpl implements Property {
    public PropertyImpl(String uriref) {
       // super(uriref);
    }

    @Override
    public boolean isProperty() {
        return true;
    }

    @Override
    public String getNameSpace() {
        return null;
    }

    @Override
    public Property inModel(Model model) {
        return null;
    }
}
