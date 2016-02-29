/**
 * Copyright 2016 Myrle Krantz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mifos.module.stellar.listener;

import org.springframework.context.ApplicationEvent;

public class StellarPaymentEvent extends ApplicationEvent {
  private final Long stellarCursorId;
  private final String mifosTenantId;
  private final String assetCode;
  private final String amount;

  public StellarPaymentEvent(
      final Object source,
      final Long stellarCursorId,
      final String mifosTenantId,
      final String assetCode,
      final String amount) {
    super(source);
    this.stellarCursorId = stellarCursorId;
    this.mifosTenantId = mifosTenantId;
    this.assetCode = assetCode;
    this.amount = amount;
  }

  public Long getStellarCursorId() {
    return stellarCursorId;
  }

  public String getMifosTenantId() {
    return mifosTenantId;
  }

  public String getAssetCode() {
    return assetCode;
  }

  public String getAmount() {
    return amount;
  }
}