/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.messenger.messaging;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.MediatorLiveData;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.MessageConstants;
import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.bluetooth.UserAccountListLiveData;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.interfaces.AppFactory;
import com.android.car.messenger.interfaces.DataModel;
import com.android.car.messenger.messaging.utils.CursorUtils;
import com.android.car.messenger.messaging.utils.MmsSmsMessage;
import com.android.car.messenger.util.CarStateListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Publishes a stream of {@link Conversation} with unread messages that was received on the user
 * device after the car's connection to the{@link UserAccount}.
 */
public class NewMessageLiveData extends MediatorLiveData<Conversation> implements
        InMemoryConversationLog.InMemoryConversationLogObserver {
    private static final String TAG = "CM.NewMessageLiveData";

    private final DataModel mDataModel;
    @NonNull
    private final UserAccountListLiveData mUserAccountListLiveData =
            UserAccountListLiveData.getInstance();

    @VisibleForTesting
    @NonNull
    Collection<UserAccount> mUserAccounts = new ArrayList<>();

    @NonNull
    private final CarStateListener mCarStateListener = AppFactory.get().getCarStateListener();

    NewMessageLiveData() {
        InMemoryConversationLog.get().register(this);
        mDataModel = AppFactory.get().getDataModel();
    }

    @Override
    protected void onActive() {
        super.onActive();
        addSource(mUserAccountListLiveData, it -> mUserAccounts = it.getAccounts());
        if (getValue() == null) {
            onDataChanged();
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        removeSource(mUserAccountListLiveData);
        mUserAccounts.clear();
    }

    private void onDataChanged() {
        for (UserAccount userAccount : mUserAccounts) {
            if (hasProjectionInForeground(userAccount)) {
                continue;
            }
            HashMap<Integer, MmsSmsMessage> unseenMap =
                    InMemoryConversationLog.get().getUnseenConversationIndex();
            MmsSmsMessage msg = unseenMap.get(userAccount.getId());
            if (msg != null) {
                L.d(TAG, "Posting new message convId:%s subId:%s",
                        msg.getThreadId(), userAccount.getId());
                postNewMessage(msg, userAccount);
                break;
            }
        }
    }

    /** Post a new message if one is found, and returns true if so, false otherwise */
    private void postNewMessage(MmsSmsMessage msg, @NonNull UserAccount userAccount) {
        Conversation conversation = InMemoryConversationLog.get().getConversation(
                userAccount.getId(), Long.toString(msg.getThreadId()));
        conversation.getExtras().putInt(MessageConstants.EXTRA_ACCOUNT_ID, userAccount.getId());
        postValue(conversation);
        CursorUtils.ContentType type = msg.getContentType() == 0
                ? CursorUtils.ContentType.SMS
                : CursorUtils.ContentType.MMS;
        mDataModel.markAsSeen(msg.getId(), type);
    }

    private boolean hasProjectionInForeground(@NonNull UserAccount userAccount) {
        return mCarStateListener.isProjectionInActiveForeground(userAccount.getIccId());
    }

    @Override
    public void onConversationLogChanged() {
        onDataChanged();
    }
}
