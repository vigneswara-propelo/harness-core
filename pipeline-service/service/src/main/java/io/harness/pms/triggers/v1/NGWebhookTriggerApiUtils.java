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
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
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
}
