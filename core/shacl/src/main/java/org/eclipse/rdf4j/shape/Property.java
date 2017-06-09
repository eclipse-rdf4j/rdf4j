package org.eclipse.rdf4j.shape;

import org.eclipse.rdf4j.model.Model;

/**
 * Created by heshanjayasinghe on 6/8/17.
 */
public interface Property {

    boolean isProperty();

    String getNameSpace();

    Property inModel(Model model);
}
