package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;

import net.kdt.pojavlaunch.JMinecraftVersionList.Arguments.ArgValue.ArgRules;

@Keep
public class DependentLibrary {
    public ArgRules[] rules;
    public String name;
    public LibraryDownloads downloads;
    public String url;

    @Keep
/**
 * LibraryDownloads内部クラス
 */
	public static class LibraryDownloads {
		public final MinecraftLibraryArtifact artifact;
/**
 * LibraryDownloadsを構築します
 */
		public LibraryDownloads(MinecraftLibraryArtifact artifact) {
			this.artifact = artifact;
		}
	}
}

