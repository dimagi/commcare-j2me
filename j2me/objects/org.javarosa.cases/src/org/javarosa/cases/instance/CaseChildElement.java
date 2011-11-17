/**
 * 
 */
package org.javarosa.cases.instance;

import java.util.Enumeration;
import java.util.Vector;

import org.javarosa.cases.model.Case;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.ITreeVisitor;
import org.javarosa.core.model.utils.PreloadUtils;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;

/**
 * @author ctsims
 *
 */
public class CaseChildElement implements AbstractTreeElement<TreeElement> {
	
	AbstractTreeElement<CaseChildElement> parent;
	int recordId; 
	String caseId;
	int mult;
	
	IStorageUtilityIndexed storage;
	
	TreeElement cached;
	
	public CaseChildElement(AbstractTreeElement<CaseChildElement> parent, int recordId, String caseId, int mult, IStorageUtilityIndexed storage) {
		if(recordId == -1 && caseId == null) { throw new RuntimeException("Cannot create a lazy case element with no lookup identifiers!");}
		this.parent = parent;
		this.recordId = recordId;
		this.caseId = caseId;
		this.mult = mult;
		this.storage = storage;
	}
	
	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isLeaf()
	 */
	public boolean isLeaf() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isChildable()
	 */
	public boolean isChildable() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getInstanceName()
	 */
	public String getInstanceName() {
		return parent.getInstanceName();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChild(java.lang.String, int)
	 */
	public TreeElement getChild(String name, int multiplicity) {
		cache();
		TreeElement child = cached.getChild(name, multiplicity);
		if(multiplicity >= 0 && child == null) {
			TreeElement emptyNode = new TreeElement(name);
			cached.addChild(emptyNode);
			emptyNode.setParent(cached);
			return emptyNode;
		}
		return child;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildrenWithName(java.lang.String)
	 */
	public Vector getChildrenWithName(String name) {
		cache();
		return cached.getChildrenWithName(name);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getNumChildren()
	 */
	public int getNumChildren() {
		cache();
		return cached.getNumChildren();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildAt(int)
	 */
	public TreeElement getChildAt(int i) {
		cache();
		return cached.getChildAt(i);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isRepeatable()
	 */
	public boolean isRepeatable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#isAttribute()
	 */
	public boolean isAttribute() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getChildMultiplicity(java.lang.String)
	 */
	public int getChildMultiplicity(String name) {
		cache();
		return cached.getChildMultiplicity(name);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#accept(org.javarosa.core.model.instance.utils.ITreeVisitor)
	 */
	public void accept(ITreeVisitor visitor) {
		visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeCount()
	 */
	public int getAttributeCount() {
		//TODO: Attributes should be fixed and possibly only include meta-details
		cache();
		return cached.getAttributeCount();
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeNamespace(int)
	 */
	public String getAttributeNamespace(int index) {
		cache();
		return cached.getAttributeValue(index);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeName(int)
	 */
	public String getAttributeName(int index) {
		cache();
		return cached.getAttributeValue(index);

	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(int)
	 */
	public String getAttributeValue(int index) {
		cache();
		return cached.getAttributeValue(index);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttribute(java.lang.String, java.lang.String)
	 */
	public TreeElement getAttribute(String namespace, String name) {
		if(name.equals("case-id")) {
			TreeElement caseid = TreeElement.constructAttributeElement(null, name);
			if(caseId == null) { cache();}
			caseid.setValue(new StringData(caseId));
			caseid.setParent(this);
			return caseid;
		}
		cache();
		return cached.getAttribute(namespace, name);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getAttributeValue(java.lang.String, java.lang.String)
	 */
	public String getAttributeValue(String namespace, String name) {
		if(name.equals("case-id")) {
			return caseId;
		}
		cache();
		return cached.getAttributeValue(namespace, name);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getRef()
	 */
	public TreeReference getRef() {
		return TreeElement.BuildRef(this);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getDepth()
	 */
	public int getDepth() {
		return TreeElement.CalculateDepth(this);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getName()
	 */
	public String getName() {
		return "case";
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getMult()
	 */
	public int getMult() {
		// TODO Auto-generated method stub
		return mult;
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
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.model.instance.AbstractTreeElement#getDataType()
	 */
	public int getDataType() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//TODO: Thread Safety!
	public void clearCaches() {
		cached = null;
	}
	
	private boolean isCached() {
		return cached != null;
	}
	
	private void cache() {
		if(isCached()) {
			return;
		}
		if(recordId == -1) {
			Vector<Integer> ids = storage.getIDsForValue("case-id",caseId);
			recordId = ids.elementAt(0).intValue();
		}
		Case c = (Case)storage.read(recordId);
		caseId = c.getCaseId();
		cached = new TreeElement("case");
		cached.setMult(this.mult);
		
		cached.setAttribute(null, "case-id", c.getCaseId());
		cached.setAttribute(null, "external-id", c.getExternalId());
		cached.setAttribute(null, "type", c.getTypeId());
		cached.setAttribute(null, "status", c.isClosed() ? "closed" : "open");
		
		TreeElement scratch = new TreeElement("name");
		scratch.setAnswer(new StringData(c.getName()));
		cached.addChild(scratch);
		
		
		scratch = new TreeElement("date-opened");
		scratch.setAnswer(new DateData(c.getDateOpened()));
		cached.addChild(scratch);
		
		for(Enumeration en = c.getProperties().keys();en.hasMoreElements();) {
			String key = (String)en.nextElement();
			scratch = new TreeElement(key);
			//scratch.setAttribute(null, "key", key);
			Object temp = c.getProperty(key);
			if(temp instanceof String) {
				scratch.setValue(new UncastData((String)temp));
			} else {
				scratch.setValue(PreloadUtils.wrapIndeterminedObject(temp));
			}
			cached.addChild(scratch);
		}
		
//		//So there's a template at least
//		if(c.getProperties().size() == 0) {
//			scratch = new TreeElement("datum");
//			scratch.setAttribute(null, "key", "dummy");
//			scratch.setValue(null);
//			cached.addChild(scratch);
//		}
		
		cached.setParent(this.parent);
	}

	public boolean isRelevant() {
		return true;
	}

}
