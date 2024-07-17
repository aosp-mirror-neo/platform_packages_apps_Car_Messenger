/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.messenger.ui.conversationlist;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.R;
import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.util.VoiceUtil;
import com.android.car.ui.toolbar.MenuItem;

import java.util.ArrayList;

/** Fragment for Message History/Conversation Metadata List */
public class ConversationListFragment extends MessageListBaseFragment
        implements ConversationItemAdapter.OnConversationItemClickListener {
    private static final String TAG = "CM.ConversationListFragment";

    private ConversationItemAdapter mConversationItemAdapter;
    private LiveData<UserAccount> mUserAccountLiveData;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setMenuItems();

        // Don't recreate the adapter if we already have one, so that the list items
        // will display immediately upon the view being recreated.
        L.d(TAG, "In View Created, about to load message data");
        if (mConversationItemAdapter == null) {
            mConversationItemAdapter =
                    new ConversationItemAdapter(/* onConversationItemClickListener= */ this);
        }
        getRecyclerView().setAdapter(mConversationItemAdapter);

        ConversationListViewModel viewModel =
                new ViewModelProvider(getActivity()).get(ConversationListViewModel.class);
        mUserAccountLiveData = viewModel.getCurrentAccount();

        Transformations.switchMap(mUserAccountLiveData, viewModel::getConversations)
                .observe(this, conversationLog -> {
                    if (conversationLog == null || conversationLog.isEmpty()) {
                        mLoadingFrameLayout.showEmpty(R.string.no_messages);
                    } else {
                        mConversationItemAdapter.setConversationLogItems(conversationLog);
                        mLoadingFrameLayout.showContent();
                    }
                    setMenuItems();
                });

    }

    private void setMenuItems() {
        Activity activity = getActivity();
        if (activity == null || mToolbar == null) {
            return;
        }
        if (!mToolbar.getMenuItems().isEmpty()) {
            return;
        }
        if (!getResources().getBoolean(R.bool.direct_send_supported)) {
            return;
        }
        MenuItem newMessageButton = new MenuItem.Builder(activity)
                .setIcon(R.drawable.ui_icon_edit)
                .setTinted(false)
                .setShowIconAndTitle(true)
                .setTitle(R.string.new_message)
                .setPrimary(true)
                .setOnClickListener(item -> VoiceUtil.voiceRequestGenericCompose(
                        activity, mUserAccountLiveData.getValue()))
                .build();
        ArrayList<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(newMessageButton);
        mToolbar.setMenuItems(menuItems);
    }

    @Override
    public void onConversationItemClicked(@NonNull Conversation conversation) {
        UserAccount userAccount = mUserAccountLiveData.getValue();
        if (userAccount == null) {
            return;
        }
        VoiceUtil.voiceRequestReadConversation(requireActivity(), userAccount, conversation);
    }

    @Override
    public void onReplyIconClicked(@NonNull Conversation conversation) {
        UserAccount userAccount = mUserAccountLiveData.getValue();
        if (userAccount == null) {
            return;
        }
        VoiceUtil.voiceRequestReplyConversation(requireActivity(), userAccount, conversation);
    }

    @Override
    public void onPlayIconClicked(@NonNull Conversation conversation) {
        UserAccount userAccount = mUserAccountLiveData.getValue();
        if (userAccount == null) {
            return;
        }
        VoiceUtil.voiceRequestReadConversation(requireActivity(), userAccount, conversation);
    }

    /**
     * Get instance of ConversationListFragment
     */
    public static ConversationListFragment newInstance() {
        return new ConversationListFragment();
    }

    /**
     * Get unique fragment tag for this class
     */
    public static String getFragmentTag() {
        return ConversationListFragment.class.getName();
    }
}
