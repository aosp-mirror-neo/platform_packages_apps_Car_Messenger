/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.car.messenger.ui.error;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.car.apps.common.UxrButton;
import com.android.car.messenger.R;
import com.android.car.ui.core.CarUi;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;

/**
 * Error fragment shown when there is no BT MAP profile connected.
 */
public class BluetoothErrorFragment extends Fragment {
    @NonNull
    private static final String BLUETOOTH_SETTING_ACTION = "android.settings.BLUETOOTH_SETTINGS";

    @NonNull
    private static final String BLUETOOTH_SETTING_CATEGORY = "android.intent.category.DEFAULT";

    private ToolbarController mToolbar;
    private TextView mErrorText;
    private UxrButton mButton;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_view, container, false);
        mErrorText = view.findViewById(R.id.error_message);
        mButton = view.findViewById(R.id.error_action_button);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mToolbar = CarUi.getToolbar(requireActivity());
        // Null check for unit tests to pass
        if (mToolbar != null) {
            setupToolbar();
        }
        mErrorText.setText(R.string.bluetooth_disconnected);
        mButton.setText(R.string.connect_bluetooth_button_text);
        mButton.setOnClickListener(v -> handleBluetoothClick());
    }

    private void setupToolbar() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        mToolbar.setMenuItems(null);
        mToolbar.setTitle(R.string.app_name);
        mToolbar.setNavButtonMode(NavButtonMode.DISABLED);
        mToolbar.setLogo(ContextCompat.getDrawable(context, context.getApplicationInfo().icon));
    }

    private void handleBluetoothClick() {
        Intent launchIntent = new Intent();
        launchIntent.setAction(BLUETOOTH_SETTING_ACTION);
        launchIntent.addCategory(BLUETOOTH_SETTING_CATEGORY);
        startActivity(launchIntent);
    }

    /**
     * Get instance of BluetoothErrorFragment
     */
    public static BluetoothErrorFragment newInstance() {
        return new BluetoothErrorFragment();
    }

    /**
     * Get unique fragment tag for fragment loading data for user device
     */
    public static String getFragmentTag() {
        return BluetoothErrorFragment.class.getName();
    }
}
