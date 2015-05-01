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

package org.javarosa.core.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.condition.Condition;
import org.javarosa.core.model.condition.Constraint;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.condition.Recalculate;
import org.javarosa.core.model.condition.Triggerable;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.InvalidReferenceException;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.util.restorable.RestoreUtils;
import org.javarosa.core.model.utils.QuestionPreloader;
import org.javarosa.core.services.locale.Localizable;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathTypeMismatchException;

/**
 * Definition of a form. This has some meta data about the form definition and a
 * collection of groups together with question branching or skipping rules.
 *
 * @author Daniel Kayiwa, Drew Roos
 */
public class FormDef implements IFormElement, Localizable, Persistable, IMetaData {
    public static final String STORAGE_KEY = "FORMDEF";
    public static final int TEMPLATING_RECURSION_LIMIT = 10;

    private Vector children;// <IFormElement>
    /**
     * A collection of group definitions.
     */
    private int id;
    /**
     * The numeric unique identifier of the form definition on the local device
     */
    private String title;
    /**
     * The display title of the form.
     */
    private String name;

    private Vector<XFormExtension> extensions;

    /**
     * A unique external name that is used to identify the form between machines
     */
    private Localizer localizer;

    // This list is topologically ordered, meaning for any tA
    // and tB in the list, where tA comes before tB, evaluating tA cannot
    // depend on any result from evaluating tB
    public Vector<Triggerable> triggerables;

    // true if triggerables has been ordered topologically (DON'T DELETE ME
    // EVEN THOUGH I'M UNUSED)
    private boolean triggerablesInOrder;


    // <IConditionExpr> contents of <output> tags that serve as parameterized
    // arguments to captions
    private Vector outputFragments;

    public Hashtable<TreeReference, Vector<Triggerable>> triggerIndex;
    private Hashtable<TreeReference, Condition> conditionRepeatTargetIndex;
    // associates repeatable nodes with the Condition that determines their
    // relevancy
    public EvaluationContext exprEvalContext;

    private QuestionPreloader preloader = new QuestionPreloader();

    // XML ID's cannot start with numbers, so this should never conflict
    private static String DEFAULT_SUBMISSION_PROFILE = "1";

    private Hashtable<String, SubmissionProfile> submissionProfiles;

    private Hashtable<String, DataInstance> formInstances;
    private FormInstance mainInstance = null;

    Hashtable<String, Vector<Action>> eventListeners;
    
    boolean mDebugModeEnabled = false;


    /**
     *
     */
    public FormDef() {
        setID(-1);
        setChildren(null);
        triggerables = new Vector();
        triggerablesInOrder = true;
        triggerIndex = new Hashtable<TreeReference, Vector<Triggerable>>();
        //This is kind of a wreck...
        setEvaluationContext(new EvaluationContext(null));
        outputFragments = new Vector();
        submissionProfiles = new Hashtable<String, SubmissionProfile>();
        formInstances = new Hashtable<String, DataInstance>();
        eventListeners = new Hashtable<String, Vector<Action>>();
        extensions = new Vector<XFormExtension>();
    }


    /**
     * Getters and setters for the vectors tha
     */
    public void addNonMainInstance(DataInstance instance) {
        formInstances.put(instance.getInstanceId(), instance);
        this.setEvaluationContext(new EvaluationContext(null));
    }

    /**
     * Get an instance based on a name
     *
     * @param name string name
     * @return
     */
    public DataInstance getNonMainInstance(String name) {
        if (!formInstances.containsKey(name)) {
            return null;
        }
        return formInstances.get(name);
    }

    /**
     * Get the non main instances
     *
     * @return
     */
    public Enumeration getNonMainInstances() {
        return formInstances.elements();
    }

    /**
     * Set the main instance
     *
     * @param fi
     */
    public void setInstance(FormInstance fi) {
        mainInstance = fi;
        fi.setFormId(getID());
        this.setEvaluationContext(new EvaluationContext(null));
        attachControlsToInstanceData();
    }

    /**
     * Get the main instance
     *
     * @return
     */
    public FormInstance getMainInstance() {
        return mainInstance;
    }

    public FormInstance getInstance() {
        return getMainInstance();
    }

    public void fireEvent() {

    }


    // ---------- child elements
    public void addChild(IFormElement fe) {
        this.children.addElement(fe);
    }

    public IFormElement getChild(int i) {
        if (i < this.children.size())
            return (IFormElement)this.children.elementAt(i);

        throw new ArrayIndexOutOfBoundsException(
                "FormDef: invalid child index: " + i + " only "
                        + children.size() + " children");
    }

    public IFormElement getChild(FormIndex index) {
        IFormElement element = this;
        while (index != null && index.isInForm()) {
            element = element.getChild(index.getLocalIndex());
            index = index.getNextLevel();
        }
        return element;
    }

    /**
     * Dereference the form index and return a Vector of all interstitial nodes
     * (top-level parent first; index target last)
     *
     * Ignore 'new-repeat' node for now; just return/stop at ref to
     * yet-to-be-created repeat node (similar to repeats that already exist)
     *
     * @param index
     * @return
     */
    public Vector explodeIndex(FormIndex index) {
        Vector indexes = new Vector();
        Vector multiplicities = new Vector();
        Vector elements = new Vector();

        collapseIndex(index, indexes, multiplicities, elements);
        return elements;
    }

    // take a reference, find the instance node it refers to (factoring in
    // multiplicities)

    /**
     * @param index
     * @return
     */
    public TreeReference getChildInstanceRef(FormIndex index) {
        Vector indexes = new Vector();
        Vector multiplicities = new Vector();
        Vector elements = new Vector();

        collapseIndex(index, indexes, multiplicities, elements);
        return getChildInstanceRef(elements, multiplicities);
    }

    /**
     * Return a tree reference which follows the path down the concrete elements provided
     * along with the multiplicities provided.
     *
     * @param elements
     * @param multiplicities
     * @return
     */
    public TreeReference getChildInstanceRef(Vector elements, Vector multiplicities) {
        if (elements.size() == 0)
            return null;

        // get reference for target element
        TreeReference ref = FormInstance.unpackReference(((IFormElement)elements.lastElement()).getBind()).clone();
        for (int i = 0; i < ref.size(); i++) {
            //There has to be a better way to encapsulate this
            if (ref.getMultiplicity(i) != TreeReference.INDEX_ATTRIBUTE) {
                ref.setMultiplicity(i, 0);
            }
        }

        // fill in multiplicities for repeats along the way
        for (int i = 0; i < elements.size(); i++) {
            IFormElement temp = (IFormElement)elements.elementAt(i);
            if (temp instanceof GroupDef && ((GroupDef)temp).getRepeat()) {
                TreeReference repRef = FormInstance.unpackReference(temp.getBind());
                if (repRef.isParentOf(ref, false)) {
                    int repMult = ((Integer)multiplicities.elementAt(i)).intValue();
                    ref.setMultiplicity(repRef.size() - 1, repMult);
                } else {
                    return null; // question/repeat hierarchy is not consistent
                    // with instance instance and bindings
                }
            }
        }

        return ref;
    }

