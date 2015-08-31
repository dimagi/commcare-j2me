package org.javarosa.xform.parse;

import org.javarosa.core.model.QuestionDataExtension;
import org.javarosa.core.model.ImageRestrictionExtension;

import org.kxml2.kdom.Element;

/**
 * An additional parser for the "upload" question type that looks for an extra attribute
 * specifying the maximum allowable dimension for an image
 *
 * @author amstone
 */
public class ImageRestrictionExtensionParser extends QuestionExtensionParser {

    public ImageRestrictionExtensionParser() {
        setElementName("upload");
    }

    @Override
    public QuestionDataExtension parse(Element elt) {
        String s = elt.getAttributeValue(XFormParser.NAMESPACE_JAVAROSA,
                "imageDimensionScaledMax");
        if (s != null) {
            if (s.endsWith("px")) {
                s = s.substring(0, s.length() - 2);
            }
            try {
                int maxDimens = Integer.parseInt(s);
                return new ImageRestrictionExtension(maxDimens);
            } catch (NumberFormatException e) {
                throw new XFormParseException("Invalid input for image max dimension: " + s);
            }
        }
        return null;
    }

    @Override
    public String[] getUsedAttributes() {
        return new String[]{"imageDimensionScaledMax"};
    }
}
