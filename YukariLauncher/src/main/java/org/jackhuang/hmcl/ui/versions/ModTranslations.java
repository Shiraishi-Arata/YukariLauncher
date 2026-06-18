/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.ui.versions;

import static org.jackhuang.hmcl.util.Pair.pair;

import com.arata.yukarilauncher.Tools;
import com.arata.yukarilauncher.feature.download.enums.Classify;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jackhuang.hmcl.util.Pair;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.io.IOUtils;
import org.jetbrains.annotations.Nullable;

/**
 * mod_data.txtのパーサー
 *
 * @see <a href="https://www.mcmod.cn">mcmod.cn</a>
 */
public enum ModTranslations {
    MOD("/assets/mod_data.txt") {
        /**
         * MODタイプのmcmod.cn URLを返します。
         */
        @Override
        public String getMcmodUrl(Mod mod) {
            return String.format("https://www.mcmod.cn/class/%s.html", mod.getMcmod());
        }
    },
    MODPACK("/assets/modpack_data.txt") {
        /**
         * MODPACKタイプのmcmod.cn URLを返します。
         */
        @Override
        public String getMcmodUrl(Mod mod) {
            return String.format("https://www.mcmod.cn/modpack/%s.html", mod.getMcmod());
        }
    },
    EMPTY("") {
        /**
         * 空のURLを返します。
         */
        @Override
        public String getMcmodUrl(Mod mod) {
            return "";
        }
    };

    /**
     * リポジトリタイプに対応するModTranslationsを返します。
     * @param type リポジトリタイプ
     * @return 対応するModTranslations
     */
    public static ModTranslations getTranslationsByRepositoryType(Classify type) {
        switch (type) {
            case MOD:
                return MOD;
            case MODPACK:
                return MODPACK;
            default:
                return EMPTY;
        }
    }

    private final String resourceName;
    private List<Mod> mods;
    private Map<String, Mod> modIdMap;
    private Map<String, Mod> curseForgeMap;
    private List<Pair<String, Mod>> keywords;
    private int maxKeywordLength = -1;

