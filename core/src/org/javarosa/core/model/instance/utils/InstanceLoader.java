package org.javarosa.core.model.instance.utils;

import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.reference.ReferenceFactory;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.Logger;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.IElementHandler;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.javarosa.core.util.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

public class InstanceLoader {
    private final static String TAG = InstanceLoader.class.getSimpleName();

    /**
     */
    public static boolean importData(byte[] fileBytes, FormEntryController fec) {
        // convert files into a byte array

        // get the root of the saved and template instances
        TreeElement savedRoot = XFormParser.restoreDataModel(fileBytes, null).getRoot();
        TreeElement templateRoot = fec.getModel().getForm().getInstance().getRoot().deepCopy(true);

        // weak check for matching forms
        if (!savedRoot.getName().equals(templateRoot.getName()) || savedRoot.getMult() != 0) {
            Logger.log(TAG, "Saved form instance does not match template form definition");
            return false;
        } else {
            // populate the data model
            TreeReference tr = TreeReference.rootRef();
            tr.add(templateRoot.getName(), TreeReference.INDEX_UNBOUND);
            templateRoot.populate(savedRoot, fec.getModel().getForm());

            // populated model to current form
            fec.getModel().getForm().getInstance().setRoot(templateRoot);

            // fix any language issues
            // : http://bitbucket.org/javarosa/main/issue/5/itext-n-appearing-in-restored-instances
            if (fec.getModel().getLanguages() != null) {
                fec.getModel()
                        .getForm()
                        .localeChanged(fec.getModel().getLanguage(),
                            fec.getModel().getForm().getLocalizer());
            }
            return true;
        }
    }

    public static FormEntryController loadInstance(String formMediaPath, File formDefFile, byte[] fileBytes,
                                                   File formBin, File formXml, boolean isReadOnly,
                                                   PrototypeFactory protoFactory, InstanceInitializationFactory iif,
                                                   Vector<IFunctionHandler> funcHandlers,
                                                   Vector<Pair> parserHandlers,
                                                   Vector<Pair> actionParsers,
                                                   ReferenceFactory fileRefFactory) throws IOException {
        FormEntryController fec;
        FormDef fd = null;
        FileInputStream fis;

        if (formBin.exists()) {
            // if we have binary, deserialize binary
            // Log.i(TAG, "Attempting to load " + formXml.getName() +
            //        " from cached file: " + formBin.getAbsolutePath());
            fd = deserializeFormDef(formBin, protoFactory);
            if (fd == null) {
                // some error occured with deserialization. Remove the file, and make a new .formdef
                // from xml
                // Log.w(TAG, "Deserialization FAILED!  Deleting cache file: " +
                //        formBin.getAbsolutePath());
                formBin.delete();
            }
        }

        // If we couldn't find a cached version, load the form from the XML
        if (fd == null) {
            // no binary, read from xml
            // Log.i(TAG, "Attempting to load from: " + formXml.getAbsolutePath());
            fis = new FileInputStream(formXml);

            for (Pair handlerPair : parserHandlers) {
                XFormParser.registerHandler((String)handlerPair.first(), (IElementHandler)handlerPair.second());
            }

            for (Pair actionPair : actionParsers) {
                XFormParser.registerStructuredAction((String)actionPair.first(), (IElementHandler)actionPair.second());
            }

            fd = XFormUtils.getFormFromInputStream(fis);
            if (fd == null) {
                throw new RuntimeException("Error reading XForm file");
            }
        }

        // Try to write the form definition to a cached location
        try {
            serializeFormDef(fd, formDefFile);
        } catch(Exception e) {
            // The cache is a bonus, so if we can't write it, don't crash, but log 
            // it so we can clean up whatever is preventing the cached version from
            // working
            // Logger.log(AndroidLogger.TYPE_RESOURCES, "XForm could not be serialized. Error trace:\n" + ExceptionReportTask.getStackTrace(e));
        }

        for (IFunctionHandler func : funcHandlers) {
            fd.exprEvalContext.addFunctionHandler(func);
        }
        // create FormEntryController from formdef
        FormEntryModel fem = new FormEntryModel(fd);
        fec = new FormEntryController(fem);

        //TODO: Get a reasonable IIF object
        //iif = something
        // import existing data into formdef
        if (fileBytes != null) {
            // This order is important. Import data, then initialize.
            InstanceLoader.importData(fileBytes, fec);
            fd.initialize(false, iif);
        } else {
            fd.initialize(true, iif);
        }
        if(isReadOnly) {
            fd.getInstance().getRoot().setEnabled(false);
        }

        // set paths to /sdcard/odk/forms/formfilename-media/
        String formFileName = formXml.getName().substring(0, formXml.getName().lastIndexOf("."));

        // Remove previous forms
        ReferenceManager._().clearSession();

        if (formMediaPath != null) {
            ReferenceManager._().addSessionRootTranslator(
                    new RootTranslator("jr://images/", formMediaPath));
                ReferenceManager._().addSessionRootTranslator(
                    new RootTranslator("jr://audio/", formMediaPath));
                ReferenceManager._().addSessionRootTranslator(
                    new RootTranslator("jr://video/", formMediaPath));

        } else {
            // This should get moved to the Application Class
            if (ReferenceManager._().getFactories().length == 0 && fileRefFactory != null) {
                // this is /sdcard/odk
                ReferenceManager._().addReferenceFactory(fileRefFactory);
            }

            // Set jr://... to point to /sdcard/odk/forms/filename-media/
            ReferenceManager._().addSessionRootTranslator(
                new RootTranslator("jr://images/", "jr://file/forms/" + formFileName + "-media/"));
            ReferenceManager._().addSessionRootTranslator(
                new RootTranslator("jr://audio/", "jr://file/forms/" + formFileName + "-media/"));
            ReferenceManager._().addSessionRootTranslator(
                new RootTranslator("jr://video/", "jr://file/forms/" + formFileName + "-media/"));
        }
        return fec;
    }

    /**
     * Read serialized {@link FormDef} from file and recreate as object.
     * 
     * @param formDef serialized FormDef file
     * @return {@link FormDef} object
     */
    public static FormDef deserializeFormDef(File formDef, PrototypeFactory protoFactory) {
        // TODO: any way to remove reliance on jrsp?

        // need a list of classes that formdef uses
        FileInputStream fis;
        FormDef fd;
        try {
            // create new form def
            fd = new FormDef();
            fis = new FileInputStream(formDef);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));

            // read serialized formdef into new formdef
            fd.readExternal(dis, protoFactory);
            dis.close();

        } catch (FileNotFoundException | DeserializationException e) {
            e.printStackTrace();
            fd = null;
        } catch (IOException e) {
            e.printStackTrace();
            fd = null;
        } catch (Throwable e) {
            e.printStackTrace();
            fd = null;
        }

        return fd;
    }

    /**
     * Write the FormDef to the file system as a binary blob.
     */
    private static void serializeFormDef(FormDef fd, File formDefFile) throws IOException {
        // create a serialized form file if there isn't already one at this hash
        if (!formDefFile.exists()) {
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(formDefFile);
                DataOutputStream dos;
                outputStream = dos = new DataOutputStream(outputStream);
                fd.writeExternal(dos);
                dos.flush();
            } finally {
                //make sure we clean up the stream
                if(outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e){
                        // Swallow this. If we threw an exception from inside the 
                        // try, this close exception will trump it on the return 
                        // path, and we care a lot more about that exception
                        // than this one.
                    }
                }
            }
        }
    }
}
