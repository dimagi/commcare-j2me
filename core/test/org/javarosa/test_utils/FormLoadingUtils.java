/*
 * Copyright (C) 2015 JavaRosa
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

package org.javarosa.test_utils;

import java.io.IOException;
import java.io.InputStream;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Commonly used form loading utilities for testing.
 *
 * @author Phillip Mates
 */

public class FormLoadingUtils {

    /**
     * Load and parse an XML file into a form instance.
     *
     * @param formPath form resource filename that will be loaded at compile
     *                 time.
     */
    public static FormInstance loadFormInstance(String formPath) throws InvalidStructureException, IOException {
        // read in xml
        InputStream is = FormLoadingUtils.class.getResourceAsStream(formPath);
        if(is == null) { throw new IOException("No resource!!!:: " + formPath);};
        TreeElementParser parser = new TreeElementParser(ElementParser.instantiateParser(is), 0, "instance");

        // turn parsed xml into a form instance
        TreeElement root = null;
        try {
            root = parser.parse();
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage());
        } catch (UnfullfilledRequirementsException e) {
            // TODO: this error will eventually be removed from the base abstract
            // class, and then can be removed here
            throw new IOException(e.getMessage());
        }

        return new FormInstance(root, null);
    }
}
