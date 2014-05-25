/*
 * This file is part of Flow Commons, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class StringUtil {
    /**
     * Tests if this string starts with the specified prefix, ignoring case
     *
     * @param input the input
     * @param prefix the prefix
     * @return True if the input starts with the prefix when ignoring case, False if not
     */
    public static boolean startsWithIgnoreCase(String input, String prefix) {
        if (input == null || prefix == null || prefix.length() > input.length()) {
            return false;
        } else {
            for (int i = 0; i < prefix.length(); i++) {
                if (!equalsIgnoreCase(prefix.charAt(i), input.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Gets whether two characters equal each other, while ignoring case.
     *
     * @param input1 to use
     * @param input2 to use
     * @return True if input1 and input2 equal when ignoring case, False if not
     */
    public static boolean equalsIgnoreCase(char input1, char input2) {
        return Character.toLowerCase(input1) == Character.toLowerCase(input2);
    }

    /**
     * Matches a value using a name and the value .toString() method
     *
     * @param values to look into
     * @param name to match against
     * @return a collection of values that matched
     */
    public static <T> Collection<T> matchToString(Collection<T> values, String name) {
        List<T> result = new ArrayList<>();
        for (T value : values) {
            if (value == null) {
                continue;
            }
            if (startsWithIgnoreCase(value.toString(), name)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Matches a named class using a name
     *
     * @param values to look into
     * @param name to match against
     * @return a collection of values that matched
     */
    public static <T extends Named> Collection<T> matchName(Collection<T> values, String name) {
        List<T> result = new ArrayList<>();
        for (T value : values) {
            if (value == null) {
                continue;
            }
            if (startsWithIgnoreCase(value.getName(), name)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Matches a file name using a name
     *
     * @param values to look into
     * @param name to match against
     * @return a collection of values that matched
     */
    public static Collection<File> matchFile(Collection<File> values, String name) {
        List<File> result = new ArrayList<>();
        for (File value : values) {
            if (value == null) {
                continue;
            }
            if (startsWithIgnoreCase(value.getName(), name)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Gets the named object with the shortest name from the values specified
     *
     * @param values to look into
     * @return the shortest value, or null if there are no values
     */
    public static <T extends Named> T getShortest(Collection<T> values) {
        int shortestMatch = Integer.MAX_VALUE;
        T shortest = null;
        for (T value : values) {
            if (value.getName().length() < shortestMatch) {
                shortestMatch = value.getName().length();
                shortest = value;
            }
        }
        return shortest;
    }

    /**
     * Wraps all components in between brackets delimited by ','-signs
     *
     * @param components to turn into a String
     * @return a String representation of the input
     */
    public static String toString(Object... components) {
        return toNamedString(null, components);
    }

    /**
     * Converts a String expression of elements into an integer array<br> For example: "12-34, 35, 36, 11-0"
     *
     * @param elements to convert
     * @return the converted result
     */
    public static Integer[] getIntArray(String elements) {
        if (elements == null || elements.isEmpty()) {
            return new Integer[0];
        }

        List<Integer> values = new ArrayList<>();
        int index;
        int start, end;
        for (String element : elements.split(",")) {
            element = element.trim();
            index = element.indexOf('-');
            try {
                if (index != -1) {
                    start = Integer.parseInt(element.substring(0, index).trim());
                    end = Integer.parseInt(element.substring(index + 1).trim());
                    if (end == start) {
                        values.add(start);
                    } else if (end > start) {
                        for (index = start; index <= end; index++) {
                            values.add(index);
                        }
                    } else {
                        for (index = start; index >= end; index--) {
                            values.add(index);
                        }
                    }
                } else {
                    values.add(Integer.parseInt(element));
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
        return values.toArray(new Integer[0]);
    }

    /**
     * Wraps all components in between brackets delimited by ','-signs, appending the class name in front of it.
     *
     * @param object name to append in front
     * @param components to turn into a String
     * @return a String representation of the input
     */
    public static String toNamedString(Object object, Object... components) {
        StringBuilder b = new StringBuilder(components.length * 5 + 2);
        if (object != null) {
            b.append(object.getClass().getSimpleName()).append(' ');
        }
        b.append('{');
        for (int i = 0; i < components.length; i++) {
            if (i != 0) {
                b.append(", ");
            }
            b.append(components[i]);
        }
        b.append('}');
        return b.toString();
    }

    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        /*
          The difference between this impl. and the previous is that, rather
           than creating and retaining a matrix of size s.length()+1 by t.length()+1,
           we maintain two single-dimensional arrays of length s.length()+1.  The first, d,
           is the 'current working' distance array that maintains the newest distance cost
           counts as we iterate through the characters of String s.  Each time we increment
           the index of String t we are comparing, d is copied to p, the second int[].  Doing so
           allows us to retain the previous cost counts as required by the algorithm (taking
           the minimum of the cost count to the left, up one, and diagonally up and to the left
           of the current cost count being calculated).  (Note that the arrays aren't really
           copied anymore, just switched...this is clearly much better than cloning an array
           or doing a System.arraycopy() each time  through the outer loop.)

           Effectively, the difference between the two implementations is this one does not
           cause an out of memory condition when calculating the LD over two very large strings.
         */

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        } else if (m == 0) {
            return n;
        }

        int p[] = new int[n + 1]; //'previous' cost array, horizontally
        int d[] = new int[n + 1]; // cost array, horizontally
        int _d[]; //placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    private static Pattern escapeRegexPattern = Pattern.compile("[^a-zA-Z0-9]");

    public static String escapeRegex(String str) {
        return escapeRegexPattern.matcher(str).replaceAll("\\\\$0");
    }
}
