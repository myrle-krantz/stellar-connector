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
package org.mifos.module.stellar;

import com.google.gson.Gson;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import org.junit.Assert;
import org.mifos.module.stellar.restdomain.*;
import org.springframework.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;

import static com.jayway.restassured.RestAssured.given;
import static org.mifos.module.stellar.AccountBalanceMatcher.balanceMatches;

public class StellarBridgeTestHelpers {
  public static final String API_KEY_HEADER_LABEL = "X-Stellar-Bridge-API-Key";
  public static final String TENANT_ID_HEADER_LABEL = "X-Mifos-Platform-TenantId";
  public static final String ENTITY_HEADER_LABEL = "X-Mifos-Entity";
  public static final String ENTITY_HEADER_VALUE = "JOURNALENTRY";
  public static final String ACTION_HEADER_LABEL = "X-Mifos-Action";
  public static final String ACTION_HEADER_VALUE = "CREATE";
  public static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
  public static final String TEST_ADDRESS_DOMAIN = "test.org";

  /**
   * @return the api key used when accessing the tenantName
   */
  public static String createAndDestroyBridge(final String tenantName, final Cleanup testCleanup)
  {
    final String apiKey = createBridge(tenantName);
    testCleanup.addStep(() -> deleteBridge(tenantName, apiKey));
    return apiKey;
  }

  private static String createBridge(final String tenantName) {
    final AccountBridgeConfiguration newAccount =
        new AccountBridgeConfiguration(tenantName, "token_" + tenantName);
    final Response creationResponse =
        given()
            .header(CONTENT_TYPE_HEADER)
            .body(newAccount)
            .post("/modules/stellar/bridge");

    creationResponse
        .then().assertThat().statusCode(HttpStatus.CREATED.value());

    return creationResponse.getBody().as(String.class, ObjectMapperType.GSON);
  }

  public static void deleteBridge(final String tenantName, final String apiKey)
  {
    final Response deletionResponse =
        given()
            .header(StellarBridgeTestHelpers.API_KEY_HEADER_LABEL, apiKey)
            .header(StellarBridgeTestHelpers.TENANT_ID_HEADER_LABEL, tenantName)
            .delete("/modules/stellar/bridge");

    deletionResponse
        .then().assertThat().statusCode(HttpStatus.OK.value());
  }


  public static void setVaultSize(
      final String tenantName,
      final String apiKey,
      final String assetCode,
      final BigDecimal balance)
  {
    final AmountConfiguration amount = new AmountConfiguration(balance);

    given()
        .header(CONTENT_TYPE_HEADER)
        .header(StellarBridgeTestHelpers.API_KEY_HEADER_LABEL, apiKey)
        .header(StellarBridgeTestHelpers.TENANT_ID_HEADER_LABEL, tenantName)
        .pathParameter("assetCode", assetCode)
        .body(amount)
        .put("/modules/stellar/bridge/vault/{assetCode}/")
        .then().assertThat().statusCode(HttpStatus.OK.value());
  }

  public static void checkVaultSize(
      final String tenantId,
      final String apiKey,
      final String assetCode,
      final BigDecimal balance) {
    given()
        .header(CONTENT_TYPE_HEADER)
        .header(StellarBridgeTestHelpers.API_KEY_HEADER_LABEL, apiKey)
        .header(StellarBridgeTestHelpers.TENANT_ID_HEADER_LABEL, tenantId)
        .pathParameter("assetCode", assetCode)
        .get("/modules/stellar/bridge/vault/{assetCode}/")
        .then().assertThat().statusCode(HttpStatus.OK.value())
        .content(balanceMatches(balance));
  }

  public static void createAndDestroyTrustLine(
      final String fromTenant, final String fromTenantApiKey,
      final String toStellarAddress, final String assetCode, final BigDecimal amount,
      final Cleanup testCleanup) {
    createTrustLine(fromTenant, fromTenantApiKey, toStellarAddress, assetCode, amount);
    testCleanup.addStep(
        () -> deleteTrustLine(
            fromTenant, fromTenantApiKey, toStellarAddress, assetCode));
  }

