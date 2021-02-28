package com.musicplayah.Utils;


import android.util.Pair;

import java.util.Observable;

/**
 * The class ObservableBoolean contains a {@code Boolean}
 * value and a {@code String} key. Extends {@link Observable}
 * so it can be observed by an {@link java.util.Observer}.
 *
 * @see java.util.Observer
 * @see java.util.Observable
 */
public class ObservableBoolean extends Observable {
    private String key;
    private Boolean value;

    /**
     * Constructs a new ObservableBoolean.
     * @param key
     *        An {@code String} immutable key which can be
     *        used to identify the ObservableBoolean.
     * @param initialValue
     *        The {@code Boolean} mutable value.
     */
    public ObservableBoolean(String key, Boolean initialValue) {
        this.key = key;
        this.value = initialValue;
    }

    /**
     *
     * @return the {@code Boolean} value.
     */
    public Boolean getValue() {
        return value;
    }

    /**
     * Sets the new {@code Boolean} value, marks this
     * {@code Observable} as having changed and notifies
     * all the {@code Observer}s of the change, sending them
     * a key-value {@code Pair}.
     * @param value
     *        The new {@code Boolean} value.
     */
    public void setValue(Boolean value) {
        this.value = value;
        this.setChanged();
        this.notifyObservers(new Pair<>(key, value));
    }
}