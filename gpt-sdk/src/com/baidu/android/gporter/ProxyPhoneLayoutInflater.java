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
package com.baidu.android.gporter;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;

import com.baidu.android.gporter.util.Constants;

import java.lang.reflect.Constructor;

/**
 * 用于解决 setContentView ，如果插件和主进程在同一个进程，并且两个classloader中有相同的 view class。比如 support v4。
 * <p>
 * LayoutInflator 中 sConstructorMap 静态变量，导致的类型转换错误。
 *
 * @author liuhaitao
 * @since 2015年6月10日
 */
public class ProxyPhoneLayoutInflater extends LayoutInflater {

    /**
     * DEBUG 开关
     */
    public static final boolean DEBUG = true & Constants.DEBUG;
    /**
     * TAG
     */
    public static final String TAG = "ProxyPhoneLayoutInflater";

    /**
     * CLASS_PREFIX_LIST
     */
    private static final String[] CLASS_PREFIX_LIST =
            {
                    "android.widget.",
                    "android.webkit."
            };

    /**
     * 自定义的Factory代理
     */
    private Factory mFactory;

    /**
     * Instead of instantiating directly, you should retrieve an instance
     * through {@link Context#getSystemService}
     *
     * @param context The Context in which to find resources and other
     *                application-specific things.
     * @see Context#getSystemService
     */
    public ProxyPhoneLayoutInflater(Context context) {
        super(context);
    }

    /**
     * Instead of instantiating directly, you should retrieve an instance
     * through {@link Context#getSystemService}
     *
     * @param original   LayoutInflater
     * @param newContext The Context in which to find resources and other
     *                   application-specific things.
     */
    public ProxyPhoneLayoutInflater(LayoutInflater original, Context newContext) {
        super(original, newContext);
    }

    @Override
    protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {

        for (String prefix : CLASS_PREFIX_LIST) {
            try {
                View view = createView(name, prefix, attrs);
                if (view != null) {
                    return view;
                }
            } catch (ClassNotFoundException e) {
                // In this case we want to let the base class take a crack at it.
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        return super.onCreateView(name, attrs);
    }

    @Override
    public void setFactory(android.view.LayoutInflater.Factory factory) {
        /*
         * 由于自定义的ProxyPhoneLayoutInflater在getSystemService的时候已经调用了一次setFactory，
         * 为了不影响插件中再次调用该方法， 再给一次调用的机会，并将其封装到自定义的Factory类中。
         * 注意：第一次setFactory要求必须是自定义的Factory实例，这个在getSystemService已经写死。
         */
        if (mFactory == null) {
            mFactory = new ProxyPhoneLayoutInflater.Factory(getContext());
            super.setFactory(mFactory);
            if (factory != null) {
                mFactory.setFactory(factory);
            }
        } else {
            mFactory.setFactory(factory);
        }
    }

    /**
     * cloneInContext
     *
     * @param newContext Context
     * @return LayoutInflater
     */
    public LayoutInflater cloneInContext(Context newContext) {
        return new ProxyPhoneLayoutInflater(this, newContext);
    }

    /**
     * Factory
     */
    private static class Factory implements LayoutInflater.Factory {

        /**
         * LayoutInflater.Factory
         */
        private LayoutInflater.Factory mFactory;

        /**
         * Context
         */
        private Context mContext;

        /**
         * CONSTRUCTOR_SIGNATURE
         */
        static final Class<?>[] CONSTRUCTOR_SIGNATURE = new Class[]{
                Context.class, AttributeSet.class};

        /**
         * mConstructorArgs
         */
        final Object[] mConstructorArgs = new Object[2];

        /**
         * 构造方法
         */
        public Factory(Context context) {
            mContext = context;
        }

        /**
         * setFactory
         */
        public void setFactory(android.view.LayoutInflater.Factory factory) {
            if (factory == null) {
                throw new NullPointerException("Given factory can not be null");
            }

            if (mFactory == null) {
                mFactory = factory;
            } else {
                throw new IllegalStateException("A factory has already been set on this LayoutInflater");
            }
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            View view = null;

            if (mFactory != null) {
                view = mFactory.onCreateView(name, context, attrs);
                if (view != null) {
                    return view;
                }
            }

            if (-1 != name.indexOf('.')) {
                // 非系统view、自己处理。
                try {
                    view = createView(name, null, attrs);
                } catch (Exception e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            } else if ("fragment".equals(name)) {
                /*
                 * 处理 fragment createview。
                 * 因为我们自己在activity之前就set了layoutinflator，参考ActivityProxy.changeInflatorContext。
                 * 导致FragmentActivity无法自己setFactory。所以我们需要接管他。
                 */
                Activity activity = (Activity) context;
                view = activity.onCreateView(name, context, attrs);
            }

            return view;
        }

        /**
         * createView
         *
         * @param name   Name
         * @param prefix Prefix
         * @param attrs  AttributeSet
         * @return View
         * @throws Exception 主要出错时抛异常
         */
        public final View createView(String name, String prefix, AttributeSet attrs)
                throws ClassNotFoundException, InflateException {
            Class<? extends View> clazz = null;

            try {
                // Class not found in the cache, see if it's real, and try to add it
                clazz = mContext.getClassLoader().loadClass(prefix != null ? (prefix + name) : name)
                        .asSubclass(View.class);

                Constructor<? extends View> constructor = clazz.getConstructor(CONSTRUCTOR_SIGNATURE);

                Object[] args = mConstructorArgs;
                args[0] = mContext;
                args[1] = attrs;

                final View view = constructor.newInstance(args);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (view instanceof ViewStub) {
                        // always use ourselves when inflating ViewStub later
                        final ViewStub viewStub = (ViewStub) view;
                        viewStub.setLayoutInflater(LayoutInflater.from(mContext));
                    }
                }

                return view;

            } catch (NoSuchMethodException e) {
                InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class "
                        + (prefix != null ? (prefix + name) : name));
                ie.initCause(e);
                throw ie;

            } catch (ClassCastException e) {
                // If loaded class is not a View subclass
                InflateException ie = new InflateException(attrs.getPositionDescription() + ": Class is not a View "
                        + (prefix != null ? (prefix + name) : name));
                ie.initCause(e);
                throw ie;
            } catch (ClassNotFoundException e) {
                // If loadClass fails, we should propagate the exception.
                throw e;
            } catch (Exception e) {
                InflateException ie = new InflateException(attrs.getPositionDescription() + ": Error inflating class "
                        + (clazz == null ? "<unknown>" : clazz.getName()));
                ie.initCause(e);
                throw ie;
            } finally {
                // do nothing
            }
        }

    }
}
