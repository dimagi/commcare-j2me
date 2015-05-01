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

package org.javarosa.xpath;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.javarosa.core.log.FatalException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathBinaryOpExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathUnaryOpExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathConditional implements IConditionExpr {
    private XPathExpression expr;
    public String xpath; //not serialized!
    public boolean hasNow; //indicates whether this XpathConditional contains the now() function (used for timestamping)

    public XPathConditional(String xpath) throws XPathSyntaxException {
        hasNow = false;
        if (xpath.indexOf("now()") > -1) {
            hasNow = true;
        }
        this.expr = XPathParseTool.parseXPath(xpath);
        this.xpath = xpath;
    }

    public XPathConditional(XPathExpression expr) {
        this.expr = expr;
    }

    public XPathConditional() {

    }

    public XPathExpression getExpr() {
        return expr;
    }

    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        try {
            return XPathFuncExpr.unpack(expr.eval(model, evalContext));
        } catch (XPathUnsupportedException e) {
            if (xpath != null) {
                throw new XPathUnsupportedException(xpath);
            } else {
                throw e;
            }


        }
    }

    public boolean eval(DataInstance model, EvaluationContext evalContext) {
        return XPathFuncExpr.toBoolean(evalRaw(model, evalContext)).booleanValue();
    }

    public String evalReadable(DataInstance model, EvaluationContext evalContext) {
        return XPathFuncExpr.toString(evalRaw(model, evalContext));
    }

    public Vector<TreeReference> evalNodeset(DataInstance model, EvaluationContext evalContext) {
        if (expr instanceof XPathPathExpr) {
            return ((XPathPathExpr)expr).evalRaw(model, evalContext).getReferences();
        } else {
            throw new FatalException("evalNodeset: must be path expression");
        }
    }

    public Vector<TreeReference> getExprsTriggers(TreeReference originalContextRef) {
        Vector triggers = new Vector();
        getExprsTriggersAccumulator(expr, triggers, null, originalContextRef);
        return triggers;
    }

    /**
     * Recursive helper to getExprsTriggers with an accumulator trigger vector.
     *
     * @param expr               Current expression we are collecting triggers from
     * @param triggers           Accumulates the references that this object's
     *                           expression value depends upon.
     * @param contextRef         Use this updated context; used, for instance,
     *                           when we move into handling predicates
     * @param originalContextRef Context reference pointing to the nodeset
     *                           reference; used for expanding 'current()'
     */
    private static void getExprsTriggersAccumulator(XPathExpression expr,
                                                    Vector<TreeReference> triggers,
                                                    TreeReference contextRef,
                                                    TreeReference originalContextRef) {
        if (expr instanceof XPathPathExpr) {
            TreeReference ref = ((XPathPathExpr)expr).getReference();
            TreeReference contextualized = ref;

            if (ref.getContext() == TreeReference.CONTEXT_ORIGINAL) {
                // Starts with 'current()' so contextualize in terms of the
                // nodeset's original reference.
                contextualized = ref.contextualize(originalContextRef);
            } else if (contextRef != null) {
                // If present then the context has been updated, so use it.
                // Necessary if we jump into handling predicates.
                contextualized = ref.contextualize(contextRef);
            }

            // TODO: It's possible we should just handle this the same way as
            // "genericize". Not entirely clear.
            if (contextualized.hasPredicates()) {
                contextualized = contextualized.removePredicates();
            }
            if (!triggers.contains(contextualized)) {
                triggers.addElement(contextualized);
            }
            // find the references this reference depends on inside of predicates
            for (int i = 0; i < ref.size(); i++) {
                Vector<XPathExpression> predicates = ref.getPredicate(i);
                if (predicates == null) {
                    continue;
                }

                //we can't generate this properly without an absolute reference
                if (!ref.isAbsolute()) {
                    throw new IllegalArgumentException("can't get triggers for relative references");
                }
                TreeReference predicateContext = ref.getSubReference(i);

                for (XPathExpression predicate : predicates) {
                    getExprsTriggersAccumulator(predicate, triggers,
                            predicateContext, originalContextRef);
                }
            }
        } else if (expr instanceof XPathBinaryOpExpr) {
            getExprsTriggersAccumulator(((XPathBinaryOpExpr)expr).a, triggers,
                    contextRef, originalContextRef);
            getExprsTriggersAccumulator(((XPathBinaryOpExpr)expr).b, triggers,
                    contextRef, originalContextRef);
        } else if (expr instanceof XPathUnaryOpExpr) {
            getExprsTriggersAccumulator(((XPathUnaryOpExpr)expr).a, triggers,
                    contextRef, originalContextRef);
        } else if (expr instanceof XPathFuncExpr) {
            XPathFuncExpr fx = (XPathFuncExpr)expr;
            for (int i = 0; i < fx.args.length; i++)
                getExprsTriggersAccumulator(fx.args[i], triggers,
                        contextRef, originalContextRef);
        }
    }

    public int hashCode() {
        return expr.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof XPathConditional) {
            XPathConditional cond = (XPathConditional)o;
            return expr.equals(cond.expr);
        } else {
            return false;
        }
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        expr = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        hasNow = (boolean)ExtUtil.readBool(in);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(expr));
        ExtUtil.writeBool(out, hasNow);
    }

    public String toString() {
        return "xpath[" + expr.toString() + "]";
    }

    public Vector<Object> pivot(DataInstance model, EvaluationContext evalContext) throws UnpivotableExpressionException {
        return expr.pivot(model, evalContext);
    }
}
