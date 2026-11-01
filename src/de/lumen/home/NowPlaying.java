package de.lumen.home;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.view.KeyEvent;

import java.util.List;

/** Reads whatever is playing via MediaSessionManager (system, MEDIA_CONTENT_CONTROL) and controls it. */
final class NowPlaying {
    private NowPlaying() {}

    static MediaController find(Context c) {
        try {
            MediaSessionManager m = (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> all = m.getActiveSessions(null);
            MediaController best = null;
            for (MediaController mc : all) {
                PlaybackState ps = mc.getPlaybackState();
                if (ps == null) continue;
                int st = ps.getState();
                if (st == PlaybackState.STATE_PLAYING) return mc;
                if ((st == PlaybackState.STATE_PAUSED || st == PlaybackState.STATE_BUFFERING) && best == null) best = mc;
            }
            return best;
        } catch (Throwable t) { return null; }
    }

    static boolean playing(MediaController mc) {
        PlaybackState ps = mc == null ? null : mc.getPlaybackState();
        return ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
    }

    /** True if anything is playing in any window: active audio, or a media session in PLAYING state. */
    static boolean anyPlaying(Context c) {
        try {
            android.media.AudioManager au = (android.media.AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (au != null && au.isMusicActive()) return true;
        } catch (Throwable ignored) {}
        return playing(find(c));
    }

    static String title(MediaController mc) {
        MediaMetadata md = mc == null ? null : mc.getMetadata();
        if (md == null) return "";
        CharSequence t = md.getText(MediaMetadata.METADATA_KEY_TITLE);
        if (t == null) t = md.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        return t == null ? "" : t.toString();
    }

    static String sub(MediaController mc) {
        MediaMetadata md = mc == null ? null : mc.getMetadata();
        if (md == null) return "";
        CharSequence a = md.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (a == null) a = md.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        if (a == null) a = md.getText(MediaMetadata.METADATA_KEY_ALBUM);
        return a == null ? "" : a.toString();
    }

    static void toggle(MediaController mc) {
        if (mc == null) return;
        if (playing(mc)) mc.getTransportControls().pause(); else mc.getTransportControls().play();
    }

    static void next(MediaController mc) { if (mc != null) mc.getTransportControls().skipToNext(); }

    static void prev(MediaController mc) { if (mc != null) mc.getTransportControls().skipToPrevious(); }
}
