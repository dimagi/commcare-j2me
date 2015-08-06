/**
 *
 */
package org.javarosa.log.activity;

import org.javarosa.core.api.State;
import org.javarosa.core.log.StreamLogSerializer;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.properties.IPropertyRules;
import org.javarosa.core.services.properties.JavaRosaPropertyRules;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.services.storage.StorageUtilAccessor;
import org.javarosa.core.services.storage.WrappingStorageUtility;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.log.StatusReportException;
import org.javarosa.j2me.log.XmlStatusProvider;
import org.javarosa.j2me.log.XmlStreamLogSerializer;
import org.javarosa.j2me.view.ProgressIndicator;
import org.javarosa.log.util.DeviceReportElement;
import org.javarosa.log.util.LogReportUtils;
import org.javarosa.log.util.LogWriter;
import org.javarosa.services.transport.TransportListener;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportMessageStatus;
import org.javarosa.services.transport.impl.simplehttp.StreamingHTTPMessage;
import org.javarosa.services.transport.senders.SenderThread;
import org.javarosa.user.model.User;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Note: Much of this behavior should be moved into a controller
 * which is used by multiple states. This one is a non-interactive
 * behind the scenes report, but it's likely that in the future there
 * will exist manual report sending which should be vaguely interactive
 *
 * @author ctsims
 *
 */
public abstract class DeviceReportState implements State, TrivialTransitions, TransportListener, ProgressIndicator {

    public static boolean activated = false;
    private boolean force = false;

    private static final int LOG_ROLLOVER_SIZE = 750;

    public static final String XMLNS = "http://code.javarosa.org/devicereport";

    private int reportFormat;
    private long now;

    private int weeklyPending;
    private int dailyPending;

    private StreamLogSerializer logSerializer;
    protected String fileNameWrittenTo;

    protected Vector<String[]> errors;

    /**
     * Create a behind-the-scenes Device Reporting state which manages all operations
     * without user intervention.
     */
    public DeviceReportState() {
        now = new Date().getTime();
        //Get what reports are pending for the week and for the day.
        //If logging is disabled, these methods default to skipping.
        this.weeklyPending = LogReportUtils.getPendingWeeklyReportType(now);
        this.dailyPending = LogReportUtils.getPendingDailyReportType(now);

        //Pick the most verbose pending report.
        this.reportFormat = Math.max(dailyPending,weeklyPending);
    }

    public DeviceReportState(int reportFormat) {
        now = new Date().getTime();
        this.reportFormat = reportFormat;
        force = true;
    }

    Vector<DeviceReportElement> elements = new Vector<DeviceReportElement>();
    public void addSubReport(DeviceReportElement element) {
        this.elements.addElement(element);
    }

    public void start() {
        //ensure that this activity is only run once per boot of the application
        if (activated && !force) {
            Logger.log("device-report", "short-circuit");
            done();
            return;
        }
        activated = true;

        if(reportFormat == LogReportUtils.REPORT_FORMAT_SKIP) {
            //If we've gotten 600 logger lines in less than 7 days,
            //it's a problem
            if(Logger.isLoggingEnabled()) {
                determineLogFallback(LOG_ROLLOVER_SIZE);
            }
            done();
            return;
        }

        try {
            errors = new Vector();
            TransportMessage message = new StreamingHTTPMessage(getDestURL()) {
                public void _writeBody(OutputStream os) throws IOException {
                    KXmlSerializer serializer = new KXmlSerializer();
                    serializer.setOutput(os, "UTF-8");
                    createReport(serializer);
                }
            };

            logSerializer = null;
            Logger.log("device-report", "attempting to send");

            if(force) {
                //We should be in a background thread anyway
                message = TransportService.sendBlocking(message);
                this.onStatusChange(message);
                done();
            } else {
                SenderThread s = TransportService.send(message);
                s.addListener(this);

                //We have no way of knowing whether the message will get
                //off the phone successfully if it can get cached, the
                //logs are in the transport layer's hands at that point.
                if(message.isCacheable()) {
                    onSuccess();
                }

                done();
            }
        } catch (Exception e) {
            // Don't let this code break the application, ever.
            e.printStackTrace();
            dumpLogFallback(!force);
            Logger.exception("exception while trying to send logs; dumped", e);
            done();
        }
    }

    public int getIndicatorsProvided() {
        return ProgressIndicator.INDICATOR_STATUS;
    }

    public String getCurrentLoadingStatus() {
        return "Sending Device Report";
    }

    public double getProgress() {
        return -1;
    }

    public abstract String getDestURL();

    private void createReport(XmlSerializer o) throws IOException {
        o.startDocument("UTF-8", false);
        o.setPrefix("", XMLNS);
        o.startTag(XMLNS, "device_report");


        try {
        addHeader(o, errors);
        writeUserReport(o, errors);
        createDeviceLogSubreport(o, errors);
        createTransportSubreport(o, errors);
        if(reportFormat == LogReportUtils.REPORT_FORMAT_FULL) {
            createRmsSubreport(o, errors);
            //This is really not helpful.
            //createPropertiesSubreport(o, errors);
            for(DeviceReportElement subReport : elements) {
                subReport.writeToDeviceReport(o);
            }
        }

        if(errors.size() > 0) {
            logErrors(o, errors);
        }
        }finally {
            o.endTag(XMLNS, "device_report");
            o.endDocument();
        }
    }

