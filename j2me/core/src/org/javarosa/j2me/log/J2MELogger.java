/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 *
 */
package org.javarosa.j2me.log;

import org.javarosa.core.api.ILogger;
import org.javarosa.core.log.IFullLogSerializer;
import org.javarosa.core.log.LogEntry;
import org.javarosa.core.log.StreamLogSerializer;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.SortedIntSet;
import org.javarosa.j2me.storage.rms.RMSStorageUtility;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 * @author Clayton Sims
 * @date Apr 10, 2009
 *
 */
public class J2MELogger implements ILogger {

    RMSStorageUtility logStorage;
    Object lock;
    boolean storageBroken = false;

    public J2MELogger() {
        String storageName = LogEntry.STORAGE_KEY;
        for(int i = 0; i < 5 ; ++i) {
            try {
                logStorage = new RMSStorageUtility(storageName, LogEntry.class);
                if(!LogEntry.STORAGE_KEY.equals(storageName)) {
                    this.log("logger", "Old log storage broken. New storage RMS: " + storageName, new Date());
                }
                lock = logStorage.getAccessLock();
                return;
            } catch(IllegalStateException ise) {
                ise.printStackTrace();
                //The logger not working should never break anything. This error
                //signifies that the storage is broken in a pretty irreperable way, so
                //we'll just start a new storage.
                storageName += "F";
            } catch(Exception e) {
                e.printStackTrace();
                //Even worse, we don't even know what's going on.
                storageName += "E";
            }
        }
        //If we made it here, the storage is seriously messed up and we'll just skip
        //logging entirely
        storageBroken=true;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IIncidentLogger#clearLogs()
     */
    public void clearLogs() {
        if(storageBroken) { return; };
        synchronized(lock) {
            if(!checkStorage()) { return; }

            int size = logStorage.getNumRecords();
            logStorage.removeAll();
            log("logs", "purged " + size, new Date());
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IIncidentLogger#clearLogs()
     */
    protected void clearLogs(final SortedIntSet IDs) {
        if(storageBroken) { return; };
        synchronized(lock) {
            if(!checkStorage()) { return; }

            logStorage.removeAll(new EntityFilter<LogEntry> () {
                public int preFilter (int id, Hashtable<String, Object> metaData) {
                    return IDs.contains(id) ? PREFILTER_INCLUDE : PREFILTER_EXCLUDE;
                }

                public boolean matches(LogEntry e) {
                    throw new RuntimeException("can't happen");
                }
            });

            log("logs", "purged " + IDs.size() + " " + logStorage.getNumRecords(), new Date());
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IIncidentLogger#logIncident(java.lang.String, java.lang.String, java.util.Date)
     */
    public void log(String type, String message, Date logDate) {
        if(storageBroken) { return; };
        synchronized(lock) {
            LogEntry log = new LogEntry(type, message, logDate);
            try {
                logStorage.add(log);
            } catch (StorageFullException e) {
                throw new RuntimeException("uh-oh, storage full [incidentlog]"); //TODO: handle this
            }
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.api.IIncidentLogger#serializeLogs()
     */
    public <T> T serializeLogs(IFullLogSerializer<T> serializer) {
        if(storageBroken) { return null; };
        synchronized(lock) {
            if(!checkStorage()) { return null; }

            Vector logs = new Vector();
            for(IStorageIterator li = logStorage.iterate(); li.hasMore() ; ) {
                logs.addElement((LogEntry)li.nextRecord());
            }

            LogEntry[] collection = new LogEntry[logs.size()];
            logs.copyInto(collection);
            return serializer.serializeLogs(collection);
        }
    }

    /**
     * called when an attempt to write to the log fails
     */
    public void panic () {
        final String LOG_PANIC = "LOG_PANIC";

        try {
            RecordStore store = RecordStore.openRecordStore(LOG_PANIC, true);

            int time = (int)(System.currentTimeMillis() / 1000);
            byte[] record = new byte[] {
                (byte)((time / 16777216) % 256),
                (byte)((time / 65536) % 256),
                (byte)((time / 256) % 256),
                (byte)(time % 256)
            };
            store.addRecord(record, 0, record.length);

            store.closeRecordStore();
        } catch (RecordStoreException rse) {
            throw new WrappedException(rse);
        }
    }

    public void serializeLogs(StreamLogSerializer serializer) throws IOException {
        serializeLogs(serializer, 1 << 20);
    }

    public void serializeLogs(StreamLogSerializer serializer, int limit) throws IOException {
        if(storageBroken) { return; };

        //Create a copy read-only handle
        RMSStorageUtility logStorageReadOnly;
        Vector<Integer> logIds = new Vector<Integer>();

        //This should capture its own internal state when it starts to iterate.
        synchronized(lock) {
            logStorageReadOnly = new RMSStorageUtility(logStorage.getName(), LogEntry.class);
            logStorageReadOnly.setReadOnly();
            int count = 0;
            IStorageIterator li = logStorageReadOnly.iterate();
            while(li.hasMore() && count < limit) {
                int id = li.nextID();
                logIds.addElement(DataUtil.integer(id));
                count++;
            }
        }

        System.out.println("Captured: " + logIds.size() + " records for serialization");

        //Ok, so now we

        for(Integer logId :logIds) {
            LogEntry log = (LogEntry)logStorageReadOnly.read(logId.intValue());
            //In theeeeooorry, the logs could have been modified. It's really not likely.
            if(log != null) {
                serializer.serializeLog(logId.intValue(), log);
            }
        }

        serializer.setPurger(new StreamLogSerializer.Purger () {
            public void purge(SortedIntSet IDs) {
                clearLogs(IDs);
            }
        });
    }

    public int logSize() {
        if(storageBroken) { return -1; };
        synchronized(lock) {
            if(!checkStorage()) { return -1; }
            return logStorage.getNumRecords();
        }
    }


    /**
     * Check storage attempts to determine whether the storage for the logger
     * is in a safe state and can be utilized without errors occurring. If
     * the Log storage is not in a safe state, the logger shouldn't attempt to
     * perform actions on it that might crash the app.
     *
     * @return True if the log store is safe to manipulate. False otherwise.
     */
    private boolean checkStorage() {
        try{logStorage.checkNotCorrupt(); return true;}
        catch(Exception e) {
            System.out.println("Log Storage Corrupt. Attempting to repair");
            //storage isn't in good shape. Try to repair it.
            try{
                logStorage.repair();
                logStorage.checkNotCorrupt();
                this.log("logger", "Corrupted Log Storage Repaired.", new Date());
                return true;
            } catch(Exception ex) {
                System.out.println("Log Storage Corrupted and Cannot be Repaired");
                //Still isn't working. Bad scene, but nothing
                //to do about it.
                return false;
                //We should either throw a runtime exception here, or we should
                //keep trying. Possibly should just dump the old RecordStore
                //completely and just start over.
            }
        }
    }

    public void halt() {
        if(!storageBroken){
            try{
                logStorage.close();
            }catch(Exception e ) {
                System.out.println("Caught error while trying to close log storage");
            }
        }
    }
}