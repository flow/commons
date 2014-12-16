/*
 * This file is part of Flow Commons, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Flow Powered <https://flowpowered.com/>
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
package com.flowpowered.commons.store.block.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.flowpowered.math.GenericMath;

public class AtomicShortIntPaletteBackingArray extends AtomicShortIntBackingArray {
    private final static int CALCULATE_UNIQUE = -1;
    private final int width;
    private final int paletteSize;
    private final AtomicIntShortSingleUseHashMap idLookup;
    private final AtomicVariableWidthArray store;
    private final AtomicIntegerArray palette;
    private final AtomicInteger paletteCounter;
    private final boolean maxPaletteSize;

    public AtomicShortIntPaletteBackingArray(int length) {
        this(null, length, false, false, CALCULATE_UNIQUE);
    }

    public AtomicShortIntPaletteBackingArray(AtomicShortIntBackingArray previous, boolean expand) {
        this(previous, previous.length(), false, expand, CALCULATE_UNIQUE);
    }

    public AtomicShortIntPaletteBackingArray(AtomicShortIntBackingArray previous, int length, boolean compress, boolean expand, int unique) {
        super(length);
        if (previous == null) {
            width = 1;
        } else {
            if (compress) {
                if (unique == CALCULATE_UNIQUE) {
                    unique = previous.getUnique();
                }
                width = roundUpWidth(expand ? unique : (unique - 1));
            } else {
                int oldWidth = previous.width();
                width = oldWidth == 0 ? 1 : oldWidth <= 8 ? (oldWidth << 1) : (16);
            }
        }
        int allowedPalette = AtomicShortIntPaletteBackingArray.getAllowedPalette(length);
        paletteSize = Math.min(widthToPaletteSize(width), allowedPalette);
        maxPaletteSize = paletteSize == allowedPalette;
        store = new AtomicVariableWidthArray(length, width);
        palette = new AtomicIntegerArray(paletteSize);
        paletteCounter = new AtomicInteger(0);
        idLookup = new AtomicIntShortSingleUseHashMap(paletteSize + (paletteSize >> 2));
        try {
            if (previous == null) { // sets id=0 to map to value=0 (so non-set elements are zero)
                paletteCounter.incrementAndGet();
                if (!idLookup.isEmptyValue(idLookup.putIfAbsent(0, (short) 0))) {
                    throw new IllegalStateException("Entry was not zero when putting first element into HashMap");
                }
            } else {
                copyFromPrevious(previous);
            }
        } catch (PaletteFullException pfe) {
            throw new IllegalStateException("Unable to copy old array to new array, as palette was filled, length " + length + ", paletteSize " + paletteSize + ", unique " + unique);
        }
    }

    public AtomicShortIntPaletteBackingArray(int length, int unique, int[] initial) {
        super(length);
        width = roundUpWidth(unique - 1);
        int allowedPalette = AtomicShortIntPaletteBackingArray.getAllowedPalette(length);
        paletteSize = Math.min(widthToPaletteSize(width), allowedPalette);
        paletteCounter = new AtomicInteger(0);
        maxPaletteSize = paletteSize == allowedPalette;
        palette = new AtomicIntegerArray(paletteSize);
        store = new AtomicVariableWidthArray(length, width);
        idLookup = new AtomicIntShortSingleUseHashMap(paletteSize + (paletteSize >> 2));
        try {
            for (int i = 0; i < length; i++) {
                set(i, initial[i]);
            }
        } catch (PaletteFullException pfe) {
            throw new IllegalStateException("Unable to copy old array to new array, as palette was filled, length " + length + ", paletteSize " + paletteSize + ", unique " + unique);
        }
    }

    public AtomicShortIntPaletteBackingArray(int length, int[] palette, int width, int[] variableWidthBlockArray) {
        super(length);
        this.width = width;
        int allowedPalette = AtomicShortIntPaletteBackingArray.getAllowedPalette(length);
        this.paletteSize = palette.length;
        this.paletteCounter = new AtomicInteger(palette.length);
        this.maxPaletteSize = paletteSize >= allowedPalette;
        this.palette = new AtomicIntegerArray(palette);
        store = new AtomicVariableWidthArray(length, width, variableWidthBlockArray);
        idLookup = new AtomicIntShortSingleUseHashMap(paletteSize + (paletteSize >> 2));
        for (int i = 0; i < paletteSize; i++) {
            idLookup.putIfAbsent(palette[i], (short) i);
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int getPaletteSize() {
        return paletteSize;
    }

    @Override
    public int getPaletteUsage() {
        return paletteCounter.get();
    }

    @Override
    public boolean isPaletteMaxSize() {
        return maxPaletteSize;
    }

    @Override
    public int get(int i) {
        return palette.get(store.get(i));
    }

    @Override
    public int set(int i, int newValue) throws PaletteFullException {
        int id = getId(newValue);
        int oldId = store.getAndSet(i, id);
        return palette.get(oldId);
    }

    @Override
    public boolean compareAndSet(int i, int expect, int update) throws PaletteFullException {
        short expId = idLookup.get(expect);
        if (idLookup.isEmptyValue(expId)) {
            return false;
        }
        int newId = getId(update);
        return store.compareAndSet(i, expId, newId);
    }

    /**
     * Gets the id for the given value, allocating an id if required
     *
     * @return the id
     */
    private int getId(int value) throws PaletteFullException {
        short id = idLookup.get(value);
        if (!idLookup.isEmptyValue(id)) {
            return id;
        } else {
            id = (short) paletteCounter.getAndIncrement();
            if (id >= paletteSize) {
                throw new PaletteFullException();
            }
            short oldId = idLookup.putIfAbsent(value, id);
            if (!idLookup.isEmptyValue(oldId)) {
                id = oldId;
            }
            palette.set(id, value);
            return id;
        }
    }

    private static final byte[] roundLookup = new byte[65537];

    static {
        roundLookup[0] = 0;
        roundLookup[1] = 1;
        roundLookup[2] = 1;
        roundLookup[4] = 2;
        roundLookup[8] = 4;
        roundLookup[16] = 4;
        roundLookup[32] = 8;
        roundLookup[64] = 8;
        roundLookup[128] = 8;
        roundLookup[256] = 8;
        roundLookup[512] = 16;
        roundLookup[1024] = 16;
        roundLookup[2048] = 16;
        roundLookup[4096] = 16;
        roundLookup[8192] = 16;
        roundLookup[16384] = 16;
    }

    public static int roundUpWidth(int i) {
        return roundLookup[GenericMath.roundUpPow2(i + 1)];
    }

    public static int widthToPaletteSize(int width) {
        return 1 << width;
    }

    public static int getAllowedPalette(int length) {
        return length >> 2;
    }

    @Override
    public int[] getPalette() {
        return toIntArray(palette, paletteCounter.get());
    }

    @Override
    public int[] getBackingArray() {
        return store.getPacked();
    }
}
