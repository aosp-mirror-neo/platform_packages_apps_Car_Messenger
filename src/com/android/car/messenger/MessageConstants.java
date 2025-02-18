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

package com.android.car.messenger;

/** Constants. */
public final class MessageConstants {

    private MessageConstants() {}

    /** See {@link android.content.res.Resources#getIdentifier(String, String, String)} */
    public static final int INVALID_RES_ID = 0;

    /**
     * The key in Default Shared Preferences that maps to a list of conversation ids that are muted
     */
    public static final String KEY_MUTED_CONVERSATIONS = "KEY_MUTED_CONVERSATIONS";

    /** Used to reply to message. */
    public static final String ACTION_REPLY = "ACTION_REPLY";

    /** Used to mark a conversation as read */
    public static final String ACTION_MARK_AS_READ = "ACTION_MARK_AS_READ";

    /** Used to direct send to a specified phone number */
    public static final String ACTION_DIRECT_SEND = "ACTION_DIRECT_SEND";

    /** Used to mute a conversation */
    public static final String ACTION_MUTE = "ACTION_MUTE";

    /** Used to receive success and failure results of sent messages */
    public static final String ACTION_MESSAGE_SENT = "ACTION_MESSAGE_SENT";

    /** Used to receive success and failure results of the delivery of a sent message */
    public static final String ACTION_MESSAGE_DELIVERED = "ACTION_MESSAGE_DELIVERED";

    /* EXTRAS */
    /** Key under which the a Conversation Key is provided. */
    public static final String EXTRA_CONVERSATION_KEY = "EXTRA_CONVERSATION_KEY";

    /** Key under which the user account/device id is provided. */
    public static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";
}
