package org.javarosa.services.transport.impl.simplehttp;

import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.service.transport.securehttp.AuthUtils;
import org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage;
import org.javarosa.service.transport.securehttp.HttpAuthenticator;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.BasicTransportMessage;
import org.javarosa.services.transport.impl.TransportMessageStatus;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.pki.Certificate;
import javax.microedition.pki.CertificateException;

/**
 * A message which implements the simplest Http transfer - plain text via POST
 * request
 *
 */
public class SimpleHttpTransportMessage extends BasicTransportMessage {


    private IDataPayload content;
    /**
     * An http url, to which the message will be POSTed
     */
    private String url;

    private String orApiVersion = "1.0";

    /**
     * Http response code
     */
    private int responseCode;

    private byte[] responseBody;

    private boolean cacheable = true;

    private HttpRequestProperties responseProperties;

    private Hashtable<String,String> customHeaders;

    protected HttpAuthenticator authenticator;

    protected String authentication;

    /**
     * Http connection method.
     */

    private String httpConnectionMethod = HttpConnection.POST;


    public SimpleHttpTransportMessage() {
        //ONLY FOR SERIALIZATION
        customHeaders = new Hashtable<String,String>();
    }

    public SimpleHttpTransportMessage(String url) {
        this();
        this.url = url;
        this.setHttpConnectionMethod(HttpConnection.GET);
    }

    /**
     * @param str
     * @param destinationURL
     */
    public SimpleHttpTransportMessage(String str, String url) {
        this(str.getBytes(), url);
    }

    /* (non-Javadoc)
     * @see org.javarosa.services.transport.impl.BasicTransportMessage#setContentType(java.lang.String)
     */
    public void setContentType(String contentType) {
        super.setContentType(contentType);
        this.setHeader("Content-Type", contentType);
    }

    /**
     * @param str
     * @param destinationURL
     */
    public SimpleHttpTransportMessage(byte[] content, String url) {
        this();
        this.url = url;
        this.content = new ByteArrayPayload(content);
    }

    /**
     * @param is
     * @param destinationURL
     * @throws IOException
     */
    public SimpleHttpTransportMessage(IDataPayload payload, String url) throws IOException {
        this();
        this.content = payload;
        this.url = url;
    }

    public HttpRequestProperties getRequestProperties() {
        return new HttpRequestProperties(this.getConnectionMethod(), this.getContentLength(), orApiVersion, customHeaders);
    }

    public HttpRequestProperties getResponseProperties() {
        return responseProperties;
    }

    public boolean isCacheable() {
        return cacheable;
    }


    protected IDataPayload getContent() {
        return content;
    }

    public void setOpenRosaApiVersion(String orApiVersion) {
        this.orApiVersion = orApiVersion;
    }

    public InputStream getContentStream() throws IOException {
        return getContent().getPayloadStream();
    }

    public long getContentLength() {
        if(this.getContent() != null) {
            return getContent().getLength();
        } else {
            return -1;
        }
    }

    /**
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * @param responseCode
     */
    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * @return
     */
    public byte[] getResponseBody() {
        return responseBody;
    }

