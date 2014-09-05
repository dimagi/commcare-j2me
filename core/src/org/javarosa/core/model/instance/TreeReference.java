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

import org.javarosa.core.util.DataUtil;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;


//TODO: This class needs to be immutable
public class TreeReference implements Externalizable {
	
	int hashCode = -1;
	
	public static final int DEFAULT_MUTLIPLICITY = 0;//multiplicity
	public static final int INDEX_UNBOUND = -1;//multiplicity
	public static final int INDEX_TEMPLATE = -2;//multiplicity
	public static final int INDEX_ATTRIBUTE = -4;//multiplicity flag for an attribute
	public static final int INDEX_REPEAT_JUNCTURE = -10;
	
	//TODO: Roll these into RefLevel? Or more likely, take absolute
	//ref out of refLevel
	public static final int CONTEXT_ABSOLUTE = 0;
	public static final int CONTEXT_INHERITED = 1;
	public static final int CONTEXT_ORIGINAL = 2;
	public static final int CONTEXT_INSTANCE = 4;
	
	
	public static final int REF_ABSOLUTE = -1;
	
	public static final String NAME_WILDCARD = "*";
	
	private int refLevel; //0 = context node, 1 = parent, 2 = grandparent ...
	private int contextType;
	private String instanceName = null;
	private Vector<TreeReferenceLevel> data = null;

	public static TreeReference rootRef () {
		TreeReference root = new TreeReference();
		root.refLevel = REF_ABSOLUTE;
		root.contextType = CONTEXT_ABSOLUTE;
		return root;
	}
	
	public static TreeReference selfRef () {
		TreeReference self = new TreeReference();
		self.refLevel = 0;
		self.contextType = CONTEXT_INHERITED;
		return self;
	}
	
	public TreeReference () {
		instanceName = null; //dido
		data = new Vector<TreeReferenceLevel>();
	}
	
	public String getInstanceName() {
		return instanceName;
	}

	//TODO: This should be constructed I think
	public void setInstanceName(String instanceName) {
		hashCode = -1;
		if(instanceName == null) {
			if(this.refLevel == REF_ABSOLUTE) {
				this.contextType = CONTEXT_ABSOLUTE;
			} else {
				this.contextType = CONTEXT_INHERITED;
			}
		} else{
			this.contextType = CONTEXT_INSTANCE;
		}
		this.instanceName = instanceName;
	}
	
	public int getMultiplicity(int index) {
		return data.elementAt(index).getMultiplicity();
	}
	
	public String getName(int index) {
		return data.elementAt(index).getName();
	}

	public int getMultLast () {
		return data.lastElement().getMultiplicity();
	}
	
	public String getNameLast () {
		return data.lastElement().getName();
	}
	
	public void setMultiplicity (int i, int mult) {
		hashCode = -1;
		data.setElementAt(data.elementAt(i).setMultiplicity(mult), i);
	}
	
	int size = -1;
	public int size () {
		if(size == -1) {
			size = data.size();
		}
		return size;
	}
	
	private void add (TreeReferenceLevel level) {
		hashCode = -1;
		size = -1;
		data.addElement(level);
	}
	
	public void add (String name, int mult) {
		hashCode = -1;
		size = -1;
		add(new TreeReferenceLevel(name, mult).intern());
	}
	
	public void addPredicate(int key, Vector<XPathExpression> xpe)
	{
		hashCode = -1;
		data.setElementAt(data.elementAt(key).setPredicates(xpe), key);
	}
	
	public Vector<XPathExpression> getPredicate(int key)
	{
		return data.elementAt(key).getPredicates();
	}
	
	public int getRefLevel () {
		return refLevel;
	}
	
	public void setRefLevel (int refLevel) {
		hashCode = -1;
		this.refLevel = refLevel;
	}
	
	public void incrementRefLevel () {
		if (!isAbsolute()) {
			refLevel++;
		}
	}
	
	public boolean isAbsolute () {
		return refLevel == REF_ABSOLUTE;
	}
	
	//return true if this ref contains any unbound multiplicities... ie, there is ANY chance this ref
	//could ambiguously refer to more than one instance node.
	public boolean isAmbiguous () {
		//ignore level 0, as /data implies /data[0]
		for (int i = 1; i < size(); i++) {
			if (getMultiplicity(i) == INDEX_UNBOUND) {
				return true;
			}
		}
		return false;
	}
	
	//return a copy of the ref
	public TreeReference clone () {
		TreeReference newRef = new TreeReference();
		newRef.setRefLevel(this.refLevel);
		
		for(TreeReferenceLevel l : data) {
			newRef.add(l.shallowCopy());
		}

		//TODO: No more == null checks here, use context type
		//copy instances
		if(instanceName != null)
		{
			newRef.setInstanceName(instanceName);
		}
		newRef.contextType = this.contextType;
		return newRef;
	}
	
