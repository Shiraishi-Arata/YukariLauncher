package com.arata.yukarilauncher.utils.mouse;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.arata.yukarilauncher.utils.image.ImageUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

public final class CursorPackUtils {
    private static final int XCURSOR_MAGIC = 0x72756358;
    private static final int XCURSOR_IMAGE_TYPE = 0xFFFD0002;
    private static final int MAX_SCAN_FILES = 4000;

    private CursorPackUtils() {}

    private static final int GLFW_ARROW_CURSOR = 0x36001;
    private static final int GLFW_IBEAM_CURSOR = 0x36002;
    private static final int GLFW_CROSSHAIR_CURSOR = 0x36003;
    private static final int GLFW_POINTING_HAND_CURSOR = 0x36004;
    private static final int GLFW_RESIZE_EW_CURSOR = 0x36005;
    private static final int GLFW_RESIZE_NS_CURSOR = 0x36006;
    private static final int GLFW_RESIZE_NWSE_CURSOR = 0x36007;
    private static final int GLFW_RESIZE_NESW_CURSOR = 0x36008;
    private static final int GLFW_RESIZE_ALL_CURSOR = 0x36009;
    private static final int GLFW_NOT_ALLOWED_CURSOR = 0x3600A;

    /**
     * ファイルがカーソル用のアーカイブ（zip, tar, tgz, tar.gz, txz, tar.xz）かどうかを判定する
     */
    public static boolean isCursorArchive(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".zip")
                || name.endsWith(".tar")
                || name.endsWith(".tgz")
                || name.endsWith(".tar.gz")
                || name.endsWith(".txz")
                || name.endsWith(".tar.xz");
    }

    /**
     * ファイルがX11カーソルファイル（XCursor形式）かどうかを判定する
     * マジックナンバーを確認して判定する
     */
    public static boolean isXCursorFile(File file) {
        if (file == null || !file.isFile()) return false;
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int magic = Integer.reverseBytes(dataInputStream.readInt());
            return magic == XCURSOR_MAGIC;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * ファイルがサポート対象のカーソルソースかどうかを判定する
     * ディレクトリの場合は内部のカーソル候補を検索する
     */
    public static boolean isSupportedCursorSource(File file) {
        if (file == null || !file.exists()) return false;
        if (file.isDirectory()) return findCursorCandidate(file) != null;
        return ImageUtils.isImage(file) || isXCursorFile(file);
    }

    /**
     * カーソルファイルからDrawableを読み込む（デフォルトは標準矢印カーソル）
     */
    @Nullable
    public static Drawable loadCursorDrawable(File source) {
        return loadCursorDrawable(source, GLFW_ARROW_CURSOR);
    }

    /**
     * カーソルファイルから指定されたカーソルタイプのDrawableを読み込む
     * ディレクトリの場合は適切なカーソルファイルを検索する
     */
    @Nullable
    public static Drawable loadCursorDrawable(File source, int cursorType) {
        if (source == null || !source.exists()) return null;
        if (source.isDirectory()) {
            source = findCursorCandidate(source, cursorType);
            if (source == null) return null;
        }

        if (ImageUtils.isImage(source)) {
            return Drawable.createFromPath(source.getAbsolutePath());
        }

        return decodeXCursorDrawable(source);
    }

    /**
     * カーソルパックの一覧表示用に、標準カーソル（default/left_ptr/arrow）のDrawableを読み込む
     */
    @Nullable
    public static Drawable loadDefaultCursorDrawable(File source) {
        if (source == null || !source.exists()) return null;
        if (source.isDirectory()) {
            source = findDefaultCursorCandidate(source);
            if (source == null) return null;
        }

        if (ImageUtils.isImage(source)) {
            return Drawable.createFromPath(source.getAbsolutePath());
        }

        return decodeXCursorDrawable(source);
    }

    /**
     * ディレクトリから一覧表示に使う標準カーソル候補を検索する
     */
    @Nullable
    public static File findDefaultCursorCandidate(File directory) {
        return findCursorCandidate(directory, new String[]{"default", "left_ptr", "arrow", "cursor", "pointer"});
    }

    /**
     * ディレクトリからカーソル候補ファイルを検索する（デフォルトは標準矢印カーソル）
     */
    @Nullable
    public static File findCursorCandidate(File directory) {
        return findCursorCandidate(directory, GLFW_ARROW_CURSOR);
    }

    /**
     * ディレクトリから指定されたカーソルタイプに適したカーソルファイルを検索する
     * 優先名のリストで検索し、見つからない場合は任意の画像またはXCursorファイルを返す
     */
    @Nullable
    public static File findCursorCandidate(File directory, int cursorType) {
        return findCursorCandidate(directory, getPreferredNames(cursorType));
    }

    /**
     * ディレクトリから指定された優先名に適したカーソルファイルを検索する
     */
    @Nullable
    private static File findCursorCandidate(File directory, String[] preferredNames) {
        if (directory == null || !directory.isDirectory()) return null;

        List<File> allFiles = collectFiles(directory);

        for (String preferred : preferredNames) {
            for (File file : allFiles) {
                String lower = file.getName().toLowerCase(Locale.ROOT);
                if (!lower.startsWith(preferred)) continue;
                if (ImageUtils.isImage(file) || isXCursorFile(file)) return file;
            }
        }

        for (File file : allFiles) {
            if (ImageUtils.isImage(file) || isXCursorFile(file)) return file;
        }
        return null;
    }

    /**
     * カーソルタイプに対応する優先検索名のリストを取得する
     */
    private static String[] getPreferredNames(int cursorType) {
        switch (cursorType) {
            case GLFW_IBEAM_CURSOR:
                return new String[]{"ibeam", "text", "xterm", "beam"};
            case GLFW_CROSSHAIR_CURSOR:
                return new String[]{"crosshair", "cross", "crossed"};
            case GLFW_POINTING_HAND_CURSOR:
                return new String[]{"pointing_hand", "hand2", "hand", "pointer"};
            case GLFW_RESIZE_EW_CURSOR:
                return new String[]{"alternate", "col-resize", "ew-resize", "split_h", "h_double_arrow", "size_hor", "sb_h_double_arrow", "resize_h"};
            case GLFW_RESIZE_NS_CURSOR:
                return new String[]{"alternate", "row-resize", "ns-resize", "split_v", "v_double_arrow", "size_ver", "sb_v_double_arrow", "resize_v"};
            case GLFW_RESIZE_NWSE_CURSOR:
                return new String[]{"nwse-resize", "size_fdiag", "bd_double_arrow"};
            case GLFW_RESIZE_NESW_CURSOR:
                return new String[]{"nesw-resize", "size_bdiag", "fd_double_arrow"};
            case GLFW_RESIZE_ALL_CURSOR:
                return new String[]{"move", "size_all", "fleur", "all-scroll"};
            case GLFW_NOT_ALLOWED_CURSOR:
                return new String[]{"not-allowed", "forbidden", "no-drop", "crossed_circle"};
            case GLFW_ARROW_CURSOR:
            default:
                return new String[]{"left_ptr", "arrow", "default", "cursor", "pointer"};
        }
    }

    /**
     * カーソルアーカイブを指定されたルートディレクトリに展開する
     * アーカイブの種類（zip/tar/tgz/tar.gz/txz/tar.xz）に応じて適切に処理する
     */
    public static File extractCursorArchive(File archive, File destinationRoot) throws IOException {
        String archiveName = archive.getName();
        String folderName = archiveName.replaceAll("(?i)\\.(zip|tar|tgz|tar\\.gz|txz|tar\\.xz)$", "");
        File outDir = new File(destinationRoot, folderName);
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Unable to create extraction directory");
        }

        String lowerName = archiveName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".zip")) {
            extractZip(archive, outDir);
        } else if (lowerName.endsWith(".txz") || lowerName.endsWith(".tar.xz")) {
            extractTar(archive, outDir, false, true);
        } else {
            extractTar(archive, outDir, lowerName.endsWith(".tgz") || lowerName.endsWith(".tar.gz"), false);
        }
        return outDir;
    }

    /**
     * ZIPアーカイブを展開する
     */
    private static void extractZip(File archive, File destinationRoot) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                File outFile = safeResolve(destinationRoot, entry.getName());
                ensureParentExists(outFile);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }
    }

    /**
     * TARアーカイブを展開する（gzip/xz圧縮にも対応）
     * シンボリックリンクは解決してコピーする
     */
    private static void extractTar(File archive, File destinationRoot, boolean gzip, boolean xz) throws IOException {
        List<String[]> symbolicLinks = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(archive);
             BufferedInputStream bis = new BufferedInputStream(fis);
             TarArchiveInputStream tis = gzip
                     ? new TarArchiveInputStream(new GzipCompressorInputStream(bis))
                     : xz
                        ? new TarArchiveInputStream(new XZCompressorInputStream(bis))
                        : new TarArchiveInputStream(bis)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                if (entry.isDirectory()) continue;

                if (entry.isSymbolicLink()) {
                    symbolicLinks.add(new String[]{entry.getName(), entry.getLinkName()});
                    continue;
                }

                File outFile = safeResolve(destinationRoot, entry.getName());
                ensureParentExists(outFile);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = tis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }

        // シンボリックリンクを解決するために抽出後にテーマエイリアス（left_ptr, move等）が
        // シンボリックリンクをサポートしていないファイルシステムでも動作するようにコピーする
        for (String[] link : symbolicLinks) {
            File aliasFile = safeResolve(destinationRoot, link[0]);
            File targetFile = safeResolve(destinationRoot, link[1]);
            if (!targetFile.exists() || !targetFile.isFile()) continue;
            ensureParentExists(aliasFile);
            Files.copy(targetFile.toPath(), aliasFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * ZipSlip攻撃を防ぐためにパスを安全に解決する
     */
    private static File safeResolve(File root, String childPath) throws IOException {
        File resolved = new File(root, childPath);
        String rootPath = root.getCanonicalPath() + File.separator;
        String resolvedPath = resolved.getCanonicalPath();
        if (!resolvedPath.startsWith(rootPath)) {
            throw new IOException("ZipSlip blocked for entry: " + childPath);
        }
        return resolved;
    }

    /**
     * ファイルの親ディレクトリが存在することを確認し、なければ作成する
     */
    private static void ensureParentExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent == null) return;
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory: " + parent.getAbsolutePath());
        }
    }

    /**
     * ディレクトリ内の全ファイルを再帰的に収集する（最大4000ファイル）
     * 絶対パス順でソートして返す
     */
    private static List<File> collectFiles(File root) {
        List<File> files = new ArrayList<>();
        ArrayDeque<File> queue = new ArrayDeque<>();
        queue.offer(root);
        while (!queue.isEmpty() && files.size() < MAX_SCAN_FILES) {
            File current = queue.poll();
            File[] children = current.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child.isDirectory()) {
                    queue.offer(child);
                } else {
                    files.add(child);
                }
            }
        }
        files.sort(Comparator.comparing(File::getAbsolutePath));
        return files;
    }

    /**
     * XCursorファイルをデコードしてDrawableを生成する
     * アニメーションフレームを含むカーソルはAnimationDrawableとして返す
     */
    @Nullable
    private static Drawable decodeXCursorDrawable(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length == 0) return null;
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            int magic = buffer.getInt();
            if (magic != XCURSOR_MAGIC) return null;
            int header = buffer.getInt();
            buffer.getInt(); // version
            int tocCount = buffer.getInt();
            if (tocCount <= 0 || header < 16) return null;

            Map<Integer, List<Integer>> chunkPositionsBySubtype = new HashMap<>();
            int tocOffset = 16;
            for (int i = 0; i < tocCount; i++) {
                int type = buffer.getInt(tocOffset);
                int subtype = buffer.getInt(tocOffset + 4);
                int position = buffer.getInt(tocOffset + 8);
                tocOffset += 12;
                if (type == XCURSOR_IMAGE_TYPE && position > 0) {
                    chunkPositionsBySubtype.computeIfAbsent(subtype, k -> new ArrayList<>()).add(position);
                }
            }
            if (chunkPositionsBySubtype.isEmpty()) return null;

            int selectedSubtype = selectBestSubtype(chunkPositionsBySubtype);
            List<XCursorFrame> frames = new ArrayList<>();
            List<Integer> selectedPositions = new ArrayList<>(chunkPositionsBySubtype.get(selectedSubtype));
            selectedPositions.sort(Integer::compareTo);
            for (int position : selectedPositions) {
                if (position <= 0 || position >= buffer.limit()) continue;
                XCursorFrame frame = decodeXCursorFrame(buffer, position);
                if (frame != null) frames.add(frame);
            }
            if (frames.isEmpty()) return null;

            if (frames.size() == 1) {
                XCursorFrame frame = frames.get(0);
                return new CursorBitmapDrawable(frame.bitmap, frame.xhot, frame.yhot);
            }

            CursorAnimationDrawable animationDrawable = new CursorAnimationDrawable(
                    frames.get(0).xhot,
                    frames.get(0).yhot,
                    frames.get(0).bitmap.getWidth(),
                    frames.get(0).bitmap.getHeight()
            );
            animationDrawable.setOneShot(false);
            for (XCursorFrame frame : frames) {
                animationDrawable.addFrame(new BitmapDrawable(frame.bitmap), Math.max(16, frame.delay));
            }
            return animationDrawable;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 最適なカーソルサイズ（サブタイプ）を選択する
     * 32ピクセルに最も近いサイズで、フレーム数が多いものを優先する
     */
    private static int selectBestSubtype(Map<Integer, List<Integer>> chunkPositionsBySubtype) {
        int preferredSize = 32;
        int selectedSubtype = -1;
        int selectedFrameCount = -1;
        int selectedDelta = Integer.MAX_VALUE;
        for (Map.Entry<Integer, List<Integer>> entry : chunkPositionsBySubtype.entrySet()) {
            int subtype = entry.getKey();
            int frameCount = entry.getValue().size();
            int delta = Math.abs(subtype - preferredSize);
            if (frameCount > selectedFrameCount
                    || (frameCount == selectedFrameCount && (delta < selectedDelta || (delta == selectedDelta && subtype > selectedSubtype)))) {
                selectedFrameCount = frameCount;
                selectedDelta = delta;
                selectedSubtype = subtype;
            }
        }
        return selectedSubtype;
    }

    /**
     * XCursorの単一フレームをデコードする
     * 幅・高さ・ホットスポット・遅延情報を読み取り、Bitmapを生成する
     */
    @Nullable
    private static XCursorFrame decodeXCursorFrame(ByteBuffer originalBuffer, int position) {
        ByteBuffer buffer = originalBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(position);

        int chunkHeader = buffer.getInt();
        int type = buffer.getInt();
        buffer.getInt(); // subtype
        buffer.getInt(); // version
        if (chunkHeader < 36 || type != XCURSOR_IMAGE_TYPE) return null;

        int width = buffer.getInt();
        int height = buffer.getInt();
        int xhot = buffer.getInt();
        int yhot = buffer.getInt();
        int delay = buffer.getInt();
        if (width <= 0 || height <= 0 || width > 1024 || height > 1024) return null;

        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            if (buffer.remaining() < 4) return null;
            pixels[i] = buffer.getInt();
        }
        return new XCursorFrame(
                Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888),
                delay <= 0 ? 100 : delay,
                Math.max(0, Math.min(xhot, width)),
                Math.max(0, Math.min(yhot, height))
        );
    }

    /**
     * XCursorの単一フレームデータを保持する内部クラス
     */
    private static final class XCursorFrame {
        private final Bitmap bitmap;
        private final int delay;
        private final int xhot;
        private final int yhot;

        private XCursorFrame(Bitmap bitmap, int delay, int xhot, int yhot) {
            this.bitmap = bitmap;
            this.delay = delay;
            this.xhot = xhot;
            this.yhot = yhot;
        }
    }

    /**
     * 静止画カーソル用のDrawable実装（ホットスポット対応）
     */
    private static final class CursorBitmapDrawable extends BitmapDrawable implements CursorHotspotAware {
        private final int hotspotX;
        private final int hotspotY;

        private CursorBitmapDrawable(Bitmap bitmap, int hotspotX, int hotspotY) {
            super(bitmap);
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
        }

        @Override
        public int getHotspotX() {
            return hotspotX;
        }

        @Override
        public int getHotspotY() {
            return hotspotY;
        }

        @Override
        public int getBaseWidth() {
            return getIntrinsicWidth();
        }

        @Override
        public int getBaseHeight() {
            return getIntrinsicHeight();
        }
    }

    /**
     * アニメーションカーソル用のDrawable実装（ホットスポット対応）
     */
    private static final class CursorAnimationDrawable extends AnimationDrawable implements CursorHotspotAware {
        private final int hotspotX;
        private final int hotspotY;
        private final int baseWidth;
        private final int baseHeight;

        private CursorAnimationDrawable(int hotspotX, int hotspotY, int baseWidth, int baseHeight) {
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.baseWidth = baseWidth;
            this.baseHeight = baseHeight;
        }

        @Override
        public int getHotspotX() {
            return hotspotX;
        }

        @Override
        public int getHotspotY() {
            return hotspotY;
        }

        @Override
        public int getBaseWidth() {
            return baseWidth;
        }

        @Override
        public int getBaseHeight() {
            return baseHeight;
        }

        @Override
        public int getIntrinsicWidth() {
            return baseWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return baseHeight;
        }
    }
}
