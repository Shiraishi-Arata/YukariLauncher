package com.arata.yukarilauncher.utils.mouse;

import android.graphics.drawable.Drawable;

public final class CursorDrawableUtils {
    private CursorDrawableUtils() {
    }

    public static int[] getScaledHotspot(Drawable drawable, int scaledWidth, int scaledHeight) {
        if (!(drawable instanceof CursorHotspotAware)) {
            return new int[]{0, 0};
        }

        CursorHotspotAware hotspotAware = (CursorHotspotAware) drawable;
        int baseWidth = Math.max(1, hotspotAware.getBaseWidth());
        int baseHeight = Math.max(1, hotspotAware.getBaseHeight());

        int scaledHotspotX = Math.round((hotspotAware.getHotspotX() / (float) baseWidth) * scaledWidth);
        int scaledHotspotY = Math.round((hotspotAware.getHotspotY() / (float) baseHeight) * scaledHeight);
        return new int[]{scaledHotspotX, scaledHotspotY};
    }
}

