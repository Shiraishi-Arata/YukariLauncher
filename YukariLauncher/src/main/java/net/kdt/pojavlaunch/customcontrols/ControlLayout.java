package net.kdt.pojavlaunch.customcontrols;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;
import static org.lwjgl.glfw.CallbackBridge.isGrabbing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;
import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.task.Task;
import com.arata.yukarilauncher.task.TaskExecutors;
import com.arata.yukarilauncher.ui.dialog.EditControlInfoDialog;
import com.arata.yukarilauncher.ui.dialog.SelectControlsDialog;
import com.arata.yukarilauncher.ui.dialog.TipDialog;
import com.arata.yukarilauncher.ui.subassembly.customcontrols.ControlInfoData;
import com.arata.yukarilauncher.utils.path.PathManager;
import com.arata.yukarilauncher.utils.stringutils.StringUtilsKt;

import net.kdt.pojavlaunch.MinecraftGLSurface;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlButton;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlJoystick;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlSubButton;
import net.kdt.pojavlaunch.customcontrols.handleview.ActionRow;
import net.kdt.pojavlaunch.customcontrols.handleview.ControlHandleView;
import net.kdt.pojavlaunch.customcontrols.handleview.EditControlPopup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlLayout extends FrameLayout {
	protected CustomControls mLayout;
	private ControlInfoData mInfoData;
	/* ゲーム内でControlInterface実装からアクセス可能。パフォーマンスのためにキャッシュされています。 */
	private MinecraftGLSurface mGameSurface = null;

	/* パフォーマンス向上のためのボタンキャッシュ */
	private List<ControlInterface> mButtons;
	private boolean mModifiable = false;
	private boolean mIsModified;
	private boolean mControlVisible = false;

	private EditControlPopup mControlPopup = null;
	private ControlHandleView mHandleView;
	private ControlButtonMenuListener mMenuListener;
	public ActionRow mActionRow = null;
	public String mLayoutFileName;
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
	public ControlLayout(Context ctx) {
		super(ctx);
	}
/**
 * コンストラクタ。
 * このクラスの新しいインスタンスを初期化します。
 */
	public ControlLayout(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
	}
/**
 * 「load Layout」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void loadLayout(String jsonPath) throws IOException, JsonSyntaxException {
		File jsonFile = jsonPath != null ? new File(jsonPath) : new File(AllSettings.getDefaultCtrl().getValue());

		CustomControls layout;
		if (jsonFile.exists()) {
			layout = LayoutConverter.loadAndConvertIfNecessary(getContext(), jsonFile.getAbsolutePath());
		} else {
			layout = LayoutConverter.loadFromAssets(getContext(), "yukari.json");
		}
		if (layout != null) {
			loadLayout(layout);
			if (jsonFile.exists()) {
				mLayoutFileName = StringUtilsKt.removeSuffix(jsonFile.getName(), ".json");
			} else {
				mLayoutFileName = "default";
			}
		}
	}
/**
 * 「load Layout」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void loadLayout(CustomControls controlLayout) {
		boolean sanitizedModified = false;
		if(controlLayout != null) {
			sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout);
		}
		mInfoData = controlLayout == null ? null : controlLayout.mControlInfoDataList;
		if (mInfoData == null) {
			mInfoData = new ControlInfoData();
		}

		if(mActionRow == null){
			mActionRow = new ActionRow(getContext());
			addView(mActionRow);
		}

		removeAllButtons();
		if(mLayout != null) {
			mLayout.mControlDataList = null;
			mLayout = null;
		}

		System.gc();
		mapTable.clear();

		// Cleanup buttons only when input layout is null
		if (controlLayout == null) return;

		mLayout = controlLayout;
		

		// Joystick(s) first, to workaround the touch dispatch
		for(ControlJoystickData joystick : mLayout.mJoystickDataList){
			addJoystickView(joystick);
		}

		//コントロールボタン
		for (ControlData button : controlLayout.mControlDataList) {
			addControlView(button);
		}

		//コントロールドロワー
		for(ControlDrawerData drawerData : controlLayout.mDrawerDataList){
			ControlDrawer drawer = addDrawerView(drawerData);
			if(mModifiable) drawer.areButtonsVisible = true;
		}

		mLayout.scaledAt = AllSettings.getButtonScale().getValue();

		setModified(sanitizedModified);
		mButtons = null;
		getButtonChildren(); // Force refresh
	} // loadLayout
/**
 * 「add Control Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

	//コントロールボタン
	public void addControlButton(ControlData controlButton) {
		mLayout.mControlDataList.add(controlButton);
		addControlView(controlButton);
	}
/**
 * 「add Control View」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	private void addControlView(ControlData controlButton) {
		final ControlButton view = new ControlButton(this, controlButton);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

		setModified(true);
	}
/**
 * 「add Drawer」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

	// CONTROL DRAWER
	public void addDrawer(ControlDrawerData drawerData){
		mLayout.mDrawerDataList.add(drawerData);
		addDrawerView();
	}
/**
 * 「add Drawer View」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	private void addDrawerView(){
		addDrawerView(null);
	}
/**
 * 「add Drawer View」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	private ControlDrawer addDrawerView(ControlDrawerData drawerData){

		final ControlDrawer view = new ControlDrawer(this,drawerData == null ? mLayout.mDrawerDataList.get(mLayout.mDrawerDataList.size()-1) : drawerData);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);
		//コントロールサブボタン
		for (ControlData subButton : view.getDrawerData().buttonProperties) {
			addSubView(view, subButton);
		}

		setModified(true);
		return view;
	}
/**
 * 「add Sub Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

	//コントロールサブボタン
	public void addSubButton(ControlDrawer drawer, ControlData controlButton){
		//ここにはあまりありません
		drawer.getDrawerData().buttonProperties.add(controlButton);
		addSubView(drawer, drawer.getDrawerData().buttonProperties.get(drawer.getDrawerData().buttonProperties.size()-1 ));
	}
/**
 * 「add Sub View」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	private void addSubView(ControlDrawer drawer, ControlData controlButton){
		final ControlSubButton view = new ControlSubButton(this, controlButton, drawer);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}else{
			view.setVisible(true);
		}

		addView(view);
		drawer.addButton(view);


		setModified(true);
	}
/**
 * 「add Joystick Button」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

	// JOYSTICK BUTTON
	public void addJoystickButton(ControlJoystickData data){
		mLayout.mJoystickDataList.add(data);
		addJoystickView(data);
	}
/**
 * 「add Joystick View」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	private void addJoystickView(ControlJoystickData data){
		ControlJoystick view = new ControlJoystick(this, data);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

	}
/**
 * 「remove All Buttons」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

	private void removeAllButtons() {
		for(ControlInterface button : getButtonChildren()){
			removeView(button.getControlView());
		}

		System.gc();
		//i wanna be sure that all the removed Views will be removed after a reload
		//because if frames will slowly go down after many control changes it will be warm and bad
	}
/**
 * 「save Layout」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void saveLayout(String path) throws Exception {
		mLayout.save(path);
		setModified(false);
	}
/**
 * 「toggle Control Visible」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void toggleControlVisible(){
		mControlVisible = !mControlVisible;
		setControlVisible(mControlVisible);
	}
/**
 * 「LayoutScale」の値を取得します。
 */
	public float getLayoutScale(){
		return mLayout.scaledAt;
	}
/**
 * 「Layout」の値を取得します。
 */
	public CustomControls getLayout(){
		return mLayout;
	}
/**
 * 「ControlVisible」の値を設定します。
 */
	public void setControlVisible(boolean isVisible) {
		if (mModifiable) return; // Not using on custom controls activity

		mControlVisible = isVisible;
		for(ControlInterface button : getButtonChildren()){
			button.setVisible(((button.getProperties().displayInGame && isGrabbing()) || (button.getProperties().displayInMenu && !isGrabbing())) && isVisible);
		}
	}
/**
 * 「Modifiable」の値を設定します。
 */
	public void setModifiable(boolean isModifiable) {
		if(!isModifiable && mModifiable){
			removeEditWindow();
		}
		mModifiable = isModifiable;
		if(isModifiable){
			// In edit mode, all controls have to be shown
			for(ControlInterface button : getButtonChildren()){
				button.setVisible(true);
			}
		}
	}
/**
 * 「Modifiable」の値を取得します。
 */
	public boolean getModifiable(){
		return mModifiable;
	}
/**
 * 「Modified」の値を設定します。
 */
	public void setModified(boolean isModified) {
		mIsModified = isModified;
	}
/**
 * 「ButtonChildren」の値を取得します。
 */
	public List<ControlInterface> getButtonChildren(){
		if(mModifiable || mButtons == null){
			mButtons = new ArrayList<>();
			for(int i=0; i<getChildCount(); ++i){
				View v = getChildAt(i);
				if(v instanceof ControlInterface)
					mButtons.add(((ControlInterface) v));
			}
		}

		return mButtons;
	}
/**
 * 「refresh Control Button Positions」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void refreshControlButtonPositions(){
		for(ControlInterface button : getButtonChildren()){
			button.setDynamicX(button.getProperties().dynamicX);
			button.setDynamicY(button.getProperties().dynamicY);
		}
	}
/**
 * 「on View Removed」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if(child instanceof ControlInterface && mControlPopup != null){
            mControlPopup.disappearColor();
            mControlPopup.disappear();
        }
    }

    /**
	 * 必要に応じてレイアウトを読み込み、値入力の処理を委譲します
	 * to the button at hand.
	 */
	public void editControlButton(ControlInterface button){
		if(mControlPopup == null){
			// When the panel is null, it needs to inflate first.
			// So inflate it, then process it on the next frame
			mControlPopup = new EditControlPopup(getContext(), this);
			post(() -> editControlButton(button));
			return;
		}

		mControlPopup.internalChanges = true;
		mControlPopup.setCurrentlyEditedButton(button);

		mControlPopup.appear(button.getControlView().getX() + button.getControlView().getWidth()/2f < currentDisplayMetrics.widthPixels/2f);
		button.loadEditValues(mControlPopup);

		mControlPopup.internalChanges = false;
		mControlPopup.disappearColor();

		if(mHandleView == null){
			mHandleView = new ControlHandleView(getContext());
			addView(mHandleView);
		}
		mHandleView.setControlButton(button);

		//mHandleView.show();
	}

    /**
     * ボタンの位置に応じてパネルの位置を入れ替えます。
     */
    public void adaptPanelPosition(){
		if(mControlPopup != null) mControlPopup.adaptPanelPosition();
	}


	final HashMap<View, ControlInterface> mapTable = new HashMap<>();
/**
 * 「on Touch」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */

	//onTouchから呼ばれますが、ControlButtonからのみ呼び出されるべきです。
	public void onTouch(View v, MotionEvent ev) {
		ControlInterface lastControlButton = mapTable.get(v);

		// Map location to screen coordinates
		ev.offsetLocation(v.getX(), v.getY());


		//アクションがキャンセルかどうかチェックし、ビューに関連するlastControlをリセット
		if (ev.getActionMasked() == MotionEvent.ACTION_UP
				|| ev.getActionMasked() == MotionEvent.ACTION_CANCEL
				|| ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			if (lastControlButton != null) lastControlButton.sendKeyPresses(false);
			mapTable.put(v, null);
			return;
		}

		if (ev.getActionMasked() != MotionEvent.ACTION_MOVE) return;


		//すべての子を再度見ないための最適化パス
		if (lastControlButton != null) {
			System.out.println("last control button check" + ev.getX() + "-" + ev.getY() + "-" + lastControlButton.getControlView().getX() + "-" + lastControlButton.getControlView().getY());
			if (ev.getX() > lastControlButton.getControlView().getX()
					&& ev.getX() < lastControlButton.getControlView().getX() + lastControlButton.getControlView().getWidth()
					&& ev.getY() > lastControlButton.getControlView().getY()
					&& ev.getY() < lastControlButton.getControlView().getY() + lastControlButton.getControlView().getHeight()) {
				return;
			}
		}

		//最後のキーを解放
		if (lastControlButton != null) lastControlButton.sendKeyPresses(false);
		mapTable.remove(v);

		// Update the state of all swipeable buttons
		for (ControlInterface button : getButtonChildren()) {
			if (!button.getProperties().isSwipeable) continue;

			if (ev.getX() > button.getControlView().getX()
					&& ev.getX() < button.getControlView().getX() + button.getControlView().getWidth()
					&& ev.getY() > button.getControlView().getY()
					&& ev.getY() < button.getControlView().getY() + button.getControlView().getHeight()) {

				//新しいキーを押下
				if (!button.equals(lastControlButton)) {
					button.sendKeyPresses(true);
					mapTable.put(v, button);
					return;
				}

			}
		}
	}
/**
 * タッチイベントを処理します。
 * ユーザーからのタッチ入力を検出し、適切なアクションを実行します。
 */

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mModifiable && event.getActionMasked() != MotionEvent.ACTION_UP || mControlPopup == null)
			return true;

		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

		// When the input window cannot be hidden, it returns false
		if(!imm.hideSoftInputFromWindow(getWindowToken(), 0)){
			if(mControlPopup.disappearLayer()){
				mActionRow.setFollowedButton(null);
				mHandleView.hide();
			}
		}
		return true;
	}
