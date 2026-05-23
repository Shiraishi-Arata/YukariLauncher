package net.kdt.pojavlaunch.customcontrols;

import androidx.annotation.Keep;

import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlInfoData;

import net.kdt.pojavlaunch.Tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Keep
public class CustomControls {
	public int version = 7;
    public float scaledAt;
	public List<ControlData> mControlDataList;
	public List<ControlDrawerData> mDrawerDataList;
	public List<ControlJoystickData> mJoystickDataList;
	public ControlInfoData mControlInfoDataList;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
	public CustomControls() {
		this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ControlInfoData());
	}
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
	public CustomControls(List<ControlData> mControlDataList, List<ControlDrawerData> mDrawerDataList, List<ControlJoystickData> mJoystickDataList, ControlInfoData mControlInfoDataList) {
		this.mControlDataList = mControlDataList;
		this.mDrawerDataList = mDrawerDataList;
		this.mJoystickDataList = mJoystickDataList;
		this.mControlInfoDataList = mControlInfoDataList;
		this.scaledAt = 100f;
	}
/**
 * 「save」メソッド。
 * このクラスに定義された機能メソッドです。
 */
	public void save(String path) throws IOException {
		// 現在のバージョンはV3.2のため、バージョンは8とマークする必要があります！
		version = 8;
		Tools.write(path, Tools.GLOBAL_GSON.toJson(this));
	}
}
