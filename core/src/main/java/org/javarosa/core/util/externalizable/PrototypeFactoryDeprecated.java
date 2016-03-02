package org.javarosa.core.util.externalizable;

import org.javarosa.core.util.Map;

/**
 * The PrototypeFactory is a factory class for instantiating classes
 * based on their class name. This class fills a hole created by J2ME's
 * lack of reflection.
 *
 * The most common use of PrototypeFactories in JavaRoa is to instantiate
 * objects in order to deserialize them from RMS.
 *
 * Note that due to the nature of instantiating classes dynamically,
 * prototypes registered with this class must maintain a constructor with
 * no arguments.
 *
 * @author Clayton Sims
 */
public class PrototypeFactoryDeprecated {
    public final Map prototypes = new Map();

    /**
     * Adds a new class to be able to retrieve instances of
     *
     * @param name      The name of the prototype. Generally prototype.getClass().getName()
     * @param prototype The class object to be used for instantiation. Should be a class
     *                  with a constructor that takes 0 arguments.
     */
    public void addNewPrototype(String name, Class prototype) {
        prototypes.put(name, prototype);
    }

    /**
     * @param prototypeName The name of the prototype to be instantiated
     * @return a new object of the type linked to the name given in this factory. Null
     * if the name is not associated with any class in this factory.
     */
    public Object getNewInstance(String prototypeName) {
        if (prototypes.get(prototypeName) == null) {
            return null;
        }
        try {
            return ((Class)prototypes.get(prototypeName)).newInstance();
        } catch (InstantiationException e) {
            throw new CannotCreateObjectException(prototypeName);
        } catch (IllegalAccessException e) {
            throw new CannotCreateObjectException(prototypeName);
        }
    }
}
