/*
 * Copyright (C) 2020 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.hilt.android;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
// Robolectric requires Java9 to run API 29 and above, so use API 28 instead
@Config(sdk = Build.VERSION_CODES.P, application = HiltTestApplication.class)
public class DefaultViewModelFactoryTest {

  @Rule public final HiltAndroidRule rule = new HiltAndroidRule(this);

  @Test
  public void activityFactory() {
    try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
      scenario.onActivity(
          activity -> {
            assertThat(activity.getDefaultViewModelProviderFactory()).isNotNull();
            assertThat(activity.getDefaultViewModelProviderFactory())
                .isInstanceOf(HiltViewModelFactory.class);
          });
    }
  }

  @Test
  public void fragmentFactory() {
    // TODO(danysantiago): Use FragmentScenario when it becomes available.
    try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
      scenario.onActivity(
          activity -> {
            TestFragment fragment = new TestFragment();
            activity.getSupportFragmentManager().beginTransaction().add(fragment, "").commitNow();
            assertThat(fragment.getDefaultViewModelProviderFactory()).isNotNull();
            assertThat(fragment.getDefaultViewModelProviderFactory())
                .isInstanceOf(HiltViewModelFactory.class);
          });
    }
  }

  @AndroidEntryPoint(FragmentActivity.class)
  public static final class TestActivity extends Hilt_DefaultViewModelFactoryTest_TestActivity {}

  @AndroidEntryPoint(Fragment.class)
  public static final class TestFragment extends Hilt_DefaultViewModelFactoryTest_TestFragment {}
}
