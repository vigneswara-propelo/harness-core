/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.services.impl;

import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.events.VariableCreateEvent;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.ng.core.variable.entity.Variable;
import io.harness.ng.core.variable.mappers.VariableMapper;
import io.harness.ng.core.variable.services.VariableService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.variable.spring.VariableRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.jodah.failsafe.Failsafe;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

public class VariableServiceImpl implements VariableService {
  private final VariableRepository variableRepository;
  private final VariableMapper variableMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;

  @Inject
  public VariableServiceImpl(VariableRepository variableRepository, VariableMapper variableMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService) {
    this.variableRepository = variableRepository;
    this.variableMapper = variableMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
  }

  @Override
  public Variable create(String accountIdentifier, VariableDTO variableDTO) {
    if (null == variableDTO.getVariableConfig()) {
      throw new InvalidRequestException("Variable config cannot be null");
    }
    variableDTO.getVariableConfig().validate();
    try {
      Variable variable = variableMapper.toVariable(accountIdentifier, variableDTO);
      return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
        Variable savedVariable = variableRepository.save(variable);
        outboxService.save(new VariableCreateEvent(accountIdentifier, variableMapper.writeDTO(savedVariable)));
        return savedVariable;
      }));
    } catch (DuplicateKeyException de) {
      throw new DuplicateFieldException(
          String.format(
              "A variable with identifier [%s] and orgIdentifier [%s] and projectIdentifier [%s] already present.",
              variableDTO.getIdentifier(), variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier()),
          USER_SRE, de);
    }
  }

  @Override
  public List<VariableDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<Variable> variables = variableRepository.findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier);
    return variables.stream().map(variableMapper::writeDTO).collect(Collectors.toList());
  }

  @Override
  public Optional<VariableResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<Variable> variable =
        variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return variable.map(variableMapper::toResponseWrapper);
  }
}
