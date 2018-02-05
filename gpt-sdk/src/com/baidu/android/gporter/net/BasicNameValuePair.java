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
 * BasicNameValuePair
 *
 * @author liuhaitao
 * @since 2018-01-16
 */
@Deprecated
public class BasicNameValuePair implements NameValuePair, Cloneable {

    /**
     * name
     */
    private final String name;

    /**
     * value
     */
    private final String value;

    /**
     * Default Constructor taking a name and a value. The value may be null.
     *
     * @param pairName  The name.
     * @param pairValue The value.
     */
    public BasicNameValuePair(final String pairName, final String pairValue) {
        super();
        if (pairName == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = pairName;
        this.value = pairValue;
    }

    /**
     * Returns the name.
     *
     * @return String name The name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the value.
     *
     * @return String value The current value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Get a string representation of this pair.
     *
     * @return A string representation.
     */
    public String toString() {
        // don't call complex default formatting for a simple toString
        int len = this.name.length();
        if (this.value != null) {
            len += 1 + this.value.length();
        }
        StringBuffer buffer = new StringBuffer(len);
        buffer.append(this.name);
        if (this.value != null) {
            buffer.append("=");
            buffer.append(this.value);
        }
        return buffer.toString();
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (object instanceof NameValuePair) {
            BasicNameValuePair that = (BasicNameValuePair) object;
            return this.name.equals(that.name)
                    && LangUtils.equals(this.value, that.value);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.name);
        hash = LangUtils.hashCode(hash, this.value);
        return hash;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}


