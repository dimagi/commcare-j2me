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

import org.javarosa.core.log.IFullLogSerializer;
import org.javarosa.core.log.LogEntry;
import org.javarosa.core.model.utils.DateUtils;
import org.kxml2.kdom.Element;

/**
 * @author Clayton Sims
 * @date Apr 10, 2009
 *
 */
public class XmlLogSerializer implements IFullLogSerializer<Element> {

    private String topElementName;

    public XmlLogSerializer(String topElementName) {
        this.topElementName = topElementName;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.log.ILogSerializer#serializeLog(org.javarosa.core.log.IncidentLog)
     */
    private Element serializeLog(LogEntry log) {
        Element entry = new Element();
        entry.setName("log");
        entry.setAttribute(null, "date", DateUtils.formatDateTime(log.getTime(), DateUtils.FORMAT_ISO8601));

        Element type = entry.createElement(null,"type");
        type.addChild(Element.TEXT, log.getType());
        entry.addChild(Element.ELEMENT, type);

        Element message = entry.createElement(null,"msg");
        message.addChild(Element.TEXT, log.getMessage());
        entry.addChild(Element.ELEMENT, message);

        return entry;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.log.ILogSerializer#serializeLogs(org.javarosa.core.log.IncidentLog[])
     */
    public Element serializeLogs(LogEntry[] logs) {
        Element report = new Element();
        report.setName(topElementName);
        for(int i = 0; i < logs.length; ++i ) {
            report.addChild(Element.ELEMENT, this.serializeLog(logs[i]));
        }
        return report;
    }

}
