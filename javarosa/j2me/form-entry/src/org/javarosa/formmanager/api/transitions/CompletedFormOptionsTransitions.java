/**
 *
 */
package org.javarosa.formmanager.api.transitions;

/**
 * @author ctsims
 *
 */
public interface CompletedFormOptionsTransitions {
    public void sendData(String messageId);
    public void skipSend(String messageId);
}
