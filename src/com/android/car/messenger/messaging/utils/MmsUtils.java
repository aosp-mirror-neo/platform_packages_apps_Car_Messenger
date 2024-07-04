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
import static android.provider.Telephony.BaseMmsColumns.CONTENT_TYPE;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Mms.Part;

import androidx.annotation.NonNull;

import java.text.MessageFormat;
import java.time.Instant;

/** MMS Utils for parsing MMS Telephony Content */
class MmsUtils {

    @NonNull public static final String FORMAT_CONTENT_MMS_PART = "content://mms/{0}/part";
    @NonNull public static final String FORMAT_CONTENT_MMS_ADDR = "content://mms/{0}/addr";
    @NonNull public static final String FORMAT_TYPE_AND_MSG_ID = "type={0} AND msg_id={1}";

    /** MMS text messages come with extra characters and new lines that need to be removed */
    @NonNull private static final String REPLACE_CHARS = "\r\n";

    private MmsUtils() {}

    @NonNull static final String MMS_CONTENT_TYPE = "application/vnd.wap.multipart.related";
    private static final int ORIGINATOR_ADDRESS_TYPE = 137;

    /** Returns true, if item on cursor position is an MMS message */
    static Boolean isMms(@NonNull Cursor cursor) {
        int index = cursor.getColumnIndex(CONTENT_TYPE);
        return index != -1;
    }

    /**
     * Returns the parsed result as {link @MmsSmsMessage}
     *
     * @throws IllegalArgumentException if desired columns are missing.
     * @see CursorUtils#CONTENT_MMS_PROJECTION
     */
    @NonNull
    static MmsSmsMessage parseMms(@NonNull Context context, @NonNull Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(_ID));
        MmsSmsMessage message = new MmsSmsMessage.Builder()
                .setId(id)
                .setThreadId(cursor.getInt(cursor.getColumnIndex(Mms.THREAD_ID)))
                .setType(cursor.getInt(cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX)))
                .setSubscriptionId(cursor.getInt(cursor.getColumnIndex(Mms.SUBSCRIPTION_ID)))
                .setDate(Instant.ofEpochSecond(cursor.getLong(cursor.getColumnIndex(Mms.DATE))))
                .setRead(cursor.getInt(cursor.getColumnIndex(Mms.READ)) == 1)
                .setSeen(cursor.getInt(cursor.getColumnIndex(Mms.SEEN)) == 1)
                .setPhoneNumber(getOriginator(context, id))
                .setBody(getMmsBody(context, id))
                .setContentType(1)
                .build();
        return message;
    }

    private static String getMmsBody(@NonNull Context context, @NonNull String id) {
        String uriStr = MessageFormat.format(FORMAT_CONTENT_MMS_PART, id);
        Uri uriAddress = Uri.parse(uriStr);
        StringBuilder stringBuilder = new StringBuilder();

        try (Cursor cursor = CursorUtils.simpleQuery(context, uriAddress)) {
            while (cursor != null && cursor.moveToNext()) {
                stringBuilder.append(cursor.getString(cursor.getColumnIndex(Part.TEXT)));
                if (!cursor.isLast()) {
                    stringBuilder.append(" ");
                }
            }
        }
        return stringBuilder.toString().replace(REPLACE_CHARS, "");
    }

    @NonNull
    private static String getOriginator(@NonNull Context context, @NonNull String id) {
        String selection =
                MessageFormat.format(FORMAT_TYPE_AND_MSG_ID, ORIGINATOR_ADDRESS_TYPE, id);
        Uri uri = Uri.parse(MessageFormat.format(FORMAT_CONTENT_MMS_ADDR, id));
        String phoneNum = "";

        try (Cursor cursor = CursorUtils.simpleQueryWithSelection(context, uri, selection)) {
            if (cursor != null && cursor.moveToFirst()) {
                cursor.moveToFirst();
                phoneNum = cursor.getString(cursor.getColumnIndex(Addr.ADDRESS));
            }
        }
        return phoneNum;
    }
}
