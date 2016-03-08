/**
 *
 */
package org.javarosa.service.transport.securehttp;

import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.j2me.reference.HttpReference.SecurityFailureListener;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportMessageStatus;
import org.javarosa.services.transport.impl.simplehttp.HttpRequestProperties;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.util.Date;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.midlet.MIDlet;
import javax.microedition.pki.CertificateException;

import de.enough.polish.util.StreamUtil;

/**
 * An AuthenticatedHttpTransportMessage is a transport message which is used to
 * either perform a GET or POST request to an HTTP server, which includes the
 * capacity for authenticating with that server if a WWW-Authenticate challenge
 * is issued.
 *
 * AuthenticatedHttpTransportMessage are currently unable to cache themselves
 * natively with the transport service.
 *
 * @author ctsims
 *
 */
public class AuthenticatedHttpTransportMessage extends SimpleHttpTransportMessage {
    String URL;

    int responseCode;
    InputStream response;

    IDataPayload payload;

    HttpRequestProperties responseProperties;
    private SecurityFailureListener listener;

    public static MIDlet CalloutResolver;

    private AuthenticatedHttpTransportMessage(String URL, HttpAuthenticator authenticator, SecurityFailureListener listener) {
        this.setCreated(new Date());
        this.setStatus(TransportMessageStatus.QUEUED);
        this.URL = URL;
        this.authenticator = authenticator;
        this.listener = listener;
    }


    public static AuthenticatedHttpTransportMessage AuthenticatedHttpRequest(String URL, HttpAuthenticator authenticator) {
        return AuthenticatedHttpRequest(URL, authenticator, null);
    }
    /**
     * Creates a message which will perform an HTTP GET Request to the server referenced at
     * the given URL.
     *
     * @param URL The requested server URL
     * @param authenticator An authenticator which is capable of providing credentials upon
     * request.
     * @return A new authenticated HTTP message ready for sending.
     */
    public static AuthenticatedHttpTransportMessage AuthenticatedHttpRequest(String URL, HttpAuthenticator authenticator, SecurityFailureListener listener) {
        return new AuthenticatedHttpTransportMessage(URL, authenticator, listener);
    }

    /**
     * Creates a message which will perform an HTTP POST Request to the server referenced at
     * the given URL.
     *
     * @param URL The requested server URL
     * @param authenticator An authenticator which is capable of providing credentials upon
     * request.
     * @param payload A data payload which will be posted to the remote server.
     * @return A new authenticated HTTP message ready for sending.
     */
    public static AuthenticatedHttpTransportMessage AuthenticatedHttpPOST(String URL, IDataPayload payload, HttpAuthenticator authenticator) {
        AuthenticatedHttpTransportMessage message = new AuthenticatedHttpTransportMessage(URL, authenticator, null);
        message.payload = payload;
        return message;
    }



    /**
     * @return The HTTP request method (Either GET or POST) for
     * this message.
     */
    public String getConnectionMethod() {
        return (payload == null ? HttpConnection.GET : HttpConnection.POST);
    }

    /**
     * @return The HTTP URL of the server for this message
     */
    public String getUrl() {
        return URL;
    }

    /* (non-Javadoc)
     * @see org.javarosa.services.transport.TransportMessage#isCacheable()
     */
    public boolean isCacheable() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.services.transport.TransportMessage#setCacheIdentifier(java.lang.String)
     */
    public void setCacheIdentifier(String id) {
        Logger.log("transport", "warn: setting cache ID on non-cacheable message");
        //suppress; these messages are not cacheable
    }

    public void setSendingThreadDeadline(long queuingDeadline) {
        Logger.log("transport", "warn: setting cache expiry on non-cacheable message");
        //suppress; these messages are not cacheable
    }

    /**
     * @param code The response code of the most recently attempted
     * request.
     */
    public void setResponseCode(int code) {
        this.responseCode = code;
    }

