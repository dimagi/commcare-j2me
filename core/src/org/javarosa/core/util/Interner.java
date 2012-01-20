/**
 * 
 */
package org.javarosa.core.util;

import java.util.Hashtable;

/**
 * @author ctsims
 *
 */
public class Interner {
	Hashtable<String, String> cache; 
	
	public Interner(){
		cache = new Hashtable<String, String>();
	}
	
	public String intern(String in) {
		if(cache.containsKey(in)) {
			return cache.get(in);
		} else{
			cache.put(in,in);
			return in;
		}
	}
	
	public void release() {
		cache.clear();
	}
}
