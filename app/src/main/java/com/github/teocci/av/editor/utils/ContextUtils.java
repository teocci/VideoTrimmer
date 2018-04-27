package com.github.teocci.av.editor.utils;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-24
 */
public class ContextUtils
{
    private static final String ERROR_INIT = "Initialize ContextUtils with invoke init()";

    private static WeakReference<Context> weakReference;

    public static void init(Context ctx)
    {
        weakReference = new WeakReference<>(ctx);
    }

    public static Context getContext()
    {
        if (weakReference == null) {
            throw new IllegalArgumentException(ERROR_INIT);
        }
        return weakReference.get().getApplicationContext();
    }
}
