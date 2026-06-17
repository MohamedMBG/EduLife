package com.baghdad.edulife.core.storage;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Minimal in-memory {@link SharedPreferences} for host-JVM unit tests. It mirrors the
 * subset of behavior SessionStorage relies on (putString / remove / clear / commit / apply /
 * getString) so the real {@code SessionStorage} persistence logic can run without the Android
 * Keystore. {@code commit()} and {@code apply()} behave identically here (synchronous in-memory
 * write), which is sufficient for deterministic tests.
 */
class FakeSharedPreferences implements SharedPreferences {

    private static final Object TOMBSTONE = new Object();

    private final Map<String, Object> store = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(store);
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = store.get(key);
        return value instanceof String ? (String) value : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = store.get(key);
        return value instanceof Set ? (Set<String>) value : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = store.get(key);
        return value instanceof Integer ? (Integer) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = store.get(key);
        return value instanceof Long ? (Long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = store.get(key);
        return value instanceof Float ? (Float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = store.get(key);
        return value instanceof Boolean ? (Boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
        return store.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new FakeEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // No listener support needed for SessionStorage tests.
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // No listener support needed for SessionStorage tests.
    }

    private final class FakeEditor implements Editor {

        private final Map<String, Object> pending = new HashMap<>();
        private boolean clearRequested;

        @Override
        public Editor putString(String key, String value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            pending.put(key, values == null ? null : new HashSet<>(values));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            pending.put(key, TOMBSTONE);
            return this;
        }

        @Override
        public Editor clear() {
            clearRequested = true;
            return this;
        }

        @Override
        public boolean commit() {
            apply();
            return true;
        }

        @Override
        public void apply() {
            // Matches SharedPreferences semantics: clear() takes effect before this edit's puts.
            if (clearRequested) {
                store.clear();
            }
            for (Map.Entry<String, Object> entry : pending.entrySet()) {
                if (entry.getValue() == TOMBSTONE) {
                    store.remove(entry.getKey());
                } else {
                    store.put(entry.getKey(), entry.getValue());
                }
            }
            pending.clear();
            clearRequested = false;
        }
    }
}
