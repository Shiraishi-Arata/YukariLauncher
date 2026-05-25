package com.arata.yukarilauncher.utils.platform

import java.io.FileInputStream
import java.util.Scanner

/** /proc/self/maps をパースしてコールバックで各行を処理するクラス。 */
class SelfMapsParser(private val mCallback: Callback) {

    /** マップエントリを処理するためのコールバックインターフェース。 */
    interface Callback {
        /**
         * パースしたアドレス範囲を処理する。
         * @param startAddress 開始アドレス
         * @param endAddress 終了アドレス
         * @param wholeLine 元の行全体
         * @return 処理を続行する場合は true
         */
        fun process(startAddress: Long, endAddress: Long, wholeLine: String): Boolean
    }

    /**
     * /proc/self/maps を読み込み、各行をパースしてコールバックに渡す。
     * @throws NumberFormatException アドレスのパースに失敗した場合
     */
    @Throws(NumberFormatException::class)
    fun run() {
        FileInputStream("/proc/self/maps").use { fileInputStream ->
            val scanner = Scanner(fileInputStream)
            while (scanner.hasNextLine()) {
                if (!forEachLine(scanner.nextLine())) break
            }
        }
    }

    /**
     * 一行をパースしてアドレス範囲を抽出する。
     * @param line マップの一行
     * @return 処理を続行する場合は true
     * @throws NumberFormatException アドレス値のパースに失敗した場合
     */
    @Throws(NumberFormatException::class)
    private fun forEachLine(line: String): Boolean {
        val firstSpaceIndex = line.indexOf(' ')
        val addresses = line.substring(0, firstSpaceIndex)
        val addressArray = addresses.split("-")
        if (addressArray.size < 2) return true
        val begin = java.lang.Long.parseLong(addressArray[0], 16)
        val end = java.lang.Long.parseLong(addressArray[1], 16)
        return mCallback.process(begin, end, line)
    }
}
