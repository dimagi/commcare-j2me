/**
 * 
 */
package org.commcare.util;

import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.resources.model.installers.ProfileInstaller;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;

/**
 * TODO: This isn't really a great candidate for a 
 * singleton interface. It should almost certainly be
 * a more broad code-based installer/registration
 * process or something.
 * 
 * Also: It shares a lot of similarities with the 
 * Context app object in j2me. Maybe we should roll
 * some of that in.
 * 
 * @author ctsims
 *
 */
public class CommCareManager implements CommCareInstance {
	//TODO: We should make this unique using the parser to invalidate this ID or something
	private static final String APP_PROFILE_RESOURCE_ID = "commcare-application-profile";
	
	private Vector<Integer> suites;
	private int profile;
	
	public CommCareManager() {
		profile = -1;
		suites = new Vector<Integer>();
	}
	
	public void init(String profileReference, ResourceTable global) {
		try {

			if (!global.isReady()) {
				global.prepareResources(null);
			}
			
			// First, see if the appropriate profile exists
			Resource profile = global.getResourceWithId(APP_PROFILE_RESOURCE_ID);
			
			//If it does not, we need to grab it locally, and get parsing...
			if (profile == null) {

				Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
				locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, profileReference));
				
				//We need a way to identify this version...
				Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN, APP_PROFILE_RESOURCE_ID , locations);

				global.addResource(r, new ProfileInstaller(), "");
				global.prepareResources(null);
			} else{
				//Assuming it does exist, we might want to do an automatic
				//upgrade here, leaving that for a future date....
			}
		}
		catch (UnresolvedResourceException e) {
			e.printStackTrace();
			throw new RuntimeException("Initialization Failed.");
		} catch (StorageFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void upgrade(ResourceTable global, ResourceTable temporary) {

		if (!global.isReady()) {
			throw new RuntimeException("The Global Resource Table was not properly made ready");
		}
		
		//In the future: Continuable upgrades. Now: Clear old upgrade info
		temporary.clear();
		
		Profile current = getCurrentProfile();
		String profileReference = current.getAuthReference();
		
		Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
		locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, profileReference));
			
		//We need a way to identify this version...
		Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN, APP_PROFILE_RESOURCE_ID , locations);
		
		try {
			temporary.addResource(r, new ProfileInstaller(), null);
			temporary.prepareResources(global);
			global.upgradeTable(temporary);
			
			//Not implemented yet!
			//upgradeTable.destroy();
		} catch (StorageFullException e) {
			e.printStackTrace();
			throw new RuntimeException("Storage Full while trying to upgrade! Bad! Clear some room on the device and try again");
		} catch (UnresolvedResourceException e) {
			e.printStackTrace();
			throw new RuntimeException("A Resource couldn't be found while trying to upgrade!");
		}
		profile = -1;
		suites.removeAllElements();
		//Is it really possible to verify that we've un-registered everything here? Locale files are 
		//registered elsewhere, and we can't guarantee we're the only thing in there, so we can't
		//straight up clear it...
		
		initialize(global);
	}
	
	public Profile getCurrentProfile() {
		return (Profile)(StorageManager.getStorage(Profile.STORAGE_KEY).read(profile));
	}
	
	public Vector<Suite> getInstalledSuites() {
		Vector<Suite> installedSuites = new Vector<Suite>();
		IStorageUtility utility = StorageManager.getStorage(Suite.STORAGE_KEY);
		for(Integer i : suites) {
			installedSuites.addElement((Suite)(utility.read(i.intValue())));
		}
		return installedSuites;
	}
	
	public void setProfile(Profile p) {
		this.profile = p.getID();
	}
	
	
	public void registerSuite(Suite s) {
		this.suites.addElement(new Integer(s.getID()));
	}
	
	public void initialize(ResourceTable global) {
		try {
			global.initializeResources(this);
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing Resource! "+ e.getMessage());
		}
	}
}
