/**
 *
 */
package org.javarosa.services.transport;

import org.javarosa.core.api.IModule;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.j2me.reference.HttpReference.SecurityFailureListener;
import org.javarosa.j2me.reference.HttpRoot;
import org.javarosa.services.transport.impl.TransportMessageSerializationWrapper;
import org.javarosa.services.transport.impl.TransportMessageStore;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;

/**
 * @author ctsims
 *
 */
public class TransportManagerModule implements IModule {

    SecurityFailureListener listener;

    public TransportManagerModule() {

    }

    public TransportManagerModule(SecurityFailureListener listener) {
        this.listener = listener;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IModule#registerModule()
     */
    public void registerModule() {

        //Note: Do not remove fully qualified names here, otherwise the imports mess up the polish preprocessing

        //#if polish.api.wmapi
        String[] prototypes = new String[] { SimpleHttpTransportMessage.class.getName(), org.javarosa.services.transport.impl.sms.SMSTransportMessage.class.getName(), org.javarosa.services.transport.impl.binarysms.BinarySMSTransportMessage.class.getName(), TransportMessageSerializationWrapper.class.getName(), org.javarosa.core.services.transport.payload.ByteArrayPayload.class.getName(), org.javarosa.core.services.transport.payload.DataPointerPayload.class.getName(), org.javarosa.core.services.transport.payload.MultiMessagePayload.class.getName(), org.javarosa.services.transport.impl.simplehttp.multipart.HttpTransportHeader.class.getName()};
        //#else
        //# String[] prototypes = new String[] { SimpleHttpTransportMessage.class.getName(), TransportMessageSerializationWrapper.class.getName(), org.javarosa.core.services.transport.payload.ByteArrayPayload.class.getName(), org.javarosa.core.services.transport.payload.DataPointerPayload.class.getName(), org.javarosa.core.services.transport.payload.MultiMessagePayload.class.getName(), org.javarosa.services.transport.impl.simplehttp.multipart.HttpTransportHeader.class.getName()};
        //#endif

        PrototypeManager.registerPrototypes(prototypes);

        StorageManager.registerWrappedStorage(TransportMessageStore.Q_STORENAME, TransportMessageStore.Q_STORENAME, new TransportMessageSerializationWrapper());
        StorageManager.registerWrappedStorage(TransportMessageStore.RECENTLY_SENT_STORENAME, TransportMessageStore.RECENTLY_SENT_STORENAME, new TransportMessageSerializationWrapper());
        ReferenceManager._().addReferenceFactory(new HttpRoot(listener));
        
        PropertyManager._().addRules(new TransportPropertyRules());
        
        TransportService.init();
    }

}
