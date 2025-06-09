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

package com.android.car.messenger.messaging;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.core.app.Person;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Observer;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.messenger.AppFactoryTestImpl;
import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.bluetooth.UserAccountListLiveData;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.messaging.utils.CursorUtils;
import com.android.car.messenger.messaging.utils.MmsSmsMessage;
import com.android.car.messenger.util.CarStateListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class NewMessageLiveDataTest {

    private static final int USER_ACCOUNT_ID = 0;

    private NewMessageLiveData mNewMessageLiveData;
    private AppFactoryTestImpl mAppFactory;

    /** Used to execute livedata.postValue() synchronously */
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    private LifecycleRegistry mLifecycleRegistry;
    @Mock
    private LifecycleOwner mMockLifecycleOwner;
    @Mock
    private Observer<Conversation> mMockObserver;
    private Context mContext;
    @Mock
    private UserAccount mMockUserAccount;
    @Mock
    private UserAccountListLiveData mMockUserAccountListLiveData;
    @Mock
    private CarStateListener mMockCarStateListener;
    @Mock
    private TelephonyDataModel mDataModel;
    @Mock
    private InMemoryConversationLog mInMemoryConversationLog;
    private ArrayList<UserAccount> mUserAccountList;

    private static final String MESSAGE_ID = "123";
    private static final String CONVERSATION_ID = "0";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mAppFactory = new AppFactoryTestImpl(mContext, mDataModel, null, mMockCarStateListener);

        when(mMockUserAccount.getId()).thenReturn(USER_ACCOUNT_ID);
        when(mMockUserAccount.getConnectionTime()).thenReturn(Instant.ofEpochMilli(0));

        mLifecycleRegistry = new LifecycleRegistry(mMockLifecycleOwner);
        when(mMockLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);

        mUserAccountList = new ArrayList<>();
        mUserAccountList.add(mMockUserAccount);
    }

    @After
    public void teardown() {
        mAppFactory.teardown();
    }

    @Test
    @UiThreadTest
    public void testOnDataChanged() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(InMemoryConversationLog.class)
                .spyStatic(UserAccountListLiveData.class)
                .startMocking();
        try {
            HashMap<Integer, MmsSmsMessage> unseenIndex = new HashMap<>();
            MmsSmsMessage msg = new MmsSmsMessage.Builder()
                    .setId(MESSAGE_ID)
                    .setThreadId(Long.parseLong(CONVERSATION_ID))
                    .setContentType(1)
                    .build();
            unseenIndex.put(USER_ACCOUNT_ID, msg);
            Conversation conversation = new Conversation.Builder(
                    new Person.Builder().build(), CONVERSATION_ID).build();

            doReturn(mInMemoryConversationLog).when(InMemoryConversationLog::get);
            when(mInMemoryConversationLog.getConversation(
                    USER_ACCOUNT_ID, CONVERSATION_ID)).thenReturn(conversation);
            when(mInMemoryConversationLog.getUnseenConversationIndex()).thenReturn(unseenIndex);
            doReturn(mMockUserAccountListLiveData).when(UserAccountListLiveData::getInstance);

            mNewMessageLiveData = new NewMessageLiveData();
            mNewMessageLiveData.mUserAccounts = mUserAccountList;
            mNewMessageLiveData.observe(mMockLifecycleOwner, v -> mMockObserver.onChanged(v));
            assertThat(mNewMessageLiveData.getValue()).isNull();
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            assertThat(mNewMessageLiveData.getValue()).isEqualTo(conversation);

            verify(mDataModel, never()).markAsSeen(MESSAGE_ID, CursorUtils.ContentType.SMS);
            verify(mDataModel).markAsSeen(MESSAGE_ID, CursorUtils.ContentType.MMS);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testOnDataChanged_hasProjection() {
        when(mMockCarStateListener.isProjectionInActiveForeground(any())).thenReturn(true);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(InMemoryConversationLog.class)
                .spyStatic(UserAccountListLiveData.class)
                .startMocking();
        try {
            doReturn(mMockUserAccountListLiveData).when(UserAccountListLiveData::getInstance);
            doReturn(mInMemoryConversationLog).when(InMemoryConversationLog::get);
            mNewMessageLiveData = new NewMessageLiveData();
            mNewMessageLiveData.mUserAccounts = mUserAccountList;
            assertThat(mNewMessageLiveData.getValue()).isNull();
            mNewMessageLiveData.onConversationLogChanged();
            assertThat(mNewMessageLiveData.getValue()).isNull();
        } finally {
            session.finishMocking();
        }
    }
}
