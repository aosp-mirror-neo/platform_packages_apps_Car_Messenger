/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.bluetooth.UserAccountListLiveData;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.common.Conversation.Message.MessageType;
import com.android.car.messenger.messaging.utils.ConversationFetchUtil;
import com.android.car.messenger.messaging.utils.MessageUtils;
import com.android.car.messenger.messaging.utils.MmsSmsMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * In memory cache of Conversations from the telephony database.
 */
public class InMemoryConversationLog implements
        Observer<HashMap<Integer, HashMap<String, List<MmsSmsMessage>>>> {

    private static final String TAG = "CM.InMemoryConversationLog";
    private static InMemoryConversationLog sInstance;

    private final List<InMemoryConversationLogObserver> mObservers = new ArrayList<>();

    // Mapping of subscriptionId -> [conversationId, conversation]
    private final HashMap<Integer, HashMap<String, Conversation>>
            mConversationMapByAccount = new HashMap<>();

    // Mapping of subscriptionId -> latest unread MmsSmsMessage
    private final HashMap<Integer, MmsSmsMessage> mUnseenConversationIndex = new HashMap<>();

    private InMemoryConversationLog() {
        ConversationLogLiveData.getInstance().observeForever(this);
    }

    /**
     * Returns the singleton instance of this class
     */
    public static InMemoryConversationLog get() {
        if (sInstance == null) {
            sInstance = new InMemoryConversationLog();
        }
        return sInstance;
    }

    /**
     * Registers an observer to this class
     */
    public void register(InMemoryConversationLogObserver observer) {
        mObservers.add(observer);
    }

    /**
     * Returns the mapping of subscriptionIds to the latest unseen message
     */
    @NonNull
    public HashMap<Integer, MmsSmsMessage> getUnseenConversationIndex() {
        return mUnseenConversationIndex;
    }

    /**
     * Returns all conversations of the given subscriptionId
     */
    @Nullable
    public HashMap<String, Conversation> getConversationList(int subId) {
        return mConversationMapByAccount.get(subId);
    }

    /**
     * Returns the conversation of the given subscriptionId and conversationId
     */
    @Nullable
    public Conversation getConversation(int subId, String conversationId) {
        HashMap<String, Conversation> conversationMap = mConversationMapByAccount.get(subId);
        if (conversationMap != null) {
            return conversationMap.get(conversationId);
        }
        return null;
    }

    /**
     * Takes a mapping of SubscriptionId -> [ConversationId, List[MmsSmsMessage] and converts
     * each message list to a Conversation.
     */
    @Override
    public void onChanged(HashMap<Integer, HashMap<String, List<MmsSmsMessage>>> conversations) {
        L.d(TAG, "onChanged");

        mConversationMapByAccount.clear();
        mUnseenConversationIndex.clear();

        for (Entry<Integer, HashMap<String, List<MmsSmsMessage>>> accE : conversations.entrySet()) {
            int subId = accE.getKey();

            UserAccount userAccount = UserAccountListLiveData.getUserAccount(subId);
            // Telephony bug sometimes will return a invalid sub id. Skip the conversation.
            if (userAccount == null) {
                continue;
            }

            HashMap<String, List<MmsSmsMessage>> rawConvMap = accE.getValue();

            HashMap<String, Conversation> convMap = new HashMap<>();
            for (Entry<String, List<MmsSmsMessage>> convE : rawConvMap.entrySet()) {
                String convId = convE.getKey();
                List<MmsSmsMessage> rawMsgList = convE.getValue();

                Conversation conv = buildConversation(userAccount, convId, rawMsgList);
                convMap.put(convId, conv);

                for (MmsSmsMessage rawMsg : rawMsgList) {
                    if (!rawMsg.isSeen() && rawMsg.getType() == MessageType.MESSAGE_TYPE_INBOX) {
                        mUnseenConversationIndex.put(subId, rawMsg);
                        break;
                    }
                }
            }
            mConversationMapByAccount.put(subId, convMap);
        }
        for (InMemoryConversationLogObserver observer : mObservers) {
            observer.onConversationLogChanged();
        }
    }

    private Conversation buildConversation(
            @NonNull UserAccount userAccount, String convId, List<MmsSmsMessage> rawMsgs) {
        Conversation.Builder builder =
                ConversationFetchUtil.initConversationBuilder(convId, userAccount);

        // message list sorted by date desc (latest to oldest)
        List<Conversation.Message> messages = MessageUtils.getMessages(rawMsgs);
        List<Conversation.Message> messagesToRead = MessageUtils.getUnreadMessages(messages);
        int unreadCount = messagesToRead.size();

        // sort ascending
        messages.sort(comparingLong(Conversation.Message::getTimestamp));
        builder.setMessages(messages).setUnreadCount(unreadCount);
        return builder.build();
    }

    /**
     * Interface used to register an observer
     */
    public interface InMemoryConversationLogObserver {
        /** Callback when the conversation log has changed */
        void onConversationLogChanged();
    }
}
