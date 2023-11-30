// Copyright 2023 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package client

// getACLRequest returns an ACLRequest object with the specified account ID, pipeline ID, project ID, and organization ID.
func GetACLRequest(accountID, pipelineID, projectID, orgID, resource, permission string) ACLRequest {
	req := ACLRequest{
		Permissions: []Permission{Permission{
			ResourceScope: ResourceScope{
				AccountIdentifier: accountID,
				OrgIdentifier:     orgID,
				ProjectIdentifier: projectID,
			},
			ResourceType:       resource,
			ResourceIdentifier: pipelineID,
			Permission:         permission,
		}},
	}
	return req
}
