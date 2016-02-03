package org.javarosa.core.model.actions;

import org.javarosa.core.model.Action;
import org.javarosa.core.model.ActionTriggerSource;
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
import org.javarosa.xform.parse.IElementHandler;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.kxml2.kdom.Element;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class SetValueAction extends Action {

    // node that this action is targeting
    private TreeReference target;

    // the value to be assigned to the target when this action is triggered
    private XPathExpression value;

    private String explicitValue;

    public static final String ELEMENT_NAME = "setvalue";

    public SetValueAction() {
        // for externalization
    }

    public SetValueAction(TreeReference target, XPathExpression value) {
        super(ELEMENT_NAME);
        this.target = target;
        this.value = value;
    }

    public SetValueAction(TreeReference target, String explicitValue) {
        super(ELEMENT_NAME);
        this.target = target;
        this.explicitValue = explicitValue;
    }

    public static IElementHandler getHandler() {
        return new IElementHandler() {
            public void handle(XFormParser p, Element e, Object parent) {
                p.parseSetValueAction((ActionTriggerSource) parent, e);
            }
        };
    }

    @Override
    public TreeReference processAction(FormDef model, TreeReference contextRef) {

        // Qualify the reference if necessary
        TreeReference targetReference =
                contextRef == null ? target : target.contextualize(contextRef);

        //For now we only process setValue actions which are within the
        //context if a context is provided. This happens for repeats where
        //insert events should only trigger on the right nodes
        if (contextRef != null) {

            //Note: right now we're qualifying then testing parentage to see whether
            //there was a conflict, but it's not super clear whether this is a perfect
            //strategy
            if (!contextRef.isParentOf(targetReference, false)) {
                return null;
            }
        }

        //TODO: either the target or the value's node might not exist here, catch and throw reasonably
        EvaluationContext context = new EvaluationContext(model.getEvaluationContext(), targetReference);

        String failMessage = "Target of TreeReference " + target.toString(true) + " could not be resolved!";

        if (targetReference.hasPredicates()) {
            //CTS: in theory these predicates could contain logic which breaks if the qualified ref
            //contains unbound repeats (IE: nested repeats).
            Vector<TreeReference> references = context.expandReference(targetReference);
            if (references.size() == 0) {
                //If after finding our concrete reference it is a template, this action is outside of the
                //scope of the current target, so we can leave.
                if (model.getMainInstance().hasTemplatePath(target)) {
                    return null;
                }
                throw new NullPointerException(failMessage);
            } else if (references.size() > 1) {
                throw new XPathTypeMismatchException("XPath nodeset has more than one node [" + XPathNodeset.printNodeContents(references) + "]; Actions can only target a single node reference. Refine path expression to match only one node.");
            } else {
                targetReference = references.elementAt(0);
            }
        }

        AbstractTreeElement node = context.resolveReference(targetReference);
        if (node == null) {
            //After all that, there's still the possibility that the qualified reference contains
            //an unbound template, so see if such a reference could exist. Unfortunately this
            //won't be included in the above walk if the template is nested, since only the
            //top level template retains its subelement templates
            if(model.getMainInstance().hasTemplatePath(target)) {
                return null;
            } else {
                throw new NullPointerException(failMessage);
            }
        }

        Object result;

        //CTS: Is not clear whether we should be creating _another_ EC below with this newly qualified
        //ref or not. This logic used to come after the result was calculated.

        if (explicitValue != null) {
            result = explicitValue;
        } else {
            result = XPathFuncExpr.unpack(value.eval(model.getMainInstance(), context));
        }

        int dataType = node.getDataType();
        IAnswerData val = Recalculate.wrapData(result, dataType);

        if (val == null) {
            model.setValue(null, targetReference);
        } else {
            model.setValue(AnswerDataFactory.templateByDataType(dataType).cast(val.uncast()),
                    targetReference);
        }
        return targetReference;
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