    private void addHeader(XmlSerializer o, Vector errors) throws IOException {
        String deviceId = PropertyManager._().getSingularProperty(JavaRosaPropertyRules.DEVICE_ID_PROPERTY);
        String reportDate = DateUtils.formatDate(new Date(), DateUtils.FORMAT_HUMAN_READABLE_SHORT);
        String appVersion = PropertyManager._().getSingularProperty("app-version");

        writeText(o, "device_id", deviceId);
        writeText(o, "report_date", reportDate);
        writeText(o, "app_version", appVersion);
    }

    private void writeUserReport(XmlSerializer serializer, Vector errors) throws IllegalArgumentException, IllegalStateException, IOException {
        IStorageUtility<User> userStorage = StorageManager.getStorage(User.STORAGE_KEY);
        serializer.startTag(XMLNS, "user_subreport");
        try{
            for(IStorageIterator<User> it = userStorage.iterate(); it.hasMore();) {
                User u = it.nextRecord();
                try {
                    writeUser(serializer, errors, u);
                } catch(Exception e) {
                    logError(errors, new StatusReportException(e,"user_subreport","Error writing user : " + u.getUsername() + "|" + u.getUniqueId()));
                }
            }
        } finally {
            serializer.endTag(XMLNS, "user_subreport");
        }

    }

