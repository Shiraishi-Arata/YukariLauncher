package com.arata.yukarilauncher.ui.subassembly.about;

/**
 * スポンサーデータを保持するメタクラス
 */
public class SponsorMeta {
    public Sponsor[] sponsors;

    /**
     * 個別のスポンサー情報を保持する内部クラス
     */
    public static class Sponsor {
        private final String name;
        private final String time;
        private final String identifier;
        private final String avatar;
        private final float amount;

        /**
         * スポンサー情報を構築する
         */
        public Sponsor(String name, String time, String identifier, String avatar, float amount) {
            this.name = name;
            this.time = time;
            this.identifier = identifier;
            this.avatar = avatar;
            this.amount = amount;
        }

        /** スポンサー名を取得する */
        public String getName() {
            return name;
        }

        /** 寄付日時を取得する */
        public String getTime() {
            return time;
        }

        /** 識別子を取得する */
        public String getIdentifier() {
            return identifier;
        }

        /** アバターURLを取得する */
        public String getAvatar() {
            return avatar;
        }

        /** 寄付金額を取得する */
        public float getAmount() {
            return amount;
        }
    }
}
