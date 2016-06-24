package org.commcare.api.persistence;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.UserSandbox;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;

/**
 * A sandbox for user data using SqliteIndexedStorageUtility. Sandbox is per-User
 *
 * @author wspride
 */
public class UserSqlSandbox extends UserSandbox {
    private final SqliteIndexedStorageUtility<Case> caseStorage;
    private final SqliteIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqliteIndexedStorageUtility<User> userStorage;
    private final SqliteIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqliteIndexedStorageUtility<FormInstance> appFixtureStorage;
    private User user = null;
    public static final String DEFAULT_DATBASE_PATH = "dbs";

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     */
    public UserSqlSandbox(String username, String path) {
        //we can't name this table "Case" becase that's reserved by sqlite
        caseStorage = new SqliteIndexedStorageUtility<>(Case.class, username, "CCCase", path);
        ledgerStorage = new SqliteIndexedStorageUtility<>(Ledger.class, username, Ledger.STORAGE_KEY, path);
        userStorage = new SqliteIndexedStorageUtility<>(User.class, username, User.STORAGE_KEY, path);
        userFixtureStorage = new SqliteIndexedStorageUtility<>(FormInstance.class, username, "UserFixture", path);
        appFixtureStorage = new SqliteIndexedStorageUtility<>(FormInstance.class, username, "AppFixture", path);
    }

    public UserSqlSandbox(String username) {
        this(username, DEFAULT_DATBASE_PATH);
    }

    public SqliteIndexedStorageUtility<Case> getCaseStorage() {
        return caseStorage;
    }

    public SqliteIndexedStorageUtility<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    public SqliteIndexedStorageUtility<User> getUserStorage() {
        return userStorage;
    }

    public SqliteIndexedStorageUtility<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    public SqliteIndexedStorageUtility<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }

    @Override
    public User getLoggedInUser() {
        if(user == null){
            SqliteIndexedStorageUtility<User> userStorage = getUserStorage();
            JdbcSqlStorageIterator<User> iterator = userStorage.iterate();
            if(iterator.hasMore()){
                // should be only one user here
                user =  iterator.next();
            } else {
                user = null;
            }
            iterator.closeConnection();
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    //TODO WSP implement sync token stuff in next iteration, but useful to have in superclass now for AndroidSandbox
}
