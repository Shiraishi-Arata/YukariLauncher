package com.arata.yukarilauncher.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentControllerRemapperBinding
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad.Gamepad
import com.arata.yukarilauncher.ui.subassembly.customcontrols.gamepad.GamepadMapperAdapter
import fr.spse.gamepad_remapper.RemapperManager
import fr.spse.gamepad_remapper.RemapperView

/** ゲームパッドのボタン入力をキーボード入力にリマップするフラグメント。 */
class GamepadMapperFragment : FragmentWithAnim(R.layout.fragment_controller_remapper),
    View.OnKeyListener, View.OnGenericMotionListener, AdapterView.OnItemSelectedListener {

    companion object {
        /** フラグメントのタグ。 */
        const val TAG = "GamepadMapperFragment"
    }

    /** ビューバインディングインスタンス。 */
    private var binding: FragmentControllerRemapperBinding? = null
    /** リマッパービューのビルダー。全ボタンとスティックのリマップを有効化する。 */
    private val mRemapperViewBuilder = RemapperView.Builder(null)
        .remapA(true)
        .remapB(true)
        .remapX(true)
        .remapY(true)
        .remapLeftJoystick(true)
        .remapRightJoystick(true)
        .remapStart(true)
        .remapSelect(true)
        .remapLeftShoulder(true)
        .remapRightShoulder(true)
        .remapLeftTrigger(true)
        .remapRightTrigger(true)
        .remapDpad(true)
    /** 終了ボタン長押し検出用のハンドラ。 */
    private val mExitHandler = Handler(Looper.getMainLooper())
    /** 終了ボタン長押し時に実行されるRunnable。 */
    private val mExitRunnable = Runnable {
        val activity = activity
        activity?.onBackPressed()
    }
    /** リマッパーの入力管理インスタンス。 */
    private var mInputManager: RemapperManager? = null
    /** ゲームパッドマッパーのアダプター。 */
    private var mMapperAdapter: GamepadMapperAdapter? = null
    /** 接続中のゲームパッド。 */
    private var mGamepad: Gamepad? = null

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        binding = FragmentControllerRemapperBinding.inflate(layoutInflater)
        return binding?.root
    }

    /** ビュー作成後の初期化。ボタン、リサイクラービュー、スピナーの設定を行う。 */
    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        binding?.backButton?.setOnClickListener { YLTools.onBackPressed(requireActivity()) }

        mMapperAdapter = GamepadMapperAdapter(view.context)
        binding?.gamepadRemapperRecycler?.apply {
            layoutManager = LinearLayoutManager(view.context)
            adapter = mMapperAdapter
            setOnKeyListener(this@GamepadMapperFragment)
            setOnGenericMotionListener(this@GamepadMapperFragment)
            requestFocus()
        }

        mInputManager = RemapperManager(view.context, mRemapperViewBuilder)

        val mGrabStateAdapter = ArrayAdapter<String>(view.context, R.layout.support_simple_spinner_dropdown_item)
        mGrabStateAdapter.addAll(getString(R.string.controls_in_menu), getString(R.string.controls_in_game))

        binding?.gamepadRemapperModeSpinner?.apply {
            adapter = mGrabStateAdapter
            setSelection(0)
            onItemSelectedListener = this@GamepadMapperFragment
        }
    }

    /** 指定された入力デバイスからGamepadインスタンスを生成する。 */
    private fun createGamepad(mainView: View, inputDevice: InputDevice) {
        mGamepad = object : Gamepad(mainView, inputDevice, mMapperAdapter!!, false) {
            override fun handleGamepadInput(keycode: Int, value: Float) {
                if (keycode == KeyEvent.KEYCODE_BUTTON_SELECT) {
                    handleExitButton(value > 0.5f)
                }
                super.handleGamepadInput(keycode, value)
            }
        }
    }

    /** セレクトボタンの長押しによる終了処理を管理する。 */
    private fun handleExitButton(isPressed: Boolean) {
        if (isPressed) mExitHandler.postDelayed(mExitRunnable, 400)
        else mExitHandler.removeCallbacks(mExitRunnable)
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        val mainView = view
        if (event == null || !Gamepad.isGamepadEvent(event) || mainView == null) return false
        if (mGamepad == null) createGamepad(mainView, event.device!!)
        mInputManager?.handleKeyEventInput(mainView.context, event, mGamepad)
        return true
    }

    override fun onGenericMotion(v: View?, motionEvent: MotionEvent?): Boolean {
        val mainView = view
        if (motionEvent == null || !Gamepad.isGamepadEvent(motionEvent) || mainView == null) return false
        if (mGamepad == null) createGamepad(mainView, motionEvent.device!!)
        mInputManager?.handleMotionEventInput(mainView.context, motionEvent, mGamepad)
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val grab = position == 1
        mMapperAdapter?.setGrabState(grab)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    /** スライドインアニメーションを適用する。 */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding!!.controllerLayout, Animations.BounceInDown))
            .apply(AnimPlayer.Entry(binding!!.operateLayout, Animations.BounceInLeft))
    }

    /** スライドアウトアニメーションを適用する。 */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding!!.controllerLayout, Animations.FadeOutUp))
            .apply(AnimPlayer.Entry(binding!!.operateLayout, Animations.FadeOutRight))
    }
}
