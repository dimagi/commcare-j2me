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
package org.javarosa.core.model.instance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormElementStateListener;
import org.javarosa.core.model.condition.Constraint;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.data.AnswerDataFactory;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.model.instance.utils.TreeUtilities;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathPathExpr;

/**
 * An element of a FormInstance.
 *
 * TreeElements represent an XML node in the instance. It may either have a value (e.g., <name>Drew</name>),
 * a number of TreeElement children (e.g., <meta><device /><timestamp /><user_id /></meta>), or neither (e.g.,
 * <empty_node />)
 *
 * TreeElements can also represent attributes. Attributes are unique from normal elements in that they are
 * not "children" of their parent, and are always leaf nodes: IE cannot have children.
 *
 * TODO: Split out the bind-able session data from this class and leave only the mandatory values to speed up
 * new DOM-like models
 *
 * @author Clayton Sims
 */

public class TreeElement implements Externalizable, AbstractTreeElement<TreeElement> {
    protected String name; // can be null only for hidden root node
    protected int multiplicity = -1; // see TreeReference for special values
    protected AbstractTreeElement parent;


    protected IAnswerData value;

    //I made all of these null again because there are so many treeelements that they
    //take up a huuuge amount of space together.
    private Vector observers = null;
    protected Vector<TreeElement> attributes = null;
    protected Vector children = null;

    /* model properties */
    protected int dataType = Constants.DATATYPE_NULL; //TODO

    private Constraint constraint = null;
    private String preloadHandler = null;
    private String preloadParams = null;

    //private boolean required = false;// TODO
    //protected boolean repeatable;
    //protected boolean isAttribute;
    //private boolean relevant = true;
    //private boolean enabled = true;
    // inherited properties
    //private boolean relevantInherited = true;
    //private boolean enabledInherited = true;

    private static final int MASK_REQUIRED = 0x01;
    private static final int MASK_REPEATABLE = 0x02;
    private static final int MASK_ATTRIBUTE = 0x04;
    private static final int MASK_RELEVANT = 0x08;
    private static final int MASK_ENABLED = 0x10;
    private static final int MASK_RELEVANT_INH = 0x20;
    private static final int MASK_ENABLED_INH = 0x40;

    private int flags = MASK_RELEVANT | MASK_ENABLED | MASK_RELEVANT_INH | MASK_ENABLED_INH;

    protected String namespace;

    private String instanceName = null;

    /**
     * TreeElement with null name and 0 multiplicity? (a "hidden root" node?)
     */
    public TreeElement() {
        this(null, TreeReference.DEFAULT_MUTLIPLICITY);
    }

    public TreeElement(String name) {
        this(name, TreeReference.DEFAULT_MUTLIPLICITY);
    }

    public TreeElement(String name, int multiplicity) {
        this.name = name;
        this.multiplicity = multiplicity;
        this.parent = null;
    }

    /**
     * Construct a TreeElement which represents an attribute with the provided
     * namespace and name.
     *
     * @param namespace
     * @param name
     * @return A new instance of a TreeElement
     */
    public static TreeElement constructAttributeElement(String namespace, String name) {
        TreeElement element = new TreeElement(name);
        element.setIsAttribute(true);
        element.namespace = namespace;
        element.multiplicity = TreeReference.INDEX_ATTRIBUTE;
        return element;
    }

