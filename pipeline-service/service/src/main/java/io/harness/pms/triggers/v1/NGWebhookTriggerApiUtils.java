/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.triggers.v1;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerSpecV2;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.AwsCodeCommitSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitPushSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.AzureRepoSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.action.AzureRepoIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.action.AzureRepoPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.event.AzureRepoEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.event.AzureRepoIssueCommentSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.event.AzureRepoPRSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.event.AzureRepoPushSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.azurerepo.event.AzureRepoTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.BitbucketSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketPRCommentSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketPRSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketPushSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.custom.CustomTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.GithubSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubReleaseAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubIssueCommentSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubPRSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubPushSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubReleaseSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.GitlabSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabMRCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabMRCommentSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabPRSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabPushSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.HarnessSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.action.HarnessIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.action.HarnessPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.event.HarnessEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.event.HarnessIssueCommentSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.event.HarnessPRSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.event.HarnessPushSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.harness.event.HarnessTriggerEvent;
import io.harness.ngtriggers.conditionchecker.ConditionOperator;
import io.harness.spec.server.pipeline.v1.model.AwsCodeCommitWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.AwsCodeCommitWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.AzureRepoWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.AzureRepoWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.BitbucketWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.BitbucketWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.CustomWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.CustomWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GithubWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.GithubWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.GitlabWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.GitlabWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.HarnessWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.HarnessWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.IssueCommentAzureRepoWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.IssueCommentGithubWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.IssueCommentHarnessWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.MRCommentGitlabWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.MergeRequestGitlabWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PRCommentBitbucketWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PullRequestAzureRepoWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PullRequestBitbucketWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PullRequestGithubWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PullRequestHarnessWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PushAwsCodeCommitWebhookTriggerSpec;
import io.harness.spec.server.pipeline.v1.model.PushAzureRepoWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PushBitbucketWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PushGithubWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PushGitlabWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.PushHarnessWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.ReleaseGithubWebhookSpec;
import io.harness.spec.server.pipeline.v1.model.TriggerConditions;
import io.harness.spec.server.pipeline.v1.model.WebhookTriggerSpec;