    /**
     * @return code The response code of the most recently attempted
     * request.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Sets the stream of the response from a delivery attempt
     * @param response The stream provided from the http connection
     * from a deliver attempt
     */
    protected void setResponseStream(InputStream response) {
        this.response = response;
    }

    /**
     * @return The stream provided from the http connection
     * from the previous deliver attempt
     */
    public InputStream getResponse() {
        return new BufferedInputStream(response);
    }

    public HttpRequestProperties getReponseProperties() {
        return responseProperties;
    }

    /**
     * @return The properties for this http request (other than
     * authorization headers).
     */
    public HttpRequestProperties getRequestProperties() {
        //TODO: Possibly actually count content length here
        return new HttpRequestProperties(this.getConnectionMethod(), -1, "1.0", null);
    }

    public InputStream getContentStream() throws IOException {
        if (payload == null) {
            return new ByteArrayInputStream("".getBytes());
        } else {
            return payload.getPayloadStream();
        }
    }



    /*
     * (non-Javadoc)
     *
     * @see org.javarosa.services.transport.Transporter#send()
     */
    public void send() {
        HttpConnection connection = null;
        try {

            //Open the connection assuming either cached credentials
            //or no Authentication
            connection = getConnection();
            int response = connection.getResponseCode();

            if (response == HttpConnection.HTTP_UNAUTHORIZED) {

                String challenge = getChallenge(connection);
                //If authentication is needed, issue the challenge
                if (this.issueChallenge(connection, challenge)) {

                    // The challenge was handled, and authentication
                    // is now provided, try the request again after
                    //closing the current connection.
                    connection.close();
                    connection = getConnection();

                    //Handle the new response as-is, if authentication failed,
                    //the sending process can issue a new request.
                    handleResponse(connection);
                } else {
                    // The challenge couldn't be addressed. Set the message to
                    // failure.
                    handleResponse(connection);
                }
            } else {
                //The message did not fail due to authorization problems, so
                //handle the response.
                handleResponse(connection);
            }
        } catch (CertificateException e) {
            //Certificates are now (March 2016) basically useless, since no one can issue a safe SHA1
            //cert anymore. Direct the user to a browser to accept the authentication.
            noteFailure(e);
            try {
                if(CalloutResolver != null) {
                    CalloutResolver.platformRequest(URL);
                } 
            } catch(Exception ex) {
                //If the platform can't help, there's nothing to do but fail
            } 
        } catch (IOException e) {
            noteFailure(e);
        } finally {
            //CTS - New: Don't close the response if we got a stream here. the connection will be closed
            //when the stream is. (some platforms dont' properly delay closing the connection until after
            //the stream is).
            if(connection != null && response == null){
                try {
                    connection.close();
                } catch (IOException e) {
                    //shouldn't matter at this point
                }
            }
        }
    }

    private void noteFailure(Exception e) {
        e.printStackTrace();
        this.setStatus(TransportMessageStatus.FAILED);
        this.setFailureReason(WrappedException.printException(e));
    }

    public static String getChallenge(HttpConnection connection ) throws IOException {
        final String AUTH_HEADER_HACK = "X-S60-Auth";

        //technically the standard

        String challenge = null;
        if (challenge == null) {
            challenge = connection.getHeaderField(AUTH_HEADER_HACK);
        }
        if (challenge == null) {
            challenge = connection.getHeaderField(AUTH_HEADER_HACK.toLowerCase());
        }
        if (challenge == null) {
            challenge = connection.getHeaderField("WWW-Authenticate");
        }
        if(challenge == null) {
            challenge = connection.getHeaderField("www-authenticate");
        }

        return challenge;
    }

    /**
     * @return the current best-guess authorization header for this message,
     * either produced as a response to a WWW-Authenticate challenge, or
     * provided by the authentication cache based on previous requests
     * (if enabled and relevant in the message's authenticator).
     */
    public String getAuthString() {
        if(authentication == null) {
            //generally pre-challenge
            return authenticator.checkCache(this);
        }
        return authentication;
    }

