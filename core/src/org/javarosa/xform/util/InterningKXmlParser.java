/**
 * 
 */
package org.javarosa.xform.util;

import org.javarosa.core.util.Interner;
import org.kxml2.io.KXmlParser;

/**
 * @author ctsims
 *
 */
public class InterningKXmlParser extends KXmlParser{
	Interner interner;
	
	public InterningKXmlParser() {
		//TODO: automagically wipe cache?
		this(new Interner());
	}
	
	public InterningKXmlParser(Interner interner) {
		super();
		this.interner = interner;
	}
	
	public void release() {
		interner.release();
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributeName(int)
	 */
	public String getAttributeName(int arg0) {
		return interner.intern(super.getAttributeName(arg0));
		
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributeNamespace(int)
	 */
	public String getAttributeNamespace(int arg0) {
		return interner.intern(super.getAttributeNamespace(arg0));

	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributePrefix(int)
	 */
	public String getAttributePrefix(int arg0) {
		return interner.intern(super.getAttributePrefix(arg0));
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getAttributeValue(int)
	 */
	public String getAttributeValue(int arg0) {
		return interner.intern(super.getAttributeValue(arg0));

	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getNamespace(java.lang.String)
	 */
	public String getNamespace(String arg0) {
		return interner.intern(super.getNamespace(arg0));

	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getNamespaceUri(int)
	 */
	public String getNamespaceUri(int arg0) {
		return interner.intern(super.getNamespaceUri(arg0));
	}
	
	/* (non-Javadoc)
	 * @see org.kxml2.io.KXmlParser#getText()
	 */
	public String getText() {
		return interner.intern(super.getText());

	}
	
	public String getName() {
		return interner.intern(super.getName());
	}
}
