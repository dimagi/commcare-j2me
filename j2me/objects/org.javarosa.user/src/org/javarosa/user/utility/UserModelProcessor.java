/**
 *
 */
package org.javarosa.user.utility;

import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.IInstanceProcessor;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.user.model.User;
import org.javarosa.xform.util.XFormAnswerDataSerializer;

import java.util.Stack;
import java.util.Vector;

/**
 * The UserModel Processor is responsible for reading in a
 * UserXML data block, parsing out the data elements relevant
 * to creating or updating user models, and returning a created
 * user without modifying the internal storage.
 *
 * TODO: The failures are not very well handled
 *
 * @author ctsims
 *
 */
public class UserModelProcessor implements IInstanceProcessor {

    private static final String elementName = "registration";
    private  User user;

    XFormAnswerDataSerializer serializer = new XFormAnswerDataSerializer();

    /* (non-Javadoc)
     * @see org.javarosa.core.model.utils.IInstanceProcessor#processInstance(org.javarosa.core.model.instance.FormInstance)
     */
    public void processInstance(FormInstance tree) {
        Vector<AbstractTreeElement> registrationNodes = scrapeForRegistrations(tree);
        int failures = 0;
        int parsed = 0;

        String messages = "";

        for(AbstractTreeElement element : registrationNodes) {
            try {
                parseRegistration(element);
                parsed++;
            } catch (MalformedUserModelException e) {
                e.printStackTrace();
                messages += e.getMessage() + ", ";
                failures++;
            } catch (StorageFullException e) {
                e.printStackTrace();
                messages += e.getMessage() + ", ";
                failures++;
            }
        }

        if(failures > 0) {
            throw new RuntimeException("Errors while parsing or saving user model! " +
                    failures+ " models failed to parse, " + parsed +
                    " models succesfully parsed. Failure messages: " +
                    messages);
        }
    }

    public User getRegisteredUser() {
        return user;
    }

    private void parseRegistration(AbstractTreeElement head) throws MalformedUserModelException {

        String username = getString(getChild(head, "username"),head);
        String password = getString(getChild(head, "password"),head);
        String uuid = getString(getChild(head,"uuid"),head);

        User u = getUserFromStorage(uuid);

        if(u == null) {
            u = new User(username,password, uuid);
        } else {
            if(!u.getUsername().equals(username)) {
                u.setUsername(username);
            }
            if(!u.getPassword().equals(password)) {
                u.setPassword(password);
            }
        }

        AbstractTreeElement data = getChild(head,"user_data");

        for(int i = 0; i < data.getNumChildren(); ++i) {
            AbstractTreeElement datum = data.getChildAt(i);
            if(!datum.isRelevant()) {
                continue;
            }
            String keyName = datum.getAttributeValue(null,"key");
            if(keyName == null) {
                throw new MalformedUserModelException("User data for user" + username + "has a data element with no key");
            }

            String value = getStringOrNull(datum,data);
            if(value != null) {
                u.setProperty(keyName, value);
            }
        }

        user = u;
    }

    private IStorageUtilityIndexed storage() {
        return (IStorageUtilityIndexed)StorageManager.getStorage(User.STORAGE_KEY);
    }

    private User getUserFromStorage(String uuid) {
        IStorageUtilityIndexed storage = storage();
        Vector<Integer> IDs = storage.getIDsForValue(User.META_UID, uuid);
        if(!(IDs.size() > 0)) {
            return null;
        } else {
            return (User)storage.read(IDs.elementAt(0).intValue());
        }
    }

    private AbstractTreeElement getChild(AbstractTreeElement parent, String name) throws MalformedUserModelException{
        Vector<AbstractTreeElement> v = parent.getChildrenWithName(name);
        if(v.isEmpty()) {
            throw new MalformedUserModelException("Expected a node '" + name + "' in element: " + parent.getName());
        } else if(v.size() > 1) {
            throw new MalformedUserModelException("Too many children named: '" + name + "' in element: " + parent.getName());
        }

        AbstractTreeElement e = v.elementAt(0);

        if(!e.isRelevant()) {
            throw new MalformedUserModelException("Expected a node '" + name + "' in element: " + parent.getName());
        }

        return e;
    }

    private String getString(AbstractTreeElement element, AbstractTreeElement parent) throws MalformedUserModelException {


        if(element.getValue() == null) {
            throw new MalformedUserModelException("No data in Element: '" + element.getName() + "' with parent: " + parent.getName());
        }

        return element.getValue().uncast().getString();
    }

    private String getStringOrNull(AbstractTreeElement element, AbstractTreeElement parent) {


        if(element.getValue() == null) {
            return null;
        }

        return element.getValue().uncast().getString();
    }



    private Vector<AbstractTreeElement> scrapeForRegistrations(FormInstance tree) {
        Stack<AbstractTreeElement> stack = new Stack<AbstractTreeElement>();
        Vector<AbstractTreeElement> registrations = new Vector<AbstractTreeElement>();

        stack.push(tree.getRoot());

        while(!stack.empty()) {
            AbstractTreeElement seeker = stack.pop();

            //TODO: Namespace support!
            if(seeker.getName().equals(elementName) && seeker.isRelevant()) {
                registrations.addElement(seeker);
            }

            for(int i = 0; i < seeker.getNumChildren(); ++i) {
                AbstractTreeElement child = seeker.getChildAt(i);
                //Skip non-relevant kids
                if(!child.isRelevant()) {
                    continue;
                }

                //otherwise, put it in the stack!
                stack.push(child);
            }
        }
        return registrations;
    }
}
