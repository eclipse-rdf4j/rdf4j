package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;

/**
 * Created by heshanjayasinghe on 6/1/17.
 */
public interface ShaclNotifyingSailConnection extends SailConnection {

    public void addConnectionListener(SailConnectionListener listener);

    public void removeConnectionListener(SailConnectionListener listener);
}
