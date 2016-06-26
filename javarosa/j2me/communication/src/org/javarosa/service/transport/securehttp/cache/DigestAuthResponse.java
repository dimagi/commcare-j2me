/**
 *
 */
package org.javarosa.service.transport.securehttp.cache;

import org.javarosa.core.util.MD5;
import org.javarosa.core.util.MD5InputStream;
import org.javarosa.core.util.MathUtils;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.service.transport.securehttp.AuthUtils;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Random;

/**
 * A digest authorization response object which accepts the
 * various parameters associated with digest authentication
 * challenges, and creates the appropriate authorization
 * header for them.
 *
 * It is also a cachable response header, capable of creating
 * a new response later for additional messages with the same
 * credentails.
 *
 * @author ctsims
 *
 */
public class DigestAuthResponse implements AuthCacheRecord {

    public static final String QOP_UNSPECIFIED = "unspecified";
    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_INT = "auth-int";

    private String HA1;

    private String URL;
    private Hashtable<String, String> parameters;

    public DigestAuthResponse(String URL, String HA1) {
        this(URL, HA1, new OrderedHashtable());
    }

    public DigestAuthResponse(String URL, String HA1, Hashtable<String, String> parameters) {
        this.URL = URL;
        this.parameters = parameters;
        this.HA1 = HA1;
    }

    public String getUrl() {
        return URL;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.cache.AuthCacheRecord#invalidates(org.javarosa.service.transport.securehttp.cache.AuthCacheRecord)
     */
    public boolean invalidates(AuthCacheRecord record) {
        if(record.getUrl().equals(this.URL)) { return true; };

        //TODO: If the incoming message is a digest, we should
        //possibly invalidate any/all messages that it shares
        //a domain with.

        return false;

    }

    public String get(String key) {
        return parameters.get(key);
    }

    public String put(String key, String value) {
        return parameters.put(key,value);
    }

    /**
     * Builds an auth response for the provided message based on the
     * parameters currently available for authentication
     * @param message
     * @return An Authenticate HTTP header for the message if one could be
     * created, null otherwise.
     */
    public String buildResponse(SimpleHttpTransportMessage message) {
        String qop = parameters.get("qop");

        String nonce= AuthUtils.unquote(parameters.get("nonce"));

        String uri = AuthUtils.unquote(parameters.get("uri"));

        String method = message.getConnectionMethod();

        String HA2 = null;
        if(qop != DigestAuthResponse.QOP_AUTH_INT) {
            HA2 = AuthUtils.MD5(method + ":" + uri);
        } else {
            try {
                InputStream stream = message.getContentStream();
                String entityBody;
                if(stream == null){
                    entityBody = MD5.toHex("".getBytes());
                } else {
                        entityBody = new MD5InputStream(stream).getHashCode();
                }
                HA2 = AuthUtils.MD5(method + ":" + uri + ":" + entityBody);
            } catch (IOException e) {
                //Problem calculating MD5 from content stream
                e.printStackTrace();
                return null;
            }
        }

        if(qop == DigestAuthResponse.QOP_UNSPECIFIED) {
            //RFC 2069 Auth
            parameters.put("response", AuthUtils.quote(AuthUtils.MD5(HA1 + ":" + nonce + ":" + HA2)));
        } else {
            String nc = getNonceCount();

            //Generate client nonce
            String cnonce = getClientNonce();

            //Calculate response
            parameters.put("response", AuthUtils.quote(AuthUtils.MD5(HA1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + HA2)));
        }
        return "Digest " + AuthUtils.encodeQuotedParameters(parameters);
    }

    private String getClientNonce() {
        if(!parameters.contains("cnonce")) {
            Random r = MathUtils.getRand();
            byte[] b = new byte[8];
            for(int i = 0; i < b.length ; ++i) {
                b[i] = (byte)r.nextInt(256);
            }

            String cnonce = MD5.toHex(b);
            parameters.put("cnonce",AuthUtils.quote(cnonce));
            return cnonce;
        } else {
            return AuthUtils.unquote(parameters.get("cnonce"));
        }

    }

    private String getNonceCount() {
        //The nonce count represents the number of
        //times that the nonce has been used for authentication
        //and must be incremented for each request. Otherwise
        //the nonce data becomes unavailable.
        if(!parameters.containsKey("nc")) {
            String nc = "00000001";
            parameters.put("nc",nc);

            return nc;
        } else {
            String nc = parameters.get("nc");
            int count = Integer.parseInt(nc);
            count+=1;

            nc = String.valueOf(count);
            //Buffer to the left
            while(nc.length() < 8) {
                nc = "0" + nc;
            }
            parameters.put("nc",nc);
            return nc;
        }

    }

    public boolean validFor(String URI) {
        //TODO: Try to guess better probably based on
        //domain prefix
        if(this.URL.equals(URI)) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.service.transport.securehttp.cache.AuthCacheRecord#retrieve(org.javarosa.service.transport.securehttp.AuthenticatedHttpTransportMessage)
     */
    public String retrieve(SimpleHttpTransportMessage message) {
        //TODO: Extract the URI here and replace the one in the current parameter set
        //so that we can use the same nonce on multiple URI's.
        return buildResponse(message);
    }
}
