package net.kdt.pojavlaunch.modloaders.modpacks.models;

/**
 * CurseForgeのマニフェストを表すPOJO
 */
public class CurseManifest {
    public String name;
    public String version;
    public String author;
    public String manifestType;
    public int manifestVersion;
    public CurseFile[] files;
    public CurseMinecraft minecraft;
    public String overrides;

    /**
     * CurseForgeのファイルを表す
     */
    public static class CurseFile {
        public long projectID;
        public long fileID;
        public boolean required;
    }

    /**
     * CurseForgeのMinecraft設定を表す
     */
    public static class CurseMinecraft {
        public String version;
        public CurseModLoader[] modLoaders;
    }

    /**
     * CurseForgeのModローダーを表す
     */
    public static class CurseModLoader {
        public String id;
        public boolean primary;
    }
}
