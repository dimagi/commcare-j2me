/**
 *
 */
package org.javarosa.core.services.storage.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.InvalidIndexException;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;

/**
 * @author ctsims
 */
public class DummyIndexedStorageUtility<T extends Persistable> implements IStorageUtilityIndexed<T> {

    private Hashtable<String, Hashtable<Object, Vector<Integer>>> meta;

    private Hashtable<Integer, T> data;

    int curCount;

    Class<T> prototype;

    public DummyIndexedStorageUtility(Class<T> prototype) {
        meta = new Hashtable<String, Hashtable<Object, Vector<Integer>>>();
        data = new Hashtable<Integer, T>();
        curCount = 0;
        this.prototype = prototype;
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getIDsForValue(java.lang.String, java.lang.Object)
     */
    public Vector getIDsForValue(String fieldName, Object value) {
        if (meta.get(fieldName) == null || meta.get(fieldName).get(value) == null) {
            return new Vector<Integer>();
        }
        return meta.get(fieldName).get(value);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtilityIndexed#getRecordForValue(java.lang.String, java.lang.Object)
     */
    public T getRecordForValue(String fieldName, Object value) throws NoSuchElementException, InvalidIndexException {

        if (meta.get(fieldName) == null) {
            throw new NoSuchElementException("No record matching meta index " + fieldName + " with value " + value);
        }

        Vector<Integer> matches = meta.get(fieldName).get(value);

        if (matches == null || matches.size() == 0) {
            throw new NoSuchElementException("No record matching meta index " + fieldName + " with value " + value);
        }
        if (matches.size() > 1) {
            throw new InvalidIndexException("Multiple records matching meta index " + fieldName + " with value " + value, fieldName);
        }

        return data.get(matches.elementAt(0));
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#add(org.javarosa.core.util.externalizable.Externalizable)
     */
    public int add(T e) throws StorageFullException {
        data.put(DataUtil.integer(curCount), e);

        //This is not a legit pair of operations;
        curCount++;

        syncMeta();

        return curCount - 1;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#close()
     */
    public void close() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#destroy()
     */
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#exists(int)
     */
    public boolean exists(int id) {
        if (data.containsKey(DataUtil.integer(id))) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getAccessLock()
     */
    public Object getAccessLock() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getNumRecords()
     */
    public int getNumRecords() {
        return data.size();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getRecordSize(int)
     */
    public int getRecordSize(int id) {
        //serialize and test blah blah.
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#getTotalSize()
     */
    public int getTotalSize() {
        //serialize and test blah blah.
        return 0;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#isEmpty()
     */
    public boolean isEmpty() {
        if (data.size() > 0) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#iterate()
     */
    public IStorageIterator<T> iterate() {
        //We should really find a way to invalidate old iterators first here
        return new DummyStorageIterator<T>(data);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#read(int)
     */
    public T read(int id) {
        //return data.get(DataUtil.integer(id));
        try {
            T t = prototype.newInstance();
            t.readExternal(new DataInputStream(new ByteArrayInputStream(readBytes(id))), PrototypeManager.getDefault());
            return t;
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (DeserializationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#readBytes(int)
     */
    public byte[] readBytes(int id) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            data.get(DataUtil.integer(id)).writeExternal(new DataOutputStream(stream));
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't serialize data to return to readBytes");
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#remove(int)
     */
    public void remove(int id) {
        data.remove(DataUtil.integer(id));

        syncMeta();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#remove(org.javarosa.core.services.storage.Persistable)
     */
    public void remove(Persistable p) {
        this.read(p.getID());
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#removeAll()
     */
    public void removeAll() {
        data.clear();

        meta.clear();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#removeAll(org.javarosa.core.services.storage.EntityFilter)
     */
    public Vector<Integer> removeAll(EntityFilter ef) {
        Vector<Integer> removed = new Vector<Integer>();
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            Integer i = (Integer)en.nextElement();
            switch (ef.preFilter(i.intValue(), null)) {
                case EntityFilter.PREFILTER_INCLUDE:
                    removed.addElement(i);
                    break;
                case EntityFilter.PREFILTER_EXCLUDE:
                    continue;
            }
            if (ef.matches(data.get(i))) {
                removed.addElement(i);
            }
        }
        for (Integer i : removed) {
            data.remove(i);
        }

        syncMeta();

        return removed;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#repack()
     */
    public void repack() {
        //Unecessary!
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#repair()
     */
    public void repair() {
        //Unecessary!
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#update(int, org.javarosa.core.util.externalizable.Externalizable)
     */
    public void update(int id, T e) throws StorageFullException {
        data.put(DataUtil.integer(id), e);
        syncMeta();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.storage.IStorageUtility#write(org.javarosa.core.services.storage.Persistable)
     */
    public void write(Persistable p) throws StorageFullException {
        if (p.getID() != -1) {
            this.data.put(DataUtil.integer(p.getID()), (T)p);
            syncMeta();
        } else {
            p.setID(curCount);
            this.add((T)p);
        }
    }

    private void syncMeta() {
        meta.clear();
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            Integer i = (Integer)en.nextElement();
            Externalizable e = (Externalizable)data.get(i);

            if (e instanceof IMetaData) {

                IMetaData m = (IMetaData)e;
                for (String key : m.getMetaDataFields()) {
                    if (!meta.containsKey(key)) {
                        meta.put(key, new Hashtable<Object, Vector<Integer>>());
                    }
                }
                for (String key : dynamicIndices) {
                    if (!meta.containsKey(key)) {
                        meta.put(key, new Hashtable<Object, Vector<Integer>>());
                    }
                }
                for (Enumeration keys = meta.keys(); keys.hasMoreElements(); ) {
                    String key = (String)keys.nextElement();

                    Object value = m.getMetaData(key);

                    Hashtable<Object, Vector<Integer>> records = meta.get(key);

                    if (!records.containsKey(value)) {
                        records.put(value, new Vector<Integer>());
                    }
                    Vector<Integer> indices = records.get(value);
                    if (!indices.contains(i)) {
                        records.get(value).addElement(i);
                    }
                }
            }
        }
    }


    public void setReadOnly() {
        //TODO: This should have a clear contract.
    }


    Vector<String> dynamicIndices = new Vector<String>();

    public void registerIndex(String filterIndex) {
        dynamicIndices.addElement(filterIndex);
    }
}
