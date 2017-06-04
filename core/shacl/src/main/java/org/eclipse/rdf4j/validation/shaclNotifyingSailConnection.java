package org.eclipse.rdf4j.validation;

import org.eclipse.rdf4j.sail.SailConnection;

/**
 * Created by heshanjayasinghe on 6/1/17.
 */
public interface shaclNotifyingSailConnection extends SailConnection {

    public void addConnectionListener(shaclSailConnectionListener listener);

    public void removeConnectionListener(shaclSailConnectionListener listener);
}
