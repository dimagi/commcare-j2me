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

package org.javarosa.core.services;

import java.util.Vector;

import org.javarosa.core.services.properties.IPropertyRules;

/**
 * An IProperty Manager is responsible for setting and retrieving name/value pairs
 *
 * @author Yaw Anokwa
 */
public interface IPropertyManager {

    public Vector getProperty(String propertyName);

    public void setProperty(String propertyName, String propertyValue);

    public void setProperty(String propertyName, Vector propertyValue);

    public String getSingularProperty(String propertyName);

    public void addRules(IPropertyRules rules);

    public Vector getRules();
}
