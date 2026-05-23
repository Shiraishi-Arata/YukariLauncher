package com.arata.yukarilauncher.ui.subassembly.account;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.arata.yukarilauncher.R;
import com.arata.yukarilauncher.databinding.ItemAccountManagerBinding;
import com.arata.yukarilauncher.feature.accounts.AccountUtils;
import com.arata.yukarilauncher.feature.log.Logging;
import com.arata.yukarilauncher.utils.skin.SkinLoader;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.util.List;

/**
 * アカウント一覧のRecyclerViewアダプター
 */
public class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.Holder> {
    private final List<MinecraftAccount> mData;
    private AccountUpdateListener accountUpdateListener;

    /**
     * アダプターを構築する
     */
    public AccountAdapter(List<MinecraftAccount> mData) {
        this.mData = mData;
    }

    @NonNull
    @Override
    public AccountAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAccountManagerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AccountAdapter.Holder holder, int position) {
        holder.setData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    /**
     * アカウント更新リスナーを設定する
     */
    public void setAccountUpdateListener(AccountUpdateListener accountUpdateListener) {
        this.accountUpdateListener = accountUpdateListener;
    }

    /**
     * アカウント操作のコールバックリスナー
     */
    public interface AccountUpdateListener {
        /** アカウントがクリックされた */
        void onViewClick(MinecraftAccount account);

        /** アカウントの更新が要求された */
        void onRefresh(MinecraftAccount account);

        /** アカウントの削除が要求された */
        void onDelete(MinecraftAccount account);
    }

    /**
     * アカウントアイテムのビューホルダー
     */
    public class Holder extends RecyclerView.ViewHolder {
        private final Context mContext;
        private final ItemAccountManagerBinding binding;

        /**
         * ホルダーを構築する
         */
        public Holder(@NonNull ItemAccountManagerBinding binding) {
            super(binding.getRoot());
            this.mContext = binding.getRoot().getContext();
            this.binding = binding;
        }

        /**
         * アカウントデータをビューに設定する
         */
        public void setData(MinecraftAccount account) {
            if (accountUpdateListener != null) {
                itemView.setOnClickListener(v -> accountUpdateListener.onViewClick(account));
                binding.refresh.setOnClickListener(v -> accountUpdateListener.onRefresh(account));
                binding.delete.setOnClickListener(v -> accountUpdateListener.onDelete(account));
            }

            binding.name.setText(account.username);

            String loginType;
            if (AccountUtils.isMicrosoftAccount(account)) {
                setButtonClickable(binding.refresh, true);
                loginType = mContext.getString(R.string.account_microsoft_account);
            } else if (AccountUtils.isOtherLoginAccount(account)) {
                setButtonClickable(binding.refresh, true);
                loginType = account.accountType;
            } else {
                setButtonClickable(binding.refresh, false);
                loginType = mContext.getString(R.string.account_local_account);
            }

            try {
                binding.icon.setImageDrawable(SkinLoader.getAvatarDrawable(mContext, account, (int) Tools.dpToPx(mContext.getResources().getDimensionPixelSize(R.dimen._38sdp))));
            } catch (Exception e) {
                Logging.e("AccountAdapter", "Failed to load avatar.", e);
            }

            binding.loginType.setText(loginType);
        }

        /**
         * ボタンのクリック可否を設定する
         */
        private void setButtonClickable(View button, boolean clickable) {
            button.setAlpha(clickable ? 1.0f : 0.5f);
            button.setEnabled(clickable);
        }
    }
}
