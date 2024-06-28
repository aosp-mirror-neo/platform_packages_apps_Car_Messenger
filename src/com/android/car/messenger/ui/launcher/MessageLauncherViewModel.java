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

package com.android.car.messenger.ui.launcher;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.interfaces.AppFactory;
import com.android.car.messenger.interfaces.DataModel;

import java.util.List;

/** View model for MessageLauncherActivity */
public class MessageLauncherViewModel extends AndroidViewModel {
    @NonNull private final DataModel mDataSource;

    public MessageLauncherViewModel(@NonNull Application application) {
        super(application);
        mDataSource = AppFactory.get().getDataModel();
    }

    @NonNull
    public LiveData<UserAccount> getCurrentAccount() {
        return mDataSource.getCurrentAccount();
    }

    /** Get observable data with list of accounts/user accounts */
    @NonNull
    public LiveData<List<UserAccount>> getAccounts() {
        return mDataSource.getAccounts();
    }

}
