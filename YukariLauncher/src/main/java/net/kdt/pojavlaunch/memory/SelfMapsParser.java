package net.kdt.pojavlaunch.memory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * /proc/self/maps を解析してプロセスのメモリマップを読み取るパーサー。
 */
public class SelfMapsParser {
    private final Callback mCallback;

    /**
     * @param callback 各行を処理するコールバック
     */
    public SelfMapsParser(Callback callback) {
        mCallback = callback;
    }

    /**
     * マップファイルを解析し、各行をコールバックで処理します。
     */
    public void run() throws IOException, NumberFormatException {
        try (FileInputStream fileInputStream = new FileInputStream("/proc/self/maps")) {
            Scanner scanner = new Scanner(fileInputStream);
            while(scanner.hasNextLine()) {
                if(!forEachLine(scanner.nextLine())) break;
            }
        }
    }

    /**
     * 1行のマップエントリを解析します。
     */
    private boolean forEachLine(String line) throws NumberFormatException {
        int firstSpaceIndex = line.indexOf(' ');
        String addresses = line.substring(0, firstSpaceIndex);
        String[] addressArray = addresses.split("-");
        if(addressArray.length < 2) return true;
        long begin = Long.parseLong(addressArray[0], 16);
        long end = Long.parseLong(addressArray[1], 16);
        return mCallback.process(begin, end, line);
    }

    /**
     * 各行のマップエントリを処理するためのコールバックインターフェース。
     */
    public interface Callback {
        /**
         * @param startAddress 領域の開始アドレス
         * @param endAddress 領域の終了アドレス
         * @param wholeLine 元の行全体
         * @return 後続の行を処理し続ける場合はtrue
         */
        boolean process(long startAddress, long endAddress, String wholeLine);
    }
}
