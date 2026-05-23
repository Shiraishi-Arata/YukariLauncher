package com.arata.yukarilauncher.feature.login;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 認証サーバー一覧を保持するクラス。
 * GsonによるJSONデシリアライズに対応し、サーバーリストと情報を管理する。
 */
public class Servers {

    @SerializedName("server")
    private List<Server> server;
    @SerializedName("info")
    private String info;

    /**
     * サーバーリストを取得する。
     * @return サーバーリスト
     */
    public List<Server> getServer() {
        return server;
    }

    /**
     * サーバーリストを設定する。
     * @param server サーバーリスト
     */
    public void setServer(List<Server> server) {
        this.server = server;
    }

    /**
     * サーバー情報を取得する。
     * @return 情報文字列
     */
    public String getInfo() {
        return info;
    }

    /**
     * サーバー情報を設定する。
     * @param info 情報文字列
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * 個別の認証サーバー情報を保持する内部クラス。
     */
    public static class Server {
        @SerializedName("baseUrl")
        private String baseUrl;
        @SerializedName("serverName")
        private String serverName;
        @SerializedName("register")
        private String register;

        /**
         * ベースURLを取得する。
         * @return ベースURL
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * ベースURLを設定する。
         * @param baseUrl ベースURL
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * サーバー名を取得する。
         * @return サーバー名
         */
        public String getServerName() {
            return serverName;
        }

        /**
         * サーバー名を設定する。
         * @param serverName サーバー名
         */
        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        /**
         * 登録URLを取得する。
         * @return 登録URL
         */
        public String getRegister() {
            return register;
        }

        /**
         * 登録URLを設定する。
         * @param register 登録URL
         */
        public void setRegister(String register) {
            this.register = register;
        }
    }
}
