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
package com.android.car.messenger.services;

import static com.android.car.messenger.MessageConstants.ACTION_MESSAGE_DELIVERED;
import static com.android.car.messenger.MessageConstants.ACTION_MESSAGE_SENT;
import static com.android.car.messenger.MessageConstants.EXTRA_ACCOUNT_ID;
import static com.android.car.messenger.MessageConstants.EXTRA_CONVERSATION_KEY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import androidx.annotation.NonNull;

import com.android.car.apps.common.log.L;

/**
 * Logs the delivery status of a sent text message from {@link SmsManager#sendTextMessage}.
 *
 * <p>ACTION_MESSAGE_SENT is received when a text message is sent with SmsManager.
 * ACTION_MESSAGE_DELIVERED is received when a text message is successfully delivered to the
 * recipient. This intent is dependent on mobile carriers providing DeliveryReports.
 */
public class MessageStatusReceiver extends BroadcastReceiver {

    private static final String TAG = "CM.MessageStatusReceiver";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        int resultCode = getResultCode();

        if (action == null) {
            L.w(TAG, "Received null action");
            return;
        }

        L.d(TAG, "Received %s", action);
        switch (action) {
            case ACTION_MESSAGE_SENT:
                String convId = intent.getStringExtra(EXTRA_CONVERSATION_KEY);
                int accountId = intent.getIntExtra(EXTRA_ACCOUNT_ID, -1);
                L.d(TAG, "Result %d for conv %s, account %d", resultCode, convId, accountId);
                break;
            case ACTION_MESSAGE_DELIVERED:
                L.d(TAG, "Message delivered with result %d", resultCode);
                break;
        }
    }
}
