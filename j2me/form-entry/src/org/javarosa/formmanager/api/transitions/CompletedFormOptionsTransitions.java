/**
 *
 */
package org.javarosa.formmanager.api.transitions;

import org.javarosa.services.transport.TransportMessage;

/**
 * @author ctsims
 *
 */
public interface CompletedFormOptionsTransitions {
    public void sendData(String messageId);
    public void skipSend(String messageId);
}
