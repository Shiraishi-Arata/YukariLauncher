package dalvik.annotation.optimization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * CriticalNativeアノテーションのダミー実装
 * このアノテーションが存在しないデバイスでは、この宣言がエラーを防ぐ
 * 存在するデバイスでは、システム側のアノテーションで上書きされ正常に動作する
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CriticalNative {
}