    private void writeUser(XmlSerializer serializer, Vector errors, User user) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(XMLNS, "user");
        try{
            writeText(serializer, "username", user.getUsername());
            writeText(serializer, "user_id", user.getUniqueId());
            writeText(serializer, "sync_token", user.getLastSyncToken());
        } finally {
            serializer.endTag(XMLNS, "user");
        }
    }

    public static void writeText(XmlSerializer serializer, String element, String text) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(XMLNS,element);
        try {
            serializer.text(text == null ? "" : text);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            serializer.endTag(XMLNS,element);
        }
    }



    private void createTransportSubreport(XmlSerializer o, Vector errors) throws IOException {
        o.startTag(XMLNS, "transport_subreport");
        o.startTag(XMLNS, "number_unsent");
        try {
            o.text(String.valueOf(TransportService.getCachedMessagesSize()));
        }
        catch(Exception e) {
            logError(errors, new StatusReportException(e,"transport_subreport","Exception retrieving transport subreport"));
        } finally {
            o.endTag(XMLNS, "number_unsent");
            o.endTag(XMLNS, "transport_subreport");
        }
    }

    private void createDeviceLogSubreport(XmlSerializer o, Vector errors) throws IOException {
        Logger.log("logsend", Logger._().logSize() + " entries");
        o.startTag(XMLNS, "log_subreport");
        try {
            logSerializer = new XmlStreamLogSerializer(o, XMLNS);
            Logger._().serializeLogs(logSerializer);
        } catch(Exception e) {
            logError(errors, new StatusReportException(e,"log_subreport","Exception when writing device log report."));
        } finally {
            o.endTag(XMLNS, "log_subreport");
        }
    }

    private void createRmsSubreport(XmlSerializer o, Vector errors) throws IOException {
        o.startTag(XMLNS, "rms_subreport");

        try {
        String[] utils = StorageManager.listRegisteredUtilities();
        for(int i = 0 ; i < utils.length ; ++ i) {
            //TODO: This is super hacky, revisit it
            Object storage = StorageManager.getStorage(utils[i]);
            if(storage instanceof WrappingStorageUtility) {
                storage = StorageUtilAccessor.getStorage((WrappingStorageUtility)storage);
            }
            if(storage instanceof XmlStatusProvider) {
                XmlStatusProvider util = (XmlStatusProvider)storage;
                try {
                    util.getStatusReport(o, XMLNS);
                } catch(StatusReportException sre) {
                    logError(errors, sre);
                }
            }
        }
        } finally {

            o.endTag(XMLNS, "rms_subreport");
        }
    }


    private void createPropertiesSubreport(XmlSerializer o, Vector errors) throws IOException {
        o.startTag(XMLNS, "properties_subreport");
        try {
        Vector rules = PropertyManager._().getRules();
        for(Enumeration en = rules.elements(); en.hasMoreElements();) {
            IPropertyRules ruleset = (IPropertyRules)en.nextElement();
            for(Enumeration pen = ruleset.allowableProperties().elements(); pen.hasMoreElements() ;) {
                String propertyName = (String)pen.nextElement();

                try {
                    Vector list = PropertyManager._().getProperty(propertyName);
                    if(list != null) {
                        String humanName = ruleset.getHumanReadableDescription(propertyName);
                        humanName = (!propertyName.equals(humanName) ? humanName : null);

                        String valueXml = "";
                        if(list.size() == 1) {
                            valueXml = (String)list.elementAt(0);
                        } else {
                            valueXml = "{";
                            for(Enumeration ven = list.elements(); ven.hasMoreElements() ;) {
                                valueXml += ven.nextElement();
                                if(ven.hasMoreElements()) {
                                    valueXml += ",";
                                }
                            }
                            valueXml += "}";
                        }

                        o.startTag(XMLNS, "property");
                        try {
                            o.attribute(null, "name", propertyName);

                            if (humanName != null) {
                                writeText(o, "title", humanName);
                            }
                            writeText(o, "value", valueXml);
                        } finally {
                            o.endTag(XMLNS, "property");
                        }
                    }
                } catch (NoSuchElementException nsee) {
                    //Don't sweat it, not important.
                }
            }
        }
        } finally {
            o.endTag(XMLNS, "properties_subreport");
        }
    }

    private void logErrors(XmlSerializer o, Vector errors) throws IOException {
        o.startTag(XMLNS, "report_errors");
        try {
            for (Enumeration e = errors.elements(); e.hasMoreElements(); ) {
                String[] err = (String[])e.nextElement();
                o.startTag(XMLNS, "report_error");
                try {
                    o.attribute(null, "report", err[0]);
                    o.text(err[1]);
                } finally {
                    o.endTag(XMLNS, "report_error");
                }
            }
        } finally {
            o.endTag(XMLNS, "report_errors");
        }
    }

    private void logError(Vector errors, StatusReportException sre) {
        if(sre.getParent() != null) {
            sre.getParent().printStackTrace();
        }
        errors.addElement(new String[] {sre.getReportName(), sre.getMessage()});
    }



    public void onChange(TransportMessage message, String remark) {
        // TODO Auto-generated method stub

    }
    public void onStatusChange(TransportMessage message) {
        if(message.getStatus() == TransportMessageStatus.SENT) {
            //If the message isn't cacheable, we haven't wiped the
            //logs yet, since we needed to wait for success in order
            //to know they'd get off the phone.
            if(!message.isCacheable()) {
                //otherwise we did this before
                onSuccess();
            }
        } else {
            //if we failed we need to determine if the logs are too big
            //and either dump to the fileystem or just clear the logs...
            determineLogFallback(LOG_ROLLOVER_SIZE);

            //droos: maybe if this still fails after N attempts, then we start to trim the log size
        }
    }

    private void onSuccess () {
        if (logSerializer != null) {
            logSerializer.purge();
        }
        LogReportUtils.setPendingFromNow(now, this.dailyPending > 0, this.weeklyPending > 0);
    }

    private void determineLogFallback(int size) {
        boolean fallback = false;
        //Figure out if we need to dump the logs because they've gotten
        //big and might be causing problems.
        try{
            if(Logger._().logSize() > size) {
                //This is big enough that we need to try to dump them and move on...
                fallback = true;
            } else {
                fallback = false;
            }
        } catch(Exception e) {
            dumpLogFallback(true);
            return ;
        }
        //We keep this outside so that the exception handling is reasonable.
        if(fallback || force) {
            //Only purge this way if we didn't force the send
            dumpLogFallback(!force);
        }
    }

    private void dumpLogFallback(boolean purge) {
        String dumpRef = "";
        long diff = -1;
        long count = -1;
        boolean success = false;
        try{
            dumpRef = "jr://file/jr_log_dump" + DateUtils.formatDateTime(new Date(), DateUtils.FORMAT_TIMESTAMP_SUFFIX) + ".log";
            Reference ref = ReferenceManager._().DeriveReference(dumpRef);
            if(!ref.isReadOnly()) {
                success = true;
                long then = System.currentTimeMillis();
                try {
                    LogWriter writer = new LogWriter(ref.getOutputStream());
                    count = Logger._().logSize();
                    Logger._().serializeLogs(writer);
                } catch (IOException ioe) {
                    success = false;
                }

                long now = System.currentTimeMillis();

                diff = now - then;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(purge) {
        try{
                //No matter _what_, clear the logs if a logger is registered
                if(Logger._() == null) {
                    System.out.println("Logger is null. Must have failed to initailize");
                }
                Logger._().clearLogs(); //don't need to use LogPurger, since we're not in a background thread

                Logger.log("log", "archived logs to file: " + dumpRef);
                if (!success) {
                    Logger.log("log", "archive failed! logs lost!!");
                }

                if(diff != -1) {
                    Logger.log("log", "serialized  " + count + " logs to filesystem in (" + diff + "ms)");
                }

            } catch(Exception e) {
                //If this fails it's a serious problem, but not sure what to do about it.
                e.printStackTrace();
            }
        }

        if(success) {
            fileNameWrittenTo = dumpRef;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.core.util.TrivialTransitions#done()
     */
    //THIS CANNOT BE REMOVED! S40 phones will fail horribly.
    public abstract void done();
}
