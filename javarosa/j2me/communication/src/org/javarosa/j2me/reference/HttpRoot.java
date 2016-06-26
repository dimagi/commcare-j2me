/**
 *
 */
package org.javarosa.j2me.reference;

import org.javarosa.core.reference.PrefixedRootFactory;
import org.javarosa.core.reference.Reference;
import org.javarosa.j2me.reference.HttpReference.SecurityFailureListener;

/**
 * @author ctsims
 *
 */
public class HttpRoot extends PrefixedRootFactory {

    SecurityFailureListener listener;

    public HttpRoot(SecurityFailureListener listener) {
        super(new String[] {"http://","https://"});
        this.listener = listener;
    }


    protected Reference factory(String terminal, String URI) {
        return new HttpReference(URI, listener);
    }
}
