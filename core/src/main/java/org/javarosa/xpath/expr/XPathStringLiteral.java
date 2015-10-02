package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathStringLiteral extends XPathExpression {
    public String s;

    public XPathStringLiteral() {
    } //for deserialization

    public XPathStringLiteral(String s) {
        this.s = s;
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return s;
    }

    @Override
    public String toString() {
        return "{str:\'" + s + "\'}"; //TODO: s needs to be escaped (' -> \'; \ -> \\)
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathStringLiteral) {
            XPathStringLiteral x = (XPathStringLiteral)o;
            return s.equals(x.s);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return s.hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        s = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, s);
    }

    @Override
    public String toPrettyString() {
        return "'" + s + "'";
    }
}