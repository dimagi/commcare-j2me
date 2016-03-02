package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathEqExpr extends XPathBinaryOpExpr {
    public static final int EQ = 0;
    public static final int NEQ = 1;
    private boolean isEqOp;

    public XPathEqExpr() {
    } //for deserialization

    public XPathEqExpr(int op, XPathExpression a, XPathExpression b) {
        super(op, a, b);

        isEqOp = (op == EQ);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        Object aval = XPathFuncExpr.unpack(a.eval(model, evalContext));
        Object bval = XPathFuncExpr.unpack(b.eval(model, evalContext));
        boolean eq = testEquality(aval, bval);

        return new Boolean(isEqOp == eq);
    }

    @Override
    public String toString() {
        return super.toString(isEqOp ? "==" : "!=");
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        isEqOp = ExtUtil.readBool(in);
        readExpressions(in, pf);

        if (isEqOp) {
            op = XPathEqExpr.EQ;
        } else {
            op = XPathEqExpr.NEQ;
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeBool(out, isEqOp);
        writeExpressions(out);
    }

    /**
     * Test two XPath Objects for equality the same way that they would be tested
     * if they were the result of an equality operation
     *
     * @param aval XPath Value
     * @param bval XPath Value
     * @return true if the two values are equal, false otherwise
     */
    public static boolean testEquality(Object aval, Object bval) {
        boolean eq = false;

        if (aval instanceof Boolean || bval instanceof Boolean) {
            if (!(aval instanceof Boolean)) {
                aval = XPathFuncExpr.toBoolean(aval);
            } else if (!(bval instanceof Boolean)) {
                bval = XPathFuncExpr.toBoolean(bval);
            }

            boolean ba = ((Boolean)aval).booleanValue();
            boolean bb = ((Boolean)bval).booleanValue();
            eq = (ba == bb);
        } else if (aval instanceof Double || bval instanceof Double) {
            if (!(aval instanceof Double)) {
                aval = XPathFuncExpr.toNumeric(aval);
            } else if (!(bval instanceof Double)) {
                bval = XPathFuncExpr.toNumeric(bval);
            }

            double fa = ((Double)aval).doubleValue();
            double fb = ((Double)bval).doubleValue();
            eq = Math.abs(fa - fb) < 1.0e-12;
        } else {
            aval = XPathFuncExpr.toString(aval);
            bval = XPathFuncExpr.toString(bval);
            eq = (aval.equals(bval));
        }
        return eq;
    }

    @Override
    public String toPrettyString() {
        String prettyA = a.toPrettyString();
        String prettyB = b.toPrettyString();

        if (isEqOp) {
            return prettyA + " = " + prettyB;
        } else {
            return prettyA + " != " + prettyB;
        }
    }
}
