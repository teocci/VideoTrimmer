package com.github.teocci.av.editor.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.teocci.av.editor.executors.BackgroundExecutor;
import com.github.teocci.av.editor.interfaces.BasicListener;
import com.github.teocci.av.editor.interfaces.TrimVideoListener;
import com.github.teocci.av.editor.models.VideoInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-19
 */
public class TrimVideoUtil
{
    private static final String TAG = LogHelper.makeLogTag(TrimVideoUtil.class);

    public static final int VIDEO_MAX_DURATION = 15; // In seconds
    public static final int MIN_TIME_FRAME = 5;

    private static final int THUMB_WIDTH = (DeviceUtil.getDeviceWidth() - UnitConverter.dpToPx(20)) / VIDEO_MAX_DURATION;
    private static final int THUMB_HEIGHT = UnitConverter.dpToPx(60);

    private static final long ONE_FRAME_TIME = 1000000;

    public static void trim(Context context, String inputFile, String outputFile, long startMs, long endMs, final TrimVideoListener callback)
    {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String outputName = "trimmedVideo_" + timeStamp + ".mp4";
        outputFile = outputFile + "/" + outputName;

        String start = convertSecondsToTime(startMs / 1000);
        String duration = convertSecondsToTime((endMs - startMs) / 1000);

        /*
         * Trimming video with ffmpeg:
         * ffmpeg -ss START -t DURATION -i INPUT -vcodec copy -acodec copy OUTPUT
         * -ss start time, such as: 00:00:20, starting from 20 seconds;
         * -t duration, such as: 00:00:10, indicates intercepting a 10-seconds video;
         * -i input, followed by a space, followed by the input video file;
         * -vcodec copy and -acodec copy indicate the encoding format of the video and audio to be used, where copy is designated as an original copy;
         * INPUT, input video file;
         * OUTPUT, output video file
         */
        String cmd = "-ss " + start + " -t " + duration + " -i " + inputFile + " -vcodec copy -acodec copy " + outputFile;
        String[] command = cmd.split(" ");
        try {
            final String tempOutFile = outputFile;
            FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler()
            {
                @Override
                public void onFailure(String s)
                {
                }

                @Override
                public void onSuccess(String s)
                {
                    callback.onFinishTrim(tempOutFile);
                }

                @Override
                public void onStart()
                {
                    callback.onStartTrim();
                }

                @Override
                public void onFinish()
                {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    public static void extractVideoThumbnails(
            final Context context,
            final Uri videoUri,
            final BasicListener<ArrayList<Bitmap>, Integer> callback
    )
    {
        final ArrayList<Bitmap> thumbnailList = new ArrayList<>();
        BackgroundExecutor.execute(
                new BackgroundExecutor.Task("", 0L, "")
                {
                    @Override
                    public void execute()
                    {
                        try {
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(context, videoUri);
                            extractThumbnails(retriever, thumbnailList, callback);
                            retriever.release();
                        } catch (final Throwable e) {
                            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                        }
                    }
                }
        );

    }

    private static void extractThumbnails(
            MediaMetadataRetriever retriever,
            ArrayList<Bitmap> thumbnailList,
            BasicListener<ArrayList<Bitmap>, Integer> callback
    )
    {
        // Retrieve media data use microsecond
        long videoLengthInMs = Long.parseLong(retriever.extractMetadata(METADATA_KEY_DURATION)) * 1000;
        long numThumbs = videoLengthInMs < ONE_FRAME_TIME ? 1 : (videoLengthInMs / ONE_FRAME_TIME);
        final long interval = videoLengthInMs / numThumbs;

        // Callback a frame  after 3 frames
        for (long i = 0; i < numThumbs; ++i) {
            Bitmap bitmap = retriever.getFrameAtTime(i * interval, OPTION_CLOSEST_SYNC);
            try {
                bitmap = Bitmap.createScaledBitmap(bitmap, THUMB_WIDTH, THUMB_HEIGHT, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            thumbnailList.add(bitmap);
            if (thumbnailList.size() == 3) {
                callback.on(clone(thumbnailList), (int) interval);
                thumbnailList.clear();
            }
        }
        if (thumbnailList.size() > 0) {
            callback.on(clone(thumbnailList), (int) interval);
            thumbnailList.clear();
        }
    }

    public static ArrayList<VideoInfo> getAllVideoFiles(Context mContext)
    {
        VideoInfo video;
        ArrayList<VideoInfo> videos = new ArrayList<>();
        ContentResolver contentResolver = mContext.getContentResolver();
        try {
            Cursor cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null,
                    null, null, MediaStore.Video.Media.DATE_MODIFIED + " desc");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    video = new VideoInfo();
                    if (cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)) != 0) {
                        video.setDuration(cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)));
                        video.setVideoPath(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
                        video.setCreateTime(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)));
                        video.setVideoName(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));
                        videos.add(video);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videos;
    }

    private static ArrayList<Bitmap> clone(ArrayList<Bitmap> list)
    {
        ArrayList<Bitmap> clone = new ArrayList<>(list.size());
        for (Bitmap item : list) clone.add(item.copy(item.getConfig(), true));
        return clone;
    }

    public static String getVideoFilePath(String url)
    {
        if (TextUtils.isEmpty(url) || url.length() < 5)
            return "";
        if (url.substring(0, 4).equalsIgnoreCase("http")) {
            return "";
        } else {
            url = "file://" + url;
        }

        return url;
    }

    private static String convertSecondsToTime(long seconds)
    {
        return DateUtil.convertSecondsToTime(seconds);
    }
}
