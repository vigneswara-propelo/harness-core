package io.harness.ccm.commons.beans.recommendation.models;

import io.harness.ccm.commons.beans.recommendation.TotalResourceUsage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendNodePoolClusterRequest {
  RecommendClusterRequest recommendClusterRequest;
  TotalResourceUsage totalResourceUsage;
}
