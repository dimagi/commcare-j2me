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

package org.javarosa.formmanager.properties;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Properties for form management and entry.
 *
 * @author Clayton Sims
 *
 */
public class FormManagerProperties implements IPropertyRules {
        Hashtable rules;
        Vector readOnlyProperties;

        public final static String VIEW_TYPE_PROPERTY = "ViewStyle";

        // View Types
        public static final String VIEW_CHATTERBOX = "v_chatterbox";
        public static final String VIEW_SINGLEQUESTIONSCREEN = "v_singlequestionscreen";

        public final static String EXTRA_KEY_FORMAT = "extra_key_action";
        public final static String EXTRA_KEY_LANGUAGE_CYCLE = "cycle";
        public final static String EXTRA_KEY_AUDIO_PLAYBACK = "audio";

        public final static String LOOSE_MEDIA = "loose_media";
        public final static String LOOSE_MEDIA_YES = "yes";
        public final static String LOOSE_MEDIA_NO = "no";

        public final static String PLAY_AUDIO = "play_audio";
        public final static String PLAY_AUDIO_YES = "yes";
        public final static String PLAY_AUDIO_NO = "no";

        /**
         * Creates the JavaRosa set of property rules
         */
        public FormManagerProperties() {
            rules = new Hashtable();
            readOnlyProperties = new Vector();

            Vector allowableDisplays = new Vector();
            allowableDisplays.addElement(VIEW_CHATTERBOX);
            allowableDisplays.addElement(VIEW_SINGLEQUESTIONSCREEN);
            rules.put(VIEW_TYPE_PROPERTY, allowableDisplays);

            Vector hashkeyAudio = new Vector();
            hashkeyAudio.addElement(EXTRA_KEY_LANGUAGE_CYCLE);
            hashkeyAudio.addElement(EXTRA_KEY_AUDIO_PLAYBACK);
            rules.put(EXTRA_KEY_FORMAT,hashkeyAudio);

            Vector looseMedia = new Vector();
            looseMedia.addElement(LOOSE_MEDIA_YES);
            looseMedia.addElement(LOOSE_MEDIA_NO);
            rules.put(LOOSE_MEDIA, looseMedia);

            Vector playAudio = new Vector();
            playAudio.addElement(PLAY_AUDIO_YES);
            playAudio.addElement(PLAY_AUDIO_NO);
            rules.put(PLAY_AUDIO, playAudio);
        }

        /** (non-Javadoc)
         *  @see org.javarosa.properties.IPropertyRules#allowableValues(String)
         */
        public Vector allowableValues(String propertyName) {
            return (Vector)rules.get(propertyName);
        }

        /** (non-Javadoc)
         *  @see org.javarosa.properties.IPropertyRules#checkValueAllowed(String, String)
         */
        public boolean checkValueAllowed(String propertyName, String potentialValue) {
            Vector prop = ((Vector)rules.get(propertyName));
            if(prop.size() != 0) {
                //Check whether this is a dynamic property
                if(prop.size() == 1 && checkPropertyAllowed((String)prop.elementAt(0))) {
                    // If so, get its list of available values, and see whether the potentival value is acceptable.
                    return ((Vector)PropertyManager._().getProperty((String)prop.elementAt(0))).contains(potentialValue);
                }
                else {
                    return ((Vector)rules.get(propertyName)).contains(potentialValue);
                }
            }
            else
                return true;
        }

        /** (non-Javadoc)
         *  @see org.javarosa.properties.IPropertyRules#allowableProperties()
         */
        public Vector allowableProperties() {
            Vector propList = new Vector();
            Enumeration iter = rules.keys();
            while (iter.hasMoreElements()) {
                propList.addElement(iter.nextElement());
            }
            return propList;
        }

        /** (non-Javadoc)
         *  @see org.javarosa.properties.IPropertyRules#checkPropertyAllowed)
         */
        public boolean checkPropertyAllowed(String propertyName) {
            Enumeration iter = rules.keys();
            while (iter.hasMoreElements()) {
                if(propertyName.equals(iter.nextElement())) {
                    return true;
                }
            }
            return false;
        }

        /** (non-Javadoc)
         *  @see org.javarosa.properties.IPropertyRules#checkPropertyUserReadOnly)
         */
        public boolean checkPropertyUserReadOnly(String propertyName){
            return readOnlyProperties.contains(propertyName);
        }
        /*
         * (non-Javadoc)
         * @see org.javarosa.core.services.properties.IPropertyRules#getHumanReadableDescription(java.lang.String)
         */
        public String getHumanReadableDescription(String propertyName) {
            if(VIEW_TYPE_PROPERTY.equals(propertyName)) {
                return "Form Entry View";
            }else if(EXTRA_KEY_FORMAT.equals(propertyName)) {
                return "What Action Should the # Button Perform?";
            }else if(PLAY_AUDIO.equals(propertyName)){
                return "Should CommCare play audio?";
            }return propertyName;
        }

        /*
         * (non-Javadoc)
         * @see org.javarosa.core.services.properties.IPropertyRules#getHumanReadableValue(java.lang.String, java.lang.String)
         */
        public String getHumanReadableValue(String propertyName, String value) {
            if(VIEW_TYPE_PROPERTY.equals(propertyName)) {
                if(VIEW_CHATTERBOX.equals(value)) {
                    return "Chatterbox";
                } else if(VIEW_SINGLEQUESTIONSCREEN.equals(value)) {
                    return "One Question Per Screen";
                }
            }else if(EXTRA_KEY_FORMAT.equals(propertyName)) {
                if(EXTRA_KEY_AUDIO_PLAYBACK.equals(value)){
                    return "Play Audio";
                }else if(EXTRA_KEY_LANGUAGE_CYCLE.equals(value)){
                    return "Language Cycle";
                }
             }
            return value;
        }
        /*
         * (non-Javadoc)
         * @see org.javarosa.core.services.properties.IPropertyRules#handlePropertyChanges(java.lang.String)
         */
        public void handlePropertyChanges(String propertyName) {
            //Is there anything we can do here?
        }
}
