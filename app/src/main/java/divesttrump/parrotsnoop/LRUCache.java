package divesttrump.parrotsnoop;


import java.util.LinkedHashMap;


public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private int maxSize;
    private CleanupCallback<K, V> callback;

    LRUCache(int maxSize, CleanupCallback<K, V> callback) {
        super(maxSize + 1, 1, true);

        this.maxSize = maxSize;
        this.callback = callback;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        if (size() > maxSize) {
            callback.cleanup(eldest);
            return true;
        }
        return false;
    }

    public interface CleanupCallback<K, V> {
        void cleanup(Entry<K, V> eldest);
    }
}