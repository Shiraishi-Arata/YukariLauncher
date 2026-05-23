package net.kdt.pojavlaunch.contracts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * AndroidのOpenDocumentコントラクトは基本的な最低限の機能しか提供せず、ほとんど何も指定できません。
 * そこで、このクラスを作成しました。
 */
public class OpenDocumentWithExtension extends ActivityResultContract<Object, List<Uri>> {
    private final String mimeType;
    private final boolean allowMultiple;

    /**
     * 単一選択のコンストラクタ。
     * @param extension フィルターする拡張子
     */
    public OpenDocumentWithExtension(String extension) {
        this(extension, false);
    }

    /**
     * 拡張子でフィルターするドキュメント選択コントラクトを作成します。
     * デバイスのMIMEタイプデータベースで拡張子が利用できない場合、フィルターは「すべてのタイプ」にデフォルト設定されます。
     * @param extension フィルターする拡張子
     * @param allowMultiple 複数ファイル選択を許可するかどうか
     */
    public OpenDocumentWithExtension(String extension, boolean allowMultiple) {
        String extensionMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if(extensionMimeType == null) extensionMimeType = "*/*";
        mimeType = extensionMimeType;
        this.allowMultiple = allowMultiple;
    }

    /**
     * ドキュメント選択インテントを作成します。
     */
    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Object input) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);

        // コンストラクタのパラメータに基づいて複数選択を許可するかどうかを決定します
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }

        return intent;
    }

    /**
     * 同期的な結果はサポートしていません。
     */
    @Nullable
    @Override
    public final SynchronousResult<List<Uri>> getSynchronousResult(@NonNull Context context,
                                                                    @NonNull Object input) {
        return null;
    }

    /**
     * 結果を解析し、選択されたURIのリストを返します。
     */
    @Nullable
    @Override
    public final List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
        if (intent == null || resultCode != Activity.RESULT_OK) return null;

        List<Uri> uris = new ArrayList<>();
        if (intent.getClipData() != null) {
            // 複数の項目が選択されました
            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                uris.add(intent.getClipData().getItemAt(i).getUri());
            }
        } else {
            uris.add(intent.getData());
        }

        return uris;
    }
}
