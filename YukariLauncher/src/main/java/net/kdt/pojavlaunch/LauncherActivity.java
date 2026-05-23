package net.kdt.pojavlaunch;

import static com.arata.yukarilauncher.launch.LaunchGame.preLaunch;
import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.palette.graphics.Palette;

import com.kdt.mcgui.ProgressLayout;
import com.arata.anim.AnimPlayer;
import com.arata.anim.animations.Animations;
import com.arata.yukarilauncher.InfoDistributor;
import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.context.ContextExecutor;
import com.arata.yukarilauncher.databinding.ActivityLauncherBinding;
import com.arata.yukarilauncher.event.single.LaunchGameEvent;
import com.arata.yukarilauncher.event.single.MainBackgroundChangeEvent;
import com.arata.yukarilauncher.event.single.PageOpacityChangeEvent;
import com.arata.yukarilauncher.event.single.SwapToLoginEvent;
import com.arata.yukarilauncher.event.sticky.MinecraftVersionValueEvent;
import com.arata.yukarilauncher.event.value.AddFragmentEvent;
import com.arata.yukarilauncher.event.value.DownloadProgressKeyEvent;
import com.arata.yukarilauncher.event.value.InstallGameEvent;
import com.arata.yukarilauncher.event.value.InstallLocalModpackEvent;
import com.arata.yukarilauncher.event.value.LocalLoginEvent;
import com.arata.yukarilauncher.event.value.MicrosoftLoginEvent;
import com.arata.yukarilauncher.event.value.OtherLoginEvent;
import com.arata.yukarilauncher.feature.accounts.AccountType;
import com.arata.yukarilauncher.feature.accounts.AccountsManager;
import com.arata.yukarilauncher.feature.accounts.LocalAccountUtils;
import com.arata.yukarilauncher.feature.background.BackgroundManager;
import com.arata.yukarilauncher.feature.background.BackgroundType;
import com.arata.yukarilauncher.feature.download.item.ModLoaderWrapper;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.feature.mod.modpack.install.InstallExtra;
import com.arata.yukarilauncher.feature.mod.modpack.install.InstallLocalModPack;
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackInfo;
import com.arata.yukarilauncher.feature.mod.modpack.install.ModPackUtils;
import com.arata.yukarilauncher.feature.update.UpdateUtils;
import com.arata.yukarilauncher.feature.version.Version;
import com.arata.yukarilauncher.feature.version.VersionsManager;
import com.arata.yukarilauncher.feature.version.install.GameInstaller;
import com.arata.yukarilauncher.feature.version.install.InstallTask;
import com.arata.yukarilauncher.plugins.renderer.RendererPlugin;
import com.arata.yukarilauncher.plugins.renderer.RendererPluginManager;
import com.arata.yukarilauncher.setting.AllSettings;
import com.arata.yukarilauncher.task.Task;
import com.arata.yukarilauncher.task.TaskExecutors;
import com.arata.yukarilauncher.ui.activity.BaseActivity;
import com.arata.yukarilauncher.ui.activity.ErrorActivity;
import com.arata.yukarilauncher.ui.dialog.EditTextDialog;
import com.arata.yukarilauncher.ui.dialog.TipDialog;
import com.arata.yukarilauncher.ui.fragment.AccountFragment;
import com.arata.yukarilauncher.ui.fragment.BaseFragment;
import com.arata.yukarilauncher.ui.fragment.DownloadFragment;
import com.arata.yukarilauncher.ui.fragment.DownloadModFragment;
import com.arata.yukarilauncher.ui.fragment.SettingsFragment;
import com.arata.yukarilauncher.ui.subassembly.settingsbutton.ButtonType;
import com.arata.yukarilauncher.ui.subassembly.settingsbutton.SettingsButtonWrapper;
import com.arata.yukarilauncher.ui.subassembly.view.DraggableViewWrapper;
import com.arata.yukarilauncher.utils.StoragePermissionsUtils;
import com.arata.yukarilauncher.utils.YLTools;
import com.arata.yukarilauncher.utils.anim.ViewAnimUtils;
import com.arata.yukarilauncher.utils.file.FileTools;
import com.arata.yukarilauncher.utils.image.ImageUtils;
import com.arata.yukarilauncher.utils.stringutils.StringUtils;

