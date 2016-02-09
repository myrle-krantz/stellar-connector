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
package org.mifos.module.stellar.controller;

import org.mifos.module.stellar.federation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FederationServerController {
  private final LocalFederationService federationService;

  @Autowired
  public FederationServerController(
      final LocalFederationService federationService)
  {
    this.federationService = federationService;
  }

  @RequestMapping(value = "/federation", method = RequestMethod.GET,
      produces = {"application/json"})
  public ResponseEntity<FederationResponse> getId(
      @RequestParam("type") final String type,
      @RequestParam("q") final String nameToLookUp) throws InvalidStellarAddressException {

    if (!type.equalsIgnoreCase("name"))
    {
      return new ResponseEntity<>(FederationResponse.invalidType(type), HttpStatus.NOT_IMPLEMENTED);
    }

    final StellarAddress stellarAddress = StellarAddress.parse(nameToLookUp);


    final StellarAccountId accountId = federationService.getAccountId(stellarAddress);

    final FederationResponse ret;
    if (accountId.getSubAccount().isPresent()) {
      ret = FederationResponse
          .accountInMemoField(stellarAddress.toString(), accountId.getPublicKey(),
              accountId.getSubAccount().get());
    }
    else
    {
      ret = FederationResponse.account(stellarAddress.toString(), accountId.getPublicKey());
    }

    return new ResponseEntity<>(ret, HttpStatus.OK);
  }


  @ExceptionHandler
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void handleInvalidNameFormatException(
      @SuppressWarnings("unused") final InvalidStellarAddressException ex) {
    //TODO: Improve error output.
  }


  @ExceptionHandler
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void handleFederationFailedException(
      @SuppressWarnings("unused") final FederationFailedException ex) {
    //TODO: Improve error output.
  }
}