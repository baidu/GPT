/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.android.gporter.net;

/**
 * LangUtils
 *
 * @author liuhaitao
 * @since 2015-07-10
 */
@Deprecated
public final class LangUtils {

    /**
     * hash seed
     */
    public static final int HASH_SEED = 17;

    /**
     * hash offset
     */
    public static final int HASH_OFFSET = 37;

    /**
     * Disabled default constructor
     */
    private LangUtils() {
    }

    /**
     * get hash code
     *
     * @param seed     seed
     * @param hashcode hashcode
     * @return hash code
     */
    public static int hashCode(final int seed, final int hashcode) {
        return seed * HASH_OFFSET + hashcode;
    }

    /**
     * hash code
     *
     * @param seed seed
     * @param b    boolean
     * @return hash code
     */
    public static int hashCode(final int seed, final boolean b) {
        return hashCode(seed, b ? 1 : 0);
    }

    /**
     * hash code
     *
     * @param seed seed
     * @param obj  obj
     * @return hash code
     */
    public static int hashCode(final int seed, final Object obj) {
        return hashCode(seed, obj != null ? obj.hashCode() : 0);
    }


    /**
     * equals
     *
     * @param obj1 Object1
     * @param obj2 Object2
     * @return is equal
     */
    public static boolean equals(final Object obj1, final Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

    /**
     * equals
     *
     * @param a1 Object[]1
     * @param a2 Object[]2
     * @return equals
     */
    public static boolean equals(final Object[] a1, final Object[] a2) {
        if (a1 == null) {
            return a2 == null;
        } else {
            if (a2 != null && a1.length == a2.length) {
                for (int i = 0; i < a1.length; i++) {
                    if (!equals(a1[i], a2[i])) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

}



