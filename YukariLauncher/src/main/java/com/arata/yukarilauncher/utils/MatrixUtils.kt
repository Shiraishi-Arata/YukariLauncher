package com.arata.yukarilauncher.utils

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF

/** Matrixを使用した矩形変換ユーティリティを提供するオブジェクト。 */
object MatrixUtils {

    /** RectをMatrixで変換する（in-place）。 @param inOutRect 入出力矩形 @param transformMatrix 変換行列 */
    fun transformRect(inOutRect: Rect, transformMatrix: Matrix) {
        transformRect(inOutRect, inOutRect, transformMatrix)
    }

    /** RectFをMatrixで変換する（in-place）。 @param inOutRect 入出力矩形 @param transformMatrix 変換行列 */
    fun transformRect(inOutRect: RectF, transformMatrix: Matrix) {
        transformRect(inOutRect, inOutRect, transformMatrix)
    }

    /** RectFをMatrixで変換し、結果をRectに出力する。 @param inRect 入力矩形 @param outRect 出力矩形 @param transformMatrix 変換行列 */
    fun transformRect(inRect: RectF, outRect: Rect, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix) ?: return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /** RectをMatrixで変換し、結果をRectFに出力する。 @param inRect 入力矩形 @param outRect 出力矩形 @param transformMatrix 変換行列 */
    fun transformRect(inRect: Rect, outRect: RectF, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix) ?: return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /** RectをMatrixで変換し、結果をRectに出力する。 @param inRect 入力矩形 @param outRect 出力矩形 @param transformMatrix 変換行列 */
    fun transformRect(inRect: Rect, outRect: Rect, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix) ?: return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /** RectFをMatrixで変換し、結果をRectFに出力する。 @param inRect 入力矩形 @param outRect 出力矩形 @param transformMatrix 変換行列 */
    fun transformRect(inRect: RectF, outRect: RectF, transformMatrix: Matrix) {
        val inOutDecodeRect = createInOutDecodeRect(transformMatrix) ?: return
        writeInputRect(inOutDecodeRect, inRect)
        transformPoints(inOutDecodeRect, transformMatrix)
        readOutputRect(inOutDecodeRect, outRect)
    }

    /** RectFの座標をfloat配列に書き込む。 @param inOutDecodeRect 座標配列 @param inRect 入力矩形 */
    private fun writeInputRect(inOutDecodeRect: FloatArray, inRect: RectF) {
        inOutDecodeRect[0] = inRect.left
        inOutDecodeRect[1] = inRect.top
        inOutDecodeRect[2] = inRect.right
        inOutDecodeRect[3] = inRect.bottom
    }

    /** Rectの座標をfloat配列に書き込む。 @param inOutDecodeRect 座標配列 @param inRect 入力矩形 */
    private fun writeInputRect(inOutDecodeRect: FloatArray, inRect: Rect) {
        inOutDecodeRect[0] = inRect.left.toFloat()
        inOutDecodeRect[1] = inRect.top.toFloat()
        inOutDecodeRect[2] = inRect.right.toFloat()
        inOutDecodeRect[3] = inRect.bottom.toFloat()
    }

    /** float配列からRectFに座標を読み込む。 @param inOutDecodeRect 座標配列 @param outRect 出力矩形 */
    private fun readOutputRect(inOutDecodeRect: FloatArray, outRect: RectF) {
        outRect.left = inOutDecodeRect[4]
        outRect.top = inOutDecodeRect[5]
        outRect.right = inOutDecodeRect[6]
        outRect.bottom = inOutDecodeRect[7]
    }

    /** float配列からRectに座標を読み込む。 @param inOutDecodeRect 座標配列 @param outRect 出力矩形 */
    private fun readOutputRect(inOutDecodeRect: FloatArray, outRect: Rect) {
        outRect.left = inOutDecodeRect[4].toInt()
        outRect.top = inOutDecodeRect[5].toInt()
        outRect.right = inOutDecodeRect[6].toInt()
        outRect.bottom = inOutDecodeRect[7].toInt()
    }

    /** 変換用のfloat配列を生成する（恒等行列の場合はnull）。 @param transformMatrix 変換行列 @return 座標配列、恒等行列の場合はnull */
    private fun createInOutDecodeRect(transformMatrix: Matrix): FloatArray? {
        return if (transformMatrix.isIdentity) null else FloatArray(8)
    }

    /** Matrixを使ってfloat配列内の座標を変換する。 @param inOutDecodeRect 入出力座標配列 @param transformMatrix 変換行列 */
    private fun transformPoints(inOutDecodeRect: FloatArray, transformMatrix: Matrix) {
        transformMatrix.mapPoints(inOutDecodeRect, 4, inOutDecodeRect, 0, 2)
    }

    /**
     * 行列の逆行列を計算する。標準のinvertが失敗した場合は手動で計算する。
     * @param source 元の行列
     * @param destination 逆行列の出力先
     */
    fun inverse(source: Matrix, destination: Matrix) {
        if (source.invert(destination)) return
        val matrix = FloatArray(9)
        source.getValues(matrix)
        inverseMatrix(matrix)
        destination.setValues(matrix)
    }

    /**
     * 3x3行列の逆行列を手動計算する。
     * @param matrix 入出力行列（9要素のフラット配列）
     */
    private fun inverseMatrix(matrix: FloatArray) {
        val determinant = matrix[0] * (matrix[4] * matrix[8] - matrix[5] * matrix[7])
                - matrix[1] * (matrix[3] * matrix[8] - matrix[5] * matrix[6])
                + matrix[2] * (matrix[3] * matrix[7] - matrix[4] * matrix[6])

        if (determinant == 0f) {
            throw IllegalArgumentException("Matrix is not invertible")
        }

        val invDet = 1 / determinant

        val temp0 = matrix[4] * matrix[8] - matrix[5] * matrix[7]
        val temp1 = matrix[2] * matrix[7] - matrix[1] * matrix[8]
        val temp2 = matrix[1] * matrix[5] - matrix[2] * matrix[4]
        val temp3 = matrix[5] * matrix[6] - matrix[3] * matrix[8]
        val temp4 = matrix[0] * matrix[8] - matrix[2] * matrix[6]
        val temp5 = matrix[2] * matrix[3] - matrix[0] * matrix[5]
        val temp6 = matrix[3] * matrix[7] - matrix[4] * matrix[6]
        val temp7 = matrix[1] * matrix[6] - matrix[0] * matrix[7]
        val temp8 = matrix[0] * matrix[4] - matrix[1] * matrix[3]
        matrix[0] = temp0 * invDet
        matrix[1] = temp1 * invDet
        matrix[2] = temp2 * invDet
        matrix[3] = temp3 * invDet
        matrix[4] = temp4 * invDet
        matrix[5] = temp5 * invDet
        matrix[6] = temp6 * invDet
        matrix[7] = temp7 * invDet
        matrix[8] = temp8 * invDet
    }
}