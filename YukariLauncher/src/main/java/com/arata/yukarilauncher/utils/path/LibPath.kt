package com.arata.yukarilauncher.utils.path

import com.arata.yukarilauncher.utils.path.PathManager.Companion.DIR_DATA
import com.arata.yukarilauncher.utils.path.PathManager.Companion.DIR_GAME_HOME
import java.io.File

/**
 * 各種ライブラリファイルのパスを管理するクラス
 * コンポーネントや認証ライブラリなどのファイルパスを定数として保持する
 */
class LibPath {
    companion object {
        private val COMPONENTS_DIR = File(DIR_DATA, "components")
        private val OTHER_LOGIN_DIR = File(DIR_DATA, "other_login")

        /** Caciocavallo（Java 8向け）ディレクトリ */
        @JvmField val CACIO_8 = File(DIR_DATA, "caciocavallo")
        /** Caciocavallo（Java 17向け）ディレクトリ */
        @JvmField val CACIO_17 = File(DIR_DATA, "caciocavallo17")
        /** Caciocavallo Java 17用エージェントJAR */
        @JvmField val CACIO_17_AGENT = File(CACIO_17, "cacio-agent.jar")

        /** ForgeインストーラJAR */
        @JvmField val FORGE_INSTALLER = File(COMPONENTS_DIR, "forge_installer.jar")
        /** Mio FabricエージェントJAR */
        @JvmField val MIO_FABRIC_AGENT = File(COMPONENTS_DIR, "MioFabricAgent.jar")

        /** MioライブラリパッチJAR */
        @JvmField val MIO_LIB_PATCHER = File(COMPONENTS_DIR, "MioLibPatcher.jar")
        /** OptiFineリネームJAR */
        @JvmField val OPTIFINE_RENAMER = File(COMPONENTS_DIR, "OptiFineRenamer.jar")

        /** Authlib-Injector JAR */
        @JvmField val AUTHLIB_INJECTOR = File(OTHER_LOGIN_DIR, "authlib-injector.jar")
        /** Nide8認証JAR */
        @JvmField val NIDE_8_AUTH = File(OTHER_LOGIN_DIR, "nide8auth.jar")

        /** Javaサンドボックス用ポリシーファイル */
        @JvmField val JAVA_SANDBOX_POLICY = File(COMPONENTS_DIR, "java_sandbox.policy")
        /** Log4j RCEパッチ（1.7.x向け）XML */
        @JvmField val LOG4J_XML_1_7 = File(COMPONENTS_DIR, "log4j-rce-patch-1.7.xml")
        /** Log4j RCEパッチ（1.12.x向け）XML */
        @JvmField val LOG4J_XML_1_12 = File(COMPONENTS_DIR, "log4j-rce-patch-1.12.xml")
        /** Pro-GradeセキュリティJAR */
        @JvmField val PRO_GRADE = File(COMPONENTS_DIR, "pro-grade.jar")
    }
}
