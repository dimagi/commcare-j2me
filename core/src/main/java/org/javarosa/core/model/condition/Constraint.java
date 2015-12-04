package org.javarosa.core.model.condition;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.Logger;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Constraint implements Externalizable {
    public IConditionExpr constraint;
    private String constraintMsg;
    private XPathExpression xPathConstraintMsg;

    public Constraint() {
    }

    public Constraint(IConditionExpr constraint, String constraintMsg) {
        this.constraint = constraint;
        this.constraintMsg = constraintMsg;
        attemptConstraintCompile();
    }

    public String getConstraintMessage(EvaluationContext ec, FormInstance instance, String textForm) {
        if (xPathConstraintMsg == null) {
            //If the request is for getting a constraint message in a specific format (like audio) from
            //itext, and there's no xpath, we couldn't possibly fulfill it
            return textForm == null ? constraintMsg : null;
        } else {
            if (textForm != null) {
                ec.setOutputTextForm(textForm);
            }
            try {
                Object value = xPathConstraintMsg.eval(instance, ec);
                if (value != "") {
                    return (String)value;
                }
                return null;
            } catch (Exception e) {
                Logger.exception("Error evaluating a valid-looking constraint xpath ", e);
                return constraintMsg;
            }
        }
    }

    private void attemptConstraintCompile() {
        xPathConstraintMsg = null;
        try {
            if (constraintMsg != null) {
                xPathConstraintMsg = XPathParseTool.parseXPath("string(" + constraintMsg + ")");
            }
        } catch (Exception e) {
            //Expected in probably most cases.
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        constraint = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
        constraintMsg = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        attemptConstraintCompile();
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(constraint));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(constraintMsg));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Constraint) {
            final Constraint otherConstraint = (Constraint)o;
            return constraint.equals(otherConstraint.constraint) &&
                    constraintMsg.equals(otherConstraint.constraintMsg);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return constraint.hashCode() ^ constraintMsg.hashCode();
    }
}
