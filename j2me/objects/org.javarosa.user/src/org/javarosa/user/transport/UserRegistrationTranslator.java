/**
 *
 */
package org.javarosa.user.transport;

import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.UnrecognizedResponseException;
import org.javarosa.user.model.User;

import java.io.IOException;

/**
 * @author ctsims
 *
 */
public interface UserRegistrationTranslator<M extends TransportMessage> {

    public M getUserRegistrationMessage() throws IOException;

    public User readResponse(M message) throws UnrecognizedResponseException;

    /**
     * Whether this message can be processed properly asynchronously or must
     * be handeled immediately
     *
     * @return
     */
    public boolean isAsync();

    /**
     * Prepares a message to be cached an submitted later
     *
     * @param m
     */
    public void prepareMessageForCache(M m);

    public String getResponseMessageString();
}
