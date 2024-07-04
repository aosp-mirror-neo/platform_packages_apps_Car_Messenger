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

import static java.util.Comparator.comparingLong;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;

import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.messaging.utils.ConversationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** Publishes a list of {@link Conversation} for a {@link UserAccount} to subscribers */
class ConversationListLiveData extends MediatorLiveData<List<Conversation>> implements
        InMemoryConversationLog.InMemoryConversationLogObserver {
    private static final String TAG = "CM.ConversationListLiveData";

    @NonNull private final UserAccount mUserAccount;

    @NonNull
    private static final Comparator<Conversation> sConversationComparator =
            comparingLong(ConversationUtil::getConversationTimestamp).reversed();

    ConversationListLiveData(@NonNull UserAccount userAccount) {
        InMemoryConversationLog.get().register(this);
        mUserAccount = userAccount;
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (getValue() == null) {
            onDataChanged();
        }
    }

    private void onDataChanged() {
        ArrayList<Conversation> conversations = new ArrayList<>();
        HashMap<String, Conversation> convsFromAccount = InMemoryConversationLog.get()
                .getConversationList(mUserAccount.getId());
        if (convsFromAccount != null) {
            conversations = new ArrayList<>(convsFromAccount.values());
        }
        conversations.sort(sConversationComparator);
        postValue(conversations);
    }

    @Override
    public void onConversationLogChanged() {
        onDataChanged();
    }
}