    public void setLocalizer(Localizer l) {
        if (this.localizer != null) {
            this.localizer.unregisterLocalizable(this);
        }

        this.localizer = l;
        if (this.localizer != null) {
            this.localizer.registerLocalizable(this);
        }
    }

    // don't think this should ever be called(!)
    public XPathReference getBind() {
        throw new RuntimeException("method not implemented");
    }

    public void setValue(IAnswerData data, TreeReference ref) {
        setValue(data, ref, mainInstance.resolveReference(ref));
    }

    public void setValue(IAnswerData data, TreeReference ref, TreeElement node) {
        setAnswer(data, node);
        triggerTriggerables(ref);
        //TODO: pre-populate fix-count repeats here?
    }

    public void setAnswer(IAnswerData data, TreeReference ref) {
        setAnswer(data, mainInstance.resolveReference(ref));
    }

    public void setAnswer(IAnswerData data, TreeElement node) {
        node.setAnswer(data);
    }

    /**
     * Deletes the inner-most repeat that this node belongs to and returns the
     * corresponding FormIndex. Behavior is currently undefined if you call this
     * method on a node that is not contained within a repeat.
     *
     * @param index
     * @return
     */
    public FormIndex deleteRepeat(FormIndex index) {
        Vector indexes = new Vector();
        Vector multiplicities = new Vector();
        Vector elements = new Vector();
        collapseIndex(index, indexes, multiplicities, elements);

        // loop backwards through the elements, removing objects from each
        // vector, until we find a repeat
        // TODO: should probably check to make sure size > 0
        for (int i = elements.size() - 1; i >= 0; i--) {
            IFormElement e = (IFormElement)elements.elementAt(i);
            if (e instanceof GroupDef && ((GroupDef)e).getRepeat()) {
                break;
            } else {
                indexes.removeElementAt(i);
                multiplicities.removeElementAt(i);
                elements.removeElementAt(i);
            }
        }

        // build new formIndex which includes everything
        // up to the node we're going to remove
        FormIndex newIndex = buildIndex(indexes, multiplicities, elements);

        TreeReference deleteRef = getChildInstanceRef(newIndex);
        TreeElement deleteElement = mainInstance.resolveReference(deleteRef);
        TreeReference parentRef = deleteRef.getParentRef();
        TreeElement parentElement = mainInstance.resolveReference(parentRef);

        int childMult = deleteElement.getMult();
        parentElement.removeChild(deleteElement);

        // update multiplicities of other child nodes
        for (int i = 0; i < parentElement.getNumChildren(); i++) {
            TreeElement child = parentElement.getChildAt(i);
            if (child.getMult() > childMult) {
                child.setMult(child.getMult() - 1);
            }
        }

        triggerTriggerables(deleteRef);
        return newIndex;
    }

    public void createNewRepeat(FormIndex index) throws InvalidReferenceException {
        TreeReference destRef = getChildInstanceRef(index);
        TreeElement template = mainInstance.getTemplate(destRef);

        mainInstance.copyNode(template, destRef);

        preloadInstance(mainInstance.resolveReference(destRef));

        //2013-05-14 - ctsims - Events should get fired _before_ calculate stuff is fired, moved
        //this above triggering triggerables
        //Grab any actions listening to this event
        Vector<Action> listeners = getEventListeners(Action.EVENT_JR_INSERT);
        for (Action a : listeners) {
            a.processAction(this, destRef);
        }

        triggerTriggerables(destRef); // trigger conditions that depend on the creation of this new node
        initializeTriggerables(destRef); // initialize conditions for the node (and sub-nodes)
    }

    public boolean isRepeatRelevant(TreeReference repeatRef) {
        boolean relev = true;

        Condition c = (Condition)conditionRepeatTargetIndex.get(repeatRef.genericize());
        if (c != null) {
            relev = c.evalBool(mainInstance, new EvaluationContext(exprEvalContext, repeatRef));
        }

        //check the relevancy of the immediate parent
        if (relev) {
            TreeElement templNode = mainInstance.getTemplate(repeatRef);
            TreeReference parentPath = templNode.getParent().getRef().genericize();
            TreeElement parentNode = mainInstance.resolveReference(parentPath.contextualize(repeatRef));
            relev = parentNode.isRelevant();
        }

        return relev;
    }

    public boolean canCreateRepeat(TreeReference repeatRef, FormIndex repeatIndex) {
        GroupDef repeat = (GroupDef)this.getChild(repeatIndex);

        //Check to see if this repeat can have children added by the user
        if (repeat.noAddRemove) {
            //Check to see if there's a count to use to determine how many children this repeat
            //should have
            if (repeat.getCountReference() != null) {
                int currentMultiplicity = repeatIndex.getElementMultiplicity();

                AbstractTreeElement countNode = this.getMainInstance().resolveReference(repeat.getConextualizedCountReference(repeatRef));
                if (countNode == null) {
                    throw new XPathTypeMismatchException("Could not find the location " + repeat.getConextualizedCountReference(repeatRef).toString() + " where the repeat at " + repeatRef.toString(false) + " is looking for its count");
                }
                //get the total multiplicity possible
                IAnswerData count = countNode.getValue();
                int fullcount = -1;
                if (count == null) {
                    fullcount = 0;
                } else {
                    try {
                        fullcount = ((Integer)new IntegerData().cast(count.uncast()).getValue()).intValue();
                    } catch (IllegalArgumentException iae) {
                        throw new XPathTypeMismatchException("The repeat count value \"" + count.uncast().getString() + "\" at " + repeat.getConextualizedCountReference(repeatRef).toString() + " must be a number!");
                    }
                }

                if (fullcount <= currentMultiplicity) {
                    return false;
                }
            } else {
                //Otherwise the user can never add repeat instances
                return false;
            }
        }

        //TODO: If we think the node is still relevant, we also need to figure out a way to test that assumption against
        //the repeat's constraints.


        return true;
    }

    public void copyItemsetAnswer(QuestionDef q, TreeElement targetNode, IAnswerData data) throws InvalidReferenceException {
        ItemsetBinding itemset = q.getDynamicChoices();
        TreeReference targetRef = targetNode.getRef();
        TreeReference destRef = itemset.getDestRef().contextualize(targetRef);

        Vector<Selection> selections = null;
        Vector<String> selectedValues = new Vector<String>();
        if (data instanceof SelectMultiData) {
            selections = (Vector<Selection>)data.getValue();
        } else if (data instanceof SelectOneData) {
            selections = new Vector<Selection>();
            selections.addElement((Selection)data.getValue());
        }
        if (itemset.valueRef != null) {
            for (int i = 0; i < selections.size(); i++) {
                selectedValues.addElement(selections.elementAt(i).choice.getValue());
            }
        }

        //delete existing dest nodes that are not in the answer selection
        Hashtable<String, TreeElement> existingValues = new Hashtable<String, TreeElement>();
        Vector<TreeReference> existingNodes = exprEvalContext.expandReference(destRef);
        for (int i = 0; i < existingNodes.size(); i++) {
            TreeElement node = getMainInstance().resolveReference(existingNodes.elementAt(i));

            if (itemset.valueRef != null) {
                String value = itemset.getRelativeValue().evalReadable(this.getMainInstance(), new EvaluationContext(exprEvalContext, node.getRef()));
                if (selectedValues.contains(value)) {
                    existingValues.put(value, node); //cache node if in selection and already exists
                }
            }

            //delete from target
            targetNode.removeChild(node);
        }

        //copy in nodes for new answer; preserve ordering in answer
        for (int i = 0; i < selections.size(); i++) {
            Selection s = selections.elementAt(i);
            SelectChoice ch = s.choice;

            TreeElement cachedNode = null;
            if (itemset.valueRef != null) {
                String value = ch.getValue();
                if (existingValues.containsKey(value)) {
                    cachedNode = existingValues.get(value);
                }
            }

            if (cachedNode != null) {
                cachedNode.setMult(i);
                targetNode.addChild(cachedNode);
            } else {
                getMainInstance().copyItemsetNode(ch.copyNode, destRef, this);
            }
        }

        // trigger conditions that depend on the creation of these new nodes
        triggerTriggerables(destRef);

        // initialize conditions for the node (and sub-nodes)
        initializeTriggerables(destRef);
        // not 100% sure this will work since destRef is ambiguous as the last
        // step, but i think it's supposed to work
    }

