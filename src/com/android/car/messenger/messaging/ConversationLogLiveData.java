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

import static android.provider.Telephony.TextBasedSmsColumns.THREAD_ID;

import static com.android.car.messenger.messaging.utils.ConversationFetchUtil.fetchConversationThread;

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.provider.Telephony;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.bluetooth.RefreshLiveData;
import com.android.car.messenger.messaging.utils.MmsSmsMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live data that fetches all conversations, and indexes them based on the subscription id and
 * conversation id.
 *
 * Subscribes to content://mms-sms/ for any changes to the database. The data model will be rebuilt
 * every time.
 */
public class ConversationLogLiveData extends
        ContentProviderLiveData<HashMap<Integer, HashMap<String, List<MmsSmsMessage>>>> {
    private static final String TAG = "CM.ConversationLogLiveData";

    private static ConversationLogLiveData sInstance;

    private ConversationLogLiveData() {
        super(Telephony.MmsSms.CONTENT_URI);
        addSource(RefreshLiveData.getInstance(), it -> onDataChange());
    }

    /**
     * Returns the singleton instance of this class
     */
    public static ConversationLogLiveData getInstance() {
        if (sInstance == null) {
            sInstance = new ConversationLogLiveData();
        }
        return sInstance;
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (getValue() == null) {
            onDataChange();
        }
    }

    @Override
    public void onDataChange() {
        L.d(TAG, "Telephony database changed");

        // SubId -> ConversationId -> List<Messages>
        HashMap<Integer, HashMap<String, List<MmsSmsMessage>>> conversationLog = new HashMap<>();
        try (Cursor cursor = ConversationsPerDeviceFetchManager.getCursor()) {
            while (cursor != null && cursor.moveToNext()) {
                String conversationId = cursor.getString(cursor.getColumnIndex(THREAD_ID));
                HashMap<Integer, List<MmsSmsMessage>> conversationMap = new HashMap<>();
                try {
                    conversationMap = fetchConversationThread(conversationId);
                } catch (CursorIndexOutOfBoundsException e) {
                    L.w(TAG, "Error occurred fetching conversation Id: %s", conversationId);
                } finally {
                    for (Map.Entry<Integer, List<MmsSmsMessage>> e : conversationMap.entrySet()) {
                        int subId = e.getKey();
                        List<MmsSmsMessage> list = e.getValue();

                        HashMap<String, List<MmsSmsMessage>> convMap =
                                conversationLog.computeIfAbsent(subId, k -> new HashMap<>());
                        convMap.put(conversationId, list);
                    }
                }
            }
        }
        postValue(conversationLog);
    }
}
