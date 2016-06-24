/**
 *
 */
package org.javarosa.entity.model;

import org.javarosa.core.util.Iterator;

/**
 * @author ctsims
 *
 */
public interface EntitySet<E> {

    public int getCount();

    public E get(int index);

    public Iterator<E> iterate();

    public int getId(E e);
}