/**
 * 「remove Edit Window」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void removeEditWindow() {
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

		// When the input window cannot be hidden, it returns false
		imm.hideSoftInputFromWindow(getWindowToken(), 0);
		if(mControlPopup != null) {
			mControlPopup.disappearColor();
			mControlPopup.disappear();
		}

		if(mActionRow != null) mActionRow.setFollowedButton(null);
		if(mHandleView != null) mHandleView.hide();
	}
/**
 * 「save」メソッド。
 * このクラスに定義された機能メソッドです。
 */
	public void save(String path){
		try {
			mLayout.save(path);
		} catch (IOException e) {
			Logging.e("ControlLayout", "Failed to save the layout at:" + path);}
	}
/**
 * 「MenuButton」を持っているかを確認します。
 */
	public boolean hasMenuButton() {
		for(ControlInterface controlInterface : getButtonChildren()){
			for (int keycode : controlInterface.getProperties().keycodes) {
				if (keycode == ControlData.SPECIALBTN_MENU) return true;
			}
		}
		return false;
	}
/**
 * 「MenuListener」の値を設定します。
 */
	public void setMenuListener(ControlButtonMenuListener menuListener) {
		this.mMenuListener = menuListener;
	}
/**
 * 「notify App Menu」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void notifyAppMenu() {
		if(mMenuListener != null) mMenuListener.onClickedMenu();
	}

    /**
     * パフォーマンス向上のためのキャッシュされたゲッター。
     */
    public MinecraftGLSurface getGameSurface(){
		if(mGameSurface == null){
			mGameSurface = findViewById(R.id.main_game_render_view);
		}
		return mGameSurface;
	}
