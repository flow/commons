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
package com.flowpowered.commons;

/**
 * A basic TPS monitor. Updates the TPS every second. Call {@link #start()} before the loop, and {@link #update()} at the end of each iteration.
 */
public class TPSMonitor {
    private long lastUpdateTime;
    private long elapsedTime = 0;
    private int frameCount = 0;
    private int tps;

    /**
     * Starts the TPS monitor.
     */
    public void start() {
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Updates the TPS.
     */
    public void update() {
        final long time = System.currentTimeMillis();
        elapsedTime += time - lastUpdateTime;
        lastUpdateTime = time;
        frameCount++;
        if (elapsedTime >= 1000) {
            tps = frameCount;
            frameCount = 0;
            elapsedTime = 0;
        }
    }

    /**
     * Returns the TPS.
     *
     * @return The TPS
     */
    public int getTPS() {
        return tps;
    }
}
