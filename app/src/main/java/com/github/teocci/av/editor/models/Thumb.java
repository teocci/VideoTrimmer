package com.github.teocci.av.editor.models;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.github.teocci.av.editor.R;

import java.util.List;
import java.util.Vector;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-19
 */
public class Thumb
{
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private int index;
    private float value;
    private float position;
    private Bitmap bitmap;
    private int width;
    private int height;

    private float lastTouchX;
    private float lastTouchY;

    private Thumb()
    {
        value = 0;
        position = 0;
    }

    public int getIndex()
    {
        return index;
    }

    private void setIndex(int index)
    {
        this.index = index;
    }

    public float getValue()
    {
        return value;
    }

    public void setValue(float val)
    {
        value = val;
    }

    public float getPos()
    {
        return position;
    }

    public void setPos(float pos)
    {
        position = pos;
    }

    public Bitmap getBitmap()
    {
        return bitmap;
    }

    private void setBitmap(Bitmap bitmap)
    {
        this.bitmap = bitmap;
        width = bitmap.getWidth();
        height = bitmap.getHeight();
    }


    public static List<Thumb> initThumbs(Resources resources)
    {
        List<Thumb> thumbs = new Vector<>();

        for (int i = 0; i < 2; i++) {
            Thumb th = new Thumb();
            th.setIndex(i);
            if (i == 0) {
                int resImageLeft = R.drawable.video_trim_handle;
                th.setBitmap(BitmapFactory.decodeResource(resources, resImageLeft));
            } else {
                int resImageRight = R.drawable.video_trim_handle;
                th.setBitmap(BitmapFactory.decodeResource(resources, resImageRight));
            }

            thumbs.add(th);
        }

        return thumbs;
    }

    public static int getWidthBitmap(List<Thumb> thumbs)
    {
        return thumbs.get(0).getWidthBitmap();
    }

    public static int getHeightBitmap(List<Thumb> thumbs)
    {
        return thumbs.get(0).getHeightBitmap();
    }

    public float getLastTouchX()
    {
        return lastTouchX;
    }

    public void setLastTouchX(float lastTouchX)
    {
        this.lastTouchX = lastTouchX;
    }

    public float getLastTouchY()
    {
        return lastTouchY;
    }

    public void setLastTouchY(float mLastTouchY)
    {
        this.lastTouchY = mLastTouchY;
    }

    public int getWidthBitmap()
    {
        return width;
    }

    private int getHeightBitmap()
    {
        return height;
    }
}