/**
 * 「ask To Exit」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void askToExit(EditorExitable editorExitable) {
		if(mIsModified) {
			openSaveAndExitDialog(editorExitable);
		}else{
			openExitDialog(editorExitable);
		}
	}
/**
 * 「save To Directory」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public String saveToDirectory(String name) throws Exception{
		String jsonPath = PathManager.DIR_CTRLMAP_PATH + "/" + name + ".json";
		saveLayout(jsonPath);
		return jsonPath;
	}
/**
 * 「save Dialog」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	private void saveDialog(String title, Task<?> confirmTask) {
		EditControlInfoDialog infoDialog = new EditControlInfoDialog(getContext(), true, mLayoutFileName, mInfoData);

		if (title != null && !title.isEmpty()) infoDialog.setTitle(title);

		infoDialog.setOnConfirmClickListener((fileName, controlInfoData) -> {
			try {
				String jsonPath = saveToDirectory(fileName);
				Toast.makeText(getContext(), getContext().getString(R.string.generic_save) + ": " + jsonPath, Toast.LENGTH_SHORT).show();
				if (confirmTask != null) confirmTask.execute();
			} catch (Throwable th) {
				Tools.showError(getContext(), th, true);
			}

			infoDialog.dismiss();
		});
		infoDialog.show();
	}
/**
 * 「open Save Dialog」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void openSaveDialog() {
		saveDialog(getContext().getString(R.string.generic_save), null);
	}
/**
 * 「open Save And Exit Dialog」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void openSaveAndExitDialog(EditorExitable editorExitable) {
		saveDialog(getContext().getString(R.string.global_save_and_exit),
				Task.runTask(TaskExecutors.getAndroidUI(), () -> {
					editorExitable.exitEditor();
					return null;
				}));
	}
/**
 * 「open Load Dialog」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void openLoadDialog() {
		SelectControlsDialog dialog = new SelectControlsDialog(getContext(), file -> {
			try {
				loadLayout(file.getAbsolutePath());
			} catch (IOException e) {
				Tools.showError(getContext(), e);
			}
		});
		dialog.show();
	}
/**
 * 「open Set Default Dialog」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void openSetDefaultDialog() {
		SelectControlsDialog dialog = new SelectControlsDialog(getContext(), file -> {
			String absolutePath = file.getAbsolutePath();
			try {
				AllSettings.getDefaultCtrl().put(absolutePath).save();
				loadLayout(absolutePath);
			} catch (IOException|JsonSyntaxException e) {
				Tools.showError(getContext(), e);
			}
		});
		dialog.setTitleText(R.string.customctrl_selectdefault);
		dialog.show();
	}
/**
 * 「open Exit Dialog」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public void openExitDialog(EditorExitable exitListener) {
		new TipDialog.Builder(getContext())
				.setTitle(R.string.customctrl_editor_exit_title)
				.setMessage(R.string.customctrl_editor_exit_msg)
				.setConfirmClickListener(checked -> exitListener.exitEditor())
				.showDialog();
	}
/**
 * 「are Control Visible」処理を実行します。
 * このメソッドは特定の機能を提供するために実装されています。
 */
	public boolean areControlVisible(){
		return mControlVisible;
	}
}
