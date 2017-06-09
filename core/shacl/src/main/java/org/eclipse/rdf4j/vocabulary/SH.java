package org.eclipse.rdf4j.vocabulary;


import org.eclipse.rdf4j.shape.Property;
import org.eclipse.rdf4j.shape.ResourceFactory;

/**
 * Created by heshanjayasinghe on 6/8/17.
 */
public class SH {

    public final static String BASE_URI = "http://www.w3.org/ns/shacl#";

    public final static String NS = BASE_URI;

    public final static Property minCount = ResourceFactory.createProperty(NS + "minCount");


}
