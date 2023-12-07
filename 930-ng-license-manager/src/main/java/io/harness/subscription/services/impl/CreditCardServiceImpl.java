/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.subscription.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.account.AccountClient;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.manage.GlobalContextManager;
import io.harness.remote.client.CGRestUtils;
import io.harness.repositories.CreditCardRepository;
import io.harness.repositories.StripeCustomerRepository;
import io.harness.security.SourcePrincipalContextData;
import io.harness.subscription.dto.CardDTO;
import io.harness.subscription.dto.CreditCardDTO;
import io.harness.subscription.dto.PaymentMethodCollectionDTO;
import io.harness.subscription.entities.CreditCard;
import io.harness.subscription.entities.StripeCustomer;
import io.harness.subscription.helpers.StripeHelper;
import io.harness.subscription.params.CustomerParams;
import io.harness.subscription.responses.CreditCardResponse;
import io.harness.subscription.services.CreditCardService;

import com.google.inject.Inject;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreditCardServiceImpl implements CreditCardService {
  private final AccountClient accountClient;
  private final CreditCardRepository creditCardRepository;
  private final StripeCustomerRepository stripeCustomerRepository;
  private final StripeHelper stripeHelper;

  private final String SAVE_CARD_FAILED = "Could not save credit card as it is already in use in Harness";
  private final String CUSTOMER_DOES_NOT_EXIST = "Customer with account identifier %s does not exist.";
  private final String PRIMARY_CARD_NOT_FOUND = "No primary credit card found for account %s";
  private final String NO_CARDS_FOUND = "No credit cards found for customerId [%s] and account [%s]";
  private final String CARD_NOT_FOUND = "No card found with identifier [%s]";
  private final String CARD_EXPIRED = "Card with identifier [%s] has expired";

  @Inject
  public CreditCardServiceImpl(AccountClient accountClient, CreditCardRepository creditCardRepository,
      StripeCustomerRepository stripeCustomerRepository, StripeHelper stripeHelper) {
    this.accountClient = accountClient;
    this.creditCardRepository = creditCardRepository;
    this.stripeCustomerRepository = stripeCustomerRepository;
    this.stripeHelper = stripeHelper;
  }

  @Override
  public CreditCardResponse saveCreditCard(CreditCardDTO creditCardDTO) {
    StripeCustomer stripeCustomer = getStripeCustomer(creditCardDTO.getAccountIdentifier());
    PaymentMethodCollectionDTO paymentMethodCollectionDTO =
        getPaymentMethods(stripeCustomer.getCustomerId(), creditCardDTO);
    CardDTO newDefaultCard = getNewDefaultCard(paymentMethodCollectionDTO, creditCardDTO);

    checkAndDetachIfCardAlreadyExists(newDefaultCard.getFingerPrint(), stripeCustomer.getCustomerId(), creditCardDTO);
    stripeHelper.updateCustomer(CustomerParams.builder()
                                    .customerId(stripeCustomer.getCustomerId())
                                    .defaultPaymentMethod(newDefaultCard.getId())
                                    .build());
    detachRedundantCardsAndCleanCreditCardCollection(paymentMethodCollectionDTO.getPaymentMethods(),
        creditCardDTO.getCreditCardIdentifier(), stripeCustomer.getCustomerId());

    return toCreditCardResponse(creditCardRepository.save(CreditCard.builder()
                                                              .accountIdentifier(creditCardDTO.getAccountIdentifier())
                                                              .creditCardIdentifier(newDefaultCard.getId())
                                                              .fingerprint(newDefaultCard.getFingerPrint())
                                                              .build()));
  }

  @Override
  public CreditCardResponse deleteCreditCard(String accountIdentifier, String creditCardIdentifier) {
    SourcePrincipalContextData sourcePrincipalContextData =
        GlobalContextManager.get(SourcePrincipalContextData.SOURCE_PRINCIPAL);
    String userId = sourcePrincipalContextData.getPrincipal().getName();
    boolean isHarnessSupportEnabled = false;
    try {
      isHarnessSupportEnabled = CGRestUtils.getResponse(accountClient.isHarnessSupportUserId(userId));
    } catch (Exception e) {
      log.error("client call to cg-manager failed due to: ", e);
      throw e;
    }
    if (!isHarnessSupportEnabled) {
      String errorMessage = String.format(
          "User with ID %s is not authorized to delete credit cards. Only Harness support users can delete credit cards.",
          userId);
      log.error(errorMessage);
      throw new UnauthorizedException(errorMessage, WingsException.USER);
    }
    CreditCard creditCard = creditCardRepository.findByCreditCardIdentifier(creditCardIdentifier);
    if (creditCard == null) {
      String errorMessage = String.format("Could not find a credit card with identifier %s", creditCardIdentifier);
      log.error(errorMessage);
      throw new InvalidArgumentsException(errorMessage);
    }
    creditCardRepository.delete(creditCard);
    return toCreditCardResponse(creditCard);
  }

  @Override
  public boolean hasAtleastOneValidCreditCard(String accountIdentifier) {
    List<CardDTO> creditCards;

    try {
      creditCards = getCreditCards(accountIdentifier);
    } catch (Exception ex) {
      log.error("Could not fetch Stripe customer details for account {}", accountIdentifier, ex);
      return false;
    }

    return creditCards.stream().anyMatch(this::isValidCard);
  }

  @Override
  public boolean isValid(String accountIdentifier, String creditCardIdentifier) {
    List<CardDTO> creditCards;
    try {
      creditCards = getCreditCards(accountIdentifier);
    } catch (Exception ex) {
      log.error("Could not fetch Stripe customer details for account {}", accountIdentifier, ex);
      return false;
    }
    Optional<CardDTO> optionalCreditCard =
        creditCards.stream().filter(creditCard -> creditCard.getId().equals(creditCardIdentifier)).findFirst();

    return optionalCreditCard.isPresent() && isValidCard(optionalCreditCard.get());
  }

  @Override
  public CardDTO getDefaultCreditCard(String accountIdentifier) {
    StripeCustomer stripeCustomer = getStripeCustomer(accountIdentifier);

    PaymentMethodCollectionDTO paymentMethodCollectionDTO =
        stripeHelper.listPaymentMethods(stripeCustomer.getCustomerId());

    if (paymentMethodCollectionDTO == null) {
      String errorMessage = String.format(PRIMARY_CARD_NOT_FOUND, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidArgumentsException(errorMessage);
    }

    Optional<CardDTO> primaryCardDTO =
        paymentMethodCollectionDTO.getPaymentMethods().stream().filter(CardDTO::getIsDefaultCard).findFirst();
    if (primaryCardDTO.isEmpty()) {
      String errorMessage = String.format(PRIMARY_CARD_NOT_FOUND, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidArgumentsException(errorMessage);
    }

    return primaryCardDTO.get();
  }

  private CreditCardResponse toCreditCardResponse(CreditCard creditCard) {
    return CreditCardResponse.builder()
        .creditCardDTO(CreditCardDTO.builder()
                           .accountIdentifier(creditCard.getAccountIdentifier())
                           .creditCardIdentifier(creditCard.getCreditCardIdentifier())
                           .build())
        .createdAt(creditCard.getCreatedAt())
        .lastUpdatedAt(creditCard.getLastUpdatedAt())
        .build();
  }

  private List<CardDTO> getCreditCards(String accountIdentifier) {
    StripeCustomer stripeCustomer = getStripeCustomer(accountIdentifier);

    return stripeHelper.listPaymentMethods(stripeCustomer.getCustomerId()).getPaymentMethods();
  }

  private boolean isCreditCardExpired(CardDTO cardDTO) {
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

    return currentYear > cardDTO.getExpireYear()
        || (currentYear == cardDTO.getExpireYear() && currentMonth > cardDTO.getExpireMonth());
  }

  private StripeCustomer getStripeCustomer(String accountIdentifier) {
    StripeCustomer stripeCustomer = stripeCustomerRepository.findByAccountIdentifier(accountIdentifier);

    if (stripeCustomer == null) {
      String errorMessage = String.format(CUSTOMER_DOES_NOT_EXIST, accountIdentifier);
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }

    return stripeCustomer;
  }

  private void detachRedundantCardsAndCleanCreditCardCollection(
      List<CardDTO> existingCards, String creditCardIdentifier, String customerId) {
    existingCards.stream()
        .filter((CardDTO existingCard) -> !existingCard.getId().equals(creditCardIdentifier))
        .forEach((CardDTO existingCard) -> {
          stripeHelper.detachPaymentMethod(customerId, existingCard.getId());
          creditCardRepository.deleteByCreditCardIdentifier(existingCard.getId());
        });
  }

  private PaymentMethodCollectionDTO getPaymentMethods(String customerId, CreditCardDTO creditCardDTO) {
    PaymentMethodCollectionDTO paymentMethodCollectionDTO = stripeHelper.listPaymentMethods(customerId);

    if (isEmpty(paymentMethodCollectionDTO.getPaymentMethods())) {
      String errorMessage = String.format(NO_CARDS_FOUND, customerId, creditCardDTO.getAccountIdentifier());
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }

    return paymentMethodCollectionDTO;
  }

  private CardDTO getNewDefaultCard(
      PaymentMethodCollectionDTO paymentMethodCollectionDTO, CreditCardDTO creditCardDTO) {
    Optional<CardDTO> newDefaultPaymentMethod =
        paymentMethodCollectionDTO.getPaymentMethods()
            .stream()
            .filter((CardDTO existingCards) -> existingCards.getId().equals(creditCardDTO.getCreditCardIdentifier()))
            .findFirst();
    if (newDefaultPaymentMethod.isEmpty()) {
      String errorMessage = String.format(CARD_NOT_FOUND, creditCardDTO.getCreditCardIdentifier());
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }
    if (isCreditCardExpired(newDefaultPaymentMethod.get())) {
      String errorMessage = String.format(CARD_EXPIRED, creditCardDTO.getCreditCardIdentifier());
      log.error(errorMessage);
      throw new InvalidRequestException(errorMessage);
    }

    return newDefaultPaymentMethod.get();
  }

  private void checkAndDetachIfCardAlreadyExists(String fingerprint, String customerId, CreditCardDTO creditCardDTO) {
    CreditCard creditCard = creditCardRepository.findByFingerprint(fingerprint);

    if (creditCard != null && !creditCard.getAccountIdentifier().equals(creditCardDTO.getAccountIdentifier())) {
      stripeHelper.detachPaymentMethod(customerId, creditCardDTO.getCreditCardIdentifier());
      log.error(SAVE_CARD_FAILED);
      throw new InvalidRequestException(SAVE_CARD_FAILED);
    }
  }

  private boolean isValidCard(CardDTO cardDTO) {
    return doesCardExistInHarness(cardDTO) && !isCreditCardExpired(cardDTO);
  }

  private boolean doesCardExistInHarness(CardDTO cardDTO) {
    CreditCard creditCard = creditCardRepository.findByCreditCardIdentifier(cardDTO.getId());

    return creditCard != null;
  }
}
