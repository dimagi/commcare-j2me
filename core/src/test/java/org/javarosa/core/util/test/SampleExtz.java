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

package org.javarosa.core.util.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

public class SampleExtz implements Externalizable {
    String a, b;

    public SampleExtz(String a, String b) {
        this.a = a;
        this.b = b;
    }

    public SampleExtz() {
        this("", "");
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException {
        a = ExtUtil.readString(in);
        b = ExtUtil.readString(in);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, a);
        ExtUtil.writeString(out, b);
    }

    public boolean equals(Object o) {
        if (o instanceof SampleExtz) {
            SampleExtz se = (SampleExtz)o;
            return a.equals(se.a) && b.equals(se.b);
        } else {
            return false;
        }
    }

    public String toString() {
        return a + "," + b;
    }
}