    /**
     * Add a Condition to the form's Collection.
     *
     * @param condition the condition to be set
     */
    public Triggerable addTriggerable(Triggerable t) {
        int existingIx = triggerables.indexOf(t);
        if (existingIx != -1) {
            // One node may control access to many nodes; this means many nodes
            // effectively have the same condition. Let's identify when
            // conditions are the same, and store and calculate it only once.

            // nov-2-2011: ctsims - We need to merge the context nodes together
            // whenever we do this (finding the highest common ground between
            // the two), otherwise we can end up failing to trigger when the
            // ignored context exists and the used one doesn't

            Triggerable existingTriggerable = (Triggerable)triggerables.elementAt(existingIx);

            existingTriggerable.contextRef = existingTriggerable.contextRef.intersect(t.contextRef);

            return existingTriggerable;

            // NOTE: if the contextRef is unnecessarily deep, the condition
            // will be evaluated more times than needed. Perhaps detect when
            // 'identical' condition has a shorter contextRef, and use that one
            // instead?
        } else {
            triggerables.addElement(t);
            triggerablesInOrder = false;

            for (TreeReference trigger : t.getTriggers()) {
                if (!triggerIndex.containsKey(trigger)) {
                    triggerIndex.put(trigger.clone(), new Vector<Triggerable>());
                }
                Vector triggered = (Vector)triggerIndex.get(trigger);
                if (!triggered.contains(t)) {
                    triggered.addElement(t);
                }
            }

            return t;
        }
    }

    /**
     * Finalize the DAG associated with the form's triggered conditions. This will create
     * the appropriate ordering and dependencies to ensure the conditions will be evaluated
     * in the appropriate orders.
     *
     * @throws IllegalStateException - If the trigger ordering contains an illegal cycle and the
     *                               triggers can't be laid out appropriately
     */
    public void finalizeTriggerables() throws IllegalStateException {
        //
        //DAGify the triggerables based on dependencies and sort them so that
        //trigbles come only after the trigbles they depend on
        //

        Vector partialOrdering = new Vector();
        for (int i = 0; i < triggerables.size(); i++) {
            Triggerable t = (Triggerable)triggerables.elementAt(i);

            Vector deps = new Vector();
            fillTriggeredElements(t, deps);

            for (int j = 0; j < deps.size(); j++) {
                Triggerable u = (Triggerable)deps.elementAt(j);
                Triggerable[] edge = {t, u};
                partialOrdering.addElement(edge);
            }
        }

        Vector<Triggerable> vertices = new Vector<Triggerable>();
        for (int i = 0; i < triggerables.size(); i++)
            vertices.addElement(triggerables.elementAt(i));
        triggerables.removeAllElements();

        while (vertices.size() > 0) {
            //determine root nodes
            Vector<Triggerable> roots = new Vector<Triggerable>();
            for (int i = 0; i < vertices.size(); i++) {
                roots.addElement(vertices.elementAt(i));
            }
            for (int i = 0; i < partialOrdering.size(); i++) {
                Triggerable[] edge = (Triggerable[])partialOrdering.elementAt(i);
                roots.removeElement(edge[1]);
            }

            //if no root nodes while graph still has nodes, graph has cycles
            if (roots.size() == 0) {
                String hints = "";
                for (Triggerable t : vertices) {
                    for (TreeReference r : t.getTargets()) {
                        hints += "\n" + r.toString(true);
                    }
                }
                String message = "Cycle detected in form's relevant and calculation logic!";
                if (!hints.equals("")) {
                    message += "\nThe following nodes are likely involved in the loop:" + hints;
                }
                throw new IllegalStateException(message);
            }

            //remove root nodes and edges originating from them
            for (int i = 0; i < roots.size(); i++) {
                Triggerable root = (Triggerable)roots.elementAt(i);
                triggerables.addElement(root);
                vertices.removeElement(root);
            }
            for (int i = partialOrdering.size() - 1; i >= 0; i--) {
                Triggerable[] edge = (Triggerable[])partialOrdering.elementAt(i);
                if (roots.contains(edge[0]))
                    partialOrdering.removeElementAt(i);
            }
        }

        triggerablesInOrder = true;

        //
        //build the condition index for repeatable nodes
        //

        conditionRepeatTargetIndex = new Hashtable();
        for (int i = 0; i < triggerables.size(); i++) {
            Triggerable t = (Triggerable)triggerables.elementAt(i);
            if (t instanceof Condition) {
                Vector targets = t.getTargets();
                for (int j = 0; j < targets.size(); j++) {
                    TreeReference target = (TreeReference)targets.elementAt(j);
                    if (mainInstance.getTemplate(target) != null) {
                        conditionRepeatTargetIndex.put(target, (Condition)t);
                    }
                }
            }
        }

    }

    /**
     * Get all of the elements which will need to be evaluated (in order) when the
     * triggerable is fired.
     *
     * @param t
     * @param destination
     */
    public void fillTriggeredElements(Triggerable t, Vector<Triggerable> destination) {
        if (t.canCascade()) {
            for (int j = 0; j < t.getTargets().size(); j++) {
                TreeReference target = (TreeReference)t.getTargets().elementAt(j);
                Vector<TreeReference> updatedNodes = new Vector<TreeReference>();
                updatedNodes.addElement(target);

                //For certain types of triggerables, the update will affect not only the target, but
                //also the children of the target. In that case, we want to add all of those nodes
                //to the list of updated elements as well.
                if (t.isCascadingToChildren()) {
                    addChildrenOfReference(target, updatedNodes);
                }

                //Now go through each of these updated nodes (generally just 1 for a normal calculation,
                //multiple nodes if there's a relevance cascade.
                for (TreeReference ref : updatedNodes) {
                    //Check our index to see if that target is a Trigger for other conditions
                    //IE: if they are an element of a different calculation or relevancy calc

                    //We can't make this reference generic before now or we'll lose the target information,
                    //so we'll be more inclusive than needed and see if any of our triggers are keyed on
                    //the predicate-less path of this ref
                    Vector<Triggerable> triggered = (Vector<Triggerable>)triggerIndex.get(ref.hasPredicates() ? ref.removePredicates() : ref);

                    if (triggered != null) {
                        //If so, walk all of these triggerables that we found
                        for (int k = 0; k < triggered.size(); k++) {
                            Triggerable u = (Triggerable)triggered.elementAt(k);

                            //And add them to the queue if they aren't there already
                            if (!destination.contains(u))
                                destination.addElement(u);
                        }
                    }
                }

            }
        }
    }
    
