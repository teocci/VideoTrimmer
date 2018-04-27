package com.github.teocci.av.editor.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.teocci.av.editor.R;
import com.github.teocci.av.editor.handlers.VideoRequestHandler;
import com.github.teocci.av.editor.interfaces.BasicListener;
import com.github.teocci.av.editor.models.VideoInfo;
import com.github.teocci.av.editor.utils.DateUtil;
import com.github.teocci.av.editor.utils.DeviceUtil;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Dec-06
 */
public class VideoGridViewAdapter extends RecyclerView.Adapter<VideoGridViewAdapter.VideoViewHolder>
{
    private Context context;
    private BasicListener<Boolean, VideoInfo> selectListener;

    private List<VideoInfo> videoListData;
    private List<VideoInfo> videoSelect = new ArrayList<>();
    private List<ImageView> selectIconList = new ArrayList<>();

    private boolean selected = false;

    public VideoGridViewAdapter(Context context, List<VideoInfo> dataList)
    {
        this.context = context;
        this.videoListData = dataList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View view = inflater.inflate(R.layout.video_select_gridview_item, parent, false);

        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position)
    {
        VideoInfo video = videoListData.get(position);
        holder.durationTv.setText(DateUtil.convertSecondsToTime(video.getDuration() / 1000));
        Picasso.get().load(VideoRequestHandler.SCHEME_VIDEO + ":"+ video.getVideoPath()).into(holder.videoCover);
    }

    @Override
    public int getItemCount()
    {
        return videoListData.size();
    }

    public void setItemClickCallback(final BasicListener<Boolean, VideoInfo> basicListener)
    {
        this.selectListener = basicListener;
    }

    class VideoViewHolder extends RecyclerView.ViewHolder
    {

        ImageView videoCover, selectIcon;
        View videoItemView, videoSelectPanel;
        TextView durationTv;

        VideoViewHolder(final View itemView)
        {
            super(itemView);
            videoItemView = itemView.findViewById(R.id.video_view);
            videoCover = (ImageView) itemView.findViewById(R.id.cover_image);
            durationTv = (TextView) itemView.findViewById(R.id.video_duration);
            videoSelectPanel = itemView.findViewById(R.id.video_select_panel);
            selectIcon = (ImageView) itemView.findViewById(R.id.select);

            int size = DeviceUtil.getDeviceWidth() / 4;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) videoCover.getLayoutParams();
            params.width = size;
            params.height = size;
            videoCover.setLayoutParams(params);
            videoSelectPanel.setOnClickListener(v -> {
                VideoInfo videoInfo = videoListData.get(getAdapterPosition());
                if (videoSelect.size() > 0) {
                    if (videoInfo.equals(videoSelect.get(0))) {
                        selectIcon.setImageResource(R.drawable.ic_video_unselected);
                        clearAll();
                        selected = false;
                    } else {
                        selectIconList.get(0).setImageResource(R.drawable.ic_video_unselected);
                        clearAll();
                        addData(videoInfo);
                        selectIcon.setImageResource(R.drawable.ic_video_selected);
                        selected = true;
                    }

                } else {
                    clearAll();
                    addData(videoInfo);
                    selectIcon.setImageResource(R.drawable.ic_video_selected);
                    selected = true;
                }
                selectListener.on(selected, videoListData.get(getAdapterPosition()));
            });
        }

        private void addData(VideoInfo videoInfo)
        {
            videoSelect.add(videoInfo);
            selectIconList.add(selectIcon);
        }
    }

    private void clearAll()
    {
        videoSelect.clear();
        selectIconList.clear();
    }
}
