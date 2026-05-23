package net.kdt.pojavlaunch.multirt;

import java.util.Objects;

/**
 * Javaランタイムの情報を保持するクラス
 */
public class Runtime {
    public final String name;
    public final String versionString;
    public final String arch;
    public final int javaVersion;
    public boolean isProvidedByLauncher = false;

    /**
     * 破損または未知のランタイム用のコンストラクタ
     * @param name ランタイム名
     */
    public Runtime(String name) {
        this.name = name;
        this.versionString = null;
        this.arch = null;
        this.javaVersion = 0;
    }

    /**
     * 完全な情報を持つランタイム用のコンストラクタ
     * @param name ランタイム名
     * @param versionString バージョン文字列
     * @param arch アーキテクチャ
     * @param javaVersion Javaメジャーバージョン
     */
    Runtime(String name, String versionString, String arch, int javaVersion) {
        this.name = name;
        this.versionString = versionString;
        this.arch = arch;
        this.javaVersion = javaVersion;
    }
    
    /**
     * 名前が同じであれば同じランタイムと見なします。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Runtime runtime = (Runtime) o;
        return name.equals(runtime.name);
    }

    /**
     * 名前からハッシュコードを生成します。
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
