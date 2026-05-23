package net.kdt.pojavlaunch.scoped;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

/**
 * ファイルシステムアクセスにDocumentFile APIを使用するフォルダプロバイダ。
 */
public class FolderProvider {
    private DocumentFile mDocumentFile;
    private Uri mTreeUri;

    /**
     * @param documentFile プロバイダが使用するDocumentFile
     */
    public FolderProvider(DocumentFile documentFile) {
        this.mDocumentFile = documentFile;
        this.mTreeUri = documentFile.getUri();
    }

    /**
     * プロバイダが使用するDocumentFile。
     */
    public DocumentFile getDocumentFile() {
        return mDocumentFile;
    }

    /**
     * このフォルダを開くために使用されたツリーURI。
     */
    public Uri getTreeUri() {
        return mTreeUri;
    }
}
