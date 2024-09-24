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

import android.telephony.SubscriptionInfo;

import java.time.Instant;

/** MmsSmsMessage to hold metadata common to MMS and SMS Messages */
public class MmsSmsMessage {
    public MmsSmsMessage(
            String id,
            long threadId,
            String phoneNumber,
            String body,
            int type,
            int contentType,
            int subscriptionId,
            Instant date,
            boolean read,
            boolean seen) {
        mId = id;
        mThreadId = threadId;
        mPhoneNumber = phoneNumber;
        mBody = body;
        mType = type;
        mContentType = contentType;
        mSubscriptionId = subscriptionId;
        mDate = date;
        mRead = read;
        mSeen = seen;
    }

    private final String mId;
    private final long mThreadId;
    private final String mPhoneNumber;
    private final String mBody;
    private final int mType; // e.g. Inbox/Outbox
    private final int mContentType; // e.g. SMS/MMS
    private final int mSubscriptionId;
    private final Instant mDate;
    private final boolean mRead;
    private final boolean mSeen;

    /** Get message seen status */
    public boolean isSeen() {
        return mSeen;
    }

    /** Get message read status */
    public boolean isRead() {
        return mRead;
    }

    /** Get message date */
    public Instant getDate() {
        return mDate;
    }

    /** Get device {@link SubscriptionInfo#getSubscriptionId()} */
    public int getSubscriptionId() {
        return mSubscriptionId;
    }

    /** Get the content type. e.g. Sms, Mms, Rcs */
    public int getContentType() {
        return mContentType;
    }

    /** Get the box type. e.g. Inbox, Outbox */
    public int getType() {
        return mType;
    }

    /** Get the message body */
    public String getBody() {
        return mBody;
    }

    /** Get the phone number */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    /** Get the conversation/thread id */
    public long getThreadId() {
        return mThreadId;
    }

    /** Get the message id */
    public String getId() {
        return mId;
    }

    /** Get the timestamp in milliseconds */
    public long getTimestamp() {
        return mDate.toEpochMilli();
    }

    /**
     * Builder class to create a MmsSmsMessage
     */
    public static class Builder {
        private String mId;
        private long mThreadId;
        private String mPhoneNumber;
        private String mBody;
        private int mType; // e.g. Inbox/Outbox
        private int mContentType; // e.g. SMS/MMS
        private int mSubscriptionId;
        private Instant mDate;
        private boolean mRead;
        private boolean mSeen;

        /** Builds a MmsSmsMessage */
        public MmsSmsMessage build() {
            return new MmsSmsMessage(
                    mId,
                    mThreadId,
                    mPhoneNumber,
                    mBody,
                    mType,
                    mContentType,
                    mSubscriptionId,
                    mDate,
                    mRead,
                    mSeen
            );
        }

        /** Set the message id */
        public Builder setId(String id) {
            mId = id;
            return this;
        }

        /** Set the thread id */
        public Builder setThreadId(long threadId) {
            mThreadId = threadId;
            return this;
        }

        /** Set the phone number */
        public Builder setPhoneNumber(String phoneNumber) {
            mPhoneNumber = phoneNumber;
            return this;
        }

        /** Set the content body */
        public Builder setBody(String body) {
            mBody = body;
            return this;
        }

        /** Set the box type */
        public Builder setType(int type) {
            mType = type;
            return this;
        }

        /** Set the content type */
        public Builder setContentType(int contentType) {
            mContentType = contentType;
            return this;
        }

        /** Set the device id */
        public Builder setSubscriptionId(int subscriptionId) {
            mSubscriptionId = subscriptionId;
            return this;
        }

        /** Set the date */
        public Builder setDate(Instant date) {
            mDate = date;
            return this;
        }

        /** Set read status */
        public Builder setRead(boolean read) {
            mRead = read;
            return this;
        }

        /** Set seen status */
        public Builder setSeen(boolean seen) {
            mSeen = seen;
            return this;
        }
    }
}
