/*
 * This file is part of Flow Commons, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <https://spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.commons.map.impl;


import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import gnu.trove.function.TObjectFunction;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TIntSet;

import com.flowpowered.math.GenericMath;
import com.flowpowered.commons.map.TSyncIntObjectMap;

/**
 * This is a synchronised version of the Trove IntObjectHashMap.
 *
 * Read/write locks are used to synchronise access.
 *
 * By default, it creates 16 sub-maps and there is a separate read/write lock for each submap.
 *
 * @param <V> the value type
 */
public class TSyncIntObjectHashMap<V> implements TSyncIntObjectMap<V> {
    private final int mapCount;
    private final int mapMask;
    private final int hashScramble;
    private final ReadWriteLock[] lockArray;
    private final TIntObjectHashMap<V>[] mapArray;
    private final int no_entry_key;
    private final AtomicInteger totalKeys = new AtomicInteger(0);

    /**
     * Creates a synchronised map based on the Trove int object map
     */
    public TSyncIntObjectHashMap() {
        this(16);
    }

    /**
     * Creates a synchronised map based on the Trove int object map
     *
     * @param mapCount the number of sub-maps
     */
    public TSyncIntObjectHashMap(int mapCount) {
        this(mapCount, 32);
    }

    /**
     * Creates a synchronised map based on the Trove int object map
     *
     * @param mapCount the number of sub-maps
     * @param initialCapacity the initial capacity of the map
     */
    public TSyncIntObjectHashMap(int mapCount, int initialCapacity) {
        this(mapCount, initialCapacity, 0.5F);
    }

    /**
     * Creates a synchronised map based on the Trove int object map
     *
     * @param mapCount the number of sub-maps
     * @param initialCapacity the initial capacity of the map
     * @param loadFactor the load factor for the map
     */
    public TSyncIntObjectHashMap(int mapCount, int initialCapacity, float loadFactor) {
        this(mapCount, initialCapacity, loadFactor, Constants.DEFAULT_INT_NO_ENTRY_VALUE);
    }

    /**
     * Creates a synchronised map based on the Trove int object map
     *
     * @param mapCount the number of sub-maps
     * @param initialCapacity the initial capacity of the map
     * @param loadFactor the load factor for the map
     * @param noEntryKey the key used to indicate a null key
     */
    @SuppressWarnings ("unchecked")
    public TSyncIntObjectHashMap(int mapCount, int initialCapacity, float loadFactor, int noEntryKey) {
        if (mapCount > 0x100000) {
            throw new IllegalArgumentException("Map count exceeds valid range");
        }
        mapCount = GenericMath.roundUpPow2(mapCount);
        mapMask = mapCount - 1;
        this.mapCount = mapCount;
        this.hashScramble = (mapCount << 8) + 1;
        mapArray = new TIntObjectHashMap[mapCount];
        lockArray = new ReadWriteLock[mapCount];
        for (int i = 0; i < mapCount; i++) {
            mapArray[i] = new TIntObjectHashMap<>(initialCapacity / mapCount, loadFactor, noEntryKey);
            lockArray[i] = new ReentrantReadWriteLock();
        }
        this.no_entry_key = noEntryKey;
    }

    @Override
    public void clear() {
        for (int m = 0; m < mapCount; m++) {
            clear(m);
        }
    }

