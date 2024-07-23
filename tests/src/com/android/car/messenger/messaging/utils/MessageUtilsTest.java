/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.messenger.common.Conversation.Message;
import com.android.car.messenger.common.Conversation.Message.MessageStatus;
import com.android.car.messenger.common.Conversation.Message.MessageType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MessageUtilsTest {

    private static final int CURRENT_DEVICE_ID = 1;

    @Test
    public void testGetRawMessages() {
        MmsSmsMessage msg1 = createMessage(
                /* id= */ "1",
                /* subId= */ CURRENT_DEVICE_ID,
                /* timestamp= */ 1,
                /* body= */ "",
                /* type= */ MessageType.MESSAGE_TYPE_SENT,
                /* isRead= */ true);
        MmsSmsMessage msg2 = createMessage(
                /* id= */ "2",
                /* subId= */ 2,
                /* timestamp= */ 2,
                /* body= */ "text2",
                /* type= */ MessageType.MESSAGE_TYPE_ALL,
                /* isRead= */ true);
        MmsSmsMessage msg3 = createMessage(
                /* id= */ "3",
                /* subId= */ CURRENT_DEVICE_ID,
                /* timestamp= */ 3,
                /* body= */ "text3",
                /* type= */ MessageType.MESSAGE_TYPE_INBOX,
                /* isRead= */ false);
        MmsSmsMessage msg4 = createMessage(
                /* id= */ "4",
                /* subId= */ CURRENT_DEVICE_ID,
                /* timestamp= */ 4,
                /* body= */ "text4",
                /* type= */ MessageType.MESSAGE_TYPE_INBOX,
                /* isRead= */ false);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(MmsUtils.class)
                .spyStatic(SmsUtils.class)
                .startMocking();

        try {
            Cursor smsCursor = mock(Cursor.class);
            Cursor mmsCursor = mock(Cursor.class);

            // Mocks smsCursor to return a single message, and mmsCursor to return two messages.
            doReturn(true).when(() -> MmsUtils.isMms(mmsCursor));
            doReturn(false).when(() -> MmsUtils.isMms(smsCursor));
            doReturn(msg2, msg1).when(() -> MmsUtils.parseMms(any(), eq(mmsCursor)));
            doReturn(msg3, msg4).when(() -> SmsUtils.parseSms(smsCursor));
            when(smsCursor.moveToFirst()).thenReturn(true);
            when(smsCursor.moveToNext()).thenReturn(true, false);
            when(mmsCursor.moveToFirst()).thenReturn(true);
            when(mmsCursor.moveToNext()).thenReturn(true, false);

            // Tests the following:
            // 1. Empty messages are skipped
            // 2. Returned messages are in descending order
            // 3. Only return messages from the device with CURRENT_DEVICE_ID
            List<MmsSmsMessage> messages = MessageUtils.getRawMessages(mmsCursor, smsCursor);
            assertThat(messages).hasSize(3);
            assertThat(messages.get(0).getBody()).isEqualTo("text4");
            assertThat(messages.get(1).getBody()).isEqualTo("text3");
            assertThat(messages.get(2).getBody()).isEqualTo("text2");
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testGetUnreadMessages() {
        Message msg = new Message("text1", /* timestamp= */ 1, /* person= */ null)
                .setMessageStatus(MessageStatus.MESSAGE_STATUS_READ);
        Message msg2 = new Message("text2", /* timestamp= */ 2, /* person= */ null)
                .setMessageStatus(MessageStatus.MESSAGE_STATUS_UNREAD);
        Message msg3 = new Message("text3", /* timestamp= */ 3, /* person= */ null)
                .setMessageStatus(MessageStatus.MESSAGE_STATUS_UNREAD);

        List<Message> messages = MessageUtils.getUnreadMessages(Arrays.asList(msg2, msg3, msg));

        assertThat(messages).containsExactly(msg3, msg2).inOrder();
    }

    private MmsSmsMessage createMessage(
            String id, int subId, long timestamp, String body, int type, boolean isRead) {
        return new MmsSmsMessage.Builder()
                .setId(id)
                .setSubscriptionId(subId)
                .setThreadId(Integer.parseInt(id))
                .setType(type)
                .setRead(isRead)
                .setDate(Instant.ofEpochMilli(timestamp))
                .setBody(body)
                .build();
    }
}
