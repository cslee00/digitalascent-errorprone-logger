package com.digitalascent.errorprone.flogger;

import org.slf4j.Marker;

import java.util.Iterator;

public class DummySlf4JMarker implements Marker {
    private static final long serialVersionUID = 42L;

    public static final Marker INSTANCE = new DummySlf4JMarker();

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void add(Marker reference) {

    }

    @Override
    public boolean remove(Marker reference) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean hasReferences() {
        return false;
    }

    @Override
    public Iterator<Marker> iterator() {
        return null;
    }

    @Override
    public boolean contains(Marker other) {
        return false;
    }

    @Override
    public boolean contains(String name) {
        return false;
    }
}
