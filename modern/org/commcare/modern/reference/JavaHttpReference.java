/**
 *
 */
package org.commcare.modern.reference;

import org.javarosa.core.reference.Reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author ctsims
 *
 */
public class JavaHttpReference implements Reference {

    private final String uri;

    public JavaHttpReference(String uri) {
        this.uri = uri;
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.reference.Reference#doesBinaryExist()
     */
    public boolean doesBinaryExist() throws IOException {
        //For now....
        return true;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.reference.Reference#getOutputStream()
     */
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Http references are read only!");
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.reference.Reference#getStream()
     */
    public InputStream getStream() throws IOException {
        URL url = new URL(uri);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
        HttpURLConnection.setFollowRedirects(true);
        
        return conn.getInputStream();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.reference.Reference#getURI()
     */
    public String getURI() {
        return uri;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.reference.Reference#isReadOnly()
     */
    public boolean isReadOnly() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.reference.Reference#remove()
     */
    public void remove() throws IOException {
        throw new IOException("Http references are read only!");
    }


    public String getLocalURI() {
        return uri;
    }


    public Reference[] probeAlternativeReferences() {
        return new Reference[0];
    }

}
