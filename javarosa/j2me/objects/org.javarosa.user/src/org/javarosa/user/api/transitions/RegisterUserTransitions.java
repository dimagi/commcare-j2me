/**
 *
 */
package org.javarosa.user.api.transitions;

import org.javarosa.core.model.User;

/**
 * @author ctsims
 *
 */
public interface RegisterUserTransitions {
    void succesfullyRegistered(User user);
    void cancel();
}
