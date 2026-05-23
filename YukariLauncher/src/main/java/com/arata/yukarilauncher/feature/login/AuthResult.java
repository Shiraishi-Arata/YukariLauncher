package com.arata.yukarilauncher.feature.login;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AuthResult {
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("clientToken")
    private String clientToken;
    @SerializedName("availableProfiles")
    private List<AvailableProfiles> availableProfiles;
    @SerializedName("user")
    private User user;
    @SerializedName("selectedProfile")
    private SelectedProfile selectedProfile;

/**
 * accessTokenを取得する
 * @return accessTokenの値
 */
    public String getAccessToken() {
        return accessToken;
    }

/**
 * accessTokenを設定する
 * @param accessToken 設定値
 */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

/**
 * clientTokenを取得する
 * @return clientTokenの値
 */
    public String getClientToken() {
        return clientToken;
    }

/**
 * clientTokenを設定する
 * @param clientToken 設定値
 */
    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }

/**
 * availableProfilesを取得する
 * @return availableProfilesの値
 */
    public List<AvailableProfiles> getAvailableProfiles() {
        return availableProfiles;
    }

/**
 * availableProfilesを設定する
 * @param availableProfiles 設定値
 */
    public void setAvailableProfiles(List<AvailableProfiles> availableProfiles) {
        this.availableProfiles = availableProfiles;
    }

/**
 * userを取得する
 * @return userの値
 */
    public User getUser() {
        return user;
    }

/**
 * userを設定する
 * @param user 設定値
 */
    public void setUser(User user) {
        this.user = user;
    }

/**
 * selectedProfileを取得する
 * @return selectedProfileの値
 */
    public SelectedProfile getSelectedProfile() {
        return selectedProfile;
    }

/**
 * selectedProfileを設定する
 * @param selectedProfile 設定値
 */
    public void setSelectedProfile(SelectedProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }

/**
 * User内部クラス
 */
    public static class User {
        @SerializedName("id")
        private String id;
        @SerializedName("properties")
        private List<?> properties;

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

/**
 * propertiesを取得する
 * @return propertiesの値
 */
        public List<?> getProperties() {
            return properties;
        }

/**
 * propertiesを設定する
 * @param properties 設定値
 */
        public void setProperties(List<?> properties) {
            this.properties = properties;
        }
    }

/**
 * SelectedProfile内部クラス
 */
    public static class SelectedProfile {
        @SerializedName("id")
        private String id;
        @SerializedName("name")
        private String name;

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
    }

/**
 * AvailableProfiles内部クラス
 */
    public static class AvailableProfiles {
        @SerializedName("id")
        private String id;
        @SerializedName("name")
        private String name;

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
    }
}