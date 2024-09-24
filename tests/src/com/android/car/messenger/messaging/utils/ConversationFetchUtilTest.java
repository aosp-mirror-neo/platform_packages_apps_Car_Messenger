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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.Person;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.messenger.AppFactoryTestImpl;
import com.android.car.messenger.MessageConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class ConversationFetchUtilTest {

    private static final String CONVERSATION_ID = "0";
    private static final int SUBSCRIPTION_ID = 5;
    private static final int SUBSCRIPTION_ID2 = 6;
    private static final String TEST_CONTACT_ID = "TEST_CONTACT_1";
    private static final String TEST_CONTACT_ID2 = "TEST_CONTACT_2";

    private AppFactoryTestImpl mAppFactory;
    private Context mContext;
    @Mock
    private SharedPreferences mMockSharedPreferences;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mAppFactory = new AppFactoryTestImpl(
                mContext, /* dataModel= */ null, mMockSharedPreferences, /* listener= */null);
    }

    @After
    public void teardown() {
        mAppFactory.teardown();
    }

    @Test
    public void testFetchConversation_noMessages() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(CursorUtils.class)
                .spyStatic(MessageUtils.class)
                .spyStatic(ContactUtils.class)
                .startMocking();

        List<MmsSmsMessage> messages = new ArrayList<>();

        try {
            doReturn(messages).when(() -> MessageUtils.getRawMessages(any()));

            HashMap<Integer, List<MmsSmsMessage>> map =
                    ConversationFetchUtil.fetchConversationThread(CONVERSATION_ID);
            assertThat(map).isEmpty();
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testFetchConversation_multiDevice() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(CursorUtils.class)
                .spyStatic(MessageUtils.class)
                .spyStatic(ContactUtils.class)
                .startMocking();

        MmsSmsMessage msg1 = createMessage("0", SUBSCRIPTION_ID);
        MmsSmsMessage msg2 = createMessage("1", SUBSCRIPTION_ID2);
        MmsSmsMessage msg3 = createMessage("2", SUBSCRIPTION_ID2);
        MmsSmsMessage msg4 = createMessage("3", SUBSCRIPTION_ID);

        List<MmsSmsMessage> messages = List.of(msg1, msg2, msg3, msg4);

        try {
            doReturn(messages).when(() -> MessageUtils.getRawMessages(any()));

            HashMap<Integer, List<MmsSmsMessage>> map =
                    ConversationFetchUtil.fetchConversationThread(CONVERSATION_ID);
            assertThat(map).hasSize(2);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testLoadMutedLists() {
        Set<String> mutedList = new HashSet<>();
        mutedList.add(TEST_CONTACT_ID);
        mutedList.add(TEST_CONTACT_ID2);

        when(mMockSharedPreferences.getStringSet(
                eq(MessageConstants.KEY_MUTED_CONVERSATIONS), any())).thenReturn(mutedList);

        assertThat(ConversationFetchUtil.loadMutedList()).isEqualTo(mutedList);
    }

    private void setupFetch(Person person) {
        doReturn(Arrays.asList(person)).when(
                () -> ContactUtils.getRecipients(anyString(), any(), any()));
        doReturn(null).when(() -> CursorUtils.getMessagesCursor(
                anyString(), eq(CursorUtils.ContentType.MMS)));
        doReturn(null).when(() -> CursorUtils.getMessagesCursor(
                anyString(), eq(CursorUtils.ContentType.SMS)));
    }

    private MmsSmsMessage createMessage(String msgId, int subId) {
        return new MmsSmsMessage.Builder()
                .setId(msgId)
                .setSubscriptionId(subId)
                .build();
    }
}