    /**
     * @param responseBody
     */
    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
    }

    public String getConnectionMethod() {
        return this.httpConnectionMethod;
    }

    public void setHttpConnectionMethod(String httpConnectionMethod)
    {
        this.httpConnectionMethod = httpConnectionMethod;

        //There's no meaning to caching a GET
        if(this.httpConnectionMethod != HttpConnection.POST) {
            setCacheable(false);
        }
    }

    public void send() {

        if(authenticator == null) {
            this.authenticator = AuthUtils.getStaticAuthenticator();
        }
        HttpConnection conn = null;


        boolean ex = false;
        long[] responseRead = new long[] {0};
        long responseLength = -1;

        try {
            System.out.println("Ready to send: " + this);
            conn = getConnection(this.getConnectionMethod());
            System.out.println("Connection: " + conn);

            writeBody(conn);

            // Get the response
            int responseCode = conn.getResponseCode();
            System.out.println("response code: " + responseCode);

            if (responseCode == HttpConnection.HTTP_UNAUTHORIZED) {

                String challenge = AuthenticatedHttpTransportMessage.getChallenge(conn);
                //If authentication is needed, issue the challenge
                if (this.issueChallenge(conn, challenge)) {
                    // The challenge was handled, and authentication
                    // is now provided, try the request again after
                    //closing the current connection.
                    conn.close();
                    conn = getConnection(this.getConnectionMethod());
                    writeBody(conn);

                    responseCode = conn.getResponseCode();
                    System.out.println("Post-Auth response code: " + responseCode);

                    //Handle the new response as-is, if authentication failed,
                    //the sending process can issue a new request.
                    processResponse(conn, responseRead);
                } else {
                    // The challenge couldn't be addressed. Set the message to
                    // failure.
                    processResponse(conn, responseRead);
                }
            } else {
                //The message did not fail due to authorization problems, so
                //handle the response.
                processResponse(conn, responseRead);
            }
        } catch(CertificateException certe) {
            String reason = "";
            switch (certe.getReason())
            {
            case CertificateException.BAD_EXTENSIONS:
            reason = "Certificate contains unrecognized extensions";
            break;
            case CertificateException.BROKEN_CHAIN:
            reason = "Certificate was not issuied by next certificate in the chain";
            break;
            case CertificateException.CERTIFICATE_CHAIN_TOO_LONG:
            reason = "Too long certificate chain";
            break;
            case CertificateException.EXPIRED:
            reason = "Certificate has already expired";
            break;
            case CertificateException.INAPPROPRIATE_KEY_USAGE:
            reason = "Certificate usage not acceptable";
            break;
            case CertificateException.MISSING_SIGNATURE:
            reason = "Certificate does not contain a signature";
            break;
            case CertificateException.NOT_YET_VALID:
            reason = "Attempte to use a Certificate not valid yet";
            break;
            case CertificateException.ROOT_CA_EXPIRED:
            reason = "Certificate's root CA has expired";
            break;
            case CertificateException.SITENAME_MISMATCH:
            reason = "Certificate's referred sitename is incorrect";
            break;
            case CertificateException.UNAUTHORIZED_INTERMEDIATE_CA:
            reason = "One of the certificates in the chain is not authorized";
            break;
            case CertificateException.UNRECOGNIZED_ISSUER:
            reason = "Certificate's issuer is unrecognized";
            break;
            case CertificateException.UNSUPPORTED_PUBLIC_KEY_TYPE:
            reason = "The type of the public key is not supported";
            break;
            case CertificateException.UNSUPPORTED_SIGALG:
            reason = "Certificate's signature algorithm is not supported";
            break;
            case CertificateException.VERIFICATION_FAILED:
            reason = "Certificate could not be validated";
            break;
            default:
            reason = "Unknown reason";
            break;
            }

            String certinfo = "";
            Certificate cert = certe.getCertificate();
            if (cert != null)
            {

                certinfo += "DN: " + cert.getSubject();
                certinfo += "Type: " + cert.getType();
                certinfo += "Version: " + cert.getVersion() + "\r";
                certinfo += "Serial Number: " + cert.getSerialNumber() + "\r";
                certinfo += "Signature Algorithm: " + cert.getSigAlgName() + "\r";
            }

            ex = true;
            Logger.exception("Certificate error : " + reason +" . Provided cert: " + certinfo, certe);

            this.setFailureReason(WrappedException.printException(certe));
            this.incrementFailureCount();
        } catch (Exception e) {
            ex = true;
            e.printStackTrace();
            System.out.println("Connection failed: " + e.getClass() + " : "
                    + e.getMessage());
            this.setFailureReason(WrappedException.printException(e));
            this.incrementFailureCount();
        } finally {
            logRecv(responseLength, responseRead[0], ex);
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }


    /**
     * Issues an authentication challenge from the provided HttpConnection
     *
     * @param connection The connection which issued the challenge
     * @param challenge The WWW-Authenticate challenge issued.
     * @return True if the challenge was addressed by the message's authenticator,
     * and the request should be retried, False if the challenge could not be
     * addressed.
     */
    public boolean issueChallenge(HttpConnection connection, String challenge) {
        authentication = this.authenticator.challenge(connection, challenge, this);
        if(authentication == null) {
            return false;
        } else {
            return true;
        }
    }

    private long processResponse(HttpConnection conn, long[] responseRead) throws IOException {
        responseProperties = HttpRequestProperties.HttpResponsePropertyFactory(conn);

        long responseLength = -1;

        DataInputStream is = null;

        try {
            responseLength = conn.getLength();

            is = (DataInputStream) conn.openDataInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamsUtil.writeFromInputToOutput(is, baos, responseRead);

            // set return information in the message
            this.setResponseBody(baos.toByteArray());
            this.setResponseCode(conn.getResponseCode());
            if (responseCode >= 200 && responseCode <= 299) {
                this.setStatus(TransportMessageStatus.SENT);
            } else {
                Logger.log("send", this.getTag() + " http resp code: " + responseCode);
            }
            return responseLength;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing
                }
            }

        }
    }

    private void writeBody(HttpConnection conn)  throws IOException {
        OutputStream os = null;
        try {
            os = conn.openOutputStream();
            writeBody(os);
        } finally {
            if(os != null) {
                os.close();
            }
        }
    }

    protected void writeBody(OutputStream os) throws IOException {
        IDataPayload payload = this.getContent();
        if (payload != null) {
            long length = payload.getLength();
            if (length > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD) {
                Logger.log("send", "size " + length);
            }

//            InputStream stream = payload.getPayloadStream();
//            try {
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                StreamsUtil.writeFromInputToOutput(stream, baos);
//                System.out.println("content: " + new String(baos.toByteArray()));
//            } finally {
//                try {
//                    stream.close();
//                } catch(IOException e) {
//
//                }
//            }

            long[] tally = {0};
            try {
                StreamsUtil.writeFromInputToOutput(payload.getPayloadStream(), os, tally);
            } finally {
                if (tally[0] != length) {
                    Logger.log("send", "only " + tally[0] + " of " + length);
                }
            }
        } else {
            System.out.println("no request body");
        }
    }

    public static void logRecv (long total, long read, boolean ex) {
        try {
            boolean hasLength = (total >= 0);    //whether we have total length
            boolean diff;                        //whether bytes read differed from total length
            boolean logIt;                        //whether to log stats

            if (hasLength) {
                diff = (total != read);
                logIt = (total > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD || diff);
            } else {
                logIt = (read > TransportService.PAYLOAD_SIZE_REPORTING_THRESHOLD || ex);
                diff = false;
            }

            if (logIt) {
                Logger.log("recv", read + (diff ? " of " + total : ""));
            }
        } catch (Exception e) {
            //safety first!
            Logger.exception("TransportMessage.logRecv", e);
        }
    }

    /**
     * @param url
     * @return
     * @throws IOException
     */
    private HttpConnection getConnection(String connectionMethod) throws IOException {
        String url = this.getUrl();

        //Need to add some URL args if we aren't authenticating
        if(this.authenticator == null) {
            String newArg = "authtype=noauth";

            //See if we have any existing args
            if(url.indexOf("?") != -1) {
                //add to the list
                url = url + "&" + newArg;
            } else {
                //Add a param list
                url = url + "?" + newArg;
            }
        }

        HttpConnection conn = (HttpConnection) Connector.open(url);
        if (conn == null) {
            throw new RuntimeException("Null conn in getConnection()");
        }

        HttpRequestProperties requestProps = this.getRequestProperties();
        if (requestProps == null) {
            throw new RuntimeException("Null message.getRequestProperties() in getConnection()");
        }

        //Retrieve either the response auth header, or the cached guess
        String authorization = this.getAuthString();
        if(authorization != null) {
            conn.setRequestProperty("Authorization", authorization);
        }


        requestProps.configureConnection(conn);

        return conn;

    }

    /**
     * @return the current best-guess authorization header for this message,
     * either produced as a response to a WWW-Authenticate challenge, or
     * provided by the authentication cache based on previous requests
     * (if enabled and relevant in the message's authenticator).
     */
    public String getAuthString() {
        if(authentication == null && authenticator != null) {
            //generally pre-challenge
            return authenticator.checkCache(this);
        }
        return authentication;
    }



    public void setHeader(String header, String value) {
        this.customHeaders.put(header, value);
    }


    public String toString() {
        String s = "#" + getCacheIdentifier() + " (http)";
        if (getResponseCode() > 0)
            s += " " + getResponseCode();
        return s;
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        super.readExternal(in, pf);
        url = ExtUtil.readString(in);
        responseCode = (int)ExtUtil.readNumeric(in);
        responseBody = ExtUtil.nullIfEmpty(ExtUtil.readBytes(in));
        content = (IDataPayload)ExtUtil.read(in, new ExtWrapTagged(), pf);
        customHeaders = (Hashtable<String, String>) ExtUtil.read(in, new ExtWrapMap(String.class, String.class));
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out,url);
        ExtUtil.writeNumeric(out,responseCode);
        ExtUtil.writeBytes(out, ExtUtil.emptyIfNull(responseBody));
        ExtUtil.write(out, new ExtWrapTagged(content));
        ExtUtil.write(out, new ExtWrapMap(customHeaders));
    }

}
