/**
 * 
 */
package org.javarosa.services.transport;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;

/**
 * Copied from JavaRosa core properties file, describes available
 * properties and parameters for HTTP requests.
 * 
 * @author ctsims
 *
 */
public class TransportPropertyRules implements IPropertyRules {
	final Hashtable<String, Vector<String>> rules;

	final Vector<String> readOnlyProperties;

	public final static String HTTP_CERTIFICATE_REQUEST_URL = "http_cert_validate_url";

	public TransportPropertyRules() {
		rules = new Hashtable();
		readOnlyProperties = new Vector();

		// DeviceID Property
		rules.put(HTTP_CERTIFICATE_REQUEST_URL, new Vector());
	}

	public Vector allowableValues(String propertyName) {
		return (Vector) rules.get(propertyName);
	}

	public boolean checkValueAllowed(String propertyName, String potentialValue) {
		Vector prop = ((Vector) rules.get(propertyName));
		if (prop.size() != 0) {
			// Check whether this is a dynamic property
			if (prop.size() == 1
					&& checkPropertyAllowed((String) prop.elementAt(0))) {
				// If so, get its list of available values, and see whether the
				// potentival value is acceptable.
				return PropertyManager._()
						.getProperty((String) prop.elementAt(0))
						.contains(potentialValue);
			} else {
				return ((Vector) rules.get(propertyName))
						.contains(potentialValue);
			}
		} else
			return true;
	}

	public Vector allowableProperties() {
		Vector propList = new Vector();
		Enumeration iter = rules.keys();
		while (iter.hasMoreElements()) {
			propList.addElement(iter.nextElement());
		}
		return propList;
	}

	public boolean checkPropertyAllowed(String propertyName) {
		Enumeration iter = rules.keys();
		while (iter.hasMoreElements()) {
			if (propertyName.equals(iter.nextElement())) {
				return true;
			}
		}
		return false;
	}

	public boolean checkPropertyUserReadOnly(String propertyName) {
		return readOnlyProperties.contains(propertyName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.javarosa.core.services.properties.IPropertyRules#
	 * getHumanReadableDescription(java.lang.String)
	 */
	public String getHumanReadableDescription(String propertyName) {
		if (HTTP_CERTIFICATE_REQUEST_URL.equals(propertyName)) {
			return "Web Certificate Test URL";
		} 
		return propertyName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.javarosa.core.services.properties.IPropertyRules#getHumanReadableValue
	 * (java.lang.String, java.lang.String)
	 */
	public String getHumanReadableValue(String propertyName, String value) {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.javarosa.core.services.properties.IPropertyRules#handlePropertyChanges
	 * (java.lang.String)
	 */
	public void handlePropertyChanges(String propertyName) {
	}
}