	/*
	 * chop the lowest level off the ref so that the ref now represents the parent of the original ref
	 * return true if we successfully got the parent, false if there were no higher levels
	 */
	public boolean removeLastLevel () {
		int size = size();
		hashCode = -1;
		this.size = -1;
		if (size == 0) {
			if (isAbsolute()) {
				return false;
			} else {
				refLevel++;
				return true;
			}
		} else {
			data.removeElementAt(size -1);
			return true;
		}
	}
	
	public TreeReference getParentRef () {
		//TODO: level
		TreeReference ref = this.clone();
		if (ref.removeLastLevel()) {
			return ref;
		} else {
			return null;
		}
	}
	
	//return a new reference that is this reference anchored to a passed-in parent reference
	//if this reference is absolute, return self
	//if this ref has 'parent' steps (..), it can only be anchored if the parent ref is a relative ref consisting only of other 'parent' steps
	//return null in these invalid situations
	public TreeReference parent (TreeReference parentRef) {
		if (isAbsolute()) {
			return this;
		} else {
			TreeReference newRef = parentRef.clone();
			
			if (refLevel > 0) {
				if (!parentRef.isAbsolute() && parentRef.size() == 0) {
					parentRef.refLevel += refLevel;
				} else {
					return null;
				}
			}
			
			for(TreeReferenceLevel l : data) {
				newRef.add(l.shallowCopy());
			}

			return newRef;			
		}
	}
	
	
	//very similar to parent(), but assumes contextRef refers to a singular, existing node in the model
	//this means we can do '/a/b/c + ../../d/e/f = /a/d/e/f', which we couldn't do in parent()
	//return null if context ref is not absolute, or we parent up past the root node
	//NOTE: this function still works even when contextRef contains INDEX_UNBOUND multiplicites... conditions depend on this behavior,
	//  even though it's slightly icky
	public TreeReference anchor (TreeReference contextRef) {
		//TODO: Technically we should possibly be modifying context stuff here
		//instead of in the xpath stuff;
		
		if (isAbsolute()) {
			return this.clone();
		} else if (!contextRef.isAbsolute()) {
			return null;
		} else {
			TreeReference newRef = contextRef.clone();
			int contextSize = contextRef.size();
			if (refLevel > contextSize) {
				return null; //tried to do '/..'
			} else {			
				for (int i = 0; i < refLevel; i++) {
					newRef.removeLastLevel();
				}
				for (int i = 0; i < size(); i++) {
					newRef.add(data.elementAt(i).shallowCopy());
				}
				return newRef;
			}
		}
	}
	
	//TODO: merge anchor() and parent()
		
	public TreeReference contextualize (TreeReference contextRef) {
		//TODO: Technically we should possibly be modifying context stuff here
		//instead of in the xpath stuff;
		if (!contextRef.isAbsolute()){
			return null;
		}
		
		//If we're an absolute node, we should already know what our instance is, so
		//we can't apply any further contextualizaiton unless the instances match
		if(this.isAbsolute()) {
			//If this refers to the main instance, but our context ref doesn't
			if(this.getInstanceName() == null) {
				if(contextRef.getInstanceName() != null) {
					return this.clone();
				}
			} 
			//Or if this refers to another instance and the context ref doesn't refer to the 
			//same instance
			else if(!this.getInstanceName().equals(contextRef.getInstanceName())) {
				return this.clone();
			}
		}
		
		TreeReference newRef = anchor(contextRef);
		newRef.setContext(contextRef.getContext());
		
		//apply multiplicites and fill in wildcards as necessary based on the context ref
		for (int i = 0; i < contextRef.size() && i < newRef.size(); i++) {
			
			//If the the contextRef can provide a definition for a wildcard, do so
			if(TreeReference.NAME_WILDCARD.equals(newRef.getName(i)) && !TreeReference.NAME_WILDCARD.equals(contextRef.getName(i))) {
				newRef.data.setElementAt(newRef.data.elementAt(i).setName(contextRef.getName(i)), i);
			}
			
			if (contextRef.getName(i).equals(newRef.getName(i))) {
				//We can't actually merge nodes if the newRef has predicates or filters
				//on this expression, since those reset any existing resolutions which
				//may have been done.
				if(newRef.getPredicate(i) == null) {
					newRef.setMultiplicity(i, contextRef.getMultiplicity(i));
				}
			} else {
				break;
			}
		}

		return newRef;
	}
	
	public TreeReference relativize (TreeReference parent) {
		if (parent.isParentOf(this, false)) {
			TreeReference relRef = selfRef();
			for (int i = parent.size(); i < this.size(); i++) {
				relRef.add(this.getName(i), INDEX_UNBOUND);
			}
			return relRef;
		} else {
			return null;
		}
	}
	
