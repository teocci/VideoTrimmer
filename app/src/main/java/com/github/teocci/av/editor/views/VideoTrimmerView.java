package com.github.teocci.av.editor.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.github.teocci.av.editor.R;
import com.github.teocci.av.editor.executors.BackgroundExecutor;
import com.github.teocci.av.editor.executors.UIThreadExecutor;
import com.github.teocci.av.editor.interfaces.ProgressVideoListener;
import com.github.teocci.av.editor.interfaces.RangeSeekBarListener;
import com.github.teocci.av.editor.interfaces.TrimVideoListener;
import com.github.teocci.av.editor.models.Thumb;
import com.github.teocci.av.editor.utils.DeviceUtil;
import com.github.teocci.av.editor.utils.LogHelper;
import com.github.teocci.av.editor.utils.TrimVideoUtil;
import com.github.teocci.av.editor.utils.UnitConverter;
import com.github.teocci.av.editor.views.VideoThumbHorizontalListView.OnScrollStateChangedListener;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-19
 */

public class VideoTrimmerView extends FrameLayout
{
    private static final String TAG = LogHelper.makeLogTag(VideoTrimmerView.class);

    private static final int MARGIN = UnitConverter.dpToPx(6);
    private static final int SCREEN_WIDTH = DeviceUtil.getDeviceWidth() - MARGIN * 2;
    private static final int SCREEN_WIDTH_FULL = DeviceUtil.getDeviceWidth();
    private static final int SHOW_PROGRESS = 2;

    private Context context;
    private RelativeLayout linearVideo;

    private SeekBar seekBarView;
    private RangeSeekBarView rangeSeekBarView;
    private VideoView videoView;
    private ImageView playView;
    private VideoThumbHorizontalListView videoThumbListView;

    private Uri sourcePath;
    private String finalPath;
    private ProgressVideoListener progressListener;
    private TrimVideoListener trimListener;

    private long maxDuration;
    private int duration = 0;
    private long timeVideo = 0;
    private long startPosition = 0, endPosition = 0;

    private VideoThumbAdapter videoThumbAdapter;
    private long pixelRangeMax;
    private int currentPixMax; // For the playback bar
    private int scrolledOffset;
    private float leftThumbValue, rightThumbValue;
    private boolean isFromRestore = false;

    private final MessageHandler messageHandler = new MessageHandler(this);

    private OnScrollStateChangedListener scrollStateChangedListener = new OnScrollStateChangedListener()
    {
        @Override
        public void onScrollStateChanged(ScrollState scrollState, int scrolledOffset)
        {
            if (videoThumbListView.getCurrentX() == 0)
                return;

            switch (scrollState) {
                case SCROLL_STATE_FLING:
                case SCROLL_STATE_IDLE:
                case SCROLL_STATE_TOUCH_SCROLL:
                    if (scrolledOffset < 0) {
                        VideoTrimmerView.this.scrolledOffset = VideoTrimmerView.this.scrolledOffset - Math.abs(scrolledOffset);
                        if (VideoTrimmerView.this.scrolledOffset <= 0)
                            VideoTrimmerView.this.scrolledOffset = 0;
                    } else {
                        if (PixToTime(VideoTrimmerView.this.scrolledOffset + SCREEN_WIDTH) <= duration) // if it can scroll to the left
                            VideoTrimmerView.this.scrolledOffset = VideoTrimmerView.this.scrolledOffset + scrolledOffset;
                    }
                    onVideoReset();
                    onSeekThumbs(0, VideoTrimmerView.this.scrolledOffset + leftThumbValue);
                    onSeekThumbs(1, VideoTrimmerView.this.scrolledOffset + rightThumbValue);
                    rangeSeekBarView.invalidate();
                    break;
            }
        }
    };

