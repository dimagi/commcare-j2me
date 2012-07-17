/**
 * 
 */
package org.javarosa.j2me.reference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import org.javarosa.core.reference.Reference;

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
			HttpConnection connection = (HttpConnection)Connector.open(URI);
			connection.setRequestMethod(HttpConnection.GET);
			InputStream httpStream = connection.openInputStream();
			
			//This actually signals the connection to close as soon as the input stream
			//does, which we need, since after we pass it out, we have no way to manage
			//the connection.
			connection.close();
			
			return httpStream;
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
