package com.github.teocci.av.editor.ui;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.github.teocci.av.editor.R;
import com.github.teocci.av.editor.adapters.VideoGridViewAdapter;
import com.github.teocci.av.editor.databinding.VideoSelectLayoutBinding;
import com.github.teocci.av.editor.handlers.VideoRequestHandler;
import com.github.teocci.av.editor.interfaces.BasicListener;
import com.github.teocci.av.editor.models.VideoInfo;
import com.github.teocci.av.editor.utils.ContextUtils;
import com.github.teocci.av.editor.utils.LogHelper;
import com.github.teocci.av.editor.utils.TrimVideoUtil;
import com.github.teocci.av.editor.views.SpacesItemDecoration;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.github.teocci.av.editor.utils.Config.DIRECTORY_BASE;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Dec-06
 */
public class VideoSelectActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = LogHelper.makeLogTag(VideoSelectActivity.class);

    private VideoSelectLayoutBinding binding;

    private List<VideoInfo> allVideos = new ArrayList<>();

    private String videoPath;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        binding = DataBindingUtil.setContentView(this, R.layout.video_select_layout);

        ContextUtils.init(this);
        initImageLoader(this);
        initFFmpegBinary(this);
        initBaseDirectory();

        allVideos = TrimVideoUtil.getAllVideoFiles(this);
        GridLayoutManager manager = new GridLayoutManager(this, 4);
        binding.videoSelectRecyclerview.addItemDecoration(new SpacesItemDecoration(5));
        binding.videoSelectRecyclerview.setHasFixedSize(true);
        VideoGridViewAdapter videoGridViewAdapter;
        binding.videoSelectRecyclerview.setAdapter(videoGridViewAdapter = new VideoGridViewAdapter(this, allVideos));
        binding.videoSelectRecyclerview.setLayoutManager(manager);

        binding.videoShoot.setOnClickListener(this);
        binding.mBtnBack.setOnClickListener(this);
        binding.nextStep.setOnClickListener(this);

        binding.nextStep.setTextAppearance(this, getColorTextStyle(false));
        binding.nextStep.setEnabled(false);

        videoGridViewAdapter.setItemClickCallback((selected, video) -> {
            if (selected && video != null) {
                videoPath = video.getVideoPath();
            }
            binding.nextStep.setEnabled(selected);
            binding.nextStep.setTextAppearance(VideoSelectActivity.this, getColorTextStyle(selected));
        });
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        allVideos = null;
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == binding.mBtnBack.getId()) {
            finish();
        } else if (v.getId() == binding.nextStep.getId()) {
            TrimmerActivity.go(VideoSelectActivity.this, videoPath);
        }
    }

    public void initImageLoader(Context context)
    {
        Picasso picassoInstance;
        VideoRequestHandler videoRequestHandler = new VideoRequestHandler();
        picassoInstance = new Picasso.Builder(context.getApplicationContext())
                .addRequestHandler(videoRequestHandler)
                .build();

        try {
            Picasso.setSingletonInstance(picassoInstance);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

//        int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 10);
//        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
//                .memoryCache(new LRULimitedMemoryCache(memoryCacheSize))
//                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
//                .tasksProcessingOrder(QueueProcessingType.LIFO)
//                .build();
//        // Initialize ImageLoader with configuration.
//        ImageLoader.getInstance().init(config);
    }

    private void initFFmpegBinary(Context context)
    {
        try {
            FFmpeg.getInstance(context).loadBinary(new LoadBinaryResponseHandler()
            {
                @Override
                public void onFailure() {}
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }


    private void initBaseDirectory()
    {
        File directory = new File(DIRECTORY_BASE);
        // If the directory dir doesn't exist, create it
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                LogHelper.e(TAG, "Successfully created dir");
            } else {
                LogHelper.e(TAG, "Failed to create dir");
            }
        }
    }

    private int getColorTextStyle(boolean selected)
    {
        return selected ? R.style.blue_text_18_style : R.style.gray_text_18_style;
    }
}
