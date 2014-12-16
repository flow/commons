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
package com.flowpowered.commons.hashing;

public class SignedTenBitTripleHashed {
    private final static int mask = 0xFFDFFBFF;
    private final static int[] shiftMask = new int[16];

    static {
        for (int i = 0; i < 16; i++) {
            int single = 0x3FF >> i;
            shiftMask[i] = (single << 22) | (single << 11) | single;
        }
    }

    /**
     * Packs the first 8 most significant bits of each byte into an <code>int</code>
     *
     * @param x an <code>byte</code> value
     * @param y an <code>byte</code> value
     * @param z an <code>byte</code> value
     *
     * @return The first 8 most significant bits of each byte packed into an <code>int</code>
     */
    public static int key(int x, int y, int z) {
        return (x & 0x3FF) << 22 | (z & 0x3FF) << 11 | y & 0x3FF;
    }

    /**
     * Gets the first signed 10-bit integer value from an int key
     *
     * @param key to get from
     *
     * @return the first 8-bit integer value in the key
     */
    public static int key1(int key) {
        return key >> 22;
    }

    /**
     * Gets the second signed 10-bit integer value from an int key
     *
     * @param key to get from
     *
     * @return the second 8-bit integer value in the key
     */
    public static int key2(int key) {
        return (key << 22) >> 22;
    }

    /**
     * Gets the third signed 10-bit integer value from an int key
     *
     * @param key to get from
     *
     * @return the third 8-bit integer value in the key
     */
    public static int key3(int key) {
        return (key << 11) >> 22;
    }

    /**
     * Adds the given offset to the packed key
     *
     * @param key the base key
     * @param x   the x offset
     * @param y   the y offset
     * @param z   the z offset
     *
     * @return the new key
     */
    public static int add(int key, int x, int y, int z) {
        int offset = key(x, y, z);
        return (key + offset) & mask;
    }

    /**
     * Shifts the given key to the right.<br> <br> This method only works for keys if all 3 sub-keys are positive
     *
     * @param key   the key
     * @param shift the right shift
     */
    public static int positiveRightShift(int key, int shift) {
        int single = 0x3FF >> shift;
        int shiftMask = (single << 22) | (single << 11) | single;
        return shiftMask & (key >> shift);
    }
}