import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftBackgroundLogin;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.utils.NotificationUtils;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.concurrent.Future;

/**
 * ランチャーのメインアクティビティ。フラグメント管理、ゲーム起動、アカウント管理、設定などを担当します。
 */
public class LauncherActivity extends BaseActivity {
    private final AnimPlayer noticeAnimPlayer = new AnimPlayer();
    public final ActivityResultLauncher<Object> modInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (uris) -> {
                if (uris != null) {
                    Tools.launchModInstaller(this, uris.get(0));
                }
            });

    private ActivityLauncherBinding binding;
    private SettingsButtonWrapper mSettingsButtonWrapper;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private NotificationManager mNotificationManager;
    private Future<?> checkNotice;
    private boolean isVideoBackgroundPlaying;

    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener = new FragmentManager.FragmentLifecycleCallbacks() {
        /**
         * フラグメント再開時に設定ボタンの種類を更新します。
         */
        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            if (f instanceof MainMenuFragment) {
                mSettingsButtonWrapper.setButtonType(ButtonType.SETTINGS);
            } else {
                mSettingsButtonWrapper.setButtonType(ButtonType.HOME);
            }
        }
    };

    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        if (taskCount > 0) {
            TaskExecutors.runInUIThread(() -> mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START));
        }
    };

    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;

    /**
     * ページ不透明度変更イベントを処理します。
     */
    @Subscribe()
    public void event(PageOpacityChangeEvent event) {
        setPageOpacity(event.getProgress());
    }

    /**
     * メイン背景変更イベントを処理します。
     */
    @Subscribe()
    public void event(MainBackgroundChangeEvent event) {
        refreshBackground();
        setPageOpacity(AllSettings.getPageOpacity().getValue());
    }

    /**
     * ログイン画面への切り替えイベントを処理します。
     */
    @Subscribe()
    public void event(SwapToLoginEvent event) {
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment == null || getVisibleFragment(AccountFragment.TAG) != null) return;
        YLTools.swapFragmentWithAnim(currentFragment, AccountFragment.class, AccountFragment.TAG, null);
    }

    /**
     * ゲーム起動イベントを処理します。バージョンとアカウントの確認後、ゲームを起動します。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(LaunchGameEvent event) {
        if (binding.progressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }

        Version version = VersionsManager.INSTANCE.getCurrentVersion();
        if (version == null) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return;
        }

        if (AccountsManager.INSTANCE.getAllAccounts().isEmpty()) {
            Toast.makeText(this, R.string.account_no_saved_accounts, Toast.LENGTH_LONG).show();
            EventBus.getDefault().post(new SwapToLoginEvent());
            return;
        }

        RendererPlugin rendererPlugin = RendererPluginManager.getConfigurablePluginOrNull(version.getRenderer());
        if (rendererPlugin != null) {
            StoragePermissionsUtils.checkPermissions(
                    this,
                    R.string.generic_warning,
                    getString(R.string.permissions_storage_for_renderer_config, rendererPlugin.getDisplayName(), InfoDistributor.APP_NAME),
                    new StoragePermissionsUtils.PermissionGranted() {
                        @Override
                        public void granted() { launchGame(version); }
                        @Override
                        public void cancelled() { launchGame(version); }
                    }
            );
            return;
        }

        launchGame(version);
    }

    /**
     * Microsoftログインのリダイレクトイベントを処理します。
     */
    @Subscribe()
    public void event(MicrosoftLoginEvent event) {
        new MicrosoftBackgroundLogin(false, event.getUri().getQueryParameter("code")).performLogin(
                this, null,
                AccountsManager.INSTANCE.getDoneListener(),
                AccountsManager.INSTANCE.getErrorListener()
        );
    }

    /**
     * その他ログイン方式のイベントを処理します。
     */
    @Subscribe()
    public void event(OtherLoginEvent event) {
        Task.runTask(() -> {
                    event.getAccount().save();
                    Logging.i("Account", "Saved the account : " + event.getAccount().username);
                    return null;
                }).onThrowable(e -> Logging.e("Account", "Failed to save the account : " + e))
                .finallyTask(() -> AccountsManager.INSTANCE.getDoneListener().onLoginDone(event.getAccount()))
                .execute();
    }

    /**
     * ローカルアカウントログインイベントを処理します。
     */
    @Subscribe()
    public void event(LocalLoginEvent event) {
        String userName = event.getUserName();
        MinecraftAccount localAccount = new MinecraftAccount();
        localAccount.username = userName;
        localAccount.accountType = AccountType.LOCAL.getType();
        try {
            localAccount.save();
            Logging.i("Account", "Saved the account : " + localAccount.username);
        } catch (IOException e) {
            Logging.e("Account", "Failed to save the account : " + e);
        }
        AccountsManager.INSTANCE.getDoneListener().onLoginDone(localAccount);
    }

    /**
     * ローカルModパックのインストールイベントを処理します。
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(InstallLocalModpackEvent event) {
        InstallExtra installExtra = event.getInstallExtra();
        if (!installExtra.startInstall) return;

        if (binding.progressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }

        File dirGameModpackFile = new File(installExtra.modpackPath);
        ModPackInfo info = ModPackUtils.determineModpack(dirGameModpackFile);
        if (info.getType() == ModPackUtils.ModPackEnum.UNKNOWN) {
            InstallLocalModPack.showUnSupportDialog(this);
        }

        String modPackName = info.getName() != null ? info.getName() : FileTools.getFileNameWithoutExtension(dirGameModpackFile);

        new EditTextDialog.Builder(this)
                .setTitle(R.string.version_install_new)
                .setEditText(modPackName)
                .setAsRequired()
                .setConfirmListener((editText, checked) -> {
                    String customName = editText.getText().toString();
                    if (FileTools.isFilenameInvalid(editText)) return false;
                    if (VersionsManager.INSTANCE.isVersionExists(customName, true)) {
                        editText.setError(getString(R.string.version_install_exists));
                        return false;
                    }
                    Task.runTask(() -> {
                        ModLoaderWrapper modLoaderWrapper = InstallLocalModPack.installModPack(this, info.getType(), dirGameModpackFile, customName);
                        if (modLoaderWrapper != null) {
                            InstallTask downloadTask = modLoaderWrapper.getDownloadTask();
                            if (downloadTask != null) {
                                runOnUiThread(() -> Toast.makeText(this, getString(R.string.modpack_prepare_mod_loader_installation), Toast.LENGTH_SHORT).show());
                                Logging.i("Install Version", "Installing ModLoader: " + modLoaderWrapper.getModLoaderVersion());
                                File file = downloadTask.run(customName);
                                if (file != null) return new kotlin.Pair<>(modLoaderWrapper, file);
                            }
                        }
                        return null;
                    }).beforeStart(TaskExecutors.getAndroidUI(), () -> ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.generic_waiting))
                            .ended(filePair -> {
                                if (filePair != null) {
                                    try {
                                        ModPackUtils.startModLoaderInstall(filePair.getFirst(), LauncherActivity.this, filePair.getSecond(), customName);
                                    } catch (Throwable e) { throw new RuntimeException(e); }
                                }
                            }).onThrowable(TaskExecutors.getAndroidUI(), e -> Tools.showErrorRemote(this, R.string.modpack_install_download_failed, e))
                            .finallyTask(TaskExecutors.getAndroidUI(), () -> ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE))
                            .execute();
                    return true;
                }).showDialog();
    }

    /**
     * ゲームのインストールイベントを処理します。
     */
    @Subscribe()
    public void event(InstallGameEvent event) {
        new GameInstaller(this, event).installGame();
    }

    /**
     * ダウンロード進捗キーの監視・監視解除イベントを処理します。
     */
    @Subscribe()
    public void event(DownloadProgressKeyEvent event) {
        if (event.getObserve()) {
            binding.progressLayout.observe(event.getProgressKey());
        } else {
            binding.progressLayout.unObserve(event.getProgressKey());
        }
    }

    /**
     * フラグメント追加イベントを処理します。
     */
    @Subscribe()
    public synchronized void event(AddFragmentEvent event) {
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            try {
                AddFragmentEvent.FragmentActivityCallBack activityCallBack = event.getFragmentActivityCallback();
                if (activityCallBack != null) {
                    activityCallBack.callBack(currentFragment.requireActivity());
                }
                YLTools.addFragment(
                        currentFragment,
                        event.getFragmentClass(),
                        event.getFragmentTag(),
                        event.getBundle()
                );
            } catch (Exception e) {
                Logging.e("LauncherActivity", "Failed attempt to jump to a new Fragment!", e);
            }
        }
    }

    /**
     * アクティビティ作成時に呼び出されます。レイアウトの初期化、フラグメント管理、通知権限の確認を行います。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLauncherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        processFragment();
        processViews();

        mRequestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if(!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable);
                        if(runnable != null) runnable.run();
                    }
                }
        );
        checkNotificationPermission();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        ProgressKeeper.addTaskCountListener(binding.progressLayout);

        new AsyncVersionList().getVersionList(versions -> EventBus.getDefault().postSticky(
                        new MinecraftVersionValueEvent(versions)),
                false
        );

        Task.runTask(() -> {
            UpdateUtils.checkDownloadedPackage(this, false, true);
            return null;
        }).execute();
    }

    /**
     * フラグメント管理の初期化を行います。戻るボタンの処理と初期フラグメントの設定を行います。
     */
    private void processFragment() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getCurrentFragment();
                if (currentFragment instanceof BaseFragment && !((BaseFragment) currentFragment).onBackPressed()) {
                    return;
                }
                if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                    finish();
                } else {
                    getSupportFragmentManager().popBackStackImmediate();
                }
            }
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() < 1) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack(MainMenuFragment.TAG)
                    .add(R.id.container_fragment, MainMenuFragment.class, null, MainMenuFragment.TAG).commit();
        }
    }

    /**
     * ビューの初期化を行います。背景、ボタン、進捗レイアウト、通知の設定を行います。
     */
    private void processViews() {
        refreshBackground();
        setPageOpacity(AllSettings.getPageOpacity().getValue());
        mSettingsButtonWrapper = new SettingsButtonWrapper(binding.settingButton);
        mSettingsButtonWrapper.setOnTypeChangeListener(type -> ViewAnimUtils.setViewAnim(binding.settingButton, Animations.Pulse));
        binding.downloadButton.setOnClickListener(v -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(binding.containerFragment.getId());
            if (fragment != null && !(fragment instanceof DownloadFragment || fragment instanceof DownloadModFragment)) {
                ViewAnimUtils.setViewAnim(binding.downloadButton, Animations.Pulse);
                YLTools.swapFragmentWithAnim(fragment, DownloadFragment.class, DownloadFragment.TAG, null);
            }
        });
        binding.settingButton.setOnClickListener(v -> {
            ViewAnimUtils.setViewAnim(binding.settingButton, Animations.Pulse);
            Fragment fragment = getSupportFragmentManager().findFragmentById(binding.containerFragment.getId());
            if (fragment instanceof MainMenuFragment) {
                YLTools.swapFragmentWithAnim(fragment, SettingsFragment.class, SettingsFragment.TAG, null);
            } else {
                Tools.backToMainMenu(this);
            }
        });
        binding.appTitleText.setText(InfoDistributor.APP_NAME);
        binding.appTitleText.setClickable(false);
        binding.appTitleText.setFocusable(false);
        binding.appTitleText.setOnClickListener(null);

        binding.progressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        binding.progressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        binding.progressLayout.observe(ProgressLayout.INSTALL_RESOURCE);
        binding.progressLayout.observe(ProgressLayout.LOGIN_ACCOUNT);
        binding.progressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);
        binding.progressLayout.observe(ProgressLayout.CHECKING_MODS);

        binding.noticeGotButton.setOnClickListener(v -> {
            AllSettings.getNoticeDefault().put(false).save();
        });
        new DraggableViewWrapper(binding.noticeLayout, new DraggableViewWrapper.AttributesFetcher() {
            @NonNull
            @Override
            public DraggableViewWrapper.ScreenPixels getScreenPixels() {
                return new DraggableViewWrapper.ScreenPixels(0, 0,
                        currentDisplayMetrics.widthPixels - binding.noticeLayout.getWidth(),
                        currentDisplayMetrics.heightPixels - binding.noticeLayout.getHeight());
            }
            @NonNull
            @Override
            public int[] get() {
                return new int[]{(int) binding.noticeLayout.getX(), (int) binding.noticeLayout.getY()};
            }
            @Override
            public void set(int x, int y) {
                binding.noticeLayout.setX(x);
                binding.noticeLayout.setY(y);
            }
        }).init();

        if (YLTools.checkDate(4, 1)) binding.hair.setVisibility(View.VISIBLE);
        else binding.hair.setVisibility(View.GONE);
    }

    /**
     * アクティビティ再開時に呼び出されます。透明度の更新、バージョン一覧のリフレッシュ、ビデオ背景の再生を行います。
     */
    @Override
    protected void onResume() {
        super.onResume();
        setPageOpacity(AllSettings.getPageOpacity().getValue());
        VersionsManager.INSTANCE.refresh("LauncherActivity:onResume", false);
        if (isVideoBackgroundPlaying) binding.backgroundVideoView.start();
    }

    /**
     * アクティビティ一時停止時にビデオ背景を一時停止します。
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (isVideoBackgroundPlaying && binding.backgroundVideoView.isPlaying()) {
            binding.backgroundVideoView.pause();
        }
    }

    /**
     * アクティビティ開始時にフラグメントライフサイクルコールバックを登録します。
     */
    @Override
    protected void onStart() {
        super.onStart();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true);
    }

    /**
     * アクティビティ破棄時にリソースをクリーンアップします。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.progressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(binding.progressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
        ContextExecutor.clearActivity();
        stopVideoBackground();
    }

    /**
     * ウィンドウへのアタッチ時にノッチサイズを計算します。
     */
    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }

    /**
     * ゲームを起動します。ローカルアカウントの使用可否を確認してから起動します。
     */
    private void launchGame(Version version) {
        LocalAccountUtils.checkUsageAllowed(new LocalAccountUtils.CheckResultListener() {
            @Override
            public void onUsageAllowed() { preLaunch(LauncherActivity.this, version); }
            @Override
            public void onUsageDenied() {
                if (!AllSettings.getLocalAccountReminders().getValue()) {
                    preLaunch(LauncherActivity.this, version);
                } else {
                    LocalAccountUtils.openDialog(LauncherActivity.this, checked -> {
                                LocalAccountUtils.saveReminders(checked);
                                preLaunch(LauncherActivity.this, version);
                            },
                            getString(R.string.account_no_microsoft_account) + getString(R.string.account_purchase_minecraft_account_tip),
                            R.string.account_continue_to_launch_the_game);
                }
            }
        });
    }

    /**
     * メインメニューの背景画像またはビデオをリフレッシュして表示します。
     */
    private void refreshBackground() {
        File mediaFile = BackgroundManager.getBackgroundImage(BackgroundType.MAIN_MENU);
        if (mediaFile != null && BackgroundManager.isVideo(mediaFile)) {
            playVideoBackground(mediaFile);
            refreshTopBarColor(false);
            return;
        }
        stopVideoBackground();
        BackgroundManager.setBackgroundImage(this, BackgroundType.MAIN_MENU, binding.backgroundView, this::refreshTopBarColor);
    }

    /**
     * ビデオ背景を再生します。
     * @param videoFile 再生するビデオファイル
     */
    private void playVideoBackground(File videoFile) {
        binding.backgroundView.setImageDrawable(null);
        binding.backgroundView.setVisibility(View.GONE);
        binding.backgroundVideoView.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.backgroundVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        }
        binding.backgroundVideoView.setVideoPath(videoFile.getAbsolutePath());
        binding.backgroundVideoView.setOnPreparedListener(mediaPlayer -> {
            mediaPlayer.setVolume(0f, 0f);
            mediaPlayer.setLooping(true);
            binding.backgroundVideoView.start();
            isVideoBackgroundPlaying = true;
        });
        binding.backgroundVideoView.setOnErrorListener((mp, what, extra) -> {
            stopVideoBackground();
            BackgroundManager.setBackgroundImage(this, BackgroundType.MAIN_MENU, binding.backgroundView, this::refreshTopBarColor);
            return true;
        });
    }

    /**
     * ビデオ背景を停止し、静止画背景に切り替えます。
     */
    private void stopVideoBackground() {
        if (isVideoBackgroundPlaying) binding.backgroundVideoView.stopPlayback();
        isVideoBackgroundPlaying = false;
        binding.backgroundVideoView.setVisibility(View.GONE);
        binding.backgroundView.setVisibility(View.VISIBLE);
    }

    /**
     * トップバーの背景色を更新します（背景画像からパレットを生成）。
     */
    private void refreshTopBarColor(boolean loadFromBackground) {
        int backgroundMenuTop = ContextCompat.getColor(this, R.color.background_menu_top);
        if (loadFromBackground) {
            Bitmap bitmap = ImageUtils.getBitmapFromImageView(binding.backgroundView);
            if (bitmap != null) {
                Palette palette = Palette.from(bitmap).generate();
                boolean isDarkMode = YLTools.isDarkMode(this);
                binding.topLayout.setBackgroundColor(
                        isDarkMode ? palette.getDarkVibrantColor(backgroundMenuTop) : palette.getLightVibrantColor(backgroundMenuTop));
                int mutedColor = isDarkMode ? palette.getLightMutedColor(0xFFFFFFFF) : palette.getDarkMutedColor(0xFFFFFFFF);
                ColorStateList colorStateList = ColorStateList.valueOf(mutedColor);
                binding.appTitleText.setTextColor(mutedColor);
                binding.downloadButton.setImageTintList(colorStateList);
                binding.settingButton.setImageTintList(colorStateList);
                return;
            }
        }
        binding.topLayout.setBackgroundColor(backgroundMenuTop);
        binding.appTitleText.setTextColor(ContextCompat.getColor(this, R.color.menu_bar_text));
        ColorStateList colorStateList = ColorStateList.valueOf(0xFFFFFFFF);
        binding.downloadButton.setImageTintList(colorStateList);
        binding.settingButton.setImageTintList(colorStateList);
    }

    /**
     * タグから表示可能なフラグメントを取得します。
     */
    @SuppressWarnings("SameParameterValue")
    private Fragment getVisibleFragment(String tag) {
        return checkFragmentAvailability(getSupportFragmentManager().findFragmentByTag(tag));
    }

    /**
     * IDから表示可能なフラグメントを取得します。
     */
    private Fragment getVisibleFragment(int id) {
        return checkFragmentAvailability(getSupportFragmentManager().findFragmentById(id));
    }

    /**
     * 現在のコンテナのフラグメントを取得します。
     */
    private Fragment getCurrentFragment() {
        return getVisibleFragment(binding.containerFragment.getId());
    }

    /**
     * フラグメントが表示可能かどうかを確認します。
     */
    private Fragment checkFragmentAvailability(Fragment fragment) {
        if (fragment != null && fragment.isVisible()) return fragment;
        return null;
    }

    /**
     * 通知権限を確認し、必要に応じて許可を求めます。
     */
    private void checkNotificationPermission() {
        if (AllSettings.getSkipNotificationPermissionCheck().getValue() || YLTools.checkForNotificationPermission()) return;
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionReasoning();
            return;
        }
        askForNotificationPermission(null);
    }

    /**
     * 通知権限が必要な理由を説明するダイアログを表示します。
     */
    private void showNotificationPermissionReasoning() {
        new TipDialog.Builder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(getString(R.string.notification_permission_dialog_text, InfoDistributor.APP_NAME, InfoDistributor.APP_NAME))
                .setConfirmClickListener(checked -> askForNotificationPermission(null))
                .setCancelClickListener(this::handleNoNotificationPermission)
                .showDialog();
    }

    /**
     * 通知権限が得られなかった場合の処理を行います。
     */
    private void handleNoNotificationPermission() {
        AllSettings.getSkipNotificationPermissionCheck().put(true).save();
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show();
    }

    /**
     * 通知権限を要求します。
     * @param onSuccessRunnable 権限が許可された場合に実行するRunnable
     */
    public void askForNotificationPermission(Runnable onSuccessRunnable) {
        if (Build.VERSION.SDK_INT < 33) return;
        if (onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    /**
     * ページの不透明度を設定します。
     */
    private void setPageOpacity(int pageOpacity) {
        BigDecimal opacity = BigDecimal.valueOf(pageOpacity).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        float v = opacity.floatValue();
        binding.containerFragment.setAlpha(v);
        BigDecimal adjustedOpacity = BackgroundManager.hasBackgroundImage(BackgroundType.MAIN_MENU)
                ? opacity.subtract(BigDecimal.valueOf(0.1)).max(BigDecimal.ZERO)
                : BigDecimal.ONE;
        binding.topLayout.setAlpha(adjustedOpacity.floatValue());
    }
}
