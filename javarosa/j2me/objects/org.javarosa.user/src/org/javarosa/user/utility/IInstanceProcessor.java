package org.javarosa.user.utility;

import org.javarosa.core.model.instance.FormInstance;

/**
 * An interface for classes which are capable of parsing and performing actions
 * on Data Model objects.
 *
 * @author Clayton Sims
 * @date Jan 27, 2009
 */
public interface IInstanceProcessor {

    /**
     * Processes the provided data model.
     *
     * @param tree The data model that will be handled.
     */
    void processInstance(FormInstance tree);
}
