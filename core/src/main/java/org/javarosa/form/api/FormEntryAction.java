package org.javarosa.form.api;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntryAction implements Externalizable {
    private String formIndexString;
    private String value;
    private String action;
    private final static String NEW_REPEAT = "NEW_REPEAT";
    private final static String SKIP = "SKIP";
    private final static String VALUE = "VALUE";

    /**
     * For Externalization
     */
    public FormEntryAction() {
    }

    private FormEntryAction(String formIndexString, String value, String action) {
        this.formIndexString = formIndexString;
        this.value = value;
        this.action = action;
    }

    public static FormEntryAction buildNewRepeatAction(String formIndexString) {
        return new FormEntryAction(formIndexString, "", NEW_REPEAT);
    }

    public static FormEntryAction buildValueSetAction(String formIndexString, String value) {
        return new FormEntryAction(formIndexString, value, VALUE);
    }

    public static FormEntryAction buildSkipAction(String formIndexString) {
        return new FormEntryAction(formIndexString, "", SKIP);
    }

    public static FormEntryAction buildNullAction() {
        return new FormEntryAction("", "", "");
    }

    @Override
    public String toString() {
        if (NEW_REPEAT.equals(action)) {
            return "((" + formIndexString + ") (NEW_REPEAT))";
        } else if (VALUE.equals(action)) {
            // TODO PLM: escape 'value' field
            return "((" + formIndexString + ") (VALUE) (" + value + "))";
        } else if (SKIP.equals(action)) {
            return "((" + formIndexString + ") (SKIP))";
        } else {
            return "";
        }
    }

    @Override
    public int hashCode() {
        return formIndexString.hashCode() ^ value.hashCode() ^ action.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof FormEntryAction) {
            FormEntryAction otherAction = (FormEntryAction)other;
            return formIndexString.equals(otherAction.formIndexString) &&
                    value.equals(otherAction.value) &&
                    action.equals(otherAction.action);
        }
        return false;
    }

    public static FormEntryAction fromString(String entryActionString) {
        String unwrappedEntryActionString =
                entryActionString.substring(1, entryActionString.length() - 1);
        Vector<String> actionEntries = FormEntrySession.splitTopParens(unwrappedEntryActionString);
        int entryCount = actionEntries.size();

        if (entryCount != 2 && entryCount != 3) {
            throw new RuntimeException("Form entry action '" + entryActionString +
                    "' has an incorrect number of entries, expected 2 or 3, got " + entryCount);
        }

        String wrappedFormIndexString = actionEntries.elementAt(0);
        String formIndexString = wrappedFormIndexString.substring(1, wrappedFormIndexString.length() - 1);
        if (entryCount == 2) {
            if (("(" + NEW_REPEAT + ")").equals(actionEntries.elementAt(1))) {
                return buildNewRepeatAction(formIndexString);
            } else {
                return buildSkipAction(formIndexString);
            }
        } else {
            String wrappedValue = actionEntries.elementAt(2);
            String value = wrappedValue.substring(1, wrappedValue.length() - 1);
            return buildValueSetAction(formIndexString, value);
        }
    }

    public boolean isNewRepeatAction() {
        return NEW_REPEAT.equals(action);
    }

    public boolean isSkipAction() {
        return SKIP.equals(action);
    }

    public String getValue() {
        return value;
    }

    public String getFormIndexString() {
        return formIndexString;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        formIndexString = ExtUtil.readString(in);
        value = ExtUtil.readString(in);
        action = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, formIndexString);
        ExtUtil.writeString(out, value);
        ExtUtil.writeString(out, action);
    }
}
