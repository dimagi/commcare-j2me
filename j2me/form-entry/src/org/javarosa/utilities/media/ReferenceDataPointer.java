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

package org.javarosa.utilities.media;

import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.j2me.file.J2meFileReference;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of the data pointer that represents an underlying file on the file system.
 *
 * @author Cory Zue
 *
 */
public class ReferenceDataPointer implements IDataPointer {

    private String referenceName;
    private Reference ref;

    /**
     * NOTE: Only for serialization use.
     */
    public ReferenceDataPointer() {
        //You shouldn't be calling this unless you are deserializing.
    }

    /**
     * Create a FileDataPointer from a file name
     * @param fileName
     * @throws InvalidReferenceException
     */
    public ReferenceDataPointer(String referenceName) throws InvalidReferenceException {
        this.referenceName = referenceName;
        ref = ReferenceManager._().DeriveReference(referenceName);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.data.IDataPointer#getData()
     */
    public byte[] getData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamsUtil.writeFromInputToOutput(getDataStream(), baos);
        return baos.toByteArray();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.data.IDataPointer#getDataStream()
     */
    public InputStream getDataStream() throws IOException  {
        return ref.getStream();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.data.IDataPointer#getDisplayText()
     */
    public String getDisplayText() {
        return ref.getLocalURI();
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.data.IDataPointer#deleteData()
     */
    public boolean deleteData() {
        try{
            ref.remove();
            return ref.doesBinaryExist();
        } catch(IOException ioe) {throw new RuntimeException(ioe.getMessage());}

    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        referenceName = ExtUtil.readString(in);
        try {
            ref = ReferenceManager._().DeriveReference(referenceName);
        } catch (InvalidReferenceException e) {
            throw new DeserializationException("Unsupported local reference " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, referenceName);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.data.IDataPointer#getLength()
     */
    public long getLength() {
        //TODO: Really? Come on...
        if(ref instanceof J2meFileReference) {
            try {
                return ((J2meFileReference)ref).getSize();
            } catch (IOException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
