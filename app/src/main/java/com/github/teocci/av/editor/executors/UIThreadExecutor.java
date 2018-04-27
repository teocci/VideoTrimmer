package com.github.teocci.av.editor.executors;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.github.teocci.av.editor.models.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Apr-25
 */
public class UIThreadExecutor
{
    private static final Map<String, Token> tokens = new HashMap<>();

    private static final Handler handler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            Runnable callback = msg.getCallback();
            if (callback != null) {
                callback.run();
                decrementToken((Token) msg.obj);
            } else {
                super.handleMessage(msg);
            }
        }
    };

    private UIThreadExecutor()
    {
        // should not be instantiated
    }

    /**
     * Store a new task in the map for providing cancellation. This method is
     * used by AndroidAnnotations and not intended to be called by clients.
     *
     * @param id    the identifier of the task
     * @param task  the task itself
     * @param delay the delay or zero to run immediately
     */
    public static void runTask(String id, Runnable task, long delay)
    {
        if ("".equals(id)) {
            handler.postDelayed(task, delay);
            return;
        }
        long time = SystemClock.uptimeMillis() + delay;
        handler.postAtTime(task, nextToken(id), time);
    }

    private static Token nextToken(String id)
    {
        synchronized (tokens) {
            Token token = tokens.get(id);
            if (token == null) {
                token = new Token(id);
                tokens.put(id, token);
            }
            token.runnableCount++;
            return token;
        }
    }

    private static void decrementToken(Token token)
    {
        synchronized (tokens) {
            if (--token.runnableCount == 0) {
                String id = token.id;
                Token old = tokens.remove(id);
                if (old != token) {
                    // a runnable finished after cancelling, we just removed a
                    // wrong token, lets put it back
                    tokens.put(id, old);
                }
            }
        }
    }

    /**
     * Cancel all tasks having the specified <code>id</code>.
     *
     * @param id the cancellation identifier
     */
    public static void cancelAll(String id)
    {
        Token token;
        synchronized (tokens) {
            token = tokens.remove(id);
        }
        if (token == null) {
            // nothing to cancel
            return;
        }
        handler.removeCallbacksAndMessages(token);
    }
}
