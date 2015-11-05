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
package org.javarosa.user.utility;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.utils.IPreloadHandler;
import org.javarosa.core.model.User;

/**
 * @author Clayton Sims
 * @date May 12, 2009
 *
 */
public class UserPreloadHandler implements IPreloadHandler {
    User user;

    public UserPreloadHandler(User user) {
        this.user = user;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IPreloadHandler#handlePostProcess(org.javarosa.core.model.instance.TreeElement, java.lang.String)
     */
    public boolean handlePostProcess(TreeElement node, String params) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IPreloadHandler#handlePreload(java.lang.String)
     */
    public IAnswerData handlePreload(String preloadParams) {
        if(preloadParams.equals("username")) {
            return new UncastData(user.getUsername());
        } else if(preloadParams.equals("uuid")) {
            return new UncastData(user.getUniqueId());
        }
        String property = user.getProperty(preloadParams);
        if(property == null) {
            return null;
        }
        return new UncastData(property);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IPreloadHandler#preloadHandled()
     */
    public String preloadHandled() {
        return "user";
    }

}
