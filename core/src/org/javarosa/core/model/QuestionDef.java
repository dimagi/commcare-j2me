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
import java.util.Vector;
import java.lang.String;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localizable;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

/**
 * The definition of a Question to be presented to users when
 * filling out a form.
 *
 * QuestionDef requires that any XPathReferences that are used
 * are contained in the FormDefRMS's PrototypeFactoryDeprecated in order
 * to be properly deserialized. If they aren't, an exception
 * will be thrown at the time of deserialization.
 *
 * @author Daniel Kayiwa/Drew Roos
 */
public class QuestionDef implements IFormElement, Localizable {
    private int id;
    // reference to a location in the model to store data in
    private XPathReference binding;

    // The type of widget. eg TextInput,Slider,List etc.
    private int controlType;
    private String appearanceAttr;
    private String hintTextID;
    private String helpTextID;
    private String labelInnerText;
    private String hintText;
    // The id (ref) pointing to the localized values of (pic-URIs,audio-URIs,text)
    private String textID;
    private String hintInnerText;
    private String helpInnerText;
    private String helpText;

    private Vector<SelectChoice> choices;
    private ItemsetBinding dynamicChoices;

    Vector observers;

    public QuestionDef() {
        this(Constants.NULL_ID, Constants.DATATYPE_TEXT);
    }

