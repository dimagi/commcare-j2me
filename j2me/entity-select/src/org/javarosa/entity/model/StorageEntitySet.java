/**
 *
 */
package org.javarosa.entity.model;

import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.Iterator;

/**
 * @author ctsims
 *
 */
public class StorageEntitySet<E extends Persistable> implements EntitySet<E> {

    IStorageUtility<E> storage;

    public StorageEntitySet(IStorageUtility<E> storage) {
        this.storage = storage;
    }

    public int getCount() {
        return storage.getNumRecords();
    }

    public E get(int index) {
        return storage.read(index);
    }

    public Iterator<E> iterate() {
        return storage.iterate();
    }

    public int getId(E e) {
        return e.getID();
    }
}
