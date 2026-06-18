package com.arata.yukarilauncher.utils.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import java.io.File
import kotlin.math.min


class ImageUtils {
    companion object {
        /**
         * BitmapFactoryを使用してファイルが画像かどうかをチェックする
         * ソースコード: https://github.com/lamba92/KImageCheck/blob/master/src/androidMain/kotlin/com/github/lamba92/utils/KImageCheck.kt#L12
         * @param file チェックするファイル
         * @return 画像の場合はtrue、そうでない場合はfalse
         */
        @JvmStatic
        fun isImage(file: File?): Boolean {
            file?.apply {
                if (isDirectory) return false
                runCatching {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(path, options)
                    return options.outWidth != -1 || options.outHeight != -1
                }
            }
            return false
        }

        /**
         * 画像のアスペクト比を維持しながら指定サイズに収まるようにリサイズする
         * @param imageWidth 元の画像の幅
         * @param imageHeight 元の画像の高さ
         * @param maxSize 制限する最大サイズ
         * @return リサイズ後の幅と高さを含むDimensionオブジェクト
         */
        @JvmStatic
        fun resizeWithRatio(imageWidth: Int, imageHeight: Int, maxSize: Int): Dimension {
            val widthRatio = maxSize.toDouble() / imageWidth
            val heightRatio = maxSize.toDouble() / imageHeight

            // 小さい方の倍率を選択して、最大サイズを超えないようにする
            val ratio = min(widthRatio, heightRatio)
            val newWidth = (imageWidth * ratio).toInt()
            val newHeight = (imageHeight * ratio).toInt()

            return Dimension(newWidth, newHeight)
        }

        /**
         * ImageViewからDrawableを取得し、Bitmapに変換する
         */
        @JvmStatic
        fun getBitmapFromImageView(imageView: ImageView): Bitmap? {
            val drawable = imageView.drawable ?: return null

            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawable.mutate().setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }
    }
}