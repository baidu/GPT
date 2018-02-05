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
package com.baidu.android.gporter.gpt;

import android.database.MatrixCursor;
import android.os.Bundle;

/**
 * 用于contentprovider代理时返回extra
 *
 * @author liuhaitao
 * @since 2015年12月17日
 */
public class ProviderProxyCursor extends MatrixCursor {

    /**
     * Bundle
     */
    private Bundle mExtras = Bundle.EMPTY;

    /**
     * @param columnNames columnNames
     */
    public ProviderProxyCursor(String[] columnNames) {
        super(columnNames);
    }


    @Override
    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * android 2.3 没有此函数，4.0 以后是个隐藏函数。
     */
    public void setExtras(Bundle extras) {
        mExtras = (extras == null) ? Bundle.EMPTY : extras;
    }

}