	//turn unambiguous ref into a generic ref
	public TreeReference genericize () {	
		TreeReference genericRef = clone();
		for (int i = 0; i < genericRef.size(); i++) {
			//TODO: It's not super clear whether template refs should get
			//genericized or not
			if(genericRef.getMultiplicity(i) > -1 || genericRef.getMultiplicity(i) == INDEX_TEMPLATE) {
				genericRef.setMultiplicity(i, INDEX_UNBOUND);
			}
		}
		return genericRef;
	}
	
	//returns true if 'this' is parent of 'child'
	//return true if 'this' equals 'child' only if properParent is false
	public boolean isParentOf (TreeReference child, boolean properParent) {
		//Instances and context types;
		if (refLevel != child.refLevel)
			return false;
		if (child.size() < size() + (properParent ? 1 : 0))
			return false;
		
		for (int i = 0; i < size(); i++) {
			if (!this.getName(i).equals(child.getName(i))) {
				return false;
			}
			
			int parMult = this.getMultiplicity(i);
			int childMult = child.getMultiplicity(i);
			if (parMult != INDEX_UNBOUND && parMult != childMult && !(i == 0 && parMult == 0 && childMult == INDEX_UNBOUND)) {
				return false;
			}
		}
		
		return true;
	}
		
	/**
	 * clone and extend a reference by one level
	 * @param ref
	 * @param name
	 * @param mult
	 * @return
	 */
	public TreeReference extendRef (String name, int mult) {
		//TODO: Shouldn't work for this if this is an attribute ref;
		TreeReference childRef = this.clone();
		childRef.add(name, mult);
		return childRef;
	}
	
