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
package org.javarosa.core.api;

import java.io.IOException;
import java.util.Date;

import org.javarosa.core.log.IFullLogSerializer;
import org.javarosa.core.log.StreamLogSerializer;
import org.javarosa.core.util.SortedIntSet;

/**
 * IIncidentLogger's are used for instrumenting applications to identify usage
 * patterns, usability errors, and general trajectories through applications.
 *
 * @author Clayton Sims
 * @date Apr 10, 2009
 */
public interface ILogger {

    public void log(String type, String message, Date logDate);

    public void clearLogs();

    public <T> T serializeLogs(IFullLogSerializer<T> serializer);

    public void serializeLogs(StreamLogSerializer serializer) throws IOException;

    public void serializeLogs(StreamLogSerializer serializer, int limit) throws IOException;

    public void panic();

    public int logSize();

    public void halt();
}