    /**
     * Enables debug traces in this form, which can be requested as a map after this 
     * call has been performed. Debug traces will be available until they are explicitly
     * disabled.
     * 
     * This call also re-executes all triggerables in debug mode to make their current
     * traces available.
     * 
     * Must be called after this form is initialized.
     * 
     */
    public void enableDebugTraces() {
        if(!mDebugModeEnabled) {
            for (int i = 0; i < triggerables.size(); i++) {
                Triggerable t = (Triggerable)triggerables.elementAt(i);
                t.setDebug(true);
            }
            
            //Re-execute all triggerables to collect traces
            initializeTriggerables();
            mDebugModeEnabled = true;
        }
    }
    
    /**
     * Disable debug tracing for this form. Debug traces will no longer be available after
     * this call.
     */
    public void disableDebugTraces() {
        if(mDebugModeEnabled) {
            for (int i = 0; i < triggerables.size(); i++) {
                Triggerable t = (Triggerable)triggerables.elementAt(i);
                t.setDebug(false);
            }
            mDebugModeEnabled = false;
        }
    }
    
    /**
     * Aggregates a map of evaluation traces collected by the form's triggerables.
     * 
     * @return A mapping from TreeReferences to a set of evaluation traces. The traces 
     * are separated out by triggerable category (calculate, relevant, etc) and represent
     * the execution of the last expression that was executed for that trigger. 
     * 
     * @throws IllegalStateException If debugging has not been enabled.
     */
    public Hashtable<TreeReference, Hashtable<String, EvaluationTrace>> getDebugTraceMap() throws IllegalStateException {
        if(!mDebugModeEnabled) {
            throw new IllegalStateException("Debugging is not enabled");
        }
        
        //TODO: sure would be nice to be able to cache this at some point, but will
        //have to have a way to invalidate by trigger or something
        Hashtable<TreeReference, Hashtable<String, EvaluationTrace>> debugInfo = new Hashtable<TreeReference, Hashtable<String, EvaluationTrace>>();
        
        for (int i = 0; i < triggerables.size(); i++) {
            Triggerable t = (Triggerable)triggerables.elementAt(i);
            
            Hashtable<TreeReference, EvaluationTrace> triggerOutputs = t.getEvaluationTraces();
            
            for(Enumeration e = triggerOutputs.keys() ; e.hasMoreElements();) {
                TreeReference elementRef = (TreeReference)e.nextElement();
                String label = t.getDebugLabel();
                Hashtable<String, EvaluationTrace> traces = debugInfo.get(elementRef);
                if(traces == null) {
                    traces = new Hashtable<String, EvaluationTrace>();
                }
                traces.put(label, triggerOutputs.get(elementRef));
                debugInfo.put(elementRef, traces);
            }
        }

        return debugInfo;
    }


    public void initializeTriggerables() {
        initializeTriggerables(TreeReference.rootRef());
    }

    /**
     * Walks the current set of conditions, and evaluates each of them with the
     * current context.
     */
    private void initializeTriggerables(TreeReference rootRef) {
        TreeReference genericRoot = rootRef.genericize();

        Vector applicable = new Vector();
        for (int i = 0; i < triggerables.size(); i++) {
            Triggerable t = (Triggerable)triggerables.elementAt(i);
            for (int j = 0; j < t.getTargets().size(); j++) {
                TreeReference target = (TreeReference)t.getTargets().elementAt(j);
                if (genericRoot.isParentOf(target, false)) {
                    applicable.addElement(t);
                    break;
                }
            }
        }

        evaluateTriggerables(applicable, rootRef);
    }

    /**
     * The entry point for the DAG cascade after a value is changed in the model.
     *
     * @param ref The full contextualized unambiguous reference of the value that was
     *            changed.
     */
    public void triggerTriggerables(TreeReference ref) {

        //turn unambiguous ref into a generic ref
        //to identify what nodes should be triggered by this
        //reference changing
        TreeReference genericRef = ref.genericize();

        //get triggerables which are activated by the generic reference
        Vector triggered = (Vector)triggerIndex.get(genericRef);
        if (triggered == null) {
            return;
        }

        //Our vector doesn't have a shallow copy op, so make one
        Vector triggeredCopy = new Vector();
        for (int i = 0; i < triggered.size(); i++) {
            triggeredCopy.addElement(triggered.elementAt(i));
        }

        //Evaluate all of the triggerables in our new vector
        evaluateTriggerables(triggeredCopy, ref);
    }

    /**
     * Step 2 in evaluating DAG computation updates from a value being changed in
     * the instance. This step is responsible for taking the root set of directly
     * triggered conditions, identifying which conditions should further be triggered
     * due to their update, and then dispatching all of the evaluations.
     *
     * @param tv        A vector of all of the trigerrables directly triggered by the
     *                  value changed
     * @param anchorRef The reference to original value that was updated
     */
    private void evaluateTriggerables(Vector tv, TreeReference anchorRef) {
        //add all cascaded triggerables to queue

        //Iterate through all of the currently known triggerables to be triggered
        for (int i = 0; i < tv.size(); i++) {
            Triggerable t = (Triggerable)tv.elementAt(i);
            fillTriggeredElements(t, tv);
        }

        //tv should now contain all of the triggerable components which are going to need to be addressed
        //by this update.
        //'triggerables' is topologically-ordered by dependencies, so evaluate the triggerables in 'tv'
        //in the order they appear in 'triggerables'
        for (int i = 0; i < triggerables.size(); i++) {
            Triggerable t = (Triggerable)triggerables.elementAt(i);
            if (tv.contains(t)) {
                evaluateTriggerable(t, anchorRef);
            }
        }
    }

