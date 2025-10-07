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
         * 通过 BitmapFactory 检查一个文件是否为一个图片
         * @param file 文件
         * @return 返回是否为图片
         */
        //使用源代码：https://github.com/lamba92/KImageCheck/blob/master/src/androidMain/kotlin/com/github/lamba92/utils/KImageCheck.kt#L12
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
         * 通过计算图片的长款比例来计算缩放后的长款数据
         * @param imageWidth 原始图片的长
         * @param imageHeight 原始图片的宽
         * @param maxSize 需要限制在多大的空间
         * @return 返回一个缩放后的长宽数据对象
         */
        @JvmStatic
        fun resizeWithRatio(imageWidth: Int, imageHeight: Int, maxSize: Int): Dimension {
            val widthRatio = maxSize.toDouble() / imageWidth
            val heightRatio = maxSize.toDouble() / imageHeight

            //选择较小的缩放比例，确保长宽按比例缩小且不超过maxSize限制
            val ratio = min(widthRatio, heightRatio)
            val newWidth = (imageWidth * ratio).toInt()
            val newHeight = (imageHeight * ratio).toInt()

            return Dimension(newWidth, newHeight)
        }

        /**
         * 从一个 ImageView 中获取 Drawable，并将其转换为 Bitmap
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