    public VideoTrimmerView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public VideoTrimmerView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context)
    {
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.video_trimmer_view, this, true);

        seekBarView = ((SeekBar) findViewById(R.id.handlerTop));
        rangeSeekBarView = ((RangeSeekBarView) findViewById(R.id.timeLineBar));
        linearVideo = ((RelativeLayout) findViewById(R.id.layout_surface_view));
        videoView = ((VideoView) findViewById(R.id.video_loader));
        playView = ((ImageView) findViewById(R.id.icon_video_play));
        videoThumbListView = (VideoThumbHorizontalListView) findViewById(R.id.video_thumb_listview);
        videoThumbAdapter = new VideoThumbAdapter(this.context);
        videoThumbListView.setAdapter(videoThumbAdapter);

        videoThumbListView.setOnScrollStateChangedListener(scrollStateChangedListener);
        setUpListeners();

        setUpSeekBar();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpSeekBar()
    {
        seekBarView.setEnabled(false);
        seekBarView.setOnTouchListener(new OnTouchListener()
        {
            private float startX;

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        return false;
                }

                return true;
            }
        });

    }

    public void setVideoURI(final Uri videoURI)
    {
        sourcePath = videoURI;

        videoView.setVideoURI(sourcePath);
        videoView.requestFocus();

        TrimVideoUtil.extractVideoThumbnails(
                context,
                sourcePath,
                (bitmap, interval) -> UIThreadExecutor.runTask(
                        "",
                        () -> {
                            videoThumbAdapter.addAll(bitmap);
                            videoThumbAdapter.notifyDataSetChanged();
                        },
                        0L
                )
        );
    }

    private void initSeekBarPosition()
    {
        seekTo(startPosition);

        pixelRangeMax = (duration * SCREEN_WIDTH) / maxDuration;
        rangeSeekBarView.initThumbForRangeSeekBar(duration, pixelRangeMax);

        if (duration >= maxDuration) {
            endPosition = maxDuration;
            timeVideo = maxDuration;
        } else {
            endPosition = duration;
            timeVideo = duration;
        }

        // This seekBar,Waste a lot of my time
        setUpProgressBarMarginsAndWidth(MARGIN, SCREEN_WIDTH_FULL - (int) TimeToPix(endPosition) - MARGIN);

        rangeSeekBarView.setThumbValue(0, 0);
        rangeSeekBarView.setThumbValue(1, TimeToPix(endPosition));
        videoView.pause();
        setProgressBarMax();
        setProgressBarPosition(startPosition);
        rangeSeekBarView.initMaxWidth();
        rangeSeekBarView.setStartEndTime(startPosition, endPosition);

        // Record the initial position of the two cursors corresponding to the screen, these
        // two values will only be valid when the video length can be scrolled
        leftThumbValue = 0;
        rightThumbValue = duration <= maxDuration ? TimeToPix(duration) : TimeToPix(maxDuration);
    }

    private void initSeekBarFromRestore()
    {

        seekTo(startPosition);
        // Sets the left offset
        setUpProgressBarMarginsAndWidth((int) leftThumbValue, (int) (SCREEN_WIDTH_FULL - rightThumbValue - MARGIN));

        setProgressBarMax();
        setProgressBarPosition(startPosition);
        rangeSeekBarView.setStartEndTime(startPosition, endPosition);

        leftThumbValue = 0;
        rightThumbValue = duration <= maxDuration ? TimeToPix(duration) : TimeToPix(maxDuration);
    }

    private void onCancelClicked()
    {
        trimListener.onCancel();
    }

    private void onPlayerIndicatorSeekStart()
    {
        messageHandler.removeMessages(SHOW_PROGRESS);
        videoView.pause();
        notifyProgressUpdate();
    }

    private void onPlayerIndicatorSeekStop(SeekBar seekBar)
    {
        videoView.pause();
    }


    private void onVideoPrepared(MediaPlayer mp)
    {
        ViewGroup.LayoutParams lp = videoView.getLayoutParams();
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = linearVideo.getWidth();
        int screenHeight = linearVideo.getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        videoView.setLayoutParams(lp);

        duration = (videoView.getDuration() / 1000) * 1000;

        if (duration > maxDuration) {
            maxDuration = duration;
        }

        if (!getRestoreState())
            initSeekBarPosition();
        else {
            setRestoreState(false);
            initSeekBarFromRestore();
        }
    }


    private void onSeekThumbs(int index, float value)
    {
        switch (index) {
            case Thumb.LEFT: {
                startPosition = PixToTime(value);
                setProgressBarPosition(startPosition);
                break;
            }
            case Thumb.RIGHT: {
                endPosition = PixToTime(value);
                if (endPosition > duration)
                    endPosition = duration;
                break;
            }
        }
        setProgressBarMax();

        rangeSeekBarView.setStartEndTime(startPosition, endPosition);
        seekTo(startPosition);
        timeVideo = endPosition - startPosition;

        setUpProgressBarMarginsAndWidth((int) leftThumbValue, (int) (SCREEN_WIDTH_FULL - rightThumbValue - MARGIN));
    }

    private void onStopSeekThumbs()
    {
        messageHandler.removeMessages(SHOW_PROGRESS);
        setProgressBarPosition(startPosition);
        onVideoReset();
    }

    private void onVideoCompleted()
    {
        seekTo(startPosition);
        setPlayPauseViewIcon(false);
    }

    private void onVideoReset()
    {
        videoView.pause();
        setPlayPauseViewIcon(false);
    }

    public void onPause()
    {
        if (videoView.isPlaying()) {
            messageHandler.removeMessages(SHOW_PROGRESS);
            videoView.pause();
            seekTo(startPosition); // Reset
            setPlayPauseViewIcon(false);
        }
    }

    private void setProgressBarPosition(long time)
    {
        seekBarView.setProgress((int) (time - startPosition));
    }

    private void setProgressBarMax()
    {
        seekBarView.setMax((int) (endPosition - startPosition));
    }

    public void setOnTrimVideoListener(TrimVideoListener onTrimVideoListener)
    {
        trimListener = onTrimVideoListener;
    }

    /**
     * Cancel trim thread execute action when finish
     */
    public void destroy()
    {
        BackgroundExecutor.cancelAll("", true);
        UIThreadExecutor.cancelAll("");
    }

    public void setMaxDuration(int maxDuration)
    {
        this.maxDuration = maxDuration * 1000;
    }

    private void setUpListeners()
    {
        progressListener = (time, max, scale) -> updateVideoProgress(time);

        findViewById(R.id.cancelBtn).setOnClickListener(
                view -> onCancelClicked()
        );

        findViewById(R.id.finishBtn).setOnClickListener(
                view -> onSaveClicked()
        );

        rangeSeekBarView.addOnRangeSeekBarListener(new RangeSeekBarListener()
        {
            @Override
            public void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value)
            {
            }

            @Override
            public void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value)
            {
                if (index == 0) {
                    leftThumbValue = value;
                } else {
                    rightThumbValue = value;
                }

                onSeekThumbs(index, value + Math.abs(scrolledOffset));
            }

            @Override
            public void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value)
            {
                if (seekBarView.getVisibility() == View.VISIBLE)
                    seekBarView.setVisibility(GONE);
            }

            @Override
            public void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value)
            {
                onStopSeekThumbs();
            }
        });

        seekBarView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                onPlayerIndicatorSeekStart();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                onPlayerIndicatorSeekStop(seekBar);
            }
        });

        videoView.setOnPreparedListener(mp -> onVideoPrepared(mp));

        videoView.setOnCompletionListener(mp -> onVideoCompleted());

        playView.setOnClickListener(v -> onClickVideoPlayPause());
    }

    private void setUpProgressBarMarginsAndWidth(int left, int right)
    {
        if (left == 0)
            left = MARGIN;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) seekBarView.getLayoutParams();
        lp.setMargins(left, 0, right, 0);
        seekBarView.setLayoutParams(lp);
        currentPixMax = SCREEN_WIDTH_FULL - left - right;
        seekBarView.getLayoutParams().width = currentPixMax;
    }

    private void onSaveClicked()
    {
        if (endPosition / 1000 - startPosition / 1000 < TrimVideoUtil.MIN_TIME_FRAME) {
            Toast.makeText(context, "Video cannot be processed: Video length is less than 5 seconds", Toast.LENGTH_SHORT).show();
        } else {
            videoView.pause();
            TrimVideoUtil.trim(context, sourcePath.getPath(), getTrimmedVideoPath(), startPosition, endPosition, trimListener);
        }
    }

    private String getTrimmedVideoPath()
    {
        if (finalPath == null) {
            File file = context.getExternalCacheDir();
            if (file != null)
                finalPath = file.getAbsolutePath();
        }
        return finalPath;
    }

    private void onClickVideoPlayPause()
    {
        if (videoView.isPlaying()) {
            videoView.pause();
            messageHandler.removeMessages(SHOW_PROGRESS);
        } else {
            videoView.start();
            seekBarView.setVisibility(View.VISIBLE);
            messageHandler.sendEmptyMessage(SHOW_PROGRESS);
        }

        setPlayPauseViewIcon(videoView.isPlaying());
    }

    /**
     * The width of the screen is converted based on the length of the video
     */
    private long PixToTime(float value)
    {
        if (pixelRangeMax == 0)
            return 0;
        return (long) ((duration * value) / pixelRangeMax);
    }

    /**
     * The length of the video is converted based on the width of the screen
     */
    private long TimeToPix(long value)
    {
        return (pixelRangeMax * value) / duration;
    }

    private void seekTo(long msec)
    {
        videoView.seekTo((int) msec);
    }


    private boolean getRestoreState()
    {
        return isFromRestore;
    }

    public void setRestoreState(boolean fromRestore)
    {
        isFromRestore = fromRestore;
    }

    private static class MessageHandler extends Handler
    {
        private final WeakReference<VideoTrimmerView> mView;

        MessageHandler(VideoTrimmerView view)
        {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg)
        {
            VideoTrimmerView view = mView.get();
            if (view == null || view.videoView == null) {
                return;
            }

            view.notifyProgressUpdate();
            if (view.videoView.isPlaying()) {
                sendEmptyMessageDelayed(0, 10);
            }
        }
    }

    private void updateVideoProgress(int time)
    {
        if (videoView == null) {
            return;
        }
        LogHelper.i(TAG, "updateVideoProgress time = " + time);
        if (time >= endPosition) {
            messageHandler.removeMessages(SHOW_PROGRESS);
            videoView.pause();
            seekTo(startPosition);
            setPlayPauseViewIcon(false);
            return;
        }

        if (seekBarView != null) {
            setProgressBarPosition(time);
        }
    }

    private void notifyProgressUpdate()
    {
        if (duration == 0) return;

        int position = videoView.getCurrentPosition();
        LogHelper.i(TAG, "updateVideoProgress position = " + position);
        progressListener.updateProgress(position, 0, 0);
    }

    private void setPlayPauseViewIcon(boolean isPlaying)
    {
        playView.setImageResource(isPlaying ? R.drawable.ic_video_stop : R.drawable.ic_video_play);
    }

    private class VideoThumbAdapter extends ArrayAdapter<Bitmap>
    {
        VideoThumbAdapter(Context context)
        {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            VideoThumbHolder videoThumbHolder;
            if (convertView == null) {
                videoThumbHolder = new VideoThumbHolder();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_thumb_item_layout, null);
                videoThumbHolder.thumb = (ImageView) convertView.findViewById(R.id.thumb);
                convertView.setTag(videoThumbHolder);
            } else {
                videoThumbHolder = (VideoThumbHolder) convertView.getTag();
            }
            videoThumbHolder.thumb.setImageBitmap(getItem(position));
            return convertView;
        }
    }

    private static class VideoThumbHolder
    {
        public ImageView thumb;
    }
}
