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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

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
import com.android.car.messenger.common.Conversation;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ConversationListLiveDataTest {

    private ConversationListLiveData mConversationListLiveData;
    private AppFactoryTestImpl mAppFactory;

    /** Used to execute livedata.postValue() synchronously */
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    private LifecycleRegistry mLifecycleRegistry;
    @Mock
    private UserAccount mMockUserAccount;
    @Mock
    private LifecycleOwner mMockLifecycleOwner;
    @Mock
    private Observer<Collection<Conversation>> mMockObserver;
    private Context mContext;
    @Mock
    private ContentResolver mMockContentResolver;
    @Mock
    private SharedPreferences mMockSharedPreferences;
    @Mock
    InMemoryConversationLog mInMemoryConversationLog;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mAppFactory = new AppFactoryTestImpl(mContext, null, mMockSharedPreferences, null);

        when(mMockUserAccount.getId()).thenReturn(0);
        mLifecycleRegistry = new LifecycleRegistry(mMockLifecycleOwner);
        when(mMockLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    @After
    public void teardown() {
        mAppFactory.teardown();
    }

    @Test
    @UiThreadTest
    public void testOnDataChanged() {
        when(mContext.getContentResolver()).thenReturn(mMockContentResolver);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(InMemoryConversationLog.class)
                .startMocking();
        try {
            Conversation conv1 = buildConversation(/* id= */ "1", /* timestamp */ 300);
            Conversation conv2 = buildConversation(/* id= */ "2", /* timestamp */ 100);
            Conversation conv3 = buildConversation(/* id= */ "3", /* timestamp */ 200);
            HashMap<String, Conversation> conversationMap = new HashMap<>();
            conversationMap.put("1", conv1);
            conversationMap.put("2", conv2);
            conversationMap.put("3", conv3);

            doReturn(mInMemoryConversationLog).when(InMemoryConversationLog::get);
            when(mInMemoryConversationLog.getConversationList(anyInt()))
                    .thenReturn(conversationMap);

            mConversationListLiveData = new ConversationListLiveData(mMockUserAccount);
            mConversationListLiveData.observe(mMockLifecycleOwner,
                    (value) -> mMockObserver.onChanged(value));
            assertThat(mConversationListLiveData.getValue()).isNull();
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

            // verify results are sorted by timestamp desc
            assertThat(mConversationListLiveData.getValue())
                    .containsExactly(conv1, conv3, conv2).inOrder();
        } finally {
            session.finishMocking();
        }
    }

    private Conversation buildConversation(String id, long timestamp) {
        Person person = mock(Person.class);
        Conversation.Message message = new Conversation.Message("", timestamp, person);
        List<Conversation.Message> messages = Arrays.asList(message);

        return new Conversation.Builder(person, id)
                .setMessages(messages)
                .build();
    }
}