  private static void createTrustLine(
      final String fromTenant, final String fromTenantApiKey,
      final String toStellarAddress,
      final String assetCode,
      final BigDecimal amount) {
    final TrustLineConfiguration trustLine = new TrustLineConfiguration(amount);

    String issuer = "";
    try {
      issuer = URLEncoder.encode(toStellarAddress, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Assert.fail();
    }

    given().header(StellarBridgeTestHelpers.CONTENT_TYPE_HEADER)
        .header(StellarBridgeTestHelpers.API_KEY_HEADER_LABEL, fromTenantApiKey)
        .header(StellarBridgeTestHelpers.TENANT_ID_HEADER_LABEL, fromTenant)
        .pathParameter("assetCode", assetCode)
        .pathParameter("issuer", issuer)
        .body(trustLine)
        .put("/modules/stellar/bridge/trustlines/{assetCode}/{issuer}/")
        .then().assertThat().statusCode(HttpStatus.OK.value());
  }

  public static void deleteTrustLine(
      final String fromTenant,
      final String fromTenantApiKey,
      final String toStellarAddress,
      final String assetCode)
  {
    String issuer = "";
    try {
      issuer = URLEncoder.encode(toStellarAddress, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Assert.fail();
    }

    given().header(StellarBridgeTestHelpers.CONTENT_TYPE_HEADER)
        .header(StellarBridgeTestHelpers.API_KEY_HEADER_LABEL,fromTenantApiKey)
        .header(StellarBridgeTestHelpers.TENANT_ID_HEADER_LABEL, fromTenant)
        .pathParam("assetCode", assetCode)
        .pathParam("issuer", issuer)
        .body(new TrustLineConfiguration(BigDecimal.ZERO))
        .put("/modules/stellar/bridge/trustlines/{assetCode}/{issuer}/")
        .then().assertThat().statusCode(HttpStatus.OK.value());
  }

  public static void createSameCurrencyPassiveOffer(
      final String marketMakerTenantId, final String marketMakerTenantApiKey,
      final String assetCode, final BigDecimal maximumAmount,
      final String buyingIssuerAddress, final String sellingIssuerAddress)
  {
    final PassiveOfferData offer = new PassiveOfferData(
        assetCode, buyingIssuerAddress,
        assetCode, sellingIssuerAddress,
        maximumAmount, BigDecimal.ONE);

    given().header(CONTENT_TYPE_HEADER)
        .header(API_KEY_HEADER_LABEL, marketMakerTenantApiKey)
        .header(TENANT_ID_HEADER_LABEL, marketMakerTenantId)
        .body(offer)
        .post("/modules/stellar/bridge/market/")
        .then().assertThat().statusCode(HttpStatus.CREATED.value());
  }

  public static void makePayment(
      final String fromTenant,
      final String fromTenantApiKey,
      final String toTenant,
      final String assetCode,
      final BigDecimal transferAmount)
  {
    final String payment = getPaymentPayload(
        assetCode,
        transferAmount,
        TEST_ADDRESS_DOMAIN,
        toTenant);

    given().header(CONTENT_TYPE_HEADER)
        .header(API_KEY_HEADER_LABEL, fromTenantApiKey)
        .header(TENANT_ID_HEADER_LABEL, fromTenant)
        .header(ENTITY_HEADER_LABEL, ENTITY_HEADER_VALUE)
        .header(ACTION_HEADER_LABEL, ACTION_HEADER_VALUE)
        .body(payment)
        .post("/modules/stellar/bridge/payments/")
        .then().assertThat().statusCode(HttpStatus.CREATED.value());
  }

  public static void checkBalance(
      final String tenant,
      final String tenantApiKey,
      final String assetCode,
      final String issuingStellarAddress,
      final BigDecimal amount)
  {
    String issuer = "";
    try {
      issuer = URLEncoder.encode(issuingStellarAddress, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      Assert.fail();
    }

    given().header(CONTENT_TYPE_HEADER)
        .header(API_KEY_HEADER_LABEL, tenantApiKey)
        .header(TENANT_ID_HEADER_LABEL, tenant)
        .pathParam("assetCode", assetCode)
        .pathParam("issuer", issuer)
        .get("/modules/stellar/bridge/balances/{assetCode}/{issuer}/")
        .then().assertThat().statusCode(HttpStatus.OK.value())
        .content(balanceMatches(amount));
  }

  public static void waitForPaymentToComplete() throws InterruptedException {
    Thread.sleep(5000); //TODO: find a better way to determine when the payment is complete.
  }

  public static String tenantVaultStellarAddress(final String tenantId)
  {
    return tenantId + ":vault" + "*" + TEST_ADDRESS_DOMAIN;
  }

  static String getPaymentPayload(
      final String assetCode,
      final BigDecimal amount,
      final String toDomain,
      final String toTenant) {
    final JournalEntryData payment = new JournalEntryData();
    payment.currency = new CurrencyData();
    payment.currency.inMultiplesOf = 1;
    payment.currency.code = assetCode;
    payment.amount = amount;
    payment.transactionDetails = new TransactionDetailData();
    payment.transactionDetails.paymentDetails = new PaymentDetailData();
    payment.transactionDetails.paymentDetails.bankNumber = toDomain;
    payment.transactionDetails.paymentDetails.accountNumber = toTenant;

    return new Gson().toJson(payment);
  }
}