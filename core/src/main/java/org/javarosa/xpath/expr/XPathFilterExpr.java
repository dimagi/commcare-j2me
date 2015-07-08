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

package org.javarosa.xpath.expr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathUnsupportedException;

public class XPathFilterExpr extends XPathExpression {
    public XPathExpression x;
    public XPathExpression[] predicates;

    public XPathFilterExpr() {
    } //for deserialization

    public XPathFilterExpr(XPathExpression x, XPathExpression[] predicates) {
        this.x = x;
        this.predicates = predicates;
    }

    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        throw new XPathUnsupportedException("filter expression");
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("{filt-expr:");
        sb.append(x.toString());
        sb.append(",{");
        for (int i = 0; i < predicates.length; i++) {
            sb.append(predicates[i].toString());
            if (i < predicates.length - 1)
                sb.append(",");
        }
        sb.append("}}");

        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o instanceof XPathFilterExpr) {
            XPathFilterExpr fe = (XPathFilterExpr)o;

            return x.equals(fe.x) && ExtUtil.arrayEquals(predicates, fe.predicates, false);
        } else {
            return false;
        }
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        x = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        Vector v = (Vector)ExtUtil.read(in, new ExtWrapListPoly(), pf);

        predicates = new XPathExpression[v.size()];
        for (int i = 0; i < predicates.length; i++)
            predicates[i] = (XPathExpression)v.elementAt(i);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        Vector v = new Vector();
        for (int i = 0; i < predicates.length; i++)
            v.addElement(predicates[i]);

        ExtUtil.write(out, new ExtWrapTagged(x));
        ExtUtil.write(out, new ExtWrapListPoly(v));
    }

    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        throw new UnpivotableExpressionException();
    }

    public String toPrettyString() {
        return "Unsupported Predicate";
    }
}