    /**
     * Step 3 in DAG cascade. evaluate the individual triggerable expressions against
     * the anchor (the value that changed which triggered recomputation)
     *
     * @param t         The triggerable to be updated
     * @param anchorRef The reference to the value which was changed.
     */
    private void evaluateTriggerable(Triggerable t, TreeReference anchorRef) {

        //Contextualize the reference used by the triggerable against the anchor
        TreeReference contextRef = t.contextRef.contextualize(anchorRef);

        //Now identify all of the fully qualified nodes which this triggerable
        //updates. (Multiple nodes can be updated by the same trigger)
        Vector<TreeReference> v = exprEvalContext.expandReference(contextRef);

        //Go through each one and evaluate the trigger expresion
        for (int i = 0; i < v.size(); i++) {
            try {
                t.apply(mainInstance, exprEvalContext, v.elementAt(i), this);
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }

    /**
     * This is a utility method to get all of the references of a node. It can be replaced
     * when we support dependent XPath Steps (IE: /path/to//)
     */
    public void addChildrenOfReference(TreeReference original, Vector<TreeReference> toAdd) {
        for (TreeReference ref : exprEvalContext.expandReference(original)) {
            addChildrenOfElement(exprEvalContext.resolveReference(ref), toAdd);
        }
    }

    //Recursive step of utility method
    private void addChildrenOfElement(AbstractTreeElement el, Vector<TreeReference> toAdd) {
        for (int i = 0; i < el.getNumChildren(); ++i) {
            AbstractTreeElement child = el.getChildAt(i);
            toAdd.addElement(child.getRef().genericize());
            addChildrenOfElement(child, toAdd);
        }
        for (int i = 0; i < el.getAttributeCount(); ++i) {
            AbstractTreeElement child = el.getAttribute(el.getAttributeNamespace(i), el.getAttributeName(i));
            toAdd.addElement(child.getRef().genericize());
        }
    }

    public boolean evaluateConstraint(TreeReference ref, IAnswerData data) {
        if (data == null) {
            return true;
        }

        TreeElement node = mainInstance.resolveReference(ref);
        Constraint c = node.getConstraint();

        if (c == null) {
            return true;
        }
        EvaluationContext ec = new EvaluationContext(exprEvalContext, ref);
        ec.isConstraint = true;
        ec.candidateValue = data;

        return c.constraint.eval(mainInstance, ec);
    }

    /**
     * @param ec The new Evaluation Context
     */
    public void setEvaluationContext(EvaluationContext ec) {
        ec = new EvaluationContext(mainInstance, formInstances, ec);
        initEvalContext(ec);
        this.exprEvalContext = ec;
    }

    public EvaluationContext getEvaluationContext() {
        return this.exprEvalContext;
    }

    private void initEvalContext(EvaluationContext ec) {
        if (!ec.getFunctionHandlers().containsKey("jr:itext")) {
            final FormDef f = this;
            ec.addFunctionHandler(new IFunctionHandler() {
                public String getName() {
                    return "jr:itext";
                }

                public Object eval(Object[] args, EvaluationContext ec) {
                    String textID = (String)args[0];
                    try {
                        //SUUUUPER HACKY
                        String form = ec.getOutputTextForm();
                        if (form != null) {
                            textID = textID + ";" + form;
                            String result = f.getLocalizer().getRawText(f.getLocalizer().getLocale(), textID);
                            return result == null ? "" : result;
                        } else {
                            String text = f.getLocalizer().getText(textID);
                            return text == null ? "[itext:" + textID + "]" : text;
                        }
                    } catch (NoSuchElementException nsee) {
                        return "[nolocale]";
                    }
                }

                public Vector getPrototypes() {
                    Class[] proto = {String.class};
                    Vector v = new Vector();
                    v.addElement(proto);
                    return v;
                }

                public boolean rawArgs() {
                    return false;
                }

                public boolean realTime() {
                    return false;
                }
            });
        }

        /* function to reverse a select value into the display label for that choice in the question it came from
         *
         * arg 1: select value
         * arg 2: string xpath referring to origin question; must be absolute path
         *
         * this won't work at all if the original label needed to be processed/calculated in some way (<output>s, etc.) (is this even allowed?)
         * likely won't work with multi-media labels
         * _might_ work for itemsets, but probably not very well or at all; could potentially work better if we had some context info
         * DOES work with localization
         *
         * it's mainly intended for the simple case of reversing a question with compile-time-static fields, for use inside an <output>
         */
        if (!ec.getFunctionHandlers().containsKey("jr:choice-name")) {
            final FormDef f = this;
            ec.addFunctionHandler(new IFunctionHandler() {
                public String getName() {
                    return "jr:choice-name";
                }

                public Object eval(Object[] args, EvaluationContext ec) {
                    try {
                        String value = (String)args[0];
                        String questionXpath = (String)args[1];
                        TreeReference ref = RestoreUtils.xfFact.ref(questionXpath);

                        QuestionDef q = f.findQuestionByRef(ref, f);
                        if (q == null || (q.getControlType() != Constants.CONTROL_SELECT_ONE &&
                                q.getControlType() != Constants.CONTROL_SELECT_MULTI)) {
                            return "";
                        }

                        System.out.println("here!!");

                        Vector<SelectChoice> choices = q.getChoices();
                        for (SelectChoice ch : choices) {
                            if (ch.getValue().equals(value)) {
                                //this is really not ideal. we should hook into the existing code (FormEntryPrompt) for pulling
                                //display text for select choices. however, it's hard, because we don't really have
                                //any context to work with, and all the situations where that context would be used
                                //don't make sense for trying to reverse a select value back to a label in an unrelated
                                //expression

                                String textID = ch.getTextID();
                                if (textID != null) {
                                    return f.getLocalizer().getText(textID);
                                } else {
                                    return ch.getLabelInnerText();
                                }
                            }
                        }
                        return "";
                    } catch (Exception e) {
                        throw new WrappedException("error in evaluation of xpath function [choice-name]", e);
                    }
                }

                public Vector getPrototypes() {
                    Class[] proto = {String.class, String.class};
                    Vector v = new Vector();
                    v.addElement(proto);
                    return v;
                }

                public boolean rawArgs() {
                    return false;
                }

                public boolean realTime() {
                    return false;
                }
            });
        }
    }

    public String fillTemplateString(String template, TreeReference contextRef) {
        return fillTemplateString(template, contextRef, new Hashtable());
    }

    /**
     * Performs substitutions on place-holder template from form text by
     * evaluating args in template using the current context.
     *
     * @param template   String
     * @param contextRef TreeReference
     * @param variables  Hashtable<String, ?>
     * @return String with the all args in the template filled with appropriate
     * context values.
     */
    public String fillTemplateString(String template, TreeReference contextRef, Hashtable<String, ?> variables) {
        // argument to value mapping
        Hashtable<String, String> args = new Hashtable<String, String>();

        int depth = 0;
        // grab all template arguments that need to have substitutions performed
        Vector outstandingArgs = Localizer.getArgs(template);

        String templateAfterSubstitution;

        // Step through outstandingArgs from the template, looking up the value
        // they map to, evaluating that under the evaluation context and
        // storing in the local args mapping.
        // Then perform substitutions over the template until a fixpoint is found
        while (outstandingArgs.size() > 0) {
            for (int i = 0; i < outstandingArgs.size(); i++) {
                String argName = (String)outstandingArgs.elementAt(i);
                // lookup value an arg points to if it isn't in our local mapping
                if (!args.containsKey(argName)) {
                    int ix = -1;
                    try {
                        ix = Integer.parseInt(argName);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Warning: expect arguments to be numeric [" + argName + "]");
                    }

                    if (ix < 0 || ix >= outputFragments.size()) {
                        continue;
                    }

                    IConditionExpr expr = (IConditionExpr)outputFragments.elementAt(ix);
                    EvaluationContext ec = new EvaluationContext(exprEvalContext, contextRef);
                    ec.setOriginalContext(contextRef);
                    ec.setVariables(variables);
                    String value = expr.evalReadable(this.getMainInstance(), ec);
                    args.put(argName, value);
                }
            }

            templateAfterSubstitution = Localizer.processArguments(template, args);

            // The last substitution made no progress, probably because the
            // argument isn't in outputFragments, so stop looping and
            // attempting more subs!
            if (template.equals(templateAfterSubstitution)) {
                return template;
            }

            template = templateAfterSubstitution;

            // Since strings being substituted might themselves have arguments that
            // need to be further substituted, we must recompute the unperformed
            // substitutions and continue to loop.
            outstandingArgs = Localizer.getArgs(template);

            if (depth++ >= TEMPLATING_RECURSION_LIMIT) {
                throw new RuntimeException("Dependency cycle in <output>s; recursion limit exceeded!!");
            }
        }

        return template;
    }

    /**
     * Identify the itemset in the backend model, and create a set of SelectChoice
     * objects at the current question reference based on the data in the model.
     *
     * Will modify the itemset binding to contain the relevant choices
     *
     * @param itemset The binding for an itemset, where the choices will be populated
     * @param curQRef A reference to the current question's element, which will be
     *                used to determine the values to be chosen from.
     */
    public void populateDynamicChoices(ItemsetBinding itemset, TreeReference curQRef) {
        Vector<SelectChoice> choices = new Vector<SelectChoice>();

        Vector<TreeReference> matches = itemset.nodesetExpr.evalNodeset(this.getMainInstance(),
                new EvaluationContext(exprEvalContext, itemset.contextRef.contextualize(curQRef)));

        DataInstance fi = null;
        if (itemset.nodesetRef.getInstanceName() != null) //We're not dealing with the default instance
        {
            fi = getNonMainInstance(itemset.nodesetRef.getInstanceName());
            if (fi == null) {
                throw new XPathException("Instance " + itemset.nodesetRef.getInstanceName() + " not found");
            }
        } else {
            fi = getMainInstance();
        }


        if (matches == null) {
            throw new XPathException("Could not find references depended on by " + itemset.nodesetRef.getInstanceName());
        }

        for (int i = 0; i < matches.size(); i++) {
            TreeReference item = matches.elementAt(i);

            //String label = itemset.labelExpr.evalReadable(this.getMainInstance(), new EvaluationContext(exprEvalContext, item));
            String label = itemset.labelExpr.evalReadable(fi, new EvaluationContext(exprEvalContext, item));
            String value = null;
            TreeElement copyNode = null;

            if (itemset.copyMode) {
                copyNode = this.getMainInstance().resolveReference(itemset.copyRef.contextualize(item));
            }
            if (itemset.valueRef != null) {
                //value = itemset.valueExpr.evalReadable(this.getMainInstance(), new EvaluationContext(exprEvalContext, item));
                value = itemset.valueExpr.evalReadable(fi, new EvaluationContext(exprEvalContext, item));
            }
//            SelectChoice choice = new SelectChoice(labelID,labelInnerText,value,isLocalizable);
            SelectChoice choice = new SelectChoice(label, value != null ? value : "dynamic:" + i, itemset.labelIsItext);
            choice.setIndex(i);
            if (itemset.copyMode)
                choice.copyNode = copyNode;

            choices.addElement(choice);
        }

        if (choices.size() == 0) {
            //throw new RuntimeException("dynamic select question has no choices! [" + itemset.nodesetRef + "]");
            //When you exit a survey mid way through and want to save it, it seems that Collect wants to
            //go through all the questions. Well of course not all the questions are going to have answers
            //to chose from if the user hasn't filled them out. So I'm just going to make a note of this
            //and not throw an exception.
            System.out.println("dynamic multiple choice question has no choices! [" + itemset.nodesetRef + "]. If this didn't occure durring saving an incomplete form, you've got a problem.");

        }

        itemset.setChoices(choices, this.getLocalizer());
    }

    /**
     * @return the preloads
     */
    public QuestionPreloader getPreloader() {
        return preloader;
    }

    /**
     * @param preloads the preloads to set
     */
    public void setPreloader(QuestionPreloader preloads) {
        this.preloader = preloads;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.javarosa.core.model.utils.Localizable#localeChanged(java.lang.String,
     * org.javarosa.core.model.utils.Localizer)
     */
    public void localeChanged(String locale, Localizer localizer) {
        for (Enumeration e = children.elements(); e.hasMoreElements(); ) {
            ((IFormElement)e.nextElement()).localeChanged(locale, localizer);
        }
    }

    public String toString() {
        return getTitle();
    }

    /**
     * Preload the Data Model with the preload values that are enumerated in the
     * data bindings.
     */
    public void preloadInstance(TreeElement node) {
        // if (node.isLeaf()) {
        IAnswerData preload = null;
        if (node.getPreloadHandler() != null) {
            preload = preloader.getQuestionPreload(node.getPreloadHandler(),
                    node.getPreloadParams());
        }
        if (preload != null) { // what if we want to wipe out a value in the
            // instance?
            node.setAnswer(preload);
        }
        // } else {
        if (!node.isLeaf()) {
            for (int i = 0; i < node.getNumChildren(); i++) {
                TreeElement child = node.getChildAt(i);
                if (child.getMult() != TreeReference.INDEX_TEMPLATE)
                    // don't preload templates; new repeats are preloaded as they're created
                    preloadInstance(child);
            }
        }
        // }
    }

    public boolean postProcessInstance() {
        dispatchFormEvent(Action.EVENT_XFORMS_REVALIDATE);
        return postProcessInstance(mainInstance.getRoot());
    }

    /**
     * Iterate over the form's data bindings, and evaluate all post procesing
     * calls.
     *
     * @return true if the instance was modified in any way. false otherwise.
     */
    private boolean postProcessInstance(TreeElement node) {
        // we might have issues with ordering, for example, a handler that writes a value to a node,
        // and a handler that does something external with the node. if both handlers are bound to the
        // same node, we need to make sure the one that alters the node executes first. deal with that later.
        // can we even bind multiple handlers to the same node currently?

        // also have issues with conditions. it is hard to detect what conditions are affected by the actions
        // of the post-processor. normally, it wouldn't matter because we only post-process when we are exiting
        // the form, so the result of any triggered conditions is irrelevant. however, if we save a form in the
        // interim, post-processing occurs, and then we continue to edit the form. it seems like having conditions
        // dependent on data written during post-processing is a bad practice anyway, and maybe we shouldn't support it.

        if (node.isLeaf()) {
            if (node.getPreloadHandler() != null) {
                return preloader.questionPostProcess(node, node.getPreloadHandler(), node.getPreloadParams());
            } else {
                return false;
            }
        } else {
            boolean instanceModified = false;
            for (int i = 0; i < node.getNumChildren(); i++) {
                TreeElement child = node.getChildAt(i);
                if (child.getMult() != TreeReference.INDEX_TEMPLATE)
                    instanceModified |= postProcessInstance(child);
            }
            return instanceModified;
        }
    }

    /**
     * Reads the form definition object from the supplied stream.
     *
     * Requires that the instance has been set to a prototype of the instance that
     * should be used for deserialization.
     *
     * @param dis - the stream to read from.
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void readExternal(DataInputStream dis, PrototypeFactory pf) throws IOException, DeserializationException {
        setID(ExtUtil.readInt(dis));
        setName(ExtUtil.nullIfEmpty(ExtUtil.readString(dis)));
        setTitle((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setChildren((Vector)ExtUtil.read(dis, new ExtWrapListPoly(), pf));
        setInstance((FormInstance)ExtUtil.read(dis, FormInstance.class, pf));

        setLocalizer((Localizer)ExtUtil.read(dis, new ExtWrapNullable(Localizer.class), pf));

        Vector vcond = (Vector)ExtUtil.read(dis, new ExtWrapList(Condition.class), pf);
        for (Enumeration e = vcond.elements(); e.hasMoreElements(); ) {
            addTriggerable((Condition)e.nextElement());
        }
        Vector vcalc = (Vector)ExtUtil.read(dis, new ExtWrapList(Recalculate.class), pf);
        for (Enumeration e = vcalc.elements(); e.hasMoreElements(); ) {
            addTriggerable((Recalculate)e.nextElement());
        }
        finalizeTriggerables();

        outputFragments = (Vector)ExtUtil.read(dis, new ExtWrapListPoly(), pf);

        submissionProfiles = (Hashtable<String, SubmissionProfile>)ExtUtil.read(dis, new ExtWrapMap(String.class, SubmissionProfile.class), pf);

        formInstances = (Hashtable<String, DataInstance>)ExtUtil.read(dis, new ExtWrapMap(String.class, new ExtWrapTagged()), pf);

        eventListeners = (Hashtable<String, Vector<Action>>)ExtUtil.read(dis, new ExtWrapMap(String.class, new ExtWrapListPoly()), pf);

        extensions = (Vector)ExtUtil.read(dis, new ExtWrapListPoly(), pf);

        setEvaluationContext(new EvaluationContext(null));
    }

    /**
     * meant to be called after deserialization and initialization of handlers
     *
     * @param newInstance true if the form is to be used for a new entry interaction,
     *                    false if it is using an existing IDataModel
     */
    public void initialize(boolean newInstance, InstanceInitializationFactory factory) {
        for (Enumeration en = formInstances.keys(); en.hasMoreElements(); ) {
            String instanceId = (String)en.nextElement();
            DataInstance instance = formInstances.get(instanceId);
            instance.initialize(factory, instanceId);
        }
        if (newInstance) {
            // only preload new forms (we may have to revisit this)
            preloadInstance(mainInstance.getRoot());
        }

        if (getLocalizer() != null && getLocalizer().getLocale() == null) {
            getLocalizer().setToDefault();
        }

        //TODO: Hm, not 100% sure that this is right. Maybe we should be
        //using a slightly different event for "First Load" which doesn't
        //get fired again, but always fire this one?
        if (newInstance) {
            dispatchFormEvent(Action.EVENT_XFORMS_READY);
        }

        initializeTriggerables();
    }

    /**
     * Writes the form definition object to the supplied stream.
     *
     * @param dos - the stream to write to.
     * @throws IOException
     */
    public void writeExternal(DataOutputStream dos) throws IOException {
        ExtUtil.writeNumeric(dos, getID());
        ExtUtil.writeString(dos, ExtUtil.emptyIfNull(getName()));
        ExtUtil.write(dos, new ExtWrapNullable(getTitle()));
        ExtUtil.write(dos, new ExtWrapListPoly(getChildren()));
        ExtUtil.write(dos, getMainInstance());
        ExtUtil.write(dos, new ExtWrapNullable(localizer));

        Vector conditions = new Vector();
        Vector recalcs = new Vector();
        for (int i = 0; i < triggerables.size(); i++) {
            Triggerable t = (Triggerable)triggerables.elementAt(i);
            if (t instanceof Condition) {
                conditions.addElement(t);
            } else if (t instanceof Recalculate) {
                recalcs.addElement(t);
            }
        }
        ExtUtil.write(dos, new ExtWrapList(conditions));
        ExtUtil.write(dos, new ExtWrapList(recalcs));

        ExtUtil.write(dos, new ExtWrapListPoly(outputFragments));
        ExtUtil.write(dos, new ExtWrapMap(submissionProfiles));

        //for support of multi-instance forms

        ExtUtil.write(dos, new ExtWrapMap(formInstances, new ExtWrapTagged()));
        ExtUtil.write(dos, new ExtWrapMap(eventListeners, new ExtWrapListPoly()));
        ExtUtil.write(dos, new ExtWrapListPoly(extensions));
    }

    public void collapseIndex(FormIndex index, Vector indexes, Vector multiplicities, Vector elements) {
        if (!index.isInForm()) {
            return;
        }

        IFormElement element = this;
        while (index != null) {
            int i = index.getLocalIndex();
            element = element.getChild(i);

            indexes.addElement(DataUtil.integer(i));
            multiplicities.addElement(DataUtil.integer(index.getInstanceIndex() == -1 ? 0 : index.getInstanceIndex()));
            elements.addElement(element);

            index = index.getNextLevel();
        }
    }

    public FormIndex buildIndex(Vector indexes, Vector multiplicities, Vector elements) {
        FormIndex cur = null;
        Vector curMultiplicities = new Vector();
        for (int j = 0; j < multiplicities.size(); ++j) {
            curMultiplicities.addElement(multiplicities.elementAt(j));
        }

        Vector curElements = new Vector();
        for (int j = 0; j < elements.size(); ++j) {
            curElements.addElement(elements.elementAt(j));
        }

        for (int i = indexes.size() - 1; i >= 0; i--) {
            int ix = ((Integer)indexes.elementAt(i)).intValue();
            int mult = ((Integer)multiplicities.elementAt(i)).intValue();

            if (!(elements.elementAt(i) instanceof GroupDef && ((GroupDef)elements.elementAt(i)).getRepeat())) {
                mult = -1;
            }

            cur = new FormIndex(cur, ix, mult, getChildInstanceRef(curElements, curMultiplicities));
            curMultiplicities.removeElementAt(curMultiplicities.size() - 1);
            curElements.removeElementAt(curElements.size() - 1);
        }
        return cur;
    }


    public int getNumRepetitions(FormIndex index) {
        Vector indexes = new Vector();
        Vector multiplicities = new Vector();
        Vector elements = new Vector();

        if (!index.isInForm()) {
            throw new RuntimeException("not an in-form index");
        }

        collapseIndex(index, indexes, multiplicities, elements);

        if (!(elements.lastElement() instanceof GroupDef) || !((GroupDef)elements.lastElement()).getRepeat()) {
            throw new RuntimeException("current element not a repeat");
        }

        //so painful
        TreeElement templNode = mainInstance.getTemplate(index.getReference());
        TreeReference parentPath = templNode.getParent().getRef().genericize();
        TreeElement parentNode = mainInstance.resolveReference(parentPath.contextualize(index.getReference()));
        return parentNode.getChildMultiplicity(templNode.getName());
    }

    //repIndex == -1 => next repetition about to be created
    public FormIndex descendIntoRepeat(FormIndex index, int repIndex) {
        int numRepetitions = getNumRepetitions(index);

        Vector indexes = new Vector();
        Vector multiplicities = new Vector();
        Vector elements = new Vector();
        collapseIndex(index, indexes, multiplicities, elements);

        if (repIndex == -1) {
            repIndex = numRepetitions;
        } else {
            if (repIndex < 0 || repIndex >= numRepetitions) {
                throw new RuntimeException("selection exceeds current number of repetitions");
            }
        }

        multiplicities.setElementAt(DataUtil.integer(repIndex), multiplicities.size() - 1);

        return buildIndex(indexes, multiplicities, elements);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.javarosa.core.model.IFormElement#getDeepChildCount()
     */
    public int getDeepChildCount() {
        int total = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            total += ((IFormElement)e.nextElement()).getDeepChildCount();
        }
        return total;
    }

    public void registerStateObserver(FormElementStateListener qsl) {
        // NO. (Or at least not yet).
    }

    public void unregisterStateObserver(FormElementStateListener qsl) {
        // NO. (Or at least not yet).
    }

    public Vector getChildren() {
        return children;
    }

    public void setChildren(Vector children) {
        this.children = (children == null ? new Vector() : children);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Localizer getLocalizer() {
        return localizer;
    }

    public Vector getOutputFragments() {
        return outputFragments;
    }

    public void setOutputFragments(Vector outputFragments) {
        this.outputFragments = outputFragments;
    }

    public Hashtable getMetaData() {
        Hashtable metadata = new Hashtable();
        String[] fields = getMetaDataFields();

        for (int i = 0; i < fields.length; i++) {
            try {
                metadata.put(fields[i], getMetaData(fields[i]));
            } catch (NullPointerException npe) {
                if (getMetaData(fields[i]) == null) {
                    System.out.println("ERROR! XFORM MUST HAVE A NAME!");
                    npe.printStackTrace();
                }
            }
        }

        return metadata;
    }

    public Object getMetaData(String fieldName) {
        if (fieldName.equals("DESCRIPTOR")) {
            return name;
        }
        if (fieldName.equals("XMLNS")) {
            return ExtUtil.emptyIfNull(mainInstance.schema);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public String[] getMetaDataFields() {
        return new String[]{"DESCRIPTOR", "XMLNS"};
    }

    /**
     * Link a deserialized instance back up with its parent FormDef. this allows select/select1 questions to be
     * internationalizable in chatterbox, and (if using CHOICE_INDEX mode) allows the instance to be serialized
     * to xml
     */
    public void attachControlsToInstanceData() {
        attachControlsToInstanceData(getMainInstance().getRoot());
    }

    private void attachControlsToInstanceData(TreeElement node) {
        for (int i = 0; i < node.getNumChildren(); i++) {
            attachControlsToInstanceData(node.getChildAt(i));
        }

        IAnswerData val = node.getValue();
        Vector selections = null;
        if (val instanceof SelectOneData) {
            selections = new Vector();
            selections.addElement(val.getValue());
        } else if (val instanceof SelectMultiData) {
            selections = (Vector)val.getValue();
        }

        if (selections != null) {
            QuestionDef q = findQuestionByRef(node.getRef(), this);
            if (q == null) {
                throw new RuntimeException("FormDef.attachControlsToInstanceData: can't find question to link");
            }

            if (q.getDynamicChoices() != null) {
                //droos: i think we should do something like initializing the itemset here, so that default answers
                //can be linked to the selectchoices. however, there are complications. for example, the itemset might
                //not be ready to be evaluated at form initialization; it may require certain questions to be answered
                //first. e.g., if we evaluate an itemset and it has no choices, the xform engine will throw an error
                //itemset TODO
            }

            for (int i = 0; i < selections.size(); i++) {
                Selection s = (Selection)selections.elementAt(i);
                s.attachChoice(q);
            }
        }
    }

    public static QuestionDef findQuestionByRef(TreeReference ref, IFormElement fe) {
        if (fe instanceof FormDef) {
            ref = ref.genericize();
        }

        if (fe instanceof QuestionDef) {
            QuestionDef q = (QuestionDef)fe;
            TreeReference bind = FormInstance.unpackReference(q.getBind());
            return (ref.equals(bind) ? q : null);
        } else {
            for (int i = 0; i < fe.getChildren().size(); i++) {
                QuestionDef ret = findQuestionByRef(ref, fe.getChild(i));
                if (ret != null)
                    return ret;
            }
            return null;
        }
    }


    /**
     * Appearance isn't a valid attribute for form, but this method must be included
     * as a result of conforming to the IFormElement interface.
     */
    public String getAppearanceAttr() {
        throw new RuntimeException("This method call is not relevant for FormDefs getAppearanceAttr ()");
    }

    /**
     * Appearance isn't a valid attribute for form, but this method must be included
     * as a result of conforming to the IFormElement interface.
     */
    public void setAppearanceAttr(String appearanceAttr) {
        throw new RuntimeException("This method call is not relevant for FormDefs setAppearanceAttr()");
    }

    /**
     * Not applicable here.
     */
    public String getLabelInnerText() {
        return null;
    }

    /**
     * Not applicable
     */
    public String getTextID() {
        return null;
    }

    /**
     * Not applicable
     */
    public void setTextID(String textID) {
        throw new RuntimeException("This method call is not relevant for FormDefs [setTextID()]");
    }


    public void setDefaultSubmission(SubmissionProfile profile) {
        submissionProfiles.put(DEFAULT_SUBMISSION_PROFILE, profile);
    }

    public void addSubmissionProfile(String submissionId, SubmissionProfile profile) {
        submissionProfiles.put(submissionId, profile);
    }

    public SubmissionProfile getSubmissionProfile() {
        //At some point these profiles will be set by the <submit> control in the form.
        //In the mean time, though, we can only promise that the default one will be used.

        return submissionProfiles.get(DEFAULT_SUBMISSION_PROFILE);
    }

    public Vector<Action> getEventListeners(String event) {
        if (this.eventListeners.containsKey(event)) {
            return eventListeners.get(event);
        }
        return new Vector<Action>();
    }

    public void registerEventListener(String event, Action action) {
        Vector<Action> actions;

        if (this.eventListeners.containsKey(event)) {
            actions = eventListeners.get(event);
        } else {
            actions = new Vector<Action>();
        }
        actions.addElement(action);
        this.eventListeners.put(event, actions);
    }

    public void dispatchFormEvent(String event) {
        for (Action action : getEventListeners(event)) {
            action.processAction(this, null);
        }
    }

    public <X extends XFormExtension> X getExtension(Class<X> extension) {
        for (XFormExtension ex : extensions) {
            if (ex.getClass().isAssignableFrom(extension)) {
                return (X)ex;
            }
        }
        X newEx;
        try {
            newEx = extension.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Illegally Structured XForm Extension " + extension.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Illegally Structured XForm Extension " + extension.getName());
        }
        extensions.addElement(newEx);
        return newEx;
    }


    /**
     * Frees all of the components of this form which are no longer needed once it is completed.
     *
     * Once this is called, the form is no longer capable of functioning, but all data should be retained.
     */
    public void seal() {
        triggerables = null;
        triggerIndex = null;
        conditionRepeatTargetIndex = null;
        //We may need ths one, actually
        exprEvalContext = null;
    }
}