    public QuestionDef(int id, int controlType) {
        setID(id);
        setControlType(controlType);
        observers = new Vector();
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public XPathReference getBind() {
        return binding;
    }

    public void setBind(XPathReference binding) {
        this.binding = binding;
    }

    public int getControlType() {
        return controlType;
    }

    public void setControlType(int controlType) {
        this.controlType = controlType;
    }

    public String getAppearanceAttr() {
        return appearanceAttr;
    }

    public void setAppearanceAttr(String appearanceAttr) {
        this.appearanceAttr = appearanceAttr;
    }

    /**
     * Only if there is no localizable version of the &lt;hint&gt; available should this method be used
     */
    public String getHintText() {
        return hintText;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String text) {
        helpText = text;
    }

    /**
     * Only if there is no localizable version of the &lt;hint&gt; available should this method be used
     */
    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    public String getHintTextID() {
        return hintTextID;
    }

    public String getHelpTextID() {
        return helpTextID;
    }

    public void setHintTextID(String textID) {
        this.hintTextID = textID;
    }

    public void setHelpTextID(String textID) {
        this.helpTextID = textID;
    }

    public void addSelectChoice(SelectChoice choice) {
        if (choices == null) {
            choices = new Vector<SelectChoice>();
        }
        choice.setIndex(choices.size());
        choices.addElement(choice);
    }

    public void removeSelectChoice(SelectChoice choice) {
        if (choices == null) {
            choice.setIndex(0);
            return;
        }

        if (choices.contains(choice)) {
            choices.removeElement(choice);
        }
    }

    public void removeAllSelectChoices() {
        if (choices != null) {
            choices.removeAllElements();
        }
    }

    public Vector<SelectChoice> getChoices() {
        return choices;
    }

    public SelectChoice getChoice(int i) {
        return choices.elementAt(i);
    }

    public int getNumChoices() {
        return (choices != null ? choices.size() : 0);
    }

    public SelectChoice getChoiceForValue(String value) {
        for (int i = 0; i < getNumChoices(); i++) {
            if (getChoice(i).getValue().equals(value)) {
                return getChoice(i);
            }
        }
        return null;
    }

    public ItemsetBinding getDynamicChoices() {
        return dynamicChoices;
    }

    public void setDynamicChoices(ItemsetBinding ib) {
        if (ib != null) {
            ib.setDestRef(this);
        }
        this.dynamicChoices = ib;
    }

    /**
     * Determine if a question's answer is xml tree data.
     *
     * @return does the answer to this question yields xml tree data, and not a simple string value?
     */
    public boolean isComplex() {
        return (dynamicChoices != null && dynamicChoices.copyMode);
    }

    //Deprecated
    public void localeChanged(String locale, Localizer localizer) {
        if (choices != null) {
            for (int i = 0; i < choices.size(); i++) {
                choices.elementAt(i).localeChanged(null, localizer);
            }
        }

        if (dynamicChoices != null) {
            dynamicChoices.localeChanged(locale, localizer);
        }

        alertStateObservers(FormElementStateListener.CHANGE_LOCALE);
    }

    public Vector getChildren() {
        return null;
    }

    public void setChildren(Vector v) {
        throw new IllegalStateException("Can't add children to question def");
    }

    public void addChild(IFormElement fe) {
        throw new IllegalStateException("Can't add children to question def");
    }

    public IFormElement getChild(int i) {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.Externalizable#readExternal(java.io.DataInputStream)
     */
    public void readExternal(DataInputStream dis, PrototypeFactory pf) throws IOException, DeserializationException {
        setID(ExtUtil.readInt(dis));
        binding = (XPathReference)ExtUtil.read(dis, new ExtWrapNullable(new ExtWrapTagged()), pf);
        setAppearanceAttr((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setTextID((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setLabelInnerText((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setHintText((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setHintTextID((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setHintInnerText((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setHelpText((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setHelpTextID((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));
        setHelpInnerText((String)ExtUtil.read(dis, new ExtWrapNullable(String.class), pf));

        setControlType(ExtUtil.readInt(dis));
        choices = ExtUtil.nullIfEmpty((Vector)ExtUtil.read(dis, new ExtWrapList(SelectChoice.class), pf));
        for (int i = 0; i < getNumChoices(); i++) {
            choices.elementAt(i).setIndex(i);
        }
        setDynamicChoices((ItemsetBinding)ExtUtil.read(dis, new ExtWrapNullable(ItemsetBinding.class)));
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream dos) throws IOException {
        ExtUtil.writeNumeric(dos, getID());
        ExtUtil.write(dos, new ExtWrapNullable(binding == null ? null : new ExtWrapTagged(binding)));
        ExtUtil.write(dos, new ExtWrapNullable(getAppearanceAttr()));
        ExtUtil.write(dos, new ExtWrapNullable(getTextID()));
        ExtUtil.write(dos, new ExtWrapNullable(getLabelInnerText()));
        ExtUtil.write(dos, new ExtWrapNullable(getHintText()));
        ExtUtil.write(dos, new ExtWrapNullable(getHintTextID()));
        ExtUtil.write(dos, new ExtWrapNullable(getHintInnerText()));
        ExtUtil.write(dos, new ExtWrapNullable(getHelpText()));
        ExtUtil.write(dos, new ExtWrapNullable(getHelpTextID()));
        ExtUtil.write(dos, new ExtWrapNullable(getHelpInnerText()));

        ExtUtil.writeNumeric(dos, getControlType());

        ExtUtil.write(dos, new ExtWrapList(ExtUtil.emptyIfNull(choices)));
        ExtUtil.write(dos, new ExtWrapNullable(dynamicChoices));
    }

    /* === MANAGING OBSERVERS === */

    public void registerStateObserver(FormElementStateListener qsl) {
        if (!observers.contains(qsl)) {
            observers.addElement(qsl);
        }
    }

    public void unregisterStateObserver(FormElementStateListener qsl) {
        observers.removeElement(qsl);
    }

    public void unregisterAll() {
        observers.removeAllElements();
    }

    public void alertStateObservers(int changeFlags) {
        for (Enumeration e = observers.elements(); e.hasMoreElements(); )
            ((FormElementStateListener)e.nextElement()).formElementStateChanged(this, changeFlags);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.model.IFormElement#getDeepChildCount()
     */
    public int getDeepChildCount() {
        return 1;
    }

    public void setLabelInnerText(String labelInnerText) {
        this.labelInnerText = labelInnerText;
    }

    public String getLabelInnerText() {
        return labelInnerText;
    }

    public void setHintInnerText(String innerText) {
        this.hintInnerText = innerText;
    }

    public void setHelpInnerText(String innerText) {
        this.helpInnerText = innerText;
    }

    public String getHintInnerText() {
        return hintInnerText;
    }

    public String getHelpInnerText() {
        return helpInnerText;
    }

    public String getTextID() {
        return textID;
    }

    public void setTextID(String textID) {
        if (DateUtils.stringContains(textID, ";")) {
            System.err.println("Warning: TextID contains ;form modifier:: \"" + textID.substring(textID.indexOf(";")) + "\"... will be stripped.");
            textID = textID.substring(0, textID.indexOf(";")); //trim away the form specifier
        }
        this.textID = textID;
    }
}
