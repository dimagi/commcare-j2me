/**
 * 
 */
package org.javarosa.core.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class Action implements Externalizable {
	//Some named events
	public static final String EVENT_XFORMS_READY = "xforms-ready";

	public static final String EVENT_XFORMS_REVALIDATE = "xforms-revalidate";
	
	private String name;
	
	public Action() {
		
	}
	
	public Action(String name) {
		this.name = name;
	}
	
	public void processAction(FormDef target) {
		//TODO: Big block of handlers for basic named action types
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		name = ExtUtil.readString(in);
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out,  name);
	}
}