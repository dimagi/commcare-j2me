package org.javarosa.xml.util;

/**
 * @author ctsims
 */
public class UnfullfilledRequirementsException extends Exception {

    /**
     * This requirement may be ignorable, but the user should be prompted *
     */
    public static final int SEVERITY_PROMPT = 1;
    /**
     * Something is missing from the environment, but it should be able to be provided *
     */
    public static final int SEVERITY_ENVIRONMENT = 2;

    /**
     * The profile is incompatible with the major version of the current CommCare installation *
     */
    public static final int REQUIREMENT_MAJOR_APP_VERSION = 1;
    /**
     * The profile is incompatible with the minor version of the current CommCare installation *
     */
    public static final int REQUIREMENT_MINOR_APP_VERSION = 2;

    public static final int REQUIREMENT_NO_DUPLICATE_APPS = 3;

    /**
     * One of the currently installed apps is not/is no longer compatible with multiple apps, so
     * cannot install another one
     */
    public static final int REQUIREMENT_MULTIPLE_APPS_COMPAT_EXISTING = 4;

    /**
     * The app for which install is being attempted is not compatible with multiple apps, and
     * there are already 1 or more apps installed
     */
    public static final int REQUIREMENT_MULTIPLE_APPS_COMPAT_NEW = 5;


    private final int severity;
    private final int requirement;

    /**
     * Version Numbers if version is incompatible *
     */
    private final int maR;
    private final int miR;
    private final int maA;
    private final int miA;

    public UnfullfilledRequirementsException(String message, int severity) {
        this(message, severity, -1, -1, -1, -1, -1);
    }

    public UnfullfilledRequirementsException(String message, int severity, int requirement) {
        this(message, severity, requirement, -1, -1, -1, -1);
    }

    /**
     * Constructor for unfulfilled version requirements.
     */
    public UnfullfilledRequirementsException(String message, int severity,
                                             int requirement,
                                             int requiredMajor, int requiredMinor, int availableMajor, int availableMinor) {
        super(message);
        this.severity = severity;
        this.requirement = requirement;

        this.maR = requiredMajor;
        this.miR = requiredMinor;

        this.maA = availableMajor;
        this.miA = availableMinor;
    }

    /**
     * @return A human readable version string describing the required version
     */
    public String getRequiredVersionString() {
        return maR + "." + miR;
    }

    /**
     * @return A human readable version string describing the available version
     */
    public String getAvailableVesionString() {
        return maA + "." + miA;
    }

    public int getSeverity() {
        return severity;
    }

    public int getRequirementCode() {
        return requirement;
    }

    /**
     * @return true if this exception was thrown due to an attempt at installing a duplicate app
     */
    public boolean isDuplicateException() {
        return requirement == REQUIREMENT_NO_DUPLICATE_APPS;
    }

    /**
     * @return true if this exception was thrown due to an attempt to install an app that is not
     * multiple apps compatible, with 1 more more apps already installed
     */
    public boolean isMultipleAppsViolationByNew() {
        return requirement == REQUIREMENT_MULTIPLE_APPS_COMPAT_NEW;
    }

    /**
     * @return true if this exception was thrown due to an attempt to install an app with 1 or
     * more existing apps already installed that are not MA-compatible
     */
    public boolean isMultipleAppsViolationByExisting() {
        return requirement == REQUIREMENT_MULTIPLE_APPS_COMPAT_EXISTING;
    }
}
