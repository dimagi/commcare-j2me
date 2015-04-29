/**
 *
 */
package org.javarosa.xform.parse;


import java.util.Hashtable;

import org.javarosa.model.xform.XPathReference;
import org.javarosa.core.model.SubmissionProfile;
import org.kxml2.kdom.Element;

/**
 * A Submission Profile
 *
 * @author ctsims
 */
public class SubmissionParser {

    public SubmissionProfile parseSubmission(String method, String action, XPathReference ref, Element element) {
        String mediatype = element.getAttributeValue(null, "mediatype");
        Hashtable<String, String> attributeMap = new Hashtable<String, String>();
        int nAttr = element.getAttributeCount();
        for (int i = 0; i < nAttr; ++i) {
            String name = element.getAttributeName(i);
            if (name.equals("ref")) continue;
            if (name.equals("bind")) continue;
            if (name.equals("method")) continue;
            if (name.equals("action")) continue;
            String value = element.getAttributeValue(i);
            attributeMap.put(name, value);
        }
        return new SubmissionProfile(ref, method, action, mediatype, attributeMap);
    }

    public boolean matchesCustomMethod(String method) {
        return false;
    }
}
