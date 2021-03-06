/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.apps.exposurenotification.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.NestedScrollView.OnScrollChangeListener;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.apps.exposurenotification.R;
import com.google.android.apps.exposurenotification.home.ExposureNotificationViewModel;
import com.google.android.apps.exposurenotification.home.ExposureNotificationViewModel.ExposureNotificationState;
import com.google.android.apps.exposurenotification.home.HomeFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Consent dialog for the API not already enabled case.
 */
@AndroidEntryPoint
public class OnboardingPermissionDisabledFragment extends Fragment {

  private static final String SAVED_INSTANCE_STATE_SKIP_DIALOG_SHOWN =
      "OnboardingPermissionDisabledFragment.SAVED_INSTANCE_STATE_SKIP_DIALOG_SHOWN";

  private ExposureNotificationViewModel exposureNotificationViewModel;
  private OnboardingViewModel onboardingViewModel;

  private LinearLayout onboardingButtons;
  private Button nextButton;
  private NestedScrollView scroller;

  private boolean skipDialogShown = false;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_onboarding_permission_disabled, parent, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    getActivity().setTitle(R.string.onboarding_opt_in_title);

    exposureNotificationViewModel =
        new ViewModelProvider(requireActivity()).get(ExposureNotificationViewModel.class);
    onboardingViewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

    ViewSwitcher onboardingButtonsLoadingSwitcher =
        view.findViewById(R.id.onboarding_buttons_loading_switcher);
    SwitchMaterial analyticsSwitch = view.findViewById(R.id.onboarding_app_analytics_switch);
    TextView onboardingDetail = view.findViewById(R.id.onboarding_exposure_notifications_detail);

    onboardingDetail.setText(
        getString(R.string.onboarding_exposure_notifications_detail, getString(R.string.app_name)));

    exposureNotificationViewModel
        .getEnEnabledLiveData()
        .observe(
            getViewLifecycleOwner(),
            isEnabled -> {
              if (isEnabled) {
                onboardingViewModel.setAppAnalyticsState(analyticsSwitch.isChecked());
                onboardingButtonsLoadingSwitcher.setDisplayedChild(1);
                onboardingViewModel.setOnboardedState(true);
                HomeFragment.transitionToHomeFragment(this);
              } else {
                onboardingButtonsLoadingSwitcher.setDisplayedChild(0);
              }
            });

    exposureNotificationViewModel
        .getApiErrorLiveEvent()
        .observe(getViewLifecycleOwner(), unused -> {
          View rootView = getView();
          if (rootView != null) {
            Snackbar.make(rootView, R.string.generic_error_message, Snackbar.LENGTH_LONG).show();
          }
        });

    onboardingButtons = view.findViewById(R.id.onboarding_buttons);
    nextButton = view.findViewById(R.id.onboarding_next_button);
    scroller = view.findViewById(R.id.onboarding_scroll);

    updateAtBottom(false);
    scroller.setOnScrollChangeListener(
        (OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
          if (scroller.getChildAt(0).getBottom()
              <= (scroller.getHeight() + scroller.getScrollY())) {
            updateAtBottom(true);
          } else {
            updateAtBottom(false);
          }
        });
    ViewTreeObserver observer = scroller.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(() -> {
      if (scroller.getMeasuredHeight() >= scroller.getChildAt(0).getHeight()) {
        // Not scrollable so set at bottom.
        updateAtBottom(true);
      }
    });

    Button dismissButton = view.findViewById(R.id.onboarding_no_thanks_button);
    dismissButton.setOnClickListener(v -> skipOnboarding());
    if (savedInstanceState != null) {
      if (savedInstanceState.getBoolean(SAVED_INSTANCE_STATE_SKIP_DIALOG_SHOWN, false)) {
        skipOnboarding();
      }
    }

    ProgressBar progressBar = view.findViewById(R.id.onboarding_progress_bar);

    exposureNotificationViewModel
        .getInFlightLiveData()
        .observe(getViewLifecycleOwner(), inFlight -> {
          nextButton.setEnabled(!inFlight);
          dismissButton.setEnabled(!inFlight);
          progressBar.setVisibility(inFlight ? View.VISIBLE : View.INVISIBLE);
          nextButton.setVisibility(inFlight ? View.INVISIBLE : View.VISIBLE);
        });

  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(SAVED_INSTANCE_STATE_SKIP_DIALOG_SHOWN, skipDialogShown);
  }

  /**
   * Update the UI depending on whether scrolling is at the bottom or not.
   */
  private void updateAtBottom(boolean atBottom) {
    if (atBottom) {
      nextButton.setText(R.string.btn_turn_on);
      nextButton.setOnClickListener(v2 -> nextAction());
      onboardingButtons.setElevation(0F);
    } else {
      nextButton.setText(R.string.btn_continue);
      nextButton.setOnClickListener(v2 -> scroller.fullScroll(View.FOCUS_DOWN));
      onboardingButtons
          .setElevation(getResources().getDimension(R.dimen.onboarding_button_elevation));
    }
    if (nextButton.isAccessibilityFocused()) {
      // Let accessibility service announce when button text change.
      nextButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }
  }

  private void skipOnboarding() {
    if (skipDialogShown) {
      return;
    }
    skipDialogShown = true;
    new MaterialAlertDialogBuilder(getContext())
        .setTitle(R.string.onboarding_confirm_later_title)
        .setMessage(R.string.onboarding_confirm_later_detail)
        .setCancelable(true)
        .setNegativeButton(R.string.btn_no_go_back, (dialog, i) -> {
          skipDialogShown = false;
          dialog.cancel();
        })
        .setPositiveButton(R.string.btn_yes_continue, (dialog, i) -> {
          skipDialogShown = false;
          onboardingViewModel.setOnboardedState(false);
          HomeFragment.transitionToHomeFragment(this);
        })
        .setOnCancelListener((dialog) -> skipDialogShown = false).show();
  }

  private void nextAction() {
    exposureNotificationViewModel.startExposureNotifications();
  }

}
