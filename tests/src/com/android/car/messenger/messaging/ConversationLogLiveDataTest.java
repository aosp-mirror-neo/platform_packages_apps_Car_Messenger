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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Observer;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.messenger.AppFactoryTestImpl;
import com.android.car.messenger.messaging.utils.ConversationFetchUtil;
import com.android.car.messenger.messaging.utils.CursorUtils;
import com.android.car.messenger.messaging.utils.MmsSmsMessage;
import com.android.car.testing.common.InstantTaskExecutorRule;

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

import java.util.HashMap;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ConversationLogLiveDataTest {

    private static final String CONV_ID = "123";
    private static final String CONV_ID2 = "456";
    private static final int SUB_ID = 22;
    private static final int SUB_ID2 = 33;

    private ConversationLogLiveData mConversationLogLiveData;
    private AppFactoryTestImpl mAppFactory;

    /** Used to execute livedata.postValue() synchronously */
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    private LifecycleRegistry mLifecycleRegistry;
    @Mock
    private LifecycleOwner mMockLifecycleOwner;
    @Mock
    private Observer<HashMap<Integer, HashMap<String, List<MmsSmsMessage>>>> mMockObserver;
    private Context mContext;
    @Mock
    private ContentResolver mMockContentResolver;
    @Mock
    private Cursor mMockCursor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mAppFactory = new AppFactoryTestImpl(mContext, null, null, null);

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
        when(mMockCursor.moveToNext()).thenReturn(true, true, false);
        when(mMockCursor.getColumnIndex(anyString())).thenReturn(1);
        when(mMockCursor.getString(anyInt())).thenReturn(CONV_ID, CONV_ID2);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(CursorUtils.class)
                .spyStatic(ConversationFetchUtil.class)
                .startMocking();
        try {
            HashMap<Integer, List<MmsSmsMessage>> convList = new HashMap<>();
            List<MmsSmsMessage> c1s1 = List.of(createMessage("1"), createMessage("2"));
            List<MmsSmsMessage> c1s2 = List.of(createMessage("3"), createMessage("4"));
            convList.put(SUB_ID, c1s1);
            convList.put(SUB_ID2, c1s2);

            HashMap<Integer, List<MmsSmsMessage>> convList2 = new HashMap<>();
            List<MmsSmsMessage> c2s1 = List.of(createMessage("5"), createMessage("6"));
            List<MmsSmsMessage> c2s2 = List.of(createMessage("7"), createMessage("8"));
            convList2.put(SUB_ID, c2s1);
            convList2.put(SUB_ID2, c2s2);

            doReturn(mMockCursor).when(CursorUtils::getConversationsCursor);
            doReturn(convList).when(() -> ConversationFetchUtil.fetchConversationThread(CONV_ID));
            doReturn(convList2).when(() -> ConversationFetchUtil.fetchConversationThread(CONV_ID2));

            mConversationLogLiveData = ConversationLogLiveData.getInstance();
            mConversationLogLiveData.observe(mMockLifecycleOwner,
                    (value) -> mMockObserver.onChanged(value));
            assertThat(mConversationLogLiveData.getValue()).isNull();
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

            // verify live data outputs messages indexed by subscription id and conversation id
            HashMap<Integer, HashMap<String, List<MmsSmsMessage>>> conversationLog =
                    mConversationLogLiveData.getValue();

            assertThat(conversationLog).hasSize(2);
            assertThat(conversationLog.get(SUB_ID)).hasSize(2);
            assertThat(conversationLog.get(SUB_ID).get(CONV_ID)).isEqualTo(c1s1);
            assertThat(conversationLog.get(SUB_ID).get(CONV_ID2)).isEqualTo(c2s1);
            assertThat(conversationLog.get(SUB_ID2)).hasSize(2);
            assertThat(conversationLog.get(SUB_ID2).get(CONV_ID)).isEqualTo(c1s2);
            assertThat(conversationLog.get(SUB_ID2).get(CONV_ID2)).isEqualTo(c2s2);
        } finally {
            session.finishMocking();
        }
    }

    private MmsSmsMessage createMessage(String id) {
        return new MmsSmsMessage.Builder().setId(id).build();
    }
}
