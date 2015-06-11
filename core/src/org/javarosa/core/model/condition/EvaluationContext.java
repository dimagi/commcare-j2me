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

package org.javarosa.core.model.condition;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.model.utils.CacheHost;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * A collection of objects that affect the evaluation of an expression, like
 * function handlers and (not supported) variable bindings.
 */
public class EvaluationContext {
    /**
     * Whether XPath expressions being evaluated should be traced during
     * execution for debugging.
     */
    private boolean mAccumulateExprs = false;

    /**
     * During debugging this context is the base that holds the trace root and
     * aggregates ongoing execution.
     */
    private EvaluationContext mDebugCore;

    /**
     * The current execution trace being evaluated in debug mode
     */
    private EvaluationTrace mCurrentTraceLevel = null;

    /**
     * The root of the current execution trace
     */
    private EvaluationTrace mTraceRoot = null;

    // Unambiguous anchor reference for relative paths
    private TreeReference contextNode;

    private Hashtable functionHandlers;
    private Hashtable variables;

    // Do we want to evaluate constraints?
    public boolean isConstraint;

    // validate this value when isConstraint is set
    public IAnswerData candidateValue;

    // Responsible for informing itext what form is requested if relevant
    private String outputTextForm = null;

    private Hashtable<String, DataInstance> formInstances;

    // original context reference used for evaluating current()
    private TreeReference original;

    /**
     * What element in a nodeset is the context currently pointing to?
     * Used for calculating the position() xpath function.
     */
    private int currentContextPosition = -1;

    DataInstance instance;

    /**
     * Two-element array to keep track of how many candidate references have
     * had their (complex) predicates evaluated during reference expansion.
     *
     * 1st element counts how refs have been processed, 2nd counts total
     * references with (complex) predicates. Complex meaning not handled by
     * tryBatchChildFetch.
     */
    int[] predicateEvaluationProgress;

    /**
     * Copy Constructor
     */
    private EvaluationContext(EvaluationContext base) {
        //TODO: These should be deep, not shallow
        this.functionHandlers = base.functionHandlers;
        this.formInstances = base.formInstances;
        this.variables = new Hashtable();

        //TODO: this is actually potentially much slower than
        //our old strategy (but is needed for this object to
        //be threadsafe). We should evaluate the potential impact.
        this.setVariables(base.variables);

        this.contextNode = base.contextNode;
        this.instance = base.instance;

        this.isConstraint = base.isConstraint;
        this.candidateValue = base.candidateValue;

        this.outputTextForm = base.outputTextForm;
        this.original = base.original;

        //Hrm....... not sure about this one. this only happens after a rescoping,
        //and is fixed on the context. Anything that changes the context should
        //invalidate this
        this.currentContextPosition = base.currentContextPosition;

        if (base.mAccumulateExprs) {
            this.mAccumulateExprs = true;
            this.mDebugCore = base.mDebugCore;
        }
    }

    public EvaluationContext(EvaluationContext base, TreeReference context) {
        this(base);
        this.contextNode = context;
    }

    public EvaluationContext(EvaluationContext base, Hashtable<String, DataInstance> formInstances, TreeReference context) {
        this(base, context);
        this.formInstances = formInstances;
    }

    public EvaluationContext(FormInstance instance, Hashtable<String, DataInstance> formInstances, EvaluationContext base) {
        this(base);
        this.formInstances = formInstances;
        this.instance = instance;
    }

    public EvaluationContext(DataInstance instance) {
        this(instance, new Hashtable<String, DataInstance>());
    }

    public EvaluationContext(DataInstance instance, Hashtable<String, DataInstance> formInstances) {
        this.formInstances = formInstances;
        this.instance = instance;
        this.contextNode = TreeReference.rootRef();
        functionHandlers = new Hashtable();
        variables = new Hashtable();
    }

    public DataInstance getInstance(String id) {
        return formInstances.containsKey(id) ? formInstances.get(id) : null;
    }

    public TreeReference getContextRef() {
        return contextNode;
    }

    public void setOriginalContext(TreeReference ref) {
        this.original = ref;
    }

    public TreeReference getOriginalContext() {
        if (this.original == null) {
            return this.contextNode;
        } else {
            return this.original;
        }
    }

    public void addFunctionHandler(IFunctionHandler fh) {
        functionHandlers.put(fh.getName(), fh);
    }

    public Hashtable getFunctionHandlers() {
        return functionHandlers;
    }

    public void setOutputTextForm(String form) {
        this.outputTextForm = form;
    }

    public String getOutputTextForm() {
        return outputTextForm;
    }