	public boolean equals (Object o) {
		if (this == o) {
			return true;
		} else if (o instanceof TreeReference) {
			TreeReference ref = (TreeReference)o;
			
			if (this.refLevel == ref.refLevel && this.size() == ref.size()) {
				
				for(int i = 0 ; i < this.size(); i++) {
					TreeReferenceLevel l = data.elementAt(i);
					TreeReferenceLevel other = ref.data.elementAt(i);
					
					//we should expect this to hit a lot due to interning
					if(l.equals(other)) {
						continue;
					} else { return false;}
				}
				
//				for (int i = 0; i < this.size(); i++) {
//					String nameA = this.getName(i);
//					String nameB = ref.getName(i);
//					int multA = this.getMultiplicity(i);
//					int multB = ref.getMultiplicity(i);
//					
//					Vector<XPathExpression> predA = this.getPredicate(i);
//					Vector<XPathExpression> predB = ref.getPredicate(i);
//					
//					if (!nameA.equals(nameB)) {
//						return false;
//					} else if (multA != multB) {
//						if (i == 0 && (multA == 0 || multA == INDEX_UNBOUND) && (multB == 0 || multB == INDEX_UNBOUND)) {
//							// /data and /data[0] are functionally the same
//						} else {
//							return false;
//						}
//					} else if(predA != null && predB != null) {
//						if(predA.size() != predB.size()) { return false;}
//						for(int j = 0 ; j < predA.size() ; ++j) {
//							if(!predA.elementAt(j).equals(predB.elementAt(j))) {
//								return false;
//							}
//						}
//					} else if((predA == null && predB != null) || (predA != null && predB == null)){
//						return false;
//					}
//				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public int hashCode () {
		if(hashCode != -1 ) {
			return hashCode;
		}
		int hash = (DataUtil.integer(refLevel)).hashCode();
		for (int i = 0; i < size(); i++) {
			//NOTE(ctsims): It looks like this is only using Integer to
			//get the hashcode method, but that method
			//is just returning the int value, I think, so
			//this should potentially just be replaced by
			//an int.
			Integer mult = DataUtil.integer(getMultiplicity(i));
			if (i == 0 && mult.intValue() == INDEX_UNBOUND)
				mult = DataUtil.integer(0);
			
			hash ^= getName(i).hashCode();
			hash ^= mult.hashCode();
			Vector<XPathExpression> predicates = this.getPredicate(i);
			if(predicates == null) {
				continue;
			}
			int val = 0;
			for(XPathExpression xpe : predicates) {
				hash ^= val; 
				hash ^= xpe.hashCode();
				++val;
			}
		}
		hashCode = hash;
		return hash;
	}
	
	public String toString () {
		return toString(true);
	}
	
	public String toString (boolean includePredicates) {
		StringBuffer sb = new StringBuffer();
		if(instanceName != null)
		{
			sb.append("instance("+instanceName+")");
		} else if(contextType == CONTEXT_ORIGINAL) {
			sb.append("current()");
		}
		if (isAbsolute()) {
			sb.append("/");
		} else {
			for (int i = 0; i < refLevel; i++)
				sb.append("../");
		}
		for (int i = 0; i < size(); i++) {
			String name = getName(i);
			int mult = getMultiplicity(i);
			
			if(mult == INDEX_ATTRIBUTE) {
				sb.append("@");
			}
			sb.append(name);
			
			if (includePredicates) {
				switch (mult) {
				case INDEX_UNBOUND: break;
				case INDEX_TEMPLATE: sb.append("[@template]"); break;
				case INDEX_REPEAT_JUNCTURE: sb.append("[@juncture]"); break;
				default:
					if ((i > 0 || mult != 0) && mult !=-4)
						sb.append("[" + (mult + 1) + "]");
					break;
				}
			}
			
			if (i < size() - 1)
				sb.append("/");
		}
		return sb.toString();
	}

	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		refLevel = ExtUtil.readInt(in);
		instanceName = (String)ExtUtil.read(in, new ExtWrapNullable(String.class),pf);
		contextType = ExtUtil.readInt(in);
		int size = ExtUtil.readInt(in);
		for(int i = 0 ; i < size; ++i) {
			TreeReferenceLevel level = (TreeReferenceLevel)ExtUtil.read(in, TreeReferenceLevel.class);
			this.add(level.intern());
		}
	}

	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, refLevel);
		ExtUtil.write(out, new ExtWrapNullable(instanceName));
		ExtUtil.writeNumeric(out, contextType);
		ExtUtil.writeNumeric(out, size());
		for(TreeReferenceLevel l : data) {
			ExtUtil.write(out, l);
		}
	}

	/** Intersect this tree reference with another, returning a new tree reference
	 *  which contains all of the common elements, starting with the root element.
	 *  
	 *  Note that relative references by their nature can't share steps, so intersecting
	 *  any (or by any) relative ref will result in the root ref. Additionally, if the
	 *  two references don't share any steps, the intersection will consist of the root
	 *  reference.
	 *  
	 * @param b The tree reference to intersect
	 * @return The tree reference containing the common basis of this ref and b
	 */
	public TreeReference intersect(TreeReference b) {
		if(!this.isAbsolute() || !b.isAbsolute()) {
			return TreeReference.rootRef();
		}
		if(this.equals(b)) { return this;}
	
	
		TreeReference a;
		//A should always be bigger if one ref is larger than the other
		if(this.size() < b.size()) { a = b.clone() ; b = this.clone();}
		else { a= this.clone(); b = b.clone();}
		
		//Now, trim the refs to the same length.
		int diff = a.size() - b.size();
		for(int i = 0; i < diff; ++i) {
			a.removeLastLevel();
		}
		
		int aSize = a.size();
		//easy, but requires a lot of re-evaluation.
		for(int i = 0 ; i <=  aSize; ++i) {
			if(a.equals(b)) {
				return a;
			} else if(a.size() == 0) {
				return TreeReference.rootRef();
			} else {
				if(!a.removeLastLevel() || !b.removeLastLevel()) {
					//I don't think it should be possible for us to get here, so flip if we do
					throw new RuntimeException("Dug too deply into TreeReference during intersection");
				}
			}
		}
		
		//The only way to get here is if a's size is -1
		throw new RuntimeException("Impossible state");
	}

	//TODO: This should be in construction
	public void setContext(int context) {
		hashCode = -1;
		this.contextType = context;
	}

	public int getContext() {
		return this.contextType;
	}
	
	/**
	 * Returns the subreference of this reference up to the level specified.
	 * 
	 * Used to identify the reference context for a predicate at the same level
	 * 
	 * Must be an absolute reference, otherwise will throw IllegalArgumentException
	 * 
	 * @param i
	 * @return
	 */
	public TreeReference getSubReference(int level) {
		if(!this.isAbsolute()) { throw new IllegalArgumentException("Cannot subreference a non-absolute ref"); }
		
		//Copy construct
		TreeReference ret = new TreeReference();
		ret.refLevel = this.refLevel;
		ret.contextType = this.contextType;
		ret.instanceName = this.instanceName;
		ret.data = new Vector<TreeReferenceLevel>();
		for(int i = 0 ; i <= level ; ++i) {
			ret.data.addElement(this.data.elementAt(i));
		}
		return ret;
	}

	public boolean hasPredicates() {
		for(TreeReferenceLevel level : data) {
			if(level.getPredicates() != null) {
				return true;
			}
		}
		return false;
	}

	public TreeReference removePredicates() {
		hashCode = -1;
		TreeReference predicateless = clone();
		for(int i = 0; i < predicateless.data.size(); ++i) {
			predicateless.data.setElementAt(predicateless.data.elementAt(i).setPredicates(null), i	);
		}
		return predicateless;
	}
}