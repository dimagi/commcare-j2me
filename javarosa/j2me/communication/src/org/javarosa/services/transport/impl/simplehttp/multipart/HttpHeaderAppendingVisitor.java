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

package org.javarosa.services.transport.impl.simplehttp.multipart;

import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.core.services.transport.payload.DataPointerPayload;
import org.javarosa.core.services.transport.payload.IDataPayload;
import org.javarosa.core.services.transport.payload.IDataPayloadVisitor;
import org.javarosa.core.services.transport.payload.MultiMessagePayload;

import java.util.Enumeration;

/**
 * @author Clayton Sims
 * @date Dec 18, 2008
 *
 */
public class HttpHeaderAppendingVisitor implements IDataPayloadVisitor<IDataPayload> {

    private boolean top = false;
    private boolean first = true;
    private String divider;

    private String contentType;

    public HttpHeaderAppendingVisitor() {
        top = true;
    }

    private HttpHeaderAppendingVisitor(String divider) {
        this.divider = divider;
        this.top = false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayloadVisitor#visit(org.javarosa.core.services.transport.ByteArrayPayload)
     */
    public IDataPayload visit(ByteArrayPayload payload) {
        return visitIndividual(payload);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.services.transport.IDataPayloadVisitor#visit(org.javarosa.core.services.transport.MultiMessagePayload)
     */
    public IDataPayload visit(MultiMessagePayload payload) {
        MultiMessagePayload ret = new MultiMessagePayload();
        if(top) {
            //TODO: Create a reasonable divider, and
            divider = "7_Clj7N9Heh_NJsJunQMlTQoHRzO0-0vA]";
            contentType = "multipart/form-data; boundary=" + divider + "";
        }
        HttpHeaderAppendingVisitor newVis = new HttpHeaderAppendingVisitor(divider);
        Enumeration en = payload.getPayloads().elements();
        while(en.hasMoreElements()) {
            IDataPayload child = (IDataPayload)en.nextElement();
            ret.addPayload((IDataPayload)child.accept(newVis));
        }
        HttpTransportHeader footer = new HttpTransportHeader();
        footer.addHeader("\r\n--", divider + "--");
        ret.addPayload(footer);
        return ret;
    }

    public IDataPayload visit(DataPointerPayload payload) {
        return visitIndividual(payload);
    }

    private IDataPayload visitIndividual(IDataPayload payload) {
        if(divider != null) {
            MultiMessagePayload message = new MultiMessagePayload();
            HttpTransportHeader divHeader = new HttpTransportHeader();
            if(first) {
                divHeader.addHeader("--", divider);
                first = false;
            } else {
                divHeader.addHeader("\r\n--", divider);
            }
            HttpTransportHeader header = new HttpTransportHeader();
            switch(payload.getPayloadType()) {
            case IDataPayload.PAYLOAD_TYPE_JPG:
                if(payload.getPayloadId() != null) {
                    header.addHeader("Content-Disposition: ", "form-data; name=\"" + payload.getPayloadId() + "\"; filename=\"" + payload.getPayloadId() + "\"");
                }
                header.addHeader("Content-Type: ", getContentTypeFromId(payload.getPayloadType()));
                header.addHeader("Content-Transfer-Encoding: ", "binary");
                break;
            default:
                if(payload.getPayloadId() != null) {
                    header.addHeader("Content-Disposition: ", "form-data; name=\"" + payload.getPayloadId() + "\"; filename=\"" + payload.getPayloadId() + "\"");
                }
                header.addHeader("Content-Type: ", getContentTypeFromId(payload.getPayloadType()));
                header.addHeader("Content-Transfer-Encoding: ", "binary");
            }

            HttpTransportHeader finalheader = new HttpTransportHeader();
            finalheader.addHeaderNoNewline("\r\n", "");

            message.addPayload(divHeader);
            message.addPayload(header);
            message.addPayload(finalheader);
            message.addPayload(payload);
            return message;
        }
        else {
            contentType = getContentTypeFromId(payload.getPayloadType());
            return payload;
        }
    }

    public String getOverallContentType() {
        return contentType;
    }

    private String getContentTypeFromId(int id) {
        switch(id) {
        case IDataPayload.PAYLOAD_TYPE_TEXT:
            return "text/plain";
        case IDataPayload.PAYLOAD_TYPE_XML:
            return "text/xml";
        case IDataPayload.PAYLOAD_TYPE_JPG:
            return "image/jpeg";
            //TODO: Handle this
            //header.addHeader("Content-transfer-encoding: ", "binary");
        }
        return "text/plain";
    }
}