    private void handleResponse(final HttpConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        responseProperties = HttpRequestProperties.HttpResponsePropertyFactory(connection);
        long responseLength = connection.getLength();

        if (responseLength > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD) {
            Logger.log("recv", "size " + responseLength);
        }

        if(responseCode >= 200 && responseCode < 300) {
            //It's all good, message was a success.
            this.setResponseCode(responseCode);
            this.setStatus(TransportMessageStatus.SENT);

            //Wire up the input stream from the connection to the message.
            this.setResponseStream(new InputStreamC(connection.openInputStream(), responseLength, this.getTag()) {
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
            });
        } else {
            this.setStatus(TransportMessageStatus.FAILED);
            this.setResponseCode(responseCode);

            //We'll assume that any failures come with a message which is sufficiently
            //small that they can be fit into memory.
            byte[] response = StreamUtil.readFully(connection.openInputStream());
            connection.close();
            String reason = responseCode + ": " + new String(response);
            reason = PropertyUtils.trim(reason, 400);
            this.setFailureReason(reason);
        }
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private HttpConnection getConnection() throws IOException {
        try {
            HttpConnection conn = (HttpConnection) Connector.open(this.getUrl());
            if (conn == null)
                throw new RuntimeException("Null conn in getConnection()");

            HttpRequestProperties requestProps = this.getRequestProperties();
            if (requestProps == null) {
                throw new RuntimeException("Null message.getRequestProperties() in getConnection()");
            }
            requestProps.configureConnection(conn);

            //Retrieve either the response auth header, or the cached guess
            String authorization = this.getAuthString();
            if(authorization != null) {
                conn.setRequestProperty("Authorization", authorization);
            }

            return conn;
        } catch(SecurityException se) {
            if(this.listener != null) {
                listener.onSecurityException(se);
            }
            throw new IOException("Couldn't retrieve data from " + this.getUrl() + " due to lack of permissions.");
        }
    }

    protected class InputStreamC extends InputStream {
        private InputStream is;
        private long total;
        private long read;
        private String tag;

        boolean logged = false;

        public InputStreamC (InputStream is, long totalLength, String tag) {
            this.is = is;
            this.total = totalLength;
            this.read = 0;
            this.tag = tag;
        }

        public int read() throws IOException {
            try {
                int c = is.read();
                read += 1;
                return c;
            } catch (IOException ioe) {
                log(true);
                throw ioe;
            }
        }

        public int read(byte[] b) throws IOException {
            try {
                int k = is.read(b);
                read += Math.max(k, 0);
                return k;
            } catch (IOException ioe) {
                log(true);
                throw ioe;
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            try {
                int k = is.read(b, off, len);
                read += Math.max(k, 0);
                return k;
            } catch (IOException ioe) {
                log(true);
                throw ioe;
            }
        }

        public long skip(long n) throws IOException {
            try {
                long k = is.skip(n);
                read += k;
                return k;
            } catch (IOException ioe) {
                log(true);
                throw ioe;
            }
        }

        public void close() throws IOException {
            log(false);
            is.close();
        }

        private void log (boolean ex) {
            if (logged)
                return;
            logged = true;

            try {
                boolean hasLength = (total >= 0);    //whether we have total length
                boolean diff;                        //whether bytes read differed from total length
                boolean logIt;                        //whether to log stats

                if (hasLength) {
                    diff = (total != read);
                    logIt = diff;
                } else {
                    logIt = (read > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD || ex);
                    diff = false;
                }

                if (logIt) {
                    Logger.log("recv", "<" + tag + "> " + read + (diff ? " of " + total : ""));
                }
            } catch (Exception e) {
                //extrasafe
                Logger.exception("InputStreamC.log", e);
            }
        }

        public int available() throws IOException {
            return is.available();
        }

        public void mark(int rl) {
            is.mark(rl);
        }

        public void reset() throws IOException {
            is.reset();
        }

        public boolean markSupported() {
            return is.markSupported();
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        //doesn't cache;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        //doesn't cache;
    }
}