    private void clear(int m) {
        Lock lock = lockArray[m].writeLock();
        lock.lock();
        try {
            totalKeys.addAndGet(-mapArray[m].size());
            mapArray[m].clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsKey(int key) {
        int m = mapHash(key);
        Lock lock = lockArray[m].readLock();
        lock.lock();
        try {
            return mapArray[m].containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        for (int m = 0; m < mapCount; m++) {
            if (containsValue(m, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsValue(int m, Object value) {
        Lock lock = lockArray[m].readLock();
        lock.lock();
        try {
            return mapArray[m].containsValue(value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean forEachEntry(TIntObjectProcedure<? super V> arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public boolean forEachKey(TIntProcedure arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public boolean forEachValue(TObjectProcedure<? super V> arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public V get(int key) {
        int m = mapHash(key);
        Lock lock = lockArray[m].readLock();
        lock.lock();
        try {
            return mapArray[m].get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getNoEntryKey() {
        return no_entry_key;
    }

    @Override
    public boolean isEmpty() {
        return totalKeys.get() == 0;
    }

    @Override
    public TIntObjectIterator<V> iterator() {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public TIntSet keySet() {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public int[] keys(int[] dest) {
        for (int m = 0; m < mapCount; m++) {
            lockArray[m].readLock().lock();
        }
        try {
            int localSize = totalKeys.get();
            int[] keys;
            if (dest == null || dest.length < localSize) {
                keys = new int[localSize];
            } else {
                keys = dest;
            }
            int position = 0;
            for (int m = 0; m < mapCount; m++) {
                int[] mapKeys = mapArray[m].keys();
                for (int mapKey : mapKeys) {
                    keys[position++] = mapKey;
                }
            }
            if (position != localSize) {
                throw new IllegalStateException("Key counter does not match actual total map size");
            }
            return keys;
        } finally {
            for (int m = 0; m < mapCount; m++) {
                lockArray[m].readLock().unlock();
            }
        }
    }

    @Override
    public int[] keys() {
        return keys(null);
    }

    @Override
    public V put(int key, V value) {
        int m = mapHash(key);
        Lock lock = lockArray[m].writeLock();
        lock.lock();
        try {
            V previous = mapArray[m].put(key, value);
            if (previous == null && value != null) {
                totalKeys.incrementAndGet();
            }
            return previous;
        } finally {
            lock.unlock();
        }
    }

    // TODO - these two methods could be easily implemented
    @Override
    public void putAll(Map<? extends Integer, ? extends V> arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public void putAll(TIntObjectMap<? extends V> arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public V putIfAbsent(int key, V value) {
        int m = mapHash(key);
        Lock lock = lockArray[m].writeLock();
        lock.lock();
        try {
            V previous = mapArray[m].putIfAbsent(key, value);
            if (previous == null && value != null) {
                totalKeys.incrementAndGet();
            }
            return previous;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V remove(int key) {
        int m = mapHash(key);
        Lock lock = lockArray[m].writeLock();
        lock.lock();
        try {
            V previous = mapArray[m].remove(key);
            if (previous != null) {
                totalKeys.decrementAndGet();
            }
            return previous;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(int key, V value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot remove null values");
        }
        int m = mapHash(key);
        Lock lock = lockArray[m].writeLock();
        lock.lock();
        try {
            V current = mapArray[m].get(key);
            if (current != value) {
                return false;
            }

            totalKeys.decrementAndGet();
            mapArray[m].remove(key);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean retainEntries(TIntObjectProcedure<? super V> arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public int size() {
        return totalKeys.get();
    }

    @Override
    public void transformValues(TObjectFunction<V, V> arg0) {
        throw new UnsupportedOperationException("This operation is not supported");
    }

    @Override
    public Collection<V> valueCollection() {
        HashSet<V> collection = new HashSet<>();
        for (int m = 0; m < mapCount; m++) {
            lockArray[m].readLock().lock();
        }
        try {
            for (int m = 0; m < mapCount; m++) {
                collection.addAll(mapArray[m].valueCollection());
            }
        } finally {
            for (int m = 0; m < mapCount; m++) {
                lockArray[m].readLock().unlock();
            }
        }
        return Collections.unmodifiableCollection(collection);
    }

    @Override
    public V[] values() {
        return values(null);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public V[] values(V[] dest) {
        for (int m = 0; m < mapCount; m++) {
            lockArray[m].readLock().lock();
        }
        try {
            int localSize = totalKeys.get();
            V[] values;
            if (dest == null) {
                values = (V[]) new Object[localSize];
            } else if (dest.length == localSize) {
                values = dest;
            } else {
                values = (V[]) Array.newInstance(dest.getClass().getComponentType(), localSize);
            }
            int position = 0;
            for (int m = 0; m < mapCount; m++) {
                V[] mapValues = (V[]) mapArray[m].values();
                for (V mapValue : mapValues) {
                    values[position++] = mapValue;
                }
            }
            if (position != localSize) {
                throw new IllegalStateException("Key counter does not match actual total map size");
            }
            return values;
        } finally {
            for (int m = 0; m < mapCount; m++) {
                lockArray[m].readLock().unlock();
            }
        }
    }

    private int mapHash(int key) {
        int intKey = (key >> (32 ^ key));

        return (0x7FFFFFFF & intKey) % hashScramble & mapMask;
    }
}