import java.util.Objects;
import java.util.stream.Collectors;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_TRIGGERS})
@OwnedBy(HarnessTeam.PIPELINE)
public class NGWebhookTriggerApiUtils {
  WebhookTriggerType toWebhookTriggerType(WebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case GITHUB:
        return WebhookTriggerType.GITHUB;
      case GITLAB:
        return WebhookTriggerType.GITLAB;
      case AZUREREPO:
        return WebhookTriggerType.AZURE;
      case BITBUCKET:
        return WebhookTriggerType.BITBUCKET;
      case AWSCODECOMMIT:
        return WebhookTriggerType.AWS_CODECOMMIT;
      case CUSTOM:
        return WebhookTriggerType.CUSTOM;
      case HARNESS:
        return WebhookTriggerType.HARNESS;
      default:
        throw new InvalidRequestException("Webhook Trigger Type " + typeEnum + " is invalid");
    }
  }
  WebhookTriggerSpecV2 toWebhookTriggerSpec(WebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case GITHUB:
        GithubWebhookTriggerSpec githubWebhookSpec = ((GithubWebhookSpec) spec).getSpec();
        return GithubSpec.builder()
            .type(toGithubTriggerEvent(githubWebhookSpec.getType()))
            .spec(toGithubEventSpec(githubWebhookSpec))
            .build();
      case GITLAB:
        GitlabWebhookTriggerSpec gitlabWebhookSpec = ((GitlabWebhookSpec) spec).getSpec();
        return GitlabSpec.builder()
            .type(toGitlabTriggerEvent(gitlabWebhookSpec.getType()))
            .spec(toGitlabEventSpec(gitlabWebhookSpec))
            .build();
      case BITBUCKET:
        BitbucketWebhookTriggerSpec bitbucketWebhookTriggerSpec = ((BitbucketWebhookSpec) spec).getSpec();
        return BitbucketSpec.builder()
            .type(toBitbucketTriggerEvent(bitbucketWebhookTriggerSpec.getType()))
            .spec(toBitbucketEventSpec(bitbucketWebhookTriggerSpec))
            .build();
      case AWSCODECOMMIT:
        AwsCodeCommitWebhookTriggerSpec awsCodeCommitTriggerSpec = ((AwsCodeCommitWebhookSpec) spec).getSpec();
        return AwsCodeCommitSpec.builder()
            .type(toAwsCodeCommitTriggerEvent(awsCodeCommitTriggerSpec.getType()))
            .spec(toAwsCodeCommitEventSpec(awsCodeCommitTriggerSpec))
            .build();
      case AZUREREPO:
        AzureRepoWebhookTriggerSpec azureRepoWebhookTriggerSpec = ((AzureRepoWebhookSpec) spec).getSpec();
        return AzureRepoSpec.builder()
            .type(toAzureRepoTriggerEvent(azureRepoWebhookTriggerSpec.getType()))
            .spec(toAzureRepoEventSpec(azureRepoWebhookTriggerSpec))
            .build();
      case HARNESS:
        HarnessWebhookTriggerSpec harnessWebhookTriggerSpec = ((HarnessWebhookSpec) spec).getSpec();
        return HarnessSpec.builder()
            .type(toHarnessTriggerEvent(harnessWebhookTriggerSpec.getType()))
            .spec(toHarnessEventSpec(harnessWebhookTriggerSpec))
            .build();
      case CUSTOM:
        CustomWebhookTriggerSpec customWebhookSpec = ((CustomWebhookSpec) spec).getSpec();
        return CustomTriggerSpec.builder()
            .payloadConditions(customWebhookSpec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .headerConditions(customWebhookSpec.getHeaderConditions()
                                  .stream()
                                  .map(this::toTriggerEventDataCondition)
                                  .collect(Collectors.toList()))
            .jexlCondition(customWebhookSpec.getJexlCondition())
            .build();
      default:
        throw new InvalidRequestException("Webhook Trigger Type " + spec.getType() + " is invalid");
    }
  }

  BitbucketEventSpec toBitbucketEventSpec(BitbucketWebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case PUSH:
        return BitbucketPushSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .jexlCondition(spec.getJexlCondition())
            .repoName(spec.getRepoName())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .build();
      case PRCOMMENT:
        PRCommentBitbucketWebhookSpec prCommentBitbucketWebhookSpec = (PRCommentBitbucketWebhookSpec) spec;
        return BitbucketPRCommentSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .jexlCondition(spec.getJexlCondition())
            .repoName(spec.getRepoName())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .actions(prCommentBitbucketWebhookSpec.getActions()
                         .stream()
                         .map(this::toBitbucketPRCommentAction)
                         .collect(Collectors.toList()))
            .build();
      case PULLREQUEST:
        PullRequestBitbucketWebhookSpec pullRequestBitbucketWebhookSpec = (PullRequestBitbucketWebhookSpec) spec;
        return BitbucketPRSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .jexlCondition(spec.getJexlCondition())
            .repoName(spec.getRepoName())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .actions(pullRequestBitbucketWebhookSpec.getActions()
                         .stream()
                         .map(this::toBitbucketPRAction)
                         .collect(Collectors.toList()))
            .build();
      default:
        throw new InvalidRequestException("Bitbucket Webhook Trigger Event Type " + spec.getType() + " is invalid");
    }
  }

  AwsCodeCommitEventSpec toAwsCodeCommitEventSpec(AwsCodeCommitWebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case PUSH:
        PushAwsCodeCommitWebhookTriggerSpec pushAwsCodeCommitWebhookTriggerSpec = spec.getSpec();
        return AwsCodeCommitPushSpec.builder()
            .connectorRef(pushAwsCodeCommitWebhookTriggerSpec.getConnectorRef())
            .jexlCondition(pushAwsCodeCommitWebhookTriggerSpec.getJexlCondition())
            .payloadConditions(pushAwsCodeCommitWebhookTriggerSpec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(pushAwsCodeCommitWebhookTriggerSpec.getRepoName())
            .build();
      default:
        throw new InvalidRequestException(
            "Aws Code Commit Webhook Trigger Event Type " + spec.getType() + " is invalid");
    }
  }

  AzureRepoEventSpec toAzureRepoEventSpec(AzureRepoWebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case ISSUECOMMENT:
        IssueCommentAzureRepoWebhookSpec issueCommentAzureRepoWebhookSpec = (IssueCommentAzureRepoWebhookSpec) spec;
        return AzureRepoIssueCommentSpec.builder()
            .repoName(spec.getRepoName())
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .jexlCondition(spec.getJexlCondition())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .actions(issueCommentAzureRepoWebhookSpec.getActions()
                         .stream()
                         .map(this::toAzureRepoIssueCommentAction)
                         .collect(Collectors.toList()))
            .build();
      case PUSH:
        return AzureRepoPushSpec.builder()
            .repoName(spec.getRepoName())
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .jexlCondition(spec.getJexlCondition())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .build();
      case PULLREQUEST:
        PullRequestAzureRepoWebhookSpec pullRequestAzureRepoWebhookSpec = (PullRequestAzureRepoWebhookSpec) spec;
        return AzureRepoPRSpec.builder()
            .repoName(spec.getRepoName())
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .jexlCondition(spec.getJexlCondition())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .actions(pullRequestAzureRepoWebhookSpec.getActions()
                         .stream()
                         .map(this::toAzureRepoPRAction)
                         .collect(Collectors.toList()))
            .build();
      default:
        throw new InvalidRequestException("Azure Repo Webhook Trigger Event Type " + spec.getType() + " is invalid");
    }
  }

  HarnessEventSpec toHarnessEventSpec(HarnessWebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case ISSUECOMMENT:
        IssueCommentHarnessWebhookSpec issueCommentHarnessWebhookSpec = (IssueCommentHarnessWebhookSpec) spec;
        return HarnessIssueCommentSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .actions(issueCommentHarnessWebhookSpec.getActions()
                         .stream()
                         .map(this::toHarnessIssueCommentAction)
                         .collect(Collectors.toList()))
            .build();
      case PULLREQUEST:
        PullRequestHarnessWebhookSpec pullRequestHarnessWebhookSpec = (PullRequestHarnessWebhookSpec) spec;
        return HarnessPRSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .actions(pullRequestHarnessWebhookSpec.getActions()
                         .stream()
                         .map(this::toHarnessPRAction)
                         .collect(Collectors.toList()))
            .build();
      case PUSH:
        return HarnessPushSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .build();
      default:
        throw new InvalidRequestException("Harness Webhook Trigger Event Type " + spec.getType() + " is invalid");
    }
  }

  GitlabEventSpec toGitlabEventSpec(GitlabWebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case PUSH:
        return GitlabPushSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .repoName(spec.getRepoName())
            .build();
      case MRCOMMENT:
        MRCommentGitlabWebhookSpec mrCommentGitlabWebhookSpec = (MRCommentGitlabWebhookSpec) spec;
        return GitlabMRCommentSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .repoName(spec.getRepoName())
            .actions(mrCommentGitlabWebhookSpec.getActions()
                         .stream()
                         .map(this::toGitlabMRCommentAction)
                         .collect(Collectors.toList()))
            .build();
      case MERGEREQUEST:
        MergeRequestGitlabWebhookSpec mergeRequestGitlabWebhookSpec = (MergeRequestGitlabWebhookSpec) spec;
        return GitlabPRSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .repoName(spec.getRepoName())
            .actions(mergeRequestGitlabWebhookSpec.getActions()
                         .stream()
                         .map(this::toGitlabPRAction)
                         .collect(Collectors.toList()))
            .build();
      default:
        throw new InvalidRequestException("Gitlab Webhook Trigger Event Type " + spec.getType() + " is invalid");
    }
  }

  GithubEventSpec toGithubEventSpec(GithubWebhookTriggerSpec spec) {
    switch (spec.getType()) {
      case PULLREQUEST:
        PullRequestGithubWebhookSpec pullRequestGithubWebhookSpec = (PullRequestGithubWebhookSpec) spec;
        return GithubPRSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .actions(pullRequestGithubWebhookSpec.getActions()
                         .stream()
                         .map(this::toGithubPRAction)
                         .collect(Collectors.toList()))
            .build();
      case PUSH:
        return GithubPushSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .build();
      case RELEASE:
        ReleaseGithubWebhookSpec releaseGithubWebhookSpec = (ReleaseGithubWebhookSpec) spec;
        return GithubReleaseSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .actions(releaseGithubWebhookSpec.getActions()
                         .stream()
                         .map(this::toGithubReleaseAction)
                         .collect(Collectors.toList()))
            .build();
      case ISSUECOMMENT:
        IssueCommentGithubWebhookSpec issueCommentGithubWebhookSpec = (IssueCommentGithubWebhookSpec) spec;
        return GithubIssueCommentSpec.builder()
            .autoAbortPreviousExecutions(unboxBoolean(spec.isAutoAbortPreviousExecutions()))
            .connectorRef(spec.getConnectorRef())
            .headerConditions(
                spec.getHeaderConditions().stream().map(this::toTriggerEventDataCondition).collect(Collectors.toList()))
            .jexlCondition(spec.getJexlCondition())
            .payloadConditions(spec.getPayloadConditions()
                                   .stream()
                                   .map(this::toTriggerEventDataCondition)
                                   .collect(Collectors.toList()))
            .actions(issueCommentGithubWebhookSpec.getActions()
                         .stream()
                         .map(this::toGithubIssueCommentAction)
                         .collect(Collectors.toList()))
            .repoName(spec.getRepoName())
            .build();
      default:
        throw new InvalidRequestException("Github Webhook Trigger Event Type " + spec.getType() + " is invalid");
    }
  }

  GitlabPRAction toGitlabPRAction(MergeRequestGitlabWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case REOPEN:
        return GitlabPRAction.REOPEN;
      case CLOSE:
        return GitlabPRAction.CLOSE;
      case OPEN:
        return GitlabPRAction.OPEN;
      case SYNC:
        return GitlabPRAction.SYNC;
      case MERGE:
        return GitlabPRAction.MERGE;
      case UPDATE:
        return GitlabPRAction.UPDATE;
      default:
        throw new InvalidRequestException("Gitlab PR Action " + actionsEnum + " is invalid");
    }
  }

  BitbucketPRCommentAction toBitbucketPRCommentAction(PRCommentBitbucketWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case CREATE:
        return BitbucketPRCommentAction.CREATE;
      case EDIT:
        return BitbucketPRCommentAction.EDIT;
      case DELETE:
        return BitbucketPRCommentAction.DELETE;
      default:
        throw new InvalidRequestException("Bitbucket PR Comment Action " + actionsEnum + " is invalid");
    }
  }

  BitbucketPRAction toBitbucketPRAction(PullRequestBitbucketWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case CREATE:
        return BitbucketPRAction.CREATE;
      case UPDATE:
        return BitbucketPRAction.UPDATE;
      case MERGE:
        return BitbucketPRAction.MERGE;
      case DECLINE:
        return BitbucketPRAction.DECLINE;
      default:
        throw new InvalidRequestException("Bitbucket PR Action " + actionsEnum + " is invalid");
    }
  }

  AzureRepoPRAction toAzureRepoPRAction(PullRequestAzureRepoWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case CREATE:
        return AzureRepoPRAction.CREATE;
      case MERGE:
        return AzureRepoPRAction.MERGE;
      case UPDATE:
        return AzureRepoPRAction.UPDATE;
      default:
        throw new InvalidRequestException("Azure Repo PR Action " + actionsEnum + " is invalid");
    }
  }

  AzureRepoIssueCommentAction toAzureRepoIssueCommentAction(IssueCommentAzureRepoWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case CREATE:
        return AzureRepoIssueCommentAction.CREATE;
      case DELETE:
        return AzureRepoIssueCommentAction.DELETE;
      case EDIT:
        return AzureRepoIssueCommentAction.EDIT;
      default:
        throw new InvalidRequestException("Azure Repo Issue comment Action " + actionsEnum + " is invalid");
    }
  }

  HarnessIssueCommentAction toHarnessIssueCommentAction(IssueCommentHarnessWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case CREATE:
        return HarnessIssueCommentAction.CREATE;
      case EDIT:
        return HarnessIssueCommentAction.EDIT;
      case DELETE:
        return HarnessIssueCommentAction.DELETE;
      default:
        throw new InvalidRequestException("Harness Issue Comment Action " + actionsEnum + " is invalid");
    }
  }

  HarnessPRAction toHarnessPRAction(PullRequestHarnessWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case EDIT:
        return HarnessPRAction.EDIT;
      case OPEN:
        return HarnessPRAction.OPEN;
      case CLOSE:
        return HarnessPRAction.CLOSE;
      case REOPEN:
        return HarnessPRAction.REOPEN;
      case SYNCHRONIZE:
        return HarnessPRAction.SYNCHRONIZE;
      case UNLABEL:
        return HarnessPRAction.UNLABEL;
      case LABEL:
        return HarnessPRAction.LABEL;
      default:
        throw new InvalidRequestException("Harness PR Action " + actionsEnum + " is invalid");
    }
  }

  GitlabMRCommentAction toGitlabMRCommentAction(MRCommentGitlabWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case CREATE:
        return GitlabMRCommentAction.CREATE;
      default:
        throw new InvalidRequestException("Gitlab MR Comment Action " + actionsEnum + " is invalid");
    }
  }

  GithubPRAction toGithubPRAction(PullRequestGithubWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case EDIT:
        return GithubPRAction.EDIT;
      case OPEN:
        return GithubPRAction.OPEN;
      case CLOSE:
        return GithubPRAction.CLOSE;
      case LABEL:
        return GithubPRAction.LABEL;
      case REOPEN:
        return GithubPRAction.REOPEN;
      case UNLABEL:
        return GithubPRAction.UNLABEL;
      case SYNCHRONIZE:
        return GithubPRAction.SYNCHRONIZE;
      case READYFORREVIEW:
        return GithubPRAction.REVIEWREADY;
      default:
        throw new InvalidRequestException("Github PR Action " + actionsEnum + " is invalid");
    }
  }

  GithubReleaseAction toGithubReleaseAction(ReleaseGithubWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case RELEASE:
        return GithubReleaseAction.RELEASE;
      case EDIT:
        return GithubReleaseAction.EDIT;
      case CREATE:
        return GithubReleaseAction.CREATE;
      case DELETE:
        return GithubReleaseAction.DELETE;
      case PUBLISH:
        return GithubReleaseAction.PUBLISH;
      case PRERELEASE:
        return GithubReleaseAction.PRERELEASE;
      case UNPUBLISH:
        return GithubReleaseAction.UNPUBLISH;
      default:
        throw new InvalidRequestException("Github Release Action " + actionsEnum + " is invalid");
    }
  }

  GithubIssueCommentAction toGithubIssueCommentAction(IssueCommentGithubWebhookSpec.ActionsEnum actionsEnum) {
    switch (actionsEnum) {
      case DELETE:
        return GithubIssueCommentAction.DELETE;
      case CREATE:
        return GithubIssueCommentAction.CREATE;
      case EDIT:
        return GithubIssueCommentAction.EDIT;
      default:
        throw new InvalidRequestException("Github Issue Comment Action " + actionsEnum + " is invalid");
    }
  }

  ConditionOperator toConditionOperator(TriggerConditions.OperatorEnum operatorEnum) {
    switch (operatorEnum) {
      case IN:
        return ConditionOperator.IN;
      case NOTIN:
        return ConditionOperator.NOT_IN;
      case EQUALS:
        return ConditionOperator.EQUALS;
      case NOTEQUALS:
        return ConditionOperator.NOT_EQUALS;
      case REGEX:
        return ConditionOperator.REGEX;
      case CONTAINS:
        return ConditionOperator.CONTAINS;
      case DOESNOTCONTAIN:
        return ConditionOperator.DOES_NOT_CONTAIN;
      case ENDSWITH:
        return ConditionOperator.ENDS_WITH;
      case STARTSWITH:
        return ConditionOperator.STARTS_WITH;
      default:
        throw new InvalidRequestException("Conditional Operator " + operatorEnum + " is invalid");
    }
  }

  TriggerEventDataCondition toTriggerEventDataCondition(TriggerConditions triggerConditions) {
    return TriggerEventDataCondition.builder()
        .key(triggerConditions.getKey())
        .operator(toConditionOperator(triggerConditions.getOperator()))
        .value(triggerConditions.getValue())
        .build();
  }

  GithubTriggerEvent toGithubTriggerEvent(GithubWebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case PULLREQUEST:
        return GithubTriggerEvent.PULL_REQUEST;
      case PUSH:
        return GithubTriggerEvent.PUSH;
      case RELEASE:
        return GithubTriggerEvent.RELEASE;
      case ISSUECOMMENT:
        return GithubTriggerEvent.ISSUE_COMMENT;
      default:
        throw new InvalidRequestException("Github Webhook Trigger Event Type " + typeEnum + " is invalid");
    }
  }

  GitlabTriggerEvent toGitlabTriggerEvent(GitlabWebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case MERGEREQUEST:
        return GitlabTriggerEvent.MERGE_REQUEST;
      case MRCOMMENT:
        return GitlabTriggerEvent.MR_COMMENT;
      case PUSH:
        return GitlabTriggerEvent.PUSH;
      default:
        throw new InvalidRequestException("Gitlab Webhook Trigger Event Type " + typeEnum + " is invalid");
    }
  }

  HarnessTriggerEvent toHarnessTriggerEvent(HarnessWebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case PUSH:
        return HarnessTriggerEvent.PUSH;
      case PULLREQUEST:
        return HarnessTriggerEvent.PULL_REQUEST;
      case ISSUECOMMENT:
        return HarnessTriggerEvent.ISSUE_COMMENT;
      default:
        throw new InvalidRequestException("Harness Webhook Trigger Event Type " + typeEnum + " is invalid");
    }
  }

  BitbucketTriggerEvent toBitbucketTriggerEvent(BitbucketWebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case PUSH:
        return BitbucketTriggerEvent.PUSH;
      case PULLREQUEST:
        return BitbucketTriggerEvent.PULL_REQUEST;
      case PRCOMMENT:
        return BitbucketTriggerEvent.PR_COMMENT;
      default:
        throw new InvalidRequestException("Bitbucket Webhook Trigger Event Type " + typeEnum + " is invalid");
    }
  }

  AwsCodeCommitTriggerEvent toAwsCodeCommitTriggerEvent(AwsCodeCommitWebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case PUSH:
        return AwsCodeCommitTriggerEvent.PUSH;
      default:
        throw new InvalidRequestException("Aws Code Commit Webhook Trigger Event Type " + typeEnum + " is invalid");
    }
  }

  AzureRepoTriggerEvent toAzureRepoTriggerEvent(AzureRepoWebhookTriggerSpec.TypeEnum typeEnum) {
    switch (typeEnum) {
      case PULLREQUEST:
        return AzureRepoTriggerEvent.PULL_REQUEST;
      case PUSH:
        return AzureRepoTriggerEvent.PUSH;
      case ISSUECOMMENT:
        return AzureRepoTriggerEvent.ISSUE_COMMENT;
      default:
        throw new InvalidRequestException("Azure Repo Webhook Trigger Event Type " + typeEnum + " is invalid");
    }
  }

  boolean unboxBoolean(Boolean b) {
    return Objects.requireNonNullElse(b, false);
  }

  WebhookTriggerSpec toWebhookTriggerApiSpec(NGTriggerSpecV2 spec) {
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) spec;
    switch (webhookTriggerConfigV2.getType()) {
      case AWS_CODECOMMIT:
        AwsCodeCommitSpec awsCodeCommitSpec = (AwsCodeCommitSpec) webhookTriggerConfigV2.getSpec();
        AwsCodeCommitWebhookSpec awsCodeCommitWebhookSpec = new AwsCodeCommitWebhookSpec();
        awsCodeCommitWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.AWSCODECOMMIT);
        AwsCodeCommitWebhookTriggerSpec awsCodeCommitWebhookTriggerSpec = new AwsCodeCommitWebhookTriggerSpec();
        awsCodeCommitWebhookTriggerSpec.setType(AwsCodeCommitWebhookTriggerSpec.TypeEnum.PUSH);
        awsCodeCommitWebhookTriggerSpec.setSpec(toAwsCodeCommitTriggerSpec(awsCodeCommitSpec));
        awsCodeCommitWebhookSpec.setSpec(awsCodeCommitWebhookTriggerSpec);
        return awsCodeCommitWebhookSpec;
      case HARNESS:
        HarnessSpec harnessSpec = (HarnessSpec) webhookTriggerConfigV2.getSpec();
        HarnessWebhookSpec harnessWebhookSpec = new HarnessWebhookSpec();
        harnessWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.HARNESS);
        harnessWebhookSpec.setSpec(toHarnessWebhookTriggerSpec(harnessSpec));
        return harnessWebhookSpec;
      case AZURE:
        AzureRepoSpec azureRepoSpec = (AzureRepoSpec) webhookTriggerConfigV2.getSpec();
        AzureRepoWebhookSpec azureRepoWebhookSpec = new AzureRepoWebhookSpec();
        azureRepoWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.AZUREREPO);
        azureRepoWebhookSpec.setSpec(toAzureRepoWebhookTriggerSpec(azureRepoSpec));
        return azureRepoWebhookSpec;
      case GITHUB:
        GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
        GithubWebhookSpec githubWebhookSpec = new GithubWebhookSpec();
        githubWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.GITHUB);
        githubWebhookSpec.setSpec(toGithubWebhookTriggerSpec(githubSpec));
        return githubWebhookSpec;
      case GITLAB:
        GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfigV2.getSpec();
        GitlabWebhookSpec gitlabWebhookSpec = new GitlabWebhookSpec();
        gitlabWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.GITLAB);
        gitlabWebhookSpec.setSpec(toGitlabWebhookTriggerSpec(gitlabSpec));
        return gitlabWebhookSpec;
      case BITBUCKET:
        BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfigV2.getSpec();
        BitbucketWebhookSpec bitbucketWebhookSpec = new BitbucketWebhookSpec();
        bitbucketWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.BITBUCKET);
        bitbucketWebhookSpec.setSpec(toBitbucketWebhookTriggerSpec(bitbucketSpec));
        return bitbucketWebhookSpec;
      case CUSTOM:
        CustomTriggerSpec customTriggerSpec = (CustomTriggerSpec) webhookTriggerConfigV2.getSpec();
        CustomWebhookSpec customWebhookSpec = new CustomWebhookSpec();
        customWebhookSpec.setType(WebhookTriggerSpec.TypeEnum.CUSTOM);
        CustomWebhookTriggerSpec customWebhookTriggerSpec = new CustomWebhookTriggerSpec();
        customWebhookTriggerSpec.setJexlCondition(customTriggerSpec.getJexlCondition());
        customWebhookTriggerSpec.setHeaderConditions(customTriggerSpec.fetchHeaderConditions()
                                                         .stream()
                                                         .map(this::toTriggerCondition)
                                                         .collect(Collectors.toList()));
        customWebhookTriggerSpec.setPayloadConditions(customTriggerSpec.fetchPayloadConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
        customWebhookSpec.setSpec(customWebhookTriggerSpec);
        return customWebhookSpec;
      default:
        throw new InvalidRequestException("Webhook Trigger Type " + webhookTriggerConfigV2.getType() + " is invalid");
    }
  }

  PushAwsCodeCommitWebhookTriggerSpec toAwsCodeCommitTriggerSpec(AwsCodeCommitSpec awsCodeCommitSpec) {
    PushAwsCodeCommitWebhookTriggerSpec pushAwsCodeCommitWebhookTriggerSpec = new PushAwsCodeCommitWebhookTriggerSpec();
    pushAwsCodeCommitWebhookTriggerSpec.setConnectorRef(awsCodeCommitSpec.getSpec().fetchConnectorRef());
    pushAwsCodeCommitWebhookTriggerSpec.setJexlCondition(awsCodeCommitSpec.getSpec().fetchJexlCondition());
    pushAwsCodeCommitWebhookTriggerSpec.setPayloadConditions(awsCodeCommitSpec.getSpec()
                                                                 .fetchPayloadConditions()
                                                                 .stream()
                                                                 .map(this::toTriggerCondition)
                                                                 .collect(Collectors.toList()));
    pushAwsCodeCommitWebhookTriggerSpec.setRepoName(awsCodeCommitSpec.getSpec().fetchRepoName());
    return pushAwsCodeCommitWebhookTriggerSpec;
  }

  HarnessWebhookTriggerSpec toHarnessWebhookTriggerSpec(HarnessSpec harnessSpec) {
    switch (harnessSpec.getType()) {
      case PUSH:
        PushHarnessWebhookSpec pushHarnessWebhookSpec = new PushHarnessWebhookSpec();
        pushHarnessWebhookSpec.setType(HarnessWebhookTriggerSpec.TypeEnum.PUSH);
        pushHarnessWebhookSpec.setAutoAbortPreviousExecutions(harnessSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pushHarnessWebhookSpec.setJexlCondition(harnessSpec.getSpec().fetchJexlCondition());
        pushHarnessWebhookSpec.setHeaderConditions(harnessSpec.getSpec()
                                                       .fetchHeaderConditions()
                                                       .stream()
                                                       .map(this::toTriggerCondition)
                                                       .collect(Collectors.toList()));
        pushHarnessWebhookSpec.setPayloadConditions(harnessSpec.getSpec()
                                                        .fetchPayloadConditions()
                                                        .stream()
                                                        .map(this::toTriggerCondition)
                                                        .collect(Collectors.toList()));
        pushHarnessWebhookSpec.setRepoName(harnessSpec.getSpec().fetchRepoName());
        return pushHarnessWebhookSpec;
      case PULL_REQUEST:
        PullRequestHarnessWebhookSpec pullRequestHarnessWebhookSpec = new PullRequestHarnessWebhookSpec();
        pullRequestHarnessWebhookSpec.setType(HarnessWebhookTriggerSpec.TypeEnum.PULLREQUEST);
        pullRequestHarnessWebhookSpec.setAutoAbortPreviousExecutions(
            harnessSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pullRequestHarnessWebhookSpec.setJexlCondition(harnessSpec.getSpec().fetchJexlCondition());
        pullRequestHarnessWebhookSpec.setHeaderConditions(harnessSpec.getSpec()
                                                              .fetchHeaderConditions()
                                                              .stream()
                                                              .map(this::toTriggerCondition)
                                                              .collect(Collectors.toList()));
        pullRequestHarnessWebhookSpec.setPayloadConditions(harnessSpec.getSpec()
                                                               .fetchPayloadConditions()
                                                               .stream()
                                                               .map(this::toTriggerCondition)
                                                               .collect(Collectors.toList()));
        pullRequestHarnessWebhookSpec.setActions(harnessSpec.getSpec()
                                                     .fetchActions()
                                                     .stream()
                                                     .map(this::toHarnessTriggerPRActions)
                                                     .collect(Collectors.toList()));
        pullRequestHarnessWebhookSpec.setRepoName(harnessSpec.getSpec().fetchRepoName());
        return pullRequestHarnessWebhookSpec;
      case ISSUE_COMMENT:
        IssueCommentHarnessWebhookSpec issueCommentHarnessWebhookSpec = new IssueCommentHarnessWebhookSpec();
        issueCommentHarnessWebhookSpec.setType(HarnessWebhookTriggerSpec.TypeEnum.ISSUECOMMENT);
        issueCommentHarnessWebhookSpec.setAutoAbortPreviousExecutions(
            harnessSpec.getSpec().fetchAutoAbortPreviousExecutions());
        issueCommentHarnessWebhookSpec.setJexlCondition(harnessSpec.getSpec().fetchJexlCondition());
        issueCommentHarnessWebhookSpec.setHeaderConditions(harnessSpec.getSpec()
                                                               .fetchHeaderConditions()
                                                               .stream()
                                                               .map(this::toTriggerCondition)
                                                               .collect(Collectors.toList()));
        issueCommentHarnessWebhookSpec.setPayloadConditions(harnessSpec.getSpec()
                                                                .fetchPayloadConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
        issueCommentHarnessWebhookSpec.setRepoName(harnessSpec.getSpec().fetchRepoName());
        issueCommentHarnessWebhookSpec.setActions(harnessSpec.getSpec()
                                                      .fetchActions()
                                                      .stream()
                                                      .map(this::toHarnessTriggerIssueCommentAction)
                                                      .collect(Collectors.toList()));
        return issueCommentHarnessWebhookSpec;
      default:
        throw new InvalidRequestException(
            "Harness Webhook Trigger Event Type " + harnessSpec.getType() + " is invalid");
    }
  }

  IssueCommentHarnessWebhookSpec.ActionsEnum toHarnessTriggerIssueCommentAction(GitAction gitAction) {
    HarnessIssueCommentAction harnessIssueCommentAction = (HarnessIssueCommentAction) gitAction;
    switch (harnessIssueCommentAction) {
      case EDIT:
        return IssueCommentHarnessWebhookSpec.ActionsEnum.EDIT;
      case DELETE:
        return IssueCommentHarnessWebhookSpec.ActionsEnum.DELETE;
      case CREATE:
        return IssueCommentHarnessWebhookSpec.ActionsEnum.CREATE;
      default:
        throw new InvalidRequestException("Harness Issue Comment Action " + harnessIssueCommentAction + " is invalid");
    }
  }

  PullRequestHarnessWebhookSpec.ActionsEnum toHarnessTriggerPRActions(GitAction gitAction) {
    HarnessPRAction harnessPRAction = (HarnessPRAction) gitAction;
    switch (harnessPRAction) {
      case EDIT:
        return PullRequestHarnessWebhookSpec.ActionsEnum.EDIT;
      case LABEL:
        return PullRequestHarnessWebhookSpec.ActionsEnum.LABEL;
      case UNLABEL:
        return PullRequestHarnessWebhookSpec.ActionsEnum.UNLABEL;
      case SYNCHRONIZE:
        return PullRequestHarnessWebhookSpec.ActionsEnum.SYNCHRONIZE;
      case REOPEN:
        return PullRequestHarnessWebhookSpec.ActionsEnum.REOPEN;
      case CLOSE:
        return PullRequestHarnessWebhookSpec.ActionsEnum.CLOSE;
      case OPEN:
        return PullRequestHarnessWebhookSpec.ActionsEnum.OPEN;
      default:
        throw new InvalidRequestException("Harness PR Action " + harnessPRAction + " is invalid");
    }
  }

  GithubWebhookTriggerSpec toGithubWebhookTriggerSpec(GithubSpec githubSpec) {
    switch (githubSpec.getType()) {
      case PUSH:
        PushGithubWebhookSpec pushGithubWebhookSpec = new PushGithubWebhookSpec();
        pushGithubWebhookSpec.setType(GithubWebhookTriggerSpec.TypeEnum.PUSH);
        pushGithubWebhookSpec.setAutoAbortPreviousExecutions(githubSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pushGithubWebhookSpec.setJexlCondition(githubSpec.getSpec().fetchJexlCondition());
        pushGithubWebhookSpec.setHeaderConditions(githubSpec.getSpec()
                                                      .fetchHeaderConditions()
                                                      .stream()
                                                      .map(this::toTriggerCondition)
                                                      .collect(Collectors.toList()));
        pushGithubWebhookSpec.setPayloadConditions(githubSpec.getSpec()
                                                       .fetchPayloadConditions()
                                                       .stream()
                                                       .map(this::toTriggerCondition)
                                                       .collect(Collectors.toList()));
        pushGithubWebhookSpec.setRepoName(githubSpec.getSpec().fetchRepoName());
        return pushGithubWebhookSpec;
      case ISSUE_COMMENT:
        IssueCommentGithubWebhookSpec issueCommentGithubWebhookSpec = new IssueCommentGithubWebhookSpec();
        issueCommentGithubWebhookSpec.setType(GithubWebhookTriggerSpec.TypeEnum.ISSUECOMMENT);
        issueCommentGithubWebhookSpec.setAutoAbortPreviousExecutions(
            githubSpec.getSpec().fetchAutoAbortPreviousExecutions());
        issueCommentGithubWebhookSpec.setJexlCondition(githubSpec.getSpec().fetchJexlCondition());
        issueCommentGithubWebhookSpec.setHeaderConditions(githubSpec.getSpec()
                                                              .fetchHeaderConditions()
                                                              .stream()
                                                              .map(this::toTriggerCondition)
                                                              .collect(Collectors.toList()));
        issueCommentGithubWebhookSpec.setPayloadConditions(githubSpec.getSpec()
                                                               .fetchPayloadConditions()
                                                               .stream()
                                                               .map(this::toTriggerCondition)
                                                               .collect(Collectors.toList()));
        issueCommentGithubWebhookSpec.setRepoName(githubSpec.getSpec().fetchRepoName());
        issueCommentGithubWebhookSpec.setActions(githubSpec.getSpec()
                                                     .fetchActions()
                                                     .stream()
                                                     .map(this::toGithubTriggerIssueCommentAction)
                                                     .collect(Collectors.toList()));
        return issueCommentGithubWebhookSpec;
      case PULL_REQUEST:
        PullRequestGithubWebhookSpec pullRequestGithubWebhookSpec = new PullRequestGithubWebhookSpec();
        pullRequestGithubWebhookSpec.setType(GithubWebhookTriggerSpec.TypeEnum.PULLREQUEST);
        pullRequestGithubWebhookSpec.setAutoAbortPreviousExecutions(
            githubSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pullRequestGithubWebhookSpec.setJexlCondition(githubSpec.getSpec().fetchJexlCondition());
        pullRequestGithubWebhookSpec.setHeaderConditions(githubSpec.getSpec()
                                                             .fetchHeaderConditions()
                                                             .stream()
                                                             .map(this::toTriggerCondition)
                                                             .collect(Collectors.toList()));
        pullRequestGithubWebhookSpec.setPayloadConditions(githubSpec.getSpec()
                                                              .fetchPayloadConditions()
                                                              .stream()
                                                              .map(this::toTriggerCondition)
                                                              .collect(Collectors.toList()));
        pullRequestGithubWebhookSpec.setRepoName(githubSpec.getSpec().fetchRepoName());
        pullRequestGithubWebhookSpec.setActions(githubSpec.getSpec()
                                                    .fetchActions()
                                                    .stream()
                                                    .map(this::toGithubTriggerPRAction)
                                                    .collect(Collectors.toList()));
        return pullRequestGithubWebhookSpec;
      case RELEASE:
        ReleaseGithubWebhookSpec releaseGithubWebhookSpec = new ReleaseGithubWebhookSpec();
        releaseGithubWebhookSpec.setType(GithubWebhookTriggerSpec.TypeEnum.RELEASE);
        releaseGithubWebhookSpec.setAutoAbortPreviousExecutions(
            githubSpec.getSpec().fetchAutoAbortPreviousExecutions());
        releaseGithubWebhookSpec.setJexlCondition(githubSpec.getSpec().fetchJexlCondition());
        releaseGithubWebhookSpec.setHeaderConditions(githubSpec.getSpec()
                                                         .fetchHeaderConditions()
                                                         .stream()
                                                         .map(this::toTriggerCondition)
                                                         .collect(Collectors.toList()));
        releaseGithubWebhookSpec.setPayloadConditions(githubSpec.getSpec()
                                                          .fetchPayloadConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
        releaseGithubWebhookSpec.setRepoName(githubSpec.getSpec().fetchRepoName());
        releaseGithubWebhookSpec.setActions(githubSpec.getSpec()
                                                .fetchActions()
                                                .stream()
                                                .map(this::toGithubTriggerReleaseAction)
                                                .collect(Collectors.toList()));
        return releaseGithubWebhookSpec;
      default:
        throw new InvalidRequestException("Github Webhook Trigger Event Type " + githubSpec.getType() + " is invalid");
    }
  }

  IssueCommentGithubWebhookSpec.ActionsEnum toGithubTriggerIssueCommentAction(GitAction gitAction) {
    GithubIssueCommentAction action = (GithubIssueCommentAction) gitAction;
    switch (action) {
      case CREATE:
        return IssueCommentGithubWebhookSpec.ActionsEnum.CREATE;
      case DELETE:
        return IssueCommentGithubWebhookSpec.ActionsEnum.DELETE;
      case EDIT:
        return IssueCommentGithubWebhookSpec.ActionsEnum.EDIT;
      default:
        throw new InvalidRequestException("Github Issue Comment Action " + action + " is invalid");
    }
  }

  PullRequestGithubWebhookSpec.ActionsEnum toGithubTriggerPRAction(GitAction gitAction) {
    GithubPRAction githubPRAction = (GithubPRAction) gitAction;
    switch (githubPRAction) {
      case EDIT:
        return PullRequestGithubWebhookSpec.ActionsEnum.EDIT;
      case OPEN:
        return PullRequestGithubWebhookSpec.ActionsEnum.OPEN;
      case CLOSE:
        return PullRequestGithubWebhookSpec.ActionsEnum.CLOSE;
      case REOPEN:
        return PullRequestGithubWebhookSpec.ActionsEnum.REOPEN;
      case SYNCHRONIZE:
        return PullRequestGithubWebhookSpec.ActionsEnum.SYNCHRONIZE;
      case UNLABEL:
        return PullRequestGithubWebhookSpec.ActionsEnum.UNLABEL;
      case LABEL:
        return PullRequestGithubWebhookSpec.ActionsEnum.LABEL;
      case REVIEWREADY:
        return PullRequestGithubWebhookSpec.ActionsEnum.READYFORREVIEW;
      default:
        throw new InvalidRequestException("Github PR Action " + githubPRAction + " is invalid");
    }
  }

  ReleaseGithubWebhookSpec.ActionsEnum toGithubTriggerReleaseAction(GitAction gitAction) {
    GithubReleaseAction githubReleaseAction = (GithubReleaseAction) gitAction;
    switch (githubReleaseAction) {
      case EDIT:
        return ReleaseGithubWebhookSpec.ActionsEnum.EDIT;
      case DELETE:
        return ReleaseGithubWebhookSpec.ActionsEnum.DELETE;
      case CREATE:
        return ReleaseGithubWebhookSpec.ActionsEnum.CREATE;
      case RELEASE:
        return ReleaseGithubWebhookSpec.ActionsEnum.RELEASE;
      case UNPUBLISH:
        return ReleaseGithubWebhookSpec.ActionsEnum.UNPUBLISH;
      case PRERELEASE:
        return ReleaseGithubWebhookSpec.ActionsEnum.PRERELEASE;
      case PUBLISH:
        return ReleaseGithubWebhookSpec.ActionsEnum.PUBLISH;
      default:
        throw new InvalidRequestException("Github Release Action " + githubReleaseAction + " is invalid");
    }
  }

  BitbucketWebhookTriggerSpec toBitbucketWebhookTriggerSpec(BitbucketSpec bitbucketSpec) {
    switch (bitbucketSpec.getType()) {
      case PUSH:
        PushBitbucketWebhookSpec pushBitbucketWebhookSpec = new PushBitbucketWebhookSpec();
        pushBitbucketWebhookSpec.setType(BitbucketWebhookTriggerSpec.TypeEnum.PUSH);
        pushBitbucketWebhookSpec.setAutoAbortPreviousExecutions(
            bitbucketSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pushBitbucketWebhookSpec.setJexlCondition(bitbucketSpec.getSpec().fetchJexlCondition());
        pushBitbucketWebhookSpec.setHeaderConditions(bitbucketSpec.getSpec()
                                                         .fetchHeaderConditions()
                                                         .stream()
                                                         .map(this::toTriggerCondition)
                                                         .collect(Collectors.toList()));
        pushBitbucketWebhookSpec.setPayloadConditions(bitbucketSpec.getSpec()
                                                          .fetchPayloadConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
        pushBitbucketWebhookSpec.setRepoName(bitbucketSpec.getSpec().fetchRepoName());
        return pushBitbucketWebhookSpec;
      case PR_COMMENT:
        PRCommentBitbucketWebhookSpec prCommentBitbucketWebhookSpec = new PRCommentBitbucketWebhookSpec();
        prCommentBitbucketWebhookSpec.setType(BitbucketWebhookTriggerSpec.TypeEnum.PRCOMMENT);
        prCommentBitbucketWebhookSpec.setAutoAbortPreviousExecutions(
            bitbucketSpec.getSpec().fetchAutoAbortPreviousExecutions());
        prCommentBitbucketWebhookSpec.setJexlCondition(bitbucketSpec.getSpec().fetchJexlCondition());
        prCommentBitbucketWebhookSpec.setHeaderConditions(bitbucketSpec.getSpec()
                                                              .fetchHeaderConditions()
                                                              .stream()
                                                              .map(this::toTriggerCondition)
                                                              .collect(Collectors.toList()));
        prCommentBitbucketWebhookSpec.setPayloadConditions(bitbucketSpec.getSpec()
                                                               .fetchPayloadConditions()
                                                               .stream()
                                                               .map(this::toTriggerCondition)
                                                               .collect(Collectors.toList()));
        prCommentBitbucketWebhookSpec.setRepoName(bitbucketSpec.getSpec().fetchRepoName());
        prCommentBitbucketWebhookSpec.setActions(bitbucketSpec.getSpec()
                                                     .fetchActions()
                                                     .stream()
                                                     .map(this::toBBTriggerPRCommentAction)
                                                     .collect(Collectors.toList()));
        return prCommentBitbucketWebhookSpec;
      case PULL_REQUEST:
        PullRequestBitbucketWebhookSpec pullRequestBitbucketWebhookSpec = new PullRequestBitbucketWebhookSpec();
        pullRequestBitbucketWebhookSpec.setType(BitbucketWebhookTriggerSpec.TypeEnum.PULLREQUEST);
        pullRequestBitbucketWebhookSpec.setAutoAbortPreviousExecutions(
            bitbucketSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pullRequestBitbucketWebhookSpec.setJexlCondition(bitbucketSpec.getSpec().fetchJexlCondition());
        pullRequestBitbucketWebhookSpec.setHeaderConditions(bitbucketSpec.getSpec()
                                                                .fetchHeaderConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
        pullRequestBitbucketWebhookSpec.setPayloadConditions(bitbucketSpec.getSpec()
                                                                 .fetchPayloadConditions()
                                                                 .stream()
                                                                 .map(this::toTriggerCondition)
                                                                 .collect(Collectors.toList()));
        pullRequestBitbucketWebhookSpec.setRepoName(bitbucketSpec.getSpec().fetchRepoName());
        pullRequestBitbucketWebhookSpec.setActions(bitbucketSpec.getSpec()
                                                       .fetchActions()
                                                       .stream()
                                                       .map(this::toBBTriggerPRAction)
                                                       .collect(Collectors.toList()));
        return pullRequestBitbucketWebhookSpec;
      default:
        throw new InvalidRequestException(
            "Bitbucket Webhook Trigger Event Type " + bitbucketSpec.getType() + " is invalid");
    }
  }

  PullRequestBitbucketWebhookSpec.ActionsEnum toBBTriggerPRAction(GitAction gitAction) {
    BitbucketPRAction bitbucketPRAction = (BitbucketPRAction) gitAction;
    switch (bitbucketPRAction) {
      case CREATE:
        return PullRequestBitbucketWebhookSpec.ActionsEnum.CREATE;
      case UPDATE:
        return PullRequestBitbucketWebhookSpec.ActionsEnum.UPDATE;
      case MERGE:
        return PullRequestBitbucketWebhookSpec.ActionsEnum.MERGE;
      case DECLINE:
        return PullRequestBitbucketWebhookSpec.ActionsEnum.DECLINE;
      default:
        throw new InvalidRequestException("Bitbucket PR Action " + bitbucketPRAction + " is invalid");
    }
  }

  PRCommentBitbucketWebhookSpec.ActionsEnum toBBTriggerPRCommentAction(GitAction gitAction) {
    BitbucketPRCommentAction bitbucketPRCommentAction = (BitbucketPRCommentAction) gitAction;
    switch (bitbucketPRCommentAction) {
      case CREATE:
        return PRCommentBitbucketWebhookSpec.ActionsEnum.CREATE;
      case DELETE:
        return PRCommentBitbucketWebhookSpec.ActionsEnum.DELETE;
      case EDIT:
        return PRCommentBitbucketWebhookSpec.ActionsEnum.EDIT;
      default:
        throw new InvalidRequestException("Bitbucket PR Action " + bitbucketPRCommentAction + " is invalid");
    }
  }

  GitlabWebhookTriggerSpec toGitlabWebhookTriggerSpec(GitlabSpec gitlabSpec) {
    switch (gitlabSpec.getType()) {
      case PUSH:
        PushGitlabWebhookSpec pushGitlabWebhookSpec = new PushGitlabWebhookSpec();
        pushGitlabWebhookSpec.setType(GitlabWebhookTriggerSpec.TypeEnum.PUSH);
        pushGitlabWebhookSpec.setAutoAbortPreviousExecutions(gitlabSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pushGitlabWebhookSpec.setJexlCondition(gitlabSpec.getSpec().fetchJexlCondition());
        pushGitlabWebhookSpec.setHeaderConditions(gitlabSpec.getSpec()
                                                      .fetchHeaderConditions()
                                                      .stream()
                                                      .map(this::toTriggerCondition)
                                                      .collect(Collectors.toList()));
        pushGitlabWebhookSpec.setPayloadConditions(gitlabSpec.getSpec()
                                                       .fetchPayloadConditions()
                                                       .stream()
                                                       .map(this::toTriggerCondition)
                                                       .collect(Collectors.toList()));
        pushGitlabWebhookSpec.setRepoName(gitlabSpec.getSpec().fetchRepoName());
        return pushGitlabWebhookSpec;
      case MR_COMMENT:
        MRCommentGitlabWebhookSpec mrCommentGitlabWebhookSpec = new MRCommentGitlabWebhookSpec();
        mrCommentGitlabWebhookSpec.setType(GitlabWebhookTriggerSpec.TypeEnum.MRCOMMENT);
        mrCommentGitlabWebhookSpec.setAutoAbortPreviousExecutions(
            gitlabSpec.getSpec().fetchAutoAbortPreviousExecutions());
        mrCommentGitlabWebhookSpec.setJexlCondition(gitlabSpec.getSpec().fetchJexlCondition());
        mrCommentGitlabWebhookSpec.setHeaderConditions(gitlabSpec.getSpec()
                                                           .fetchHeaderConditions()
                                                           .stream()
                                                           .map(this::toTriggerCondition)
                                                           .collect(Collectors.toList()));
        mrCommentGitlabWebhookSpec.setPayloadConditions(gitlabSpec.getSpec()
                                                            .fetchPayloadConditions()
                                                            .stream()
                                                            .map(this::toTriggerCondition)
                                                            .collect(Collectors.toList()));
        mrCommentGitlabWebhookSpec.setRepoName(gitlabSpec.getSpec().fetchRepoName());
        mrCommentGitlabWebhookSpec.setActions(gitlabSpec.getSpec()
                                                  .fetchActions()
                                                  .stream()
                                                  .map(this::toGitlabTriggerMRCommentAction)
                                                  .collect(Collectors.toList()));
        return mrCommentGitlabWebhookSpec;
      case MERGE_REQUEST:
        MergeRequestGitlabWebhookSpec mergeRequestGitlabWebhookSpec = new MergeRequestGitlabWebhookSpec();
        mergeRequestGitlabWebhookSpec.setType(GitlabWebhookTriggerSpec.TypeEnum.MERGEREQUEST);
        mergeRequestGitlabWebhookSpec.setAutoAbortPreviousExecutions(
            gitlabSpec.getSpec().fetchAutoAbortPreviousExecutions());
        mergeRequestGitlabWebhookSpec.setJexlCondition(gitlabSpec.getSpec().fetchJexlCondition());
        mergeRequestGitlabWebhookSpec.setHeaderConditions(gitlabSpec.getSpec()
                                                              .fetchHeaderConditions()
                                                              .stream()
                                                              .map(this::toTriggerCondition)
                                                              .collect(Collectors.toList()));
        mergeRequestGitlabWebhookSpec.setPayloadConditions(gitlabSpec.getSpec()
                                                               .fetchPayloadConditions()
                                                               .stream()
                                                               .map(this::toTriggerCondition)
                                                               .collect(Collectors.toList()));
        mergeRequestGitlabWebhookSpec.setRepoName(gitlabSpec.getSpec().fetchRepoName());
        mergeRequestGitlabWebhookSpec.setActions(gitlabSpec.getSpec()
                                                     .fetchActions()
                                                     .stream()
                                                     .map(this::toGitlabTriggerMRAction)
                                                     .collect(Collectors.toList()));
        return mergeRequestGitlabWebhookSpec;
      default:
        throw new InvalidRequestException("Gitlab Webhook Trigger Event Type " + gitlabSpec.getType() + " is invalid");
    }
  }

  MRCommentGitlabWebhookSpec.ActionsEnum toGitlabTriggerMRCommentAction(GitAction gitAction) {
    GitlabMRCommentAction gitlabMRCommentAction = (GitlabMRCommentAction) gitAction;
    switch (gitlabMRCommentAction) {
      case CREATE:
        return MRCommentGitlabWebhookSpec.ActionsEnum.CREATE;
      default:
        throw new InvalidRequestException("Gitlab MR Comment Action " + gitlabMRCommentAction + " is invalid");
    }
  }

  MergeRequestGitlabWebhookSpec.ActionsEnum toGitlabTriggerMRAction(GitAction gitAction) {
    GitlabPRAction gitlabPRAction = (GitlabPRAction) gitAction;
    switch (gitlabPRAction) {
      case MERGE:
        return MergeRequestGitlabWebhookSpec.ActionsEnum.MERGE;
      case UPDATE:
        return MergeRequestGitlabWebhookSpec.ActionsEnum.UPDATE;
      case REOPEN:
        return MergeRequestGitlabWebhookSpec.ActionsEnum.REOPEN;
      case CLOSE:
        return MergeRequestGitlabWebhookSpec.ActionsEnum.CLOSE;
      case OPEN:
        return MergeRequestGitlabWebhookSpec.ActionsEnum.OPEN;
      case SYNC:
        return MergeRequestGitlabWebhookSpec.ActionsEnum.SYNC;
      default:
        throw new InvalidRequestException("Gitlab MR Action " + gitlabPRAction + " is invalid");
    }
  }

  AzureRepoWebhookTriggerSpec toAzureRepoWebhookTriggerSpec(AzureRepoSpec azureRepoSpec) {
    switch (azureRepoSpec.getType()) {
      case ISSUE_COMMENT:
        IssueCommentAzureRepoWebhookSpec issueCommentAzureRepoWebhookSpec = new IssueCommentAzureRepoWebhookSpec();
        issueCommentAzureRepoWebhookSpec.setType(AzureRepoWebhookTriggerSpec.TypeEnum.ISSUECOMMENT);
        issueCommentAzureRepoWebhookSpec.setAutoAbortPreviousExecutions(
            azureRepoSpec.getSpec().fetchAutoAbortPreviousExecutions());
        issueCommentAzureRepoWebhookSpec.setJexlCondition(azureRepoSpec.getSpec().fetchJexlCondition());
        issueCommentAzureRepoWebhookSpec.setHeaderConditions(azureRepoSpec.getSpec()
                                                                 .fetchHeaderConditions()
                                                                 .stream()
                                                                 .map(this::toTriggerCondition)
                                                                 .collect(Collectors.toList()));
        issueCommentAzureRepoWebhookSpec.setPayloadConditions(azureRepoSpec.getSpec()
                                                                  .fetchPayloadConditions()
                                                                  .stream()
                                                                  .map(this::toTriggerCondition)
                                                                  .collect(Collectors.toList()));
        issueCommentAzureRepoWebhookSpec.setRepoName(azureRepoSpec.getSpec().fetchRepoName());
        issueCommentAzureRepoWebhookSpec.setActions(azureRepoSpec.getSpec()
                                                        .fetchActions()
                                                        .stream()
                                                        .map(this::toAzureRepoTriggerIssueCommentAction)
                                                        .collect(Collectors.toList()));
        return issueCommentAzureRepoWebhookSpec;
      case PULL_REQUEST:
        PullRequestAzureRepoWebhookSpec pullRequestAzureRepoWebhookSpec = new PullRequestAzureRepoWebhookSpec();
        pullRequestAzureRepoWebhookSpec.setType(AzureRepoWebhookTriggerSpec.TypeEnum.PULLREQUEST);
        pullRequestAzureRepoWebhookSpec.setAutoAbortPreviousExecutions(
            azureRepoSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pullRequestAzureRepoWebhookSpec.setJexlCondition(azureRepoSpec.getSpec().fetchJexlCondition());
        pullRequestAzureRepoWebhookSpec.setHeaderConditions(azureRepoSpec.getSpec()
                                                                .fetchHeaderConditions()
                                                                .stream()
                                                                .map(this::toTriggerCondition)
                                                                .collect(Collectors.toList()));
        pullRequestAzureRepoWebhookSpec.setPayloadConditions(azureRepoSpec.getSpec()
                                                                 .fetchPayloadConditions()
                                                                 .stream()
                                                                 .map(this::toTriggerCondition)
                                                                 .collect(Collectors.toList()));
        pullRequestAzureRepoWebhookSpec.setRepoName(azureRepoSpec.getSpec().fetchRepoName());
        pullRequestAzureRepoWebhookSpec.setActions(azureRepoSpec.getSpec()
                                                       .fetchActions()
                                                       .stream()
                                                       .map(this::toAzureRepoTriggerPRAction)
                                                       .collect(Collectors.toList()));
        return pullRequestAzureRepoWebhookSpec;
      case PUSH:
        PushAzureRepoWebhookSpec pushAzureRepoWebhookSpec = new PushAzureRepoWebhookSpec();
        pushAzureRepoWebhookSpec.setType(AzureRepoWebhookTriggerSpec.TypeEnum.PUSH);
        pushAzureRepoWebhookSpec.setAutoAbortPreviousExecutions(
            azureRepoSpec.getSpec().fetchAutoAbortPreviousExecutions());
        pushAzureRepoWebhookSpec.setJexlCondition(azureRepoSpec.getSpec().fetchJexlCondition());
        pushAzureRepoWebhookSpec.setHeaderConditions(azureRepoSpec.getSpec()
                                                         .fetchHeaderConditions()
                                                         .stream()
                                                         .map(this::toTriggerCondition)
                                                         .collect(Collectors.toList()));
        pushAzureRepoWebhookSpec.setPayloadConditions(azureRepoSpec.getSpec()
                                                          .fetchPayloadConditions()
                                                          .stream()
                                                          .map(this::toTriggerCondition)
                                                          .collect(Collectors.toList()));
        pushAzureRepoWebhookSpec.setRepoName(azureRepoSpec.getSpec().fetchRepoName());
        return pushAzureRepoWebhookSpec;
      default:
        throw new InvalidRequestException(
            "Azure Repo Webhook Trigger Event Type " + azureRepoSpec.getType() + " is invalid");
    }
  }

  IssueCommentAzureRepoWebhookSpec.ActionsEnum toAzureRepoTriggerIssueCommentAction(GitAction gitAction) {
    AzureRepoIssueCommentAction action = (AzureRepoIssueCommentAction) gitAction;
    switch (action) {
      case CREATE:
        return IssueCommentAzureRepoWebhookSpec.ActionsEnum.CREATE;
      case EDIT:
        return IssueCommentAzureRepoWebhookSpec.ActionsEnum.EDIT;
      case DELETE:
        return IssueCommentAzureRepoWebhookSpec.ActionsEnum.DELETE;
      default:
        throw new InvalidRequestException("Azure Repo Issue comment Action " + action + " is invalid");
    }
  }

  PullRequestAzureRepoWebhookSpec.ActionsEnum toAzureRepoTriggerPRAction(GitAction gitAction) {
    AzureRepoPRAction action = (AzureRepoPRAction) gitAction;
    switch (action) {
      case CREATE:
        return PullRequestAzureRepoWebhookSpec.ActionsEnum.CREATE;
      case UPDATE:
        return PullRequestAzureRepoWebhookSpec.ActionsEnum.UPDATE;
      case MERGE:
        return PullRequestAzureRepoWebhookSpec.ActionsEnum.MERGE;
      default:
        throw new InvalidRequestException("Azure Repo PR Action " + action + " is invalid");
    }
  }

  TriggerConditions toTriggerCondition(TriggerEventDataCondition triggerEventDataCondition) {
    TriggerConditions triggerConditions = new TriggerConditions();
    triggerConditions.setKey(triggerEventDataCondition.getKey());
    triggerConditions.setOperator(toOperatorEnum(triggerEventDataCondition.getOperator()));
    triggerConditions.setValue(triggerEventDataCondition.getValue());
    return triggerConditions;
  }

  TriggerConditions.OperatorEnum toOperatorEnum(ConditionOperator conditionOperator) {
    switch (conditionOperator) {
      case DOES_NOT_CONTAIN:
        return TriggerConditions.OperatorEnum.DOESNOTCONTAIN;
      case CONTAINS:
        return TriggerConditions.OperatorEnum.CONTAINS;
      case REGEX:
        return TriggerConditions.OperatorEnum.REGEX;
      case NOT_IN:
        return TriggerConditions.OperatorEnum.NOTIN;
      case EQUALS:
        return TriggerConditions.OperatorEnum.EQUALS;
      case IN:
        return TriggerConditions.OperatorEnum.IN;
      case ENDS_WITH:
        return TriggerConditions.OperatorEnum.ENDSWITH;
      case NOT_EQUALS:
        return TriggerConditions.OperatorEnum.NOTEQUALS;
      case STARTS_WITH:
        return TriggerConditions.OperatorEnum.STARTSWITH;
      default:
        throw new InvalidRequestException("Conditional Operator " + conditionOperator + " is invalid");
    }
  }
}