    public void setVariables(Hashtable<String, ?> variables) {
        for (Enumeration e = variables.keys(); e.hasMoreElements(); ) {
            String var = (String)e.nextElement();
            setVariable(var, variables.get(var));
        }
    }

    public void setVariable(String name, Object value) {
        //No such thing as a null xpath variable. Empty
        //values in XPath just get converted to ""
        if (value == null) {
            variables.put(name, "");
            return;
        }
        //Otherwise check whether the value is one of the normal first
        //order datatypes used in xpath evaluation
        if (value instanceof Boolean ||
                value instanceof Double ||
                value instanceof String ||
                value instanceof Date ||
                value instanceof IExprDataType) {
            variables.put(name, value);
            return;
        }

        //Some datatypes can be trivially converted to a first order
        //xpath datatype
        if (value instanceof Integer) {
            variables.put(name, new Double(((Integer)value).doubleValue()));
            return;
        }
        if (value instanceof Float) {
            variables.put(name, new Double(((Float)value).doubleValue()));
            return;
        }

        //Otherwise we just hope for the best, I suppose? Should we log this?
        else {
            variables.put(name, value);
        }
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public Vector<TreeReference> expandReference(TreeReference ref) {
        return expandReference(ref, false);
    }

    /**
     * Search for all repeated nodes that match the pattern of the 'ref'
     * argument.
     *
     * '/' returns {'/'}
     * can handle sub-repetitions (e.g., {/a[1]/b[1], /a[1]/b[2], /a[2]/b[1]})
     *
     * @param ref              Potentially ambiguous reference
     * @param includeTemplates
     * @return Null if 'ref' is relative reference. Otherwise, returns a vector
     * of references that point to nodes that match 'ref' argument. These
     * references are unambiguous (no index will ever be INDEX_UNBOUND) template
     * nodes won't be included when matching INDEX_UNBOUND, but will be when
     * INDEX_TEMPLATE is explicitly set.
     */
    public Vector<TreeReference> expandReference(TreeReference ref, boolean includeTemplates) {
        if (!ref.isAbsolute()) {
            return null;
        }

        DataInstance baseInstance = retrieveInstance(ref);
        Vector<TreeReference> v = new Vector<TreeReference>();
        expandReferenceAccumulator(ref, baseInstance, baseInstance.getRoot().getRef(), v, includeTemplates);
        return v;
    }

    /**
     * Recursive helper function for expandReference that performs the search
     * for all repeated nodes that match the pattern of the 'ref' argument.
     *
     * @param sourceRef        original path we're matching against
     * @param sourceInstance   original node obtained from sourceRef
     * @param workingRef       explicit path that refers to the current node
     * @param refs             Accumulator vector to collect matching paths. Contained
     *                         references are unambiguous. Template nodes won't be included when
     *                         matching INDEX_UNBOUND, but will be when INDEX_TEMPLATE is explicitly
     *                         set.
     * @param includeTemplates
     */
    private void expandReferenceAccumulator(TreeReference sourceRef, DataInstance sourceInstance,
                                            TreeReference workingRef, Vector<TreeReference> refs,
                                            boolean includeTemplates) {
        int depth = workingRef.size();

        if (depth == sourceRef.size()) {
            // We've matched fully
            //TODO: Should this reference be cloned?
            refs.addElement(workingRef);
            return;
        }
        // Get the next set of matching references
        String name = sourceRef.getName(depth);
        int mult = sourceRef.getMultiplicity(depth);
        Vector<XPathExpression> predicates = sourceRef.getPredicate(depth);

        // Batch fetch is going to mutate the predicates vector, create a copy
        if (predicates != null) {
            Vector<XPathExpression> predCopy = new Vector<XPathExpression>(predicates.size());
            for (XPathExpression xpe : predicates) {
                predCopy.addElement(xpe);
            }
            predicates = predCopy;
        }

        AbstractTreeElement node = sourceInstance.resolveReference(workingRef);

        // Use the reference's simple predicates to filter the potential
        // nodeset.  Predicates used in filtering are removed from the
        // predicate input argument.
        Vector<TreeReference> childSet = node.tryBatchChildFetch(name, mult, predicates, this);

        if (childSet == null) {
            childSet = loadReferencesChildren(node, name, mult, includeTemplates);
        }

        if (predicates != null && predicates.size() > 0) {
            // child references need to be filtered over remaining predicates
            incRefsToFilterCount(childSet.size());
        }

        // Create a place to store the current position markers
        int[] positionContext = new int[predicates == null ? 0 : predicates.size()];

        for (TreeReference refToExpand : childSet) {
            boolean passedAll = true;
            if (predicates != null && predicates.size() > 0) {
                // Evaluate and filter predicates not processed by
                // tryBatchChildFetch
                int predIndex = -1;
                for (XPathExpression predExpr : predicates) {
                    predIndex++;
                    // Just by getting here we're establishing a position for
                    // evaluating the current context. If we break, we won't
                    // push up the next one
                    positionContext[predIndex]++;

                    EvaluationContext evalContext = rescope(refToExpand, positionContext[predIndex]);
                    Object o = predExpr.eval(sourceInstance, evalContext);
                    o = XPathFuncExpr.unpack(o);

                    boolean passed = false;
                    if (o instanceof Double) {
                        // If a predicate expression is just an Integer, check
                        // if its equal to the current position context

                        // The spec just says "number" for when to use this;
                        // Not clear what to do with a non-integer/rounding.
                        int intVal = XPathFuncExpr.toInt(o).intValue();
                        passed = (intVal == positionContext[predIndex]);
                    } else if (o instanceof Boolean) {
                        passed = (Boolean)o;
                    }

                    if (!passed) {
                        passedAll = false;
                        break;
                    }
                }
                incRefsFilteredCount();
            }
            if (passedAll) {
                expandReferenceAccumulator(sourceRef, sourceInstance, refToExpand, refs, includeTemplates);
            }
        }
    }

    /**
     * Gather references to a nodes children with a specific name and
     * multiplicity.
     *
     * @param node             Element of which to collect child references.
     * @param childName        Only collect child references with this name.
     * @param childMult        Collect a particular element/attribute or unbounded.
     * @param includeTemplates Should the result include template elements?
     * @return A list of references to a node's children that have a given name
     * and multiplicity.
     */
    private Vector<TreeReference> loadReferencesChildren(AbstractTreeElement node,
                                                         String childName,
                                                         int childMult,
                                                         boolean includeTemplates) {
        Vector<TreeReference> childSet = new Vector<TreeReference>();
        if (node.hasChildren()) {
            if (childMult == TreeReference.INDEX_UNBOUND) {
                int count = node.getChildMultiplicity(childName);
                for (int i = 0; i < count; i++) {
                    AbstractTreeElement child = node.getChild(childName, i);
                    if (child != null) {
                        childSet.addElement(child.getRef());
                    } else {
                        throw new IllegalStateException("Missing or non-sequential nodes expanding a reference");
                    }
                }
                // TODO: this could be lifted out a conditional level, right? -- PLM
                if (includeTemplates) {
                    AbstractTreeElement template = node.getChild(childName, TreeReference.INDEX_TEMPLATE);
                    if (template != null) {
                        childSet.addElement(template.getRef());
                    }
                }
            } else if (childMult != TreeReference.INDEX_ATTRIBUTE) {
                // TODO: Make this test childMult >= 0?
                // If the multiplicity is a simple integer, just get the
                // appropriate child
                AbstractTreeElement child = node.getChild(childName, childMult);
                if (child != null) {
                    childSet.addElement(child.getRef());
                }
            }
        }

        // Working reference points to an attribute; add it to set to
        // process
        if (childMult == TreeReference.INDEX_ATTRIBUTE) {
            AbstractTreeElement attribute = node.getAttribute(null, childName);
            if (attribute != null) {
                childSet.addElement(attribute.getRef());
            }
        }
        return childSet;
    }

    /**
     * Create a copy of the evaluation context, with a new context ref.
     *
     * When determining what the original reference field of the new object
     * should be:
     * - Use the 'original' field from the original object.
     * - If it is unset, use the original objects context reference.
     * - If that is '/' then use the new context reference
     *
     * @param newContextRef      the new context anchor reference
     * @param newContextPosition the new position of the context (in a repeat
     *                           group)
     * @return a copy of this evaluation context, with a new context reference
     * set and the original context reference correspondingly updated.
     */
    private EvaluationContext rescope(TreeReference newContextRef, int newContextPosition) {
        EvaluationContext ec = new EvaluationContext(this, newContextRef);
        ec.currentContextPosition = newContextPosition;

        // If we have an original context reference, use it
        if (this.original != null) {
            ec.setOriginalContext(this.getOriginalContext());
        } else {
            // Otherwise, if the old context reference isn't '/', use that.If
            // the context ref is '/', use the new context ref as the original
            if (!TreeReference.rootRef().equals(this.getContextRef())) {
                ec.setOriginalContext(this.getContextRef());
            } else {
                // Otherwise propagate the original context reference field
                // with the new context reference argument
                ec.setOriginalContext(newContextRef);
            }
        }
        return ec;
    }

    public DataInstance getMainInstance() {
        return instance;
    }

    public AbstractTreeElement resolveReference(TreeReference qualifiedRef) {
        DataInstance instance = this.getMainInstance();
        if (qualifiedRef.getInstanceName() != null && (instance == null || instance.getInstanceId() != qualifiedRef.getInstanceName())) {
            instance = this.getInstance(qualifiedRef.getInstanceName());
        }
        return instance.resolveReference(qualifiedRef);
    }

    /**
     * The context's current position in terms the nodes available for the
     * context's path. I.e. if the context points to the 3rd node that /a/b/c
     * resolves to, then the current position is 3.
     */
    public int getContextPosition() {
        return currentContextPosition;
    }

    /**
     * Point the local progress tracking array to the address passed in. Used
     * to enable processes that call expandReference to keep track of
     * predicates evaluation over candidate reference results.
     */
    public void setPredicateProcessSet(int[] loadingDetails) {
        if (loadingDetails != null && loadingDetails.length == 2) {
            predicateEvaluationProgress = loadingDetails;
        }
    }

    /**
     * Increment the amount of references left to filter during reference
     * expansion.
     */
    private void incRefsToFilterCount(int amount) {
        if (predicateEvaluationProgress != null) {
            predicateEvaluationProgress[1] += amount;
        }
    }

    /**
     * Increment the amount of references that have been filtered during
     * reference expansion.
     */
    private void incRefsFilteredCount() {
        if (predicateEvaluationProgress != null) {
            predicateEvaluationProgress[0]++;
        }
    }


    /**
     * Get the relevant cache host for the provided ref, if one exists.
     *
     * @param ref
     * @return
     */
    public CacheHost getCacheHost(TreeReference ref) {
        DataInstance instance = retrieveInstance(ref);
        if (instance == null) {
            return null;
        }
        CacheHost host = instance.getCacheHost();
        return host;
    }

    /**
     * Get the instance of the reference argument, if it's present in this
     * context's form instances. Otherwise returns the main instance of this
     * evaluation context.
     *
     * @param ref retreive the instance of this reference, if loaded in the
     *            context
     * @return the instance that the reference argument names, if loaded,
     * otherwise the main instance if present.
     */
    private DataInstance retrieveInstance(TreeReference ref) {
        if (ref.getInstanceName() != null &&
                formInstances.containsKey(ref.getInstanceName())) {
            return formInstances.get(ref.getInstanceName());
        } else if (instance != null) {
            return instance;
        }

        throw new RuntimeException("Unable to expand reference " +
                ref.toString(true) +
                ", no appropriate instance in evaluation context");
    }

    /**
     * Creates a record that an expression is about to be evaluated.
     *
     * @param xPathExpression the expression being evaluated
     */
    public void openTrace(XPathExpression xPathExpression) {
        if (mAccumulateExprs) {
            String expressionString = xPathExpression.toPrettyString();
            EvaluationTrace newLevel = new EvaluationTrace(expressionString,
                    mDebugCore.mCurrentTraceLevel);
            if (mDebugCore.mCurrentTraceLevel != null) {
                mDebugCore.mCurrentTraceLevel.addSubTrace(newLevel);
            }

            mDebugCore.mCurrentTraceLevel = newLevel;
        }
    }

    /**
     * Closes the current evaluation trace and records the
     * relevant outcomes and context
     *
     * @param value The result of the current trace expression
     */
    public void closeTrace(Object value) {
        if (mAccumulateExprs) {
            // Lazy nodeset evaluation makes it impossible for the trace to
            // record predicate subexpressions properly, so trigger that
            // evaluation now.
            if (value instanceof XPathLazyNodeset) {
                ((XPathLazyNodeset)value).size();
            }

            mDebugCore.mCurrentTraceLevel.setOutcome(value);

            if (mDebugCore.mCurrentTraceLevel.getParent() == null) {
                mDebugCore.mTraceRoot = mDebugCore.mCurrentTraceLevel;
            }
            mDebugCore.mCurrentTraceLevel = mDebugCore.mCurrentTraceLevel.getParent();
        }
    }

    /**
     * Sets this EC to be the base of a trace capture for debugging.
     */
    public void setDebugModeOn() {
        this.mAccumulateExprs = true;
        this.mDebugCore = this;
    }


    /**
     * @return the trace of the expression evaluation that was performed
     * against this context.
     */
    public EvaluationTrace getEvaluationTrace() {
        return mTraceRoot;
    }
}
