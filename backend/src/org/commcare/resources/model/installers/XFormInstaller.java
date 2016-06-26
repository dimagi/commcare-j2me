/**
 *
 */
package org.commcare.resources.model.installers;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnreliableSourceException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.CommCareInstance;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.core.util.PrefixTreeNode;
import org.javarosa.core.util.SizeBoundUniqueVector;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.util.XFormUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Vector;

/**
 * @author ctsims
 */
public class XFormInstaller extends CacheInstaller<FormDef> {

    protected String getCacheKey() {
        return FormDef.STORAGE_KEY;
    }

    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCareInstance instance, boolean upgrade) throws UnresolvedResourceException {
        InputStream incoming = null;
        try {
            if (location.getAuthority() == Resource.RESOURCE_AUTHORITY_CACHE) {
                //If it's in the cache, we should just get it from there
                return false;
            } else {
                incoming = ref.getStream();
                if (incoming == null) {
                    return false;
                }
                FormDef formDef = XFormUtils.getFormRaw(new InputStreamReader(incoming, "UTF-8"));
                if (formDef == null) {
                    //Bad Form!
                    return false;
                }
                if (upgrade) {
                    //There's already a record in the cache with this namespace, so we can't ovewrite it.
                    //TODO: If something broke, this record might already exist. Might be worth checking.
                    formDef.getInstance().schema = formDef.getInstance().schema + UPGRADE_EXT;
                    storage().write(formDef);
                    cacheLocation = formDef.getID();

                    //Resource is installed and ready for upgrade
                    table.commit(r, Resource.RESOURCE_STATUS_UPGRADE);
                } else {
                    storage().write(formDef);
                    cacheLocation = formDef.getID();
                    //Resource is fully installed
                    table.commit(r, Resource.RESOURCE_STATUS_INSTALLED);
                }
                return true;
            }
        } catch (StorageFullException e) {
            //Couldn't install, no room left in storage.
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            throw new UnreliableSourceException(r, e.getMessage());
        } catch (XFormParseException xpe) {
            throw new UnresolvedResourceException(r, xpe.getMessage(), true);
        } finally {
            try {
                if (incoming != null) {
                    incoming.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public boolean upgrade(Resource r) throws UnresolvedResourceException {
        //Basically some content as revert. Merge;
        FormDef form = storage().read(cacheLocation);
        String tempString = form.getInstance().schema;

        //Atomic. Don't re-do this if it was already done.
        if (tempString.indexOf(UPGRADE_EXT) != -1) {
            form.getInstance().schema = tempString.substring(0, tempString.indexOf(UPGRADE_EXT));
            storage().write(form);
        }
        return true;
    }

    private static final String UPGRADE_EXT = "_TEMP";
    private static final String STAGING_EXT = "_STAGING-OPENROSA";
    private static final String[] exts = new String[]{UPGRADE_EXT, STAGING_EXT};

    public boolean unstage(Resource r, int newStatus) {
        //This either unstages back to upgrade mode or
        //to unstaged mode. Figure out which one
        String destination = UPGRADE_EXT;
        if (newStatus == Resource.RESOURCE_STATUS_UNSTAGED) {
            destination = STAGING_EXT;
        }

        //Make sure that this form's
        FormDef form = storage().read(cacheLocation);
        String tempString = form.getInstance().schema;

        //This method should basically be atomic, so don't re-temp it if it's already
        //temp'd.
        if (tempString.indexOf(destination) != -1) {
            return true;
        } else {
            form.getInstance().schema = form.getInstance().schema + destination;
            storage().write(form);
            return true;
        }
    }

    public boolean revert(Resource r, ResourceTable table) {
        //Basically some content as upgrade. Merge;
        FormDef form = storage().read(cacheLocation);
        String tempString = form.getInstance().schema;

        //TODO: Aggressively wipe out anything which might conflict with the uniqueness
        //of the new schema

        for (String ext : exts) {
            //Removing any staging/upgrade placeholders.
            if (tempString.indexOf(ext) != -1) {
                form.getInstance().schema = tempString.substring(0, tempString.indexOf(ext));
                storage().write(form);
            }
        }
        return true;
    }

    public int rollback(Resource r) {
        int status = r.getStatus();

        FormDef form = storage().read(cacheLocation);
        String currentSchema = form.getInstance().schema;

        //Just figure out whether we finished and return that

        switch (status) {
            case Resource.RESOURCE_STATUS_INSTALL_TO_UNSTAGE:
            case Resource.RESOURCE_STATUS_UNSTAGE_TO_INSTALL:
                if (currentSchema.indexOf(STAGING_EXT) != -1) {
                    return Resource.RESOURCE_STATUS_UNSTAGED;
                } else {
                    return Resource.RESOURCE_STATUS_INSTALLED;
                }
            case Resource.RESOURCE_STATUS_UPGRADE_TO_INSTALL:
            case Resource.RESOURCE_STATUS_INSTALL_TO_UPGRADE:
                if (currentSchema.indexOf(UPGRADE_EXT) != -1) {
                    return Resource.RESOURCE_STATUS_UPGRADE;
                } else {
                    return Resource.RESOURCE_STATUS_INSTALLED;
                }
            default:
                throw new RuntimeException("Unexpected status for rollback! " + status);
        }
    }


    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problems) {

        SizeBoundUniqueVector sizeBoundProblems = (SizeBoundUniqueVector)problems;

        //Check to see whether the formDef exists and reads correctly
        FormDef formDef;
        try {
            formDef = storage().read(cacheLocation);
        } catch (Exception e) {
            sizeBoundProblems.addElement(new MissingMediaException(r, "Form did not properly save into persistent storage"));
            return true;
        }
        //Otherwise, we want to figure out if the form has media, and we need to see whether it's properly
        //available
        Localizer localizer = formDef.getLocalizer();
        //get this out of the memory ASAP!
        formDef = null;
        if (localizer == null) {
            //things are fine
            return false;
        }

        for (String locale : localizer.getAvailableLocales()) {
            OrderedHashtable<String, PrefixTreeNode> localeData = localizer.getLocaleData(locale);
            for (Enumeration en = localeData.keys(); en.hasMoreElements(); ) {
                String key = (String)en.nextElement();
                if (key.indexOf(";") != -1) {
                    //got some forms here
                    String form = key.substring(key.indexOf(";") + 1, key.length());

                    if (form.equals(FormEntryCaption.TEXT_FORM_VIDEO)) {
                        String externalMedia = localeData.get(key).render();
                        InstallerUtil.checkMedia(r, externalMedia, sizeBoundProblems, InstallerUtil.MediaType.VIDEO);
                    }

                    if (form.equals(FormEntryCaption.TEXT_FORM_IMAGE)) {
                        String externalMedia = localeData.get(key).render();
                        InstallerUtil.checkMedia(r, externalMedia, sizeBoundProblems, InstallerUtil.MediaType.IMAGE);
                    }

                    if (form.equals(FormEntryCaption.TEXT_FORM_AUDIO)) {
                        String externalMedia = localeData.get(key).render();
                        InstallerUtil.checkMedia(r, externalMedia, sizeBoundProblems, InstallerUtil.MediaType.AUDIO);
                    }
                }
            }
        }
        return sizeBoundProblems.size() != 0;
    }
}
