/**
 * 
 */
package org.commcare.util;

import org.commcare.view.CommCareStartupInteraction;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.services.Logger;


/**
 * @author ctsims
 *
 */
public abstract class CommCareInitializer implements Runnable {

	protected static int RESPONSE_NONE = 0;
	protected static int RESPONSE_YES = 1;
	protected static int RESPONSE_NO = 2;
	
	private InitializationListener listener;
	int response = RESPONSE_NONE;

	public void initialize(InitializationListener listener) {
		this.listener = listener;
		Thread t = new Thread(this);
		listener.setInitThread(t);
		t.run();
	}
	
	protected abstract void runWrapper() throws UnfullfilledRequirementsException;
	
	public void run() {
		try {
			runWrapper();
			listener.onSuccess();
		} catch(Exception e) {
			Logger.exception(e);
			fail(e);
		}
	}
	
	protected void fail(Exception e) {
		setMessage(CommCareStartupInteraction.failSafeText("commcare.fail", "There was an error, and CommCare could not be started."));
		listener.onFailure();
	}
	
	protected abstract void setMessage(String message);
	
	protected abstract void askForResponse(String message, YesNoListener listener);
	
	protected boolean blockForResponse(String message) {
		response = RESPONSE_NONE;
		askForResponse(message,  new YesNoListener() {
			public void no() {
				CommCareInitializer.this.response = CommCareInitializer.RESPONSE_NO;
			}
			public void yes() {
				CommCareInitializer.this.response = CommCareInitializer.RESPONSE_YES;
			}
			
		});
		while(response == RESPONSE_NONE);
		return response == RESPONSE_YES;
	}
}