    private void setIsAttribute(boolean attribute) {
        setMaskVar(MASK_ATTRIBUTE, attribute);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
     */
    public boolean isLeaf() {
        return (children == null || children.size() == 0);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isChildable()
     */
    public boolean isChildable() {
        return (value == null);
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getInstanceName()
     */
    public String getInstanceName() {
        //CTS: I think this is a better way to do this, although I really, really don't like the duplicated code
        if (parent != null) {
            return parent.getInstanceName();
        }
        return instanceName;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setInstanceName(java.lang.String)
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setValue(org.javarosa.core.model.data.IAnswerData)
     */
    public void setValue(IAnswerData value) {
        if (isLeaf()) {
            this.value = value;
        } else {
            throw new RuntimeException("Can't set data value for node that has children!");
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
     */
    public TreeElement getChild(String name, int multiplicity) {
        if (this.children == null) {
            return null;
        }

        if (name.equals(TreeReference.NAME_WILDCARD)) {
            if (multiplicity == TreeReference.INDEX_TEMPLATE || this.children.size() < multiplicity + 1) {
                return null;
            }
            return (TreeElement)this.children.elementAt(multiplicity); //droos: i'm suspicious of this
        } else {
            for (int i = 0; i < this.children.size(); i++) {
                TreeElement child = (TreeElement)this.children.elementAt(i);
                if (((name.hashCode() == child.getName().hashCode()) || name.equals(child.getName())) && child.getMult() == multiplicity) {
                    return child;
                }
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
     */
    public Vector<TreeElement> getChildrenWithName(String name) {
        return getChildrenWithName(name, false);
    }

    private Vector<TreeElement> getChildrenWithName(String name, boolean includeTemplate) {
        Vector<TreeElement> v = new Vector<TreeElement>();
        if (children == null) {
            return v;
        }

        for (int i = 0; i < this.children.size(); i++) {
            TreeElement child = (TreeElement)this.children.elementAt(i);
            if ((child.getName().equals(name) || name.equals(TreeReference.NAME_WILDCARD))
                    && (includeTemplate || child.multiplicity != TreeReference.INDEX_TEMPLATE))
                v.addElement(child);
        }

        return v;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
     */
    public int getNumChildren() {
        return children == null ? 0 : this.children.size();
    }

    public boolean hasChildren() {
        if (getNumChildren() > 0) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
     */
    public TreeElement getChildAt(int i) {
        return (TreeElement)children.elementAt(i);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRepeatable()
     */
    public boolean isRepeatable() {
        return getMaskVar(MASK_REPEATABLE);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isAttribute()
     */
    public boolean isAttribute() {
        return getMaskVar(MASK_ATTRIBUTE);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setDataType(int)
     */
    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#addChild(org.javarosa.core.model.instance.TreeElement)
     */
    public void addChild(TreeElement child) {
        addChild(child, false);
    }

    private void addChild(TreeElement child, boolean checkDuplicate) {
        if (!isChildable()) {
            throw new RuntimeException("Can't add children to node that has data value!");
        }

        if (child.multiplicity == TreeReference.INDEX_UNBOUND) {
            throw new RuntimeException("Cannot add child with an unbound index!");
        }

        if (checkDuplicate) {
            TreeElement existingChild = getChild(child.name, child.multiplicity);
            if (existingChild != null) {
                throw new RuntimeException("Attempted to add duplicate child!");
            }
        }
        if (children == null) {
            children = new Vector();
        }

        // try to keep things in order
        int i = children.size();
        if (child.getMult() == TreeReference.INDEX_TEMPLATE) {
            TreeElement anchor = getChild(child.getName(), 0);
            if (anchor != null)
                i = children.indexOf(anchor);
        } else {
            TreeElement anchor = getChild(child.getName(),
                    (child.getMult() == 0 ? TreeReference.INDEX_TEMPLATE : child.getMult() - 1));
            if (anchor != null)
                i = children.indexOf(anchor) + 1;
        }
        children.insertElementAt(child, i);
        child.setParent(this);

        child.setRelevant(isRelevant(), true);
        child.setEnabled(isEnabled(), true);
        child.setInstanceName(getInstanceName());
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChild(org.javarosa.core.model.instance.TreeElement)
     */
    public void removeChild(TreeElement child) {
        if (children == null) {
            return;
        }
        children.removeElement(child);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChild(java.lang.String, int)
     */
    public void removeChild(String name, int multiplicity) {
        TreeElement child = getChild(name, multiplicity);
        if (child != null) {
            removeChild(child);
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChildren(java.lang.String)
     */
    public void removeChildren(String name) {
        removeChildren(name, false);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChildren(java.lang.String, boolean)
     */
    public void removeChildren(String name, boolean includeTemplate) {
        Vector v = getChildrenWithName(name, includeTemplate);
        for (int i = 0; i < v.size(); i++) {
            removeChild((TreeElement)v.elementAt(i));
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#removeChildAt(int)
     */
    public void removeChildAt(int i) {
        children.removeElementAt(i);

    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
     */
    public int getChildMultiplicity(String name) {
        return getChildrenWithName(name, false).size();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#shallowCopy()
     */
    public TreeElement shallowCopy() {
        TreeElement newNode = new TreeElement(name, multiplicity);
        newNode.parent = parent;
        newNode.setRepeatable(this.isRepeatable());
        newNode.dataType = dataType;

        // Just set the flag? side effects?
        newNode.setMaskVar(MASK_RELEVANT, this.getMaskVar(MASK_RELEVANT));
        newNode.setMaskVar(MASK_REQUIRED, this.getMaskVar(MASK_REQUIRED));
        newNode.setMaskVar(MASK_ENABLED, this.getMaskVar(MASK_ENABLED));

        newNode.constraint = constraint;
        newNode.preloadHandler = preloadHandler;
        newNode.preloadParams = preloadParams;
        newNode.instanceName = instanceName;
        newNode.namespace = namespace;

        newNode.setAttributesFromSingleStringVector(getSingleStringAttributeVector());
        if (value != null) {
            newNode.value = value.clone();
        }

        newNode.children = children;
        return newNode;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#deepCopy(boolean)
     */
    public TreeElement deepCopy(boolean includeTemplates) {
        TreeElement newNode = shallowCopy();

        if (children != null) {
            newNode.children = new Vector();
            for (int i = 0; i < children.size(); i++) {
                TreeElement child = (TreeElement)children.elementAt(i);
                if (includeTemplates || child.getMult() != TreeReference.INDEX_TEMPLATE) {
                    newNode.addChild(child.deepCopy(includeTemplates));
                }
            }
        }

        return newNode;
    }

    /* ==== MODEL PROPERTIES ==== */

    // factoring inheritance rules
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRelevant()
     */
    public boolean isRelevant() {
        return getMaskVar(MASK_RELEVANT_INH) && getMaskVar(MASK_RELEVANT);
    }

    // factoring in inheritance rules
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isEnabled()
     */
    public boolean isEnabled() {
        return getMaskVar(MASK_ENABLED_INH) && getMaskVar(MASK_ENABLED);
    }

    /* ==== SPECIAL SETTERS (SETTERS WITH SIDE-EFFECTS) ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setAnswer(org.javarosa.core.model.data.IAnswerData)
     */
    public boolean setAnswer(IAnswerData answer) {
        if (value != null || answer != null) {
            setValue(answer);
            alertStateObservers(FormElementStateListener.CHANGE_DATA);
            return true;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setRequired(boolean)
     */
    public void setRequired(boolean required) {
        if (getMaskVar(MASK_REQUIRED) != required) {
            setMaskVar(MASK_REQUIRED, required);
            alertStateObservers(FormElementStateListener.CHANGE_REQUIRED);
        }
    }

    private boolean getMaskVar(int mask) {
        return (flags & mask) == mask;
    }

    private void setMaskVar(int mask, boolean value) {
        if (value) {
            flags = flags | mask;
        } else {
            flags = flags & (Integer.MAX_VALUE - mask);
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setRelevant(boolean)
     */
    public void setRelevant(boolean relevant) {
        setRelevant(relevant, false);
    }

    private void setRelevant(boolean relevant, boolean inherited) {
        boolean oldRelevancy = isRelevant();
        if (inherited) {
            setMaskVar(MASK_RELEVANT_INH, relevant);
        } else {
            setMaskVar(MASK_RELEVANT, relevant);
        }

        if (isRelevant() != oldRelevancy) {
            if (attributes != null) {
                for (int i = 0; i < attributes.size(); ++i) {
                    attributes.elementAt(i).setRelevant(isRelevant(), true);
                }
            }
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    ((TreeElement)children.elementAt(i)).setRelevant(isRelevant(), true);
                }
            }
            alertStateObservers(FormElementStateListener.CHANGE_RELEVANT);
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled) {
        setEnabled(enabled, false);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setEnabled(boolean, boolean)
     */
    public void setEnabled(boolean enabled, boolean inherited) {
        boolean oldEnabled = isEnabled();
        if (inherited) {
            setMaskVar(MASK_ENABLED_INH, enabled);
        } else {
            setMaskVar(MASK_ENABLED, enabled);
        }

        if (isEnabled() != oldEnabled) {
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    ((TreeElement)children.elementAt(i)).setEnabled(isEnabled(),
                            true);
                }
            }
            alertStateObservers(FormElementStateListener.CHANGE_ENABLED);
        }
    }

    /* ==== OBSERVER PATTERN ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#registerStateObserver(org.javarosa.core.model.FormElementStateListener)
     */
    public void registerStateObserver(FormElementStateListener qsl) {
        if (observers == null)
            observers = new Vector();

        if (!observers.contains(qsl)) {
            observers.addElement(qsl);
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#unregisterStateObserver(org.javarosa.core.model.FormElementStateListener)
     */
    public void unregisterStateObserver(FormElementStateListener qsl) {
        if (observers != null) {
            observers.removeElement(qsl);
            if (observers.isEmpty())
                observers = null;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#unregisterAll()
     */
    public void unregisterAll() {
        observers = null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#alertStateObservers(int)
     */
    public void alertStateObservers(int changeFlags) {
        if (observers != null) {
            for (Enumeration e = observers.elements(); e.hasMoreElements(); )
                ((FormElementStateListener)e.nextElement())
                        .formElementStateChanged(this, changeFlags);
        }
    }

    /* ==== VISITOR PATTERN ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#accept(org.javarosa.core.model.instance.utils.ITreeVisitor)
     */
    public void accept(ITreeVisitor visitor) {
        visitor.visit(this);

        if (children == null) {
            return;
        }
        Enumeration en = children.elements();
        while (en.hasMoreElements()) {
            ((TreeElement)en.nextElement()).accept(visitor);
        }

    }

    /* ==== Attributes ==== */

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeCount()
     */
    public int getAttributeCount() {
        return attributes == null ? 0 : attributes.size();
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
     */
    public String getAttributeNamespace(int index) {
        return attributes.elementAt(index).namespace;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
     */
    public String getAttributeName(int index) {
        return attributes.elementAt(index).name;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
     */
    public String getAttributeValue(int index) {
        return getAttributeValue(attributes.elementAt(index));
    }

    /**
     * Get the String value of the provided attribute
     *
     * @param attribute
     * @return
     */
    private String getAttributeValue(TreeElement attribute) {
        if (attribute.getValue() == null) {
            return null;
        } else {
            return attribute.getValue().uncast().getString();
        }
    }


    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
     */
    public TreeElement getAttribute(String namespace, String name) {
        if (attributes == null) {
            return null;
        }
        for (TreeElement attribute : attributes) {
            if (attribute.getName().equals(name) && (namespace == null || namespace.equals(attribute.namespace))) {
                return attribute;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
     */
    public String getAttributeValue(String namespace, String name) {
        TreeElement element = getAttribute(namespace, name);
        return element == null ? null : getAttributeValue(element);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setAttribute(java.lang.String, java.lang.String, java.lang.String)
     */
    public void setAttribute(String namespace, String name, String value) {
        if (attributes == null) {
            this.attributes = new Vector<TreeElement>();
        }
        for (int i = attributes.size() - 1; i >= 0; i--) {
            TreeElement attribut = attributes.elementAt(i);
            if (attribut.name.equals(name) && (namespace == null || namespace.equals(attribut.namespace))) {
                if (value == null) {
                    attributes.removeElementAt(i);
                } else {
                    attribut.setValue(new UncastData(value));
                }
                return;
            }
        }

        if (namespace == null) {
            namespace = "";
        }

        TreeElement attr = TreeElement.constructAttributeElement(namespace, name);
        attr.setValue(new UncastData(value));
        attr.setParent(this);

        attributes.addElement(attr);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getSingleStringAttributeVector()
     */
    public Vector getSingleStringAttributeVector() {
        Vector strings = new Vector();
        if (attributes == null || attributes.size() == 0)
            return null;
        else {
            for (int i = 0; i < this.attributes.size(); i++) {
                TreeElement attribute = attributes.elementAt(i);
                String value = getAttributeValue(attribute);
                if (attribute.namespace == null || attribute.namespace.equals(""))
                    strings.addElement(new String(attribute.getName() + "=" + value));
                else
                    strings.addElement(new String(attribute.namespace + ":" + attribute.getName()
                            + "=" + value));
            }
            return strings;
        }
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setAttributesFromSingleStringVector(java.util.Vector)
     */
    public void setAttributesFromSingleStringVector(Vector attStrings) {
        if (attStrings != null) {
            this.attributes = new Vector(0);
            for (int i = 0; i < attStrings.size(); i++) {
                addSingleAttribute(i, attStrings);
            }
        }
    }

    private void addSingleAttribute(int i, Vector attStrings) {
        String att = (String)attStrings.elementAt(i);
        String[] array = new String[3];

        int pos = -1;

        //TODO: The only current assumption here is that the namespace/name of the attribute doesn't have
        //an equals sign in it. I think this is safe. not sure.

        //Split into first and second parts
        pos = att.indexOf("=");

        //put the value in our output
        array[2] = att.substring(pos + 1);

        //now we're left with the xmlns (possibly) and the
        //name. Get that into a single string.
        att = att.substring(0, pos);

        //reset position marker.
        pos = -1;

        // Clayton Sims - Jun 1, 2009 : Updated this code:
        //    We want to find the _last_ possible ':', not the
        // first one. Namespaces can have URLs in them.
        //int pos = att.indexOf(":");
        while (att.indexOf(":", pos + 1) != -1) {
            pos = att.indexOf(":", pos + 1);
        }

        if (pos == -1) {
            //No namespace
            array[0] = null;

            //for the name eval below
            pos = 0;
        } else {
            //there is a namespace, grab it
            array[0] = att.substring(0, pos);
        }
        // Now get the name part
        array[1] = att.substring(pos);

        this.setAttribute(array[0], array[1], array[2]);
    }

    /* ==== SERIALIZATION ==== */

    /*
     * TODO:
     *
     * this new serialization scheme is kind of lame. ideally, we shouldn't have
     * to sub-class TreeElement at all; we should have an API that can
     * seamlessly represent complex data model objects (like weight history or
     * immunizations) as if they were explicity XML subtrees underneath the
     * parent TreeElement
     *
     * failing that, we should wrap this scheme in an ExternalizableWrapper
     */

    /*
     * (non-Javadoc)
     *
     * @see
     * org.javarosa.core.services.storage.utilities.Externalizable#readExternal
     * (java.io.DataInputStream)
     */
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        multiplicity = ExtUtil.readInt(in);
        flags = ExtUtil.readInt(in);
        value = (IAnswerData)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);

        // children = ExtUtil.nullIfEmpty((Vector)ExtUtil.read(in, new
        // ExtWrapList(TreeElement.class), pf));

        // Jan 22, 2009 - csims@dimagi.com
        // old line: children = ExtUtil.nullIfEmpty((Vector)ExtUtil.read(in, new
        // ExtWrapList(TreeElement.class), pf));
        // New Child deserialization
        // 1. read null status as boolean
        // 2. read number of children
        // 3. for i < number of children
        // 3.1 if read boolean true , then create TreeElement and deserialize
        // directly.
        // 3.2 if read boolean false then create tagged element and deserialize
        // child
        if (!ExtUtil.readBool(in)) {
            // 1.
            children = null;
        } else {
            children = new Vector();
            // 2.
            int numChildren = (int)ExtUtil.readNumeric(in);
            // 3.
            for (int i = 0; i < numChildren; ++i) {
                boolean normal = ExtUtil.readBool(in);
                TreeElement child;

                if (normal) {
                    // 3.1
                    child = new TreeElement();
                    child.readExternal(in, pf);
                } else {
                    // 3.2
                    child = (TreeElement)ExtUtil.read(in, new ExtWrapTagged(), pf);
                }
                child.setParent(this);
                children.addElement(child);
            }
        }

        // end Jan 22, 2009

        dataType = ExtUtil.readInt(in);
        instanceName = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        constraint = (Constraint)ExtUtil.read(in, new ExtWrapNullable(
                Constraint.class), pf);
        preloadHandler = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        preloadParams = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        namespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));

        Vector attStrings = ExtUtil.nullIfEmpty((Vector)ExtUtil.read(in,
                new ExtWrapList(String.class), pf));
        setAttributesFromSingleStringVector(attStrings);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.javarosa.core.services.storage.utilities.Externalizable#writeExternal
     * (java.io.DataOutputStream)
     */
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
        ExtUtil.writeNumeric(out, multiplicity);
        ExtUtil.writeNumeric(out, flags);
        ExtUtil.write(out, new ExtWrapNullable(value == null ? null : new ExtWrapTagged(value)));

        // Jan 22, 2009 - csims@dimagi.com
        // old line: ExtUtil.write(out, new
        // ExtWrapList(ExtUtil.emptyIfNull(children)));
        // New Child serialization
        // 1. write null status as boolean
        // 2. write number of children
        // 3. for all child in children
        // 3.1 if child type == TreeElement write boolean true , then serialize
        // directly.
        // 3.2 if child type != TreeElement, write boolean false, then tagged
        // child
        if (children == null) {
            // 1.
            ExtUtil.writeBool(out, false);
        } else {
            // 1.
            ExtUtil.writeBool(out, true);
            // 2.
            ExtUtil.writeNumeric(out, children.size());
            // 3.
            Enumeration en = children.elements();
            while (en.hasMoreElements()) {
                TreeElement child = (TreeElement)en.nextElement();
                if (child.getClass() == TreeElement.class) {
                    // 3.1
                    ExtUtil.writeBool(out, true);
                    child.writeExternal(out);
                } else {
                    // 3.2
                    ExtUtil.writeBool(out, false);
                    ExtUtil.write(out, new ExtWrapTagged(child));
                }
            }
        }

        // end Jan 22, 2009

        ExtUtil.writeNumeric(out, dataType);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(instanceName));
        ExtUtil.write(out, new ExtWrapNullable(constraint)); // TODO: inefficient for repeats
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(preloadHandler));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(preloadParams));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(namespace));

        Vector attStrings = getSingleStringAttributeVector();
        ExtUtil.write(out, new ExtWrapList(ExtUtil.emptyIfNull(attStrings)));
    }

    //rebuilding a node from an imported instance
    //  there's a lot of error checking we could do on the received instance, but it's
    //  easier to just ignore the parts that are incorrect
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#populate(org.javarosa.core.model.instance.TreeElement, org.javarosa.core.model.FormDef)
     */
    public void populate(TreeElement incoming, FormDef f) {
        if (this.isLeaf()) {
            // check that incoming doesn't have children?

            IAnswerData value = incoming.getValue();
            if (value == null) {
                this.setValue(null);
            } else {
                this.setValue(AnswerDataFactory.templateByDataType(this.dataType).cast(value.uncast()));
            }
        } else {
            Vector names = new Vector();

            // remove all default repetitions from skeleton data model (_preserving_ templates, though)
            for (int i = 0; i < this.getNumChildren(); i++) {
                TreeElement child = this.getChildAt(i);
                if (child.getMaskVar(MASK_REPEATABLE) && child.getMult() != TreeReference.INDEX_TEMPLATE) {
                    this.removeChildAt(i);
                    i--;
                }
            }

            //We used to wipe out non-repeated elements, but we won't anymore
            //since we commonly use the pattern.
            for (int i = 0; i < this.getNumChildren(); i++) {
                TreeElement child = this.getChildAt(i);
                names.addElement(child.getName());
            }

            // make sure ordering is preserved (needed for compliance with xsd schema)
            if (this.getNumChildren() != names.size()) {
                throw new RuntimeException("sanity check failed");
            }

            for (int i = 0; i < this.getNumChildren(); i++) {
                TreeElement child = this.getChildAt(i);
                String expectedName = (String)names.elementAt(i);

                if (!child.getName().equals(expectedName)) {
                    TreeElement child2 = null;
                    int j;

                    for (j = i + 1; j < this.getNumChildren(); j++) {
                        child2 = this.getChildAt(j);
                        if (child2.getName().equals(expectedName)) {
                            break;
                        }
                    }
                    if (j == this.getNumChildren()) {
                        throw new RuntimeException("sanity check failed");
                    }

                    this.removeChildAt(j);
                    if (children == null) {
                        children = new Vector();
                    }
                    this.children.insertElementAt(child2, i);
                }
            }
            // java i hate you so much

            for (int i = 0; i < this.getNumChildren(); i++) {
                TreeElement child = this.getChildAt(i);
                Vector newChildren = incoming.getChildrenWithName(child.getName());

                if (child.getMaskVar(MASK_REPEATABLE)) {
                    for (int k = 0; k < newChildren.size(); k++) {
                        TreeElement newChild = child.deepCopy(true);
                        newChild.setMult(k);
                        if (children == null) {
                            children = new Vector();
                        }
                        this.children.insertElementAt(newChild, i + k + 1);
                        newChild.populate((TreeElement)newChildren.elementAt(k), f);
                    }
                    i += newChildren.size();
                } else {

                    if (newChildren.size() == 0) {
                        child.setRelevant(false);
                    } else {
                        child.populate((TreeElement)newChildren.elementAt(0), f);
                    }
                }
            }
            for (int i = 0; i < incoming.getAttributeCount(); i++) {
                String name = incoming.getAttributeName(i);
                String ns = incoming.getAttributeNamespace(i);
                String value = incoming.getAttributeValue(i);

                this.setAttribute(ns, name, value);
            }
        }
    }

    //this method is for copying in the answers to an itemset. the template node of the destination
    //is used for overall structure (including data types), and the itemset source node is used for
    //raw data. note that data may be coerced across types, which may result in type conversion error
    //very similar in structure to populate()
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#populateTemplate(org.javarosa.core.model.instance.TreeElement, org.javarosa.core.model.FormDef)
     */
    public void populateTemplate(TreeElement incoming, FormDef f) {
        if (this.isLeaf()) {
            IAnswerData value = incoming.getValue();
            if (value == null) {
                this.setValue(null);
            } else {
                this.setValue(AnswerDataFactory.templateByDataType(dataType).cast(value.uncast()));
            }
        } else {
            for (int i = 0; i < this.getNumChildren(); i++) {
                TreeElement child = this.getChildAt(i);
                Vector newChildren = incoming.getChildrenWithName(child.getName());

                if (child.getMaskVar(MASK_REPEATABLE)) {
                    for (int k = 0; k < newChildren.size(); k++) {
                        TreeElement template = f.getMainInstance().getTemplate(child.getRef());
                        TreeElement newChild = template.deepCopy(false);
                        newChild.setMult(k);
                        if (children == null) {
                            children = new Vector();
                        }
                        this.children.insertElementAt(newChild, i + k + 1);
                        newChild.populateTemplate((TreeElement)newChildren.elementAt(k), f);
                    }
                    i += newChildren.size();
                } else {
                    child.populateTemplate((TreeElement)newChildren.elementAt(0), f);
                }
            }
        }
    }

    //TODO: This is probably silly because this object is likely already
    //not thread safe in any way. Also, we should be wrapping all of the
    //setters.
    TreeReference[] refCache = new TreeReference[1];

    private void expireReferenceCache() {
        synchronized (refCache) {
            refCache[0] = null;
        }
    }

    //return the tree reference that corresponds to this tree element
    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
     */
    public TreeReference getRef() {
        //TODO: Expire cache somehow;
        synchronized (refCache) {
            if (refCache[0] == null) {
                refCache[0] = TreeElement.BuildRef(this);
            }
            return refCache[0];
        }
    }

    public static TreeReference BuildRef(AbstractTreeElement elem) {
        TreeReference ref = TreeReference.selfRef();

        while (elem != null) {
            TreeReference step;

            if (elem.getName() != null) {
                step = TreeReference.selfRef();
                step.add(elem.getName(), elem.getMult());
                step.setInstanceName(elem.getInstanceName());
            } else {
                step = TreeReference.rootRef();
                //All TreeElements are part of a consistent tree, so the root should be in the same instance
                step.setInstanceName(elem.getInstanceName());
            }

            ref = ref.parent(step);
            elem = elem.getParent();
        }
        return ref;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDepth()
     */
    public int getDepth() {
        return TreeElement.CalculateDepth(this);
    }

    public static int CalculateDepth(AbstractTreeElement elem) {
        int depth = 0;

        while (elem.getName() != null) {
            depth++;
            elem = elem.getParent();
        }

        return depth;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getPreloadHandler()
     */
    public String getPreloadHandler() {
        return preloadHandler;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getConstraint()
     */
    public Constraint getConstraint() {
        return constraint;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setPreloadHandler(java.lang.String)
     */
    public void setPreloadHandler(String preloadHandler) {
        this.preloadHandler = preloadHandler;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setConstraint(org.javarosa.core.model.condition.Constraint)
     */
    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getPreloadParams()
     */
    public String getPreloadParams() {
        return preloadParams;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setPreloadParams(java.lang.String)
     */
    public void setPreloadParams(String preloadParams) {
        this.preloadParams = preloadParams;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setName(java.lang.String)
     */
    public void setName(String name) {
        expireReferenceCache();
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
     */
    public int getMult() {
        return multiplicity;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setMult(int)
     */
    public void setMult(int multiplicity) {
        expireReferenceCache();
        this.multiplicity = multiplicity;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setParent(org.javarosa.core.model.instance.TreeElement)
     */
    public void setParent(AbstractTreeElement parent) {
        expireReferenceCache();
        this.parent = parent;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getParent()
     */
    public AbstractTreeElement getParent() {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getValue()
     */
    public IAnswerData getValue() {
        return value;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#toString()
     */
    public String toString() {
        String name = "NULL";
        if (this.name != null) {
            name = this.name;
        }

        String childrenCount = "-1";
        if (this.children != null) {
            childrenCount = Integer.toString(this.children.size());
        }

        return name + " - Children: " + childrenCount;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#getDataType()
     */
    public int getDataType() {
        return dataType;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#isRequired()
     */
    public boolean isRequired() {
        return getMaskVar(MASK_REQUIRED);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#setRepeatable(boolean)
     */
    public void setRepeatable(boolean repeatable) {
        setMaskVar(MASK_REPEATABLE, repeatable);
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void clearCaches() {
        expireReferenceCache();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * An optional mapping of this element's children based on a path step result that can be used to quickly index child nodes *
     */
    Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> mChildStepMapping = null;

    /**
     * Adds a hint mapping which can be used to directly index this node's children. This is used
     * when performing batch child fetches through static evaluation.
     *
     * This map should contain a table of cannonical XPath path expression steps which can be optimized
     * (like "@someattr") along with a table of which values of that attribute correspond with which
     * of tihs element's children.
     *
     * Notes:
     * 1) The table of string -> child elements must be _comprehensive_, but this is not checked by
     * this method, so the caller is responsible for ensuring that the map is valid
     *
     * 2) TreeElements also do not automatically expire their attribute maps, so this method
     * should only be used on static tree structures.
     *
     * 3) The path steps matched must be direct, ie: @someattr = 'value', no other operations are supported.
     *
     * @param childAttributeHintMap A table of Path Steps which can be indexed during batch fetch, along with
     *                              a mapping of which values of those steps match which children
     */
    public void addAttributeMap(Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> childAttributeHintMap) {
        this.mChildStepMapping = childAttributeHintMap;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.instance.AbstractTreeElement#tryBatchChildFetch(java.lang.String, int, java.util.Vector, org.javarosa.core.model.condition.EvaluationContext)
     */
    public Vector<TreeReference> tryBatchChildFetch(String name, int mult, Vector<XPathExpression> predicates, EvaluationContext evalContext) {
        return TreeUtilities.tryBatchChildFetch(this, mChildStepMapping, name, mult, predicates, evalContext);
    }
}
