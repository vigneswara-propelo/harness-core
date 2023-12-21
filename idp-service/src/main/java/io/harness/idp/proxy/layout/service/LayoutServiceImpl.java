/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
package io.harness.idp.proxy.layout.service;

import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.PersistenceUtils.DEFAULT_RETRY_POLICY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.proxy.layout.beans.entity.LayoutEntity;
import io.harness.idp.proxy.layout.events.LayoutCreateEvent;
import io.harness.idp.proxy.layout.events.LayoutUpdateEvent;
import io.harness.idp.proxy.layout.mappers.LayoutMapper;
import io.harness.idp.proxy.layout.repositories.LayoutRepository;
import io.harness.outbox.api.OutboxService;
import io.harness.spec.server.idp.v1.model.LayoutRequest;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.IDP)
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @com.google.inject.Inject }))
public class LayoutServiceImpl implements LayoutService {
  LayoutRepository layoutsRepository;

  @Inject @Named(OUTBOX_TRANSACTION_TEMPLATE) private TransactionTemplate transactionTemplate;
  @Inject private OutboxService outboxService;
  private static final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_RETRY_POLICY;

  @Override
  public void saveOrUpdateLayouts(LayoutRequest layoutRequest, String accountIdentifier) {
    Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      LayoutEntity existingLayoutEntity = layoutsRepository.findByAccountIdentifierAndNameAndType(
          accountIdentifier, layoutRequest.getName(), layoutRequest.getType());

      LayoutRequest existingLayoutDTO = new LayoutRequest();
      LayoutEntity toSaveLayoutEntity = LayoutMapper.fromDTO(layoutRequest, accountIdentifier);

      if (existingLayoutEntity != null) {
        existingLayoutDTO = LayoutMapper.toDTO(existingLayoutEntity);
        toSaveLayoutEntity.setId(existingLayoutEntity.getId());
        toSaveLayoutEntity.setCreatedAt(existingLayoutEntity.getCreatedAt());
      }

      layoutsRepository.save(toSaveLayoutEntity);

      if (existingLayoutEntity == null) {
        outboxService.save(new LayoutCreateEvent(layoutRequest, accountIdentifier));
      } else {
        if (!existingLayoutEntity.getYaml().equals(layoutRequest.getYaml())) {
          outboxService.save(new LayoutUpdateEvent(layoutRequest, existingLayoutDTO, accountIdentifier));
        }
      }
      return true;
    }));
  }
}
