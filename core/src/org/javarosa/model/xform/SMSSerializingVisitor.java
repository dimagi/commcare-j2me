package org.javarosa.model.xform;

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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.javarosa.core.data.IDataPointer;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.IAnswerDataSerializer;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.IInstanceSerializingVisitor;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.core.services.transport.payload.DataPointerPayload;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.services.transport.payload.MultiMessagePayload;
import org.javarosa.xform.util.XFormAnswerDataSerializer;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

/**
 * A modified version of Clayton's XFormSerializingVisitor that constructs
 * SMS's.
 *
 * @author Munaf Sheikh, Cell-Life
 */
public class SMSSerializingVisitor implements IInstanceSerializingVisitor {

    private String theSmsStr = null; // sms string to be returned
    private String nodeSet = null; // which nodeset the sms contents are in
    private String xmlns = null;
    private String delimeter = null;
    private String prefix = null;
    private String method = null;
    private TreeReference rootRef;

    /**
     * The serializer to be used in constructing XML for AnswerData elements
     */
    IAnswerDataSerializer serializer;

    /**
     * The schema to be used to serialize answer data
     */
    FormDef schema; // not used

    Vector dataPointers;

    private void init() {
        theSmsStr = null;
        schema = null;
        dataPointers = new Vector();
        theSmsStr = "";
    }

    public byte[] serializeInstance(FormInstance model, FormDef formDef) throws IOException {
        init();
        this.schema = formDef;
        return serializeInstance(model);
    }


    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.utils.IInstanceSerializingVisitor#serializeInstance(org.javarosa.core.model.instance.FormInstance)
     */
    public byte[] serializeInstance(FormInstance model) throws IOException {
        return this.serializeInstance(model, new XPathReference("/"));
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.utils.IInstanceSerializingVisitor#serializeInstance(org.javarosa.core.model.instance.FormInstance, org.javarosa.core.model.XPathReference)
     */
    public byte[] serializeInstance(FormInstance model, XPathReference ref) throws IOException {
        init();
        rootRef = model.unpackReference(ref);
        if (this.serializer == null) {
            this.setAnswerDataSerializer(new XFormAnswerDataSerializer());
        }
        model.accept(this);
        if (theSmsStr != null) {
            //Encode in UTF-16 by default, since it's the default for complex messages
            return theSmsStr.getBytes("UTF-16BE");
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.utils.IInstanceSerializingVisitor#createSerializedPayload(org.javarosa.core.model.instance.FormInstance)
     */
    public IDataPayload createSerializedPayload(FormInstance model) throws IOException {
        return createSerializedPayload(model, new XPathReference("/"));
    }

    public IDataPayload createSerializedPayload(FormInstance model, XPathReference ref)
            throws IOException {
        init();
        rootRef = model.unpackReference(ref);
        if (this.serializer == null) {
            this.setAnswerDataSerializer(new XFormAnswerDataSerializer());
        }
        model.accept(this);
        if (theSmsStr != null) {
            byte[] form = theSmsStr.getBytes("UTF-16");
            return new ByteArrayPayload(form, null, IDataPayload.PAYLOAD_TYPE_SMS);
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.javarosa.core.model.utils.ITreeVisitor#visit(org.javarosa.core.model
     * .DataModelTree)
     */
    public void visit(FormInstance tree) {
        nodeSet = new String();

        //TreeElement root = tree.getRoot();
        TreeElement root = tree.resolveReference(rootRef);

        xmlns = root.getAttributeValue("", "xmlns");
        delimeter = root.getAttributeValue("", "delimeter");
        prefix = root.getAttributeValue("", "prefix");

        xmlns = (xmlns != null) ? xmlns : " ";
        delimeter = (delimeter != null) ? delimeter : " ";
        prefix = (prefix != null) ? prefix : " ";

        //Don't bother adding any delimiters, yet. Delimiters are
        //added before tags/data
        theSmsStr = prefix;

        // serialize each node to get it's answers
        for (int j = 0; j < root.getNumChildren(); j++) {
            TreeElement tee = root.getChildAt(j);
            String e = serializeNode(tee);
            if (e != null) {
                theSmsStr += e;
            }
        }
        theSmsStr = theSmsStr.trim();
    }

    public String serializeNode(TreeElement instanceNode) {
        String ae = "";
        // don't serialize template nodes or non-relevant nodes
        if (!instanceNode.isRelevant()
                || instanceNode.getMult() == TreeReference.INDEX_TEMPLATE)
            return null;

        if (instanceNode.getValue() != null) {
            Object serializedAnswer = serializer.serializeAnswerData(
                    instanceNode.getValue(), instanceNode.getDataType());

            if (serializedAnswer instanceof Element) {
                // DON"T handle this.
                throw new RuntimeException("Can't handle serialized output for"
                        + instanceNode.getValue().toString() + ", "
                        + serializedAnswer);
            } else if (serializedAnswer instanceof String) {
                Element e = new Element();
                e.addChild(Node.TEXT, (String)serializedAnswer);

                String tag = instanceNode.getAttributeValue("", "tag");
                ae += ((tag != null) ? tag + delimeter : delimeter); // tag
                // might
                // be
                // null

                for (int k = 0; k < e.getChildCount(); k++) {
                    ae += e.getChild(k).toString() + delimeter;
                }

            } else {
                throw new RuntimeException("Can't handle serialized output for"
                        + instanceNode.getValue().toString() + ", "
                        + serializedAnswer);
            }

            if (serializer.containsExternalData(instanceNode.getValue())
                    .booleanValue()) {
                IDataPointer[] pointer = serializer
                        .retrieveExternalDataPointer(instanceNode.getValue());
                for (int i = 0; i < pointer.length; ++i) {
                    dataPointers.addElement(pointer[i]);
                }
            }
        }
        return ae;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.javarosa.core.model.utils.IInstanceSerializingVisitor#
     * setAnswerDataSerializer(org.javarosa.core.model.IAnswerDataSerializer)
     */
    public void setAnswerDataSerializer(IAnswerDataSerializer ads) {
        this.serializer = ads;
    }

    public IInstanceSerializingVisitor newInstance() {
        XFormSerializingVisitor modelSerializer = new XFormSerializingVisitor();
        modelSerializer.setAnswerDataSerializer(this.serializer);
        return modelSerializer;
    }
}
