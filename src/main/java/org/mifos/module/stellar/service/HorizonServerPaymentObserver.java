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
package org.mifos.module.stellar.service;

import org.mifos.module.stellar.federation.StellarAccountId;
import org.mifos.module.stellar.persistencedomain.StellarCursorPersistency;
import org.mifos.module.stellar.repository.AccountBridgeRepository;
import org.mifos.module.stellar.repository.StellarCursorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.base.KeyPair;
import org.stellar.sdk.requests.PaymentsRequestBuilder;

import javax.annotation.PostConstruct;
import java.net.URI;

@Component
public class HorizonServerPaymentObserver {

  @Value("${stellar.horizon-address}")
  private String serverAddress;

  private final AccountBridgeRepository accountBridgeRepository;
  private final StellarCursorRepository stellarCursorRepository;
  private final HorizonServerPaymentListener listener;

  @PostConstruct
  void init()
  {
    final String cursor = getCurrentCursor();

    accountBridgeRepository.findAll()
        .forEach(bridge -> setupListeningForAccount(
                    StellarAccountId.mainAccount(bridge.getStellarAccountId()), cursor));
  }

  @Autowired
  HorizonServerPaymentObserver(
      final AccountBridgeRepository accountBridgeRepository,
      final StellarCursorRepository stellarCursorRepository,
      final HorizonServerPaymentListener listener)
  {
    this.accountBridgeRepository = accountBridgeRepository;
    this.stellarCursorRepository = stellarCursorRepository;

    this.listener = listener;
  }

  void setupListeningForAccount(final StellarAccountId stellarAccountId)
  {
    setupListeningForAccount(stellarAccountId, null);
  }

  private String getCurrentCursor() {
    final StellarCursorPersistency cursorPersistency
        = stellarCursorRepository.findByProcessedTrueOrderByIdDesc();
    if (cursorPersistency == null)
      return null;
    else
      return cursorPersistency.getCursor();
  }

  private void setupListeningForAccount(
      final StellarAccountId stellarAccountId, final String cursor)
  {
    final PaymentsRequestBuilder paymentsRequestBuilder
        = new PaymentsRequestBuilder(URI.create(serverAddress));
    paymentsRequestBuilder.forAccount(KeyPair.fromAccountId(stellarAccountId.getPublicKey()));
    if (cursor != null)
      paymentsRequestBuilder.cursor(cursor);

    paymentsRequestBuilder.stream(listener);
  }
}