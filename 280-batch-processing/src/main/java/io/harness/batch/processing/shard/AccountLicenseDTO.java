package io.harness.batch.processing.shard;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountLicenseDTO {
  String accountIdentifier;
  Edition edition;
  LicenseType licenseType;
}
