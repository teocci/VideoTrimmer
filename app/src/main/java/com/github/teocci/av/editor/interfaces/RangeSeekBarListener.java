package com.github.teocci.av.editor.interfaces;

import com.github.teocci.av.editor.views.RangeSeekBarView;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-19
 */
public interface RangeSeekBarListener
{
    void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value);

    void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value);

    void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value);

    void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value);
}
