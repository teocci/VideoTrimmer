package com.github.teocci.av.editor.interfaces;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-19
 */
public interface TrimVideoListener
{
    void onStartTrim();

    void onFinishTrim(String url);

    void onCancel();
}
