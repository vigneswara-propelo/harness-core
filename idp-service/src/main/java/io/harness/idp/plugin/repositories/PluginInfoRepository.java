/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.plugin.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.plugin.entities.PluginInfoEntity;
import io.harness.spec.server.idp.v1.model.PluginInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.IDP)
public interface PluginInfoRepository extends CrudRepository<PluginInfoEntity, String>, PluginInfoRepositoryCustom {
  Optional<PluginInfoEntity> findByIdentifier(String identifier);
  Optional<PluginInfoEntity> findByIdentifierAndAccountIdentifierIn(String identifier, Set<String> accountIdentifiers);
  List<PluginInfoEntity> findByIdentifierInAndAccountIdentifierOrTypeAndAccountIdentifier(List<String> identifiers,
      String globalAccountIdentifier, PluginInfo.PluginTypeEnum type, String accountIdentifier);
}
