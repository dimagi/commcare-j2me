/**
 *
 */
package org.javarosa.core.model.actions;

import org.javarosa.core.model.Action;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.Recalculate;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class SetValueAction extends Action {
    private TreeReference target;
    private XPathExpression value;
    private String explicitValue;

    public SetValueAction() {

    }

    public SetValueAction(TreeReference target, XPathExpression value) {
        super("setvalue");
        this.target = target;
        this.value = value;
    }

    public SetValueAction(TreeReference target, String explicitValue) {
        super("setvalue");
        this.target = target;
        this.explicitValue = explicitValue;
    }

    public void processAction(FormDef model, TreeReference contextRef) {

        //Qualify the reference if necessary
        TreeReference qualifiedReference = contextRef == null ? target : target.contextualize(contextRef);

        //For now we only process setValue actions which are within the
        //context if a context is provided. This happens for repeats where
        //insert events should only trigger on the right nodes
        if (contextRef != null) {

            //Note: right now we're qualifying then testing parentage to see wheter
            //there was a conflict, but it's not super clear whether this is a perfect
            //strategy
            if (!contextRef.isParentOf(qualifiedReference, false)) {
                return;
            }
        }

        //TODO: either the target or the value's node might not exist here, catch and throw
        //reasonably
        EvaluationContext context = new EvaluationContext(model.getEvaluationContext(), qualifiedReference);

        Object result;

        if (explicitValue != null) {
            result = explicitValue;
        } else {
            result = XPathFuncExpr.unpack(value.eval(model.getMainInstance(), context));
        }
        
        String failMessage = "Target of TreeReference " + target.toString(true) + " could not be resolved!";
        
        //If the reference has predicates we need to qualify it further.
        if(qualifiedReference.hasPredicates()) {
            Vector<TreeReference> references = context.expandReference(qualifiedReference);
            if(references.size() == 0) {
                throw new NullPointerException(failMessage);
            }
            else if(references.size() > 1) {
                //The spec for single value binding says we should pick the first one,
                //but that's not how we do things to prevent errors. We're going to fail
                //the same way a nodeset does
                throw new XPathTypeMismatchException("XPath nodeset has more than one node [" + XPathNodeset.printNodeContents(references) + "]; Actions can only target a single node reference. Refine path expression to match only one node.");
            }
            else {
                qualifiedReference = references.elementAt(0);
            }
        }

        AbstractTreeElement node = context.resolveReference(qualifiedReference);
        if (node == null) {
            throw new NullPointerException(failMessage);
        }
        int dataType = node.getDataType();
        IAnswerData val = Recalculate.wrapData(result, dataType);

        model.setValue(val == null ? null : AnswerDataFactory.templateByDataType(dataType).cast(val.uncast()), qualifiedReference);
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        target = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        explicitValue = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        if (explicitValue == null) {
            value = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        }

    }

    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, target);

        ExtUtil.write(out, ExtUtil.emptyIfNull(explicitValue));
        if (explicitValue == null) {
            ExtUtil.write(out, new ExtWrapTagged(value));
        }
    }
}
