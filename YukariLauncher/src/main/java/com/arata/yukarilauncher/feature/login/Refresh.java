package com.arata.yukarilauncher.feature.login;

import com.google.gson.annotations.SerializedName;

//https://github.com/Vera-Firefly/Pojav-Glow-Worm/commit/933dcd1d275616d21fb2bccacbfbfc174b785333
public class Refresh {
    @SerializedName("selectedProfile")
    private SelectedProfile selectedProfile;
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("clientToken")
    private String clientToken;

/**
 * accessTokenを取得する
 * @return accessTokenの値
 */
    public String getAccessToken() {
        return accessToken;
    }

/**
 * clientTokenを取得する
 * @return clientTokenの値
 */
    public String getClientToken() {
        return clientToken;
    }

/**
 * accessTokenを設定する
 * @param accessToken 設定値
 */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

/**
 * selectedProfileを取得する
 * @return selectedProfileの値
 */
    public SelectedProfile getSelectedProfile() {
        return selectedProfile;
    }

/**
 * clientTokenを設定する
 * @param clientToken 設定値
 */
    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

/**
 * selectedProfileを設定する
 * @param selectedProfile 設定値
 */
    public void setSelectedProfile(SelectedProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

/**
 * SelectedProfile内部クラス
 */
    public static class SelectedProfile {
        @SerializedName("name")
        private String name;
        @SerializedName("id")
        private String id;

/**
 * nameを取得する
 * @return nameの値
 */
        public String getName() {
            return name;
        }

/**
 * nameを設定する
 * @param name 設定値
 */
        public void setName(String name) {
            this.name = name;
        }

/**
 * idを取得する
 * @return idの値
 */
        public String getId() {
            return id;
        }

/**
 * idを設定する
 * @param id 設定値
 */
        public void setId(String id) {
            this.id = id;
        }
    }
}