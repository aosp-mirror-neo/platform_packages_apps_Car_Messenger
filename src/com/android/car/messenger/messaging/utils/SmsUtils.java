/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.messenger.messaging.utils;

import static android.provider.BaseColumns._ID;

import android.database.Cursor;
import android.provider.Telephony.Sms;

import androidx.annotation.NonNull;

import java.time.Instant;

/** SMS Utils for parsing SMS Telephony Content */
class SmsUtils {

    SmsUtils() {}

    /**
     * Returns the parsed sms result as a {@link MmsSmsMessage}
     *
     * @throws IllegalArgumentException if desired columns are missing.
     * @see CursorUtils#CONTENT_SMS_PROJECTION
     */
    @NonNull
    static MmsSmsMessage parseSms(@NonNull Cursor cursor) {
        int idIndex = cursor.getColumnIndex(_ID);
        int threadIdIndex = cursor.getColumnIndex(Sms.THREAD_ID);
        int recipientsIndex = cursor.getColumnIndex(Sms.ADDRESS);
        int bodyIndex = cursor.getColumnIndex(Sms.BODY);
        int subscriptionIdIndex = cursor.getColumnIndex(Sms.SUBSCRIPTION_ID);
        int dateIndex = cursor.getColumnIndex(Sms.DATE);
        int typeIndex = cursor.getColumnIndex(Sms.TYPE);
        int readIndex = cursor.getColumnIndex(Sms.READ);
        int seenIndex = cursor.getColumnIndex(Sms.SEEN);

        return new MmsSmsMessage.Builder()
                .setId(cursor.getString(idIndex))
                .setThreadId(cursor.getInt(threadIdIndex))
                .setType(cursor.getInt(typeIndex))
                .setSubscriptionId(cursor.getInt(subscriptionIdIndex))
                .setDate(Instant.ofEpochMilli(cursor.getLong(dateIndex)))
                .setRead(cursor.getInt(readIndex) == 1)
                .setSeen(cursor.getInt(seenIndex) == 1)
                .setPhoneNumber(cursor.getString(recipientsIndex))
                .setBody(cursor.getString(bodyIndex))
                .setContentType(0)
                .build();
    }
}
