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

package com.google.android.apps.exposurenotification.logging;

import androidx.annotation.Nullable;
import com.android.volley.VolleyError;
import com.google.android.apps.exposurenotification.proto.ApiCall.ApiCallType;
import com.google.android.apps.exposurenotification.proto.RpcCall.RpcCallType;
import com.google.android.apps.exposurenotification.proto.UiInteraction.EventType;
import com.google.android.apps.exposurenotification.proto.WorkManagerTask.WorkerTask;

/** Interface for analytics logger */
public interface AnalyticsLogger {

  /** Logs UI interaction */
  void logUiInteraction(EventType event);

  /** Logs successful completion of Worker task */
  void logWorkManagerTaskSuccess(WorkerTask workerTask);

  /** Logs failure of Worker task */
  void logWorkManagerTaskFailure(WorkerTask workerTask, Throwable t);

  /** Logs failure of ExposureNotification Api call */
  void logApiCallFailure(ApiCallType apiCallType, Exception exception);

  /** Logs successful call to ExposureNotification Api */
  void logApiCallSuccess(ApiCallType apiCallType);

  /** Logs successful RPC call */
  void logRpcCallSuccess(RpcCallType rpcCallType, int payloadSize);

  /** Logs failed RPC call with server error code */
  void logRpcCallFailure(RpcCallType rpcCallType, @Nullable VolleyError error);

  /** Send logs off device if permitted by app usage & performance sharing */
  void sendLoggingBatchIfEnabled();
}
