/**
 *
 */
package org.javarosa.j2me.reference;

import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.reference.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/**
 * @author ctsims
 *
 */
public class HttpReference implements Reference {

    String URI;
    SecurityFailureListener listener;

    public HttpReference(String URI) {
        this(URI, null);
    }

    public HttpReference(String URI, SecurityFailureListener listener) {
        this.URI = URI;
        this.listener = listener;
    }

    /* (non-Javadoc)
     * @see org.commcare.reference.Reference#doesBinaryExist()
     */
    public boolean doesBinaryExist() throws IOException {
        //Do HTTP connection stuff? Look for a 404?
        return true;
    }

    public InputStream getStream() throws IOException {
        try {
            final HttpConnection connection = (HttpConnection)Connector.open(URI);
            connection.setRequestMethod(HttpConnection.GET);

            InputStream httpStream = connection.openInputStream();

            //Buffer our stream, since reading small units at a time from the network
            //increases the likelihood of network errors
            return new BufferedInputStream(httpStream) {
                /* (non-Javadoc)
                 * @see java.io.InputStream#close()
                 */
                public void close() throws IOException {
                    //Some platforms were having issues with the connection close semantics
                    //where it was supposed to close the connection when the stream was closed,
                    //so we'll go ahead and move this here.
                    connection.close();

                    super.close();
                }
            };
        } catch(SecurityException se) {
            if(this.listener != null) {
                listener.onSecurityException(se);
            }
            throw new IOException("Couldn't retrieve data from " + this.getLocalURI() + " due to lack of permissions.");
        }
    }

    public String getURI() {
        return URI;
    }

    /* (non-Javadoc)
     * @see org.commcare.reference.Reference#isReadOnly()
     */
    public boolean isReadOnly() {
        return true;
    }

    public OutputStream getOutputStream() throws IOException {
        //TODO: Support writing here?
        throw new IOException("JavaRosa HTTP References are readonly. Please use the transport manager for this op.");
    }

    public void remove() throws IOException {
        throw new IOException("JavaRosa HTTP References are readonly. Please use the transport manager for this op.");
    }

    public String getLocalURI() {
        return URI;
    }

    public Reference[] probeAlternativeReferences() {
        return new Reference [0];
    }


    public interface SecurityFailureListener {
        public void onSecurityException(SecurityException e);
    }
}