    /**
     * リソース名を指定してModTranslationsを構築します。
     * @param resourceName リソースファイル名
     */
    ModTranslations(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * CurseForge IDからModを取得します。
     * @param id CurseForge ID
     * @return 見つかったMod、またはnull
     */
    @Nullable
    public Mod getModByCurseForgeId(String id) {
        if (StringUtilsKt.isBlank(id) || !loadCurseForgeMap()) return null;

        return curseForgeMap.get(id);
    }

    /**
     * Mod IDからModを取得します。
     * @param id Mod ID
     * @return 見つかったMod、またはnull
     */
    @Nullable
    public Mod getModById(String id) {
        if (StringUtilsKt.isBlank(id) || !loadModIdMap()) return null;

        return modIdMap.get(id);
    }

    /**
     * Modのmcmod.cn URLを返します（抽象メソッド）。
     */
    public abstract String getMcmodUrl(Mod mod);

    /**
     * クエリ文字列でModを検索します。
     * 最長共通部分列（LCS）アルゴリズムを使用して類似度を計算します。
     * @param query 検索クエリ
     * @return 見つかったModのリスト（関連度順）
     */
    public List<Mod> searchMod(String query) {
        if (!loadKeywords()) return Collections.emptyList();

        StringBuilder newQuery = ((CharSequence) query).chars()
                .filter(ch -> !Character.isSpaceChar(ch))
                .collect(StringBuilder::new, (sb, value) -> sb.append((char) value), StringBuilder::append);
        query = newQuery.toString();

        StringUtils.LongestCommonSubsequence lcs = new StringUtils.LongestCommonSubsequence(query.length(), maxKeywordLength);
        List<Pair<Integer, Mod>> modList = new ArrayList<>();
        for (Pair<String, Mod> keyword : keywords) {
            int value = lcs.calc(query, keyword.getKey());
            if (value >= Math.max(1, query.length() - 3)) {
                modList.add(pair(value, keyword.getValue()));
            }
        }
        return modList.stream()
                .sorted((a, b) -> -a.getKey().compareTo(b.getKey()))
                .map(Pair::getValue)
                .collect(Collectors.toList());
    }

    /**
     * リソースファイルからModデータを読み込みます。
     * @return 読み込みに成功した場合はtrue
     */
    private boolean loadFromResource() {
        if (mods != null) return true;
        if (StringUtilsKt.isBlank(resourceName)) {
            mods = Collections.emptyList();
            return true;
        }

        try {
            String modData = IOUtils.readFullyAsString(ModTranslations.class.getResourceAsStream(resourceName));
            mods = Arrays.stream(modData.split("\n")).filter(line -> !line.startsWith("#")).map(Mod::new).collect(Collectors.toList());
            return true;
        } catch (Exception e) {
            Logging.w("Failed to load " + resourceName, Tools.printToString(e));
            return false;
        }
    }

    /**
     * CurseForge IDからModへのマップを読み込みます。
     */
    private boolean loadCurseForgeMap() {
        if (curseForgeMap != null) {
            return true;
        }

        if (mods == null) {
            if (!loadFromResource()) return false;
        }

        curseForgeMap = new HashMap<>();
        for (Mod mod : mods) {
            if (StringUtilsKt.isNotBlank(mod.getCurseforge())) {
                curseForgeMap.put(mod.getCurseforge(), mod);
            }
        }
        return true;
    }

    /**
     * Mod IDからModへのマップを読み込みます。
     */
    private boolean loadModIdMap() {
        if (modIdMap != null) {
            return true;
        }

        if (mods == null) {
            if (!loadFromResource()) return false;
        }

        modIdMap = new HashMap<>();
        for (Mod mod : mods) {
            for (String id : mod.getModIds()) {
                if (StringUtilsKt.isNotBlank(id) && !"examplemod".equals(id)) {
                    modIdMap.put(id, mod);
                }
            }
        }
        return true;
    }

    /**
     * 検索用のキーワードリストを読み込みます。
     */
    private boolean loadKeywords() {
        if (keywords != null) {
            return true;
        }

        if (mods == null) {
            if (!loadFromResource()) return false;
        }

        keywords = new ArrayList<>();
        maxKeywordLength = -1;
        for (Mod mod : mods) {
            if (StringUtilsKt.isNotBlank(mod.getName())) {
                keywords.add(pair(mod.getName(), mod));
                maxKeywordLength = Math.max(maxKeywordLength, mod.getName().length());
            }
            if (StringUtilsKt.isNotBlank(mod.getSubname())) {
                keywords.add(pair(mod.getSubname(), mod));
                maxKeywordLength = Math.max(maxKeywordLength, mod.getSubname().length());
            }
            if (StringUtilsKt.isNotBlank(mod.getAbbr())) {
                keywords.add(pair(mod.getAbbr(), mod));
                maxKeywordLength = Math.max(maxKeywordLength, mod.getAbbr().length());
            }
        }
        return true;
    }

    /**
     * Modのデータモデル
     */
    public static final class Mod {
        private final String curseforge;
        private final String mcmod;
        private final List<String> modIds;
        private final String name;
        private final String subname;
        private final String abbr;

        /**
         * セミコロン区切りの行からModを構築します。
         * @param line データ行（6つのフィールドが必要）
         */
        public Mod(String line) {
            String[] items = line.split(";", -1);
            if (items.length != 6) {
                throw new IllegalArgumentException("Illegal mod data line, 6 items expected " + line);
            }

            curseforge = items[0];
            mcmod = items[1];
            modIds = Collections.unmodifiableList(Arrays.asList(items[2].split(",")));
            name = items[3];
            subname = items[4];
            abbr = items[5];
        }

        /**
         * すべてのフィールドを指定してModを構築します。
         */
        public Mod(String curseforge, String mcmod, List<String> modIds, String name, String subname, String abbr) {
            this.curseforge = curseforge;
            this.mcmod = mcmod;
            this.modIds = modIds;
            this.name = name;
            this.subname = subname;
            this.abbr = abbr;
        }

        /**
         * 表示名を返します（省略名、名前、サブ名を結合）。
         */
        public String getDisplayName() {
            StringBuilder builder = new StringBuilder();
            if (StringUtilsKt.isNotBlank(abbr)) {
                builder.append("[").append(abbr.trim()).append("] ");
            }
            builder.append(name);
            if (StringUtilsKt.isNotBlank(subname)) {
                builder.append(" (").append(subname).append(")");
            }
            return builder.toString();
        }

        /**
         * CurseForge IDを返します。
         */
        public String getCurseforge() {
            return curseforge;
        }

        /**
         * mcmod.cn IDを返します。
         */
        public String getMcmod() {
            return mcmod;
        }

        /**
         * Mod IDのリストを返します。
         */
        public List<String> getModIds() {
            return modIds;
        }

        /**
         * Mod名を返します。
         */
        public String getName() {
            return name;
        }

        /**
         * サブ名を返します。
         */
        public String getSubname() {
            return subname;
        }

        /**
         * 省略名を返します。
         */
        public String getAbbr() {
            return abbr;
        }
    }
}
