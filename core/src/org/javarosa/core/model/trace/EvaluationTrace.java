package org.javarosa.core.model.trace;

import java.util.Vector;

import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * Captures details about the outcome of a "Step" of expression execution, and
 * its recursive subexpressions.
 *
 * @author ctsims
 */
public class EvaluationTrace {

    private EvaluationTrace parent;
    private Object value;
    private String expression;

    private Vector<EvaluationTrace> children = new Vector<EvaluationTrace>();

    /**
     * Creates a trace record.
     *
     * @param expression The string representation of the expression
     *                   being evaluated
     * @param parent     The parent of this step of execution. Null if
     *                   this is the root.
     */
    public EvaluationTrace(String expression, EvaluationTrace parent) {
        this.expression = expression;
        this.parent = parent;
    }

    /**
     * @return The parent step of this trace. Null if
     * this is the root of the expression evaluation
     */
    public EvaluationTrace getParent() {
        return parent;
    }

    /**
     * Set the outcome value of this evaluation step
     *
     * @param value set the outcome of evaluating this expression
     */
    public void setOutcome(Object value) {
        this.value = value;
    }

    public void addSubTrace(EvaluationTrace child) {
        this.children.addElement(child);
    }

    public Vector<EvaluationTrace> getSubTraces() {
        return children;
    }

    public String getExpression() {
        return expression;
    }

    /**
     * @return The outcome of the expression's execution.
     */
    public String getValue() {
        // Temporarily deal with this in a flat manner until we can evaluate
        // more robustly
        if (value instanceof XPathNodeset) {
            return XPathFuncExpr.getSerializedNodeset((XPathNodeset)value);
        }
        return XPathFuncExpr.toString(value);
    }

    /**
     * @return A tag representing the type
     * of evaluation that was performed at
     * this step.
     */
    public String getEvaluationType() {
        return "xpath";
    }

}
