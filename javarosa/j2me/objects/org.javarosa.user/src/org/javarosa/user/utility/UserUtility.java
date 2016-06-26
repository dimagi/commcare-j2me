/**
 *
 */
package org.javarosa.user.utility;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.core.model.User;

import javax.microedition.midlet.MIDlet;

/**
 * @author Clayton Sims
 * @date Mar 16, 2009
 *
 */
public class UserUtility {

    public static final String ADMIN_PW_PROPERTY = "JavaRosa-Admin-Password";

    //#ifdef admin.pw.default:defined
    //#=    private static final String defaultPassword = "${admin.pw.default}";
    //#else
            private static final String defaultPassword = "234";
    //#endif

    public static void populateAdminUser(MIDlet m) {
        IStorageUtility users = StorageManager.getStorage(User.STORAGE_KEY);

        boolean adminFound = false;
        IStorageIterator ui = users.iterate();
        while (ui.hasMore()) {
            User user = (User)ui.nextRecord();
            if (User.ADMINUSER.equals(user.getUserType())) {
                adminFound = true;
                break;
            }
        }

        // There is no admin user to update, so add the user
        if(!adminFound) {
            //TODO: Test for MIDlet-Jar-RSA-SHA1 or other signing mechanism before allowing the property to be pulled?
            String defaultFromEnvironment = m == null ? null : m.getAppProperty(ADMIN_PW_PROPERTY);

            User admin = new User("admin", defaultFromEnvironment != null ? defaultFromEnvironment : defaultPassword, PropertyUtils.genGUID(25), User.ADMINUSER);
            admin.setUuid(PropertyManager._().getSingularProperty("DeviceID"));

            users.write(admin);
        }
    }

    public static User demoUser(boolean isAdmin) {
        return new User("demo", "", "demo"+PropertyUtils.genGUID(25), isAdmin ? User.ADMINUSER : User.DEMO_USER);
    }
}
