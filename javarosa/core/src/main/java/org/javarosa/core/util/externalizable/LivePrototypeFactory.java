package org.javarosa.core.util.externalizable;

import org.javarosa.core.api.ClassNameHasher;

import java.util.Hashtable;

/**
 * A prototype factory that is configured to keep track of all of the
 * case->hash pairs that it creates in order to use them for deserializaiton in
 * the future.
 *
 * Will only work reliably if it is used synchronously to hash all values that
 * are read, and should really only be expected to function for 'in memory'
 * storage like mocks.
 *
 * TODO: unify with Android storage live factory mocker
 *
 * @author ctsims
 */
public class LivePrototypeFactory extends PrototypeFactory {
    private final Hashtable<String, Class> factoryTable = new Hashtable<String, Class>();
    private final LiveHasher mLiveHasher;

    public LivePrototypeFactory() {
        this(new ClassNameHasher());
    }

    public LivePrototypeFactory(Hasher hasher) {
        this.mLiveHasher = new LiveHasher(this, hasher);
        PrototypeFactory.setStaticHasher(this.mLiveHasher);
    }

    @Override
    protected void lazyInit() {
    }

    @Override
    public void addClass(Class c) {
        byte[] hash = getLiveHasher().getHasher().getClassHashValue(c);
        factoryTable.put(ExtUtil.printBytes(hash), c);
    }

    @Override
    public Class getClass(byte[] hash) {
        String key = ExtUtil.printBytes(hash);
        return factoryTable.get(key);
    }

    @Override
    public Object getInstance(byte[] hash) {
        return PrototypeFactory.getInstance(getClass(hash));
    }

    public LiveHasher getLiveHasher(){
        return this.mLiveHasher;
    }

    public class LiveHasher extends Hasher{
        final LivePrototypeFactory pf;
        final Hasher mHasher;
        public LiveHasher(LivePrototypeFactory pf, Hasher mHasher){
            this.pf = pf;
            this.mHasher = mHasher;
        }

        @Override
        public int getHashSize() {
            return mHasher.getHashSize();
        }

        @Override
        public byte[] getHash(Class c) {
            byte[] ret = mHasher.getClassHashValue(c);
            pf.addClass(c);
            return ret;
        }

        public Hasher getHasher(){
            return mHasher;
        }
    }
}