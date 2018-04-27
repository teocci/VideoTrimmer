package com.github.teocci.av.editor.utils;

import android.os.Environment;

import static android.os.Environment.DIRECTORY_DCIM;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-19
 */
public class Config
{
    public static final String APP_NAME = "VideoTrimmer";

    public static final String LOG_PREFIX = "[" + APP_NAME + "]";

    public static final String DS = "/"; // DIRECTORY_SEPARATOR

    public static final String VT_PREFIX = "vt_";
    public static final String VC_PREFIX = "vc_";
    public static final String MP4_EXTENTION = ".mp4";


    public static final String DATE_FORMAT_FILE = "yyyyMMdd_HHmmss";


    public static final String DIRECTORY_PARENT = Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath();
    public static final String DIRECTORY_BASE = DIRECTORY_PARENT + DS + APP_NAME;

    public static final int DEFAULT_RTSP_PORT = 8086;
}
