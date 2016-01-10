/**
 *
 */
package org.javarosa.core.services.storage;

import org.javarosa.core.services.storage.WrappingStorageUtility.SerializationWrapper;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author ctsims
 */
public class TaggedSerializationWrapper implements SerializationWrapper {

    Externalizable e;

    public TaggedSerializationWrapper() {
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.WrappingStorageUtility.SerializationWrapper#getData()
     */
    public Externalizable getData() {
        return e;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.WrappingStorageUtility.SerializationWrapper#setData(org.javarosa.core.util.externalizable.Externalizable)
     */
    public void setData(Externalizable e) {
        this.e = e;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        e = (Externalizable)ExtUtil.read(in, new ExtWrapTagged());
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(e));
    }

    public void clean() {
        e = null;
    }

}
