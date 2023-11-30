// Copyright 2022 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package client

import (
	"context"
	"net/http"
)

// Error represents a json-encoded API error.
type Error struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

func (e *Error) Error() string {
	return e.Message
}

type Account struct {
	MetaData struct {
	} `json:"metaData"`
	Resource struct {
		UUID                    string        `json:"uuid"`
		AppID                   string        `json:"appId"`
		CreatedBy               interface{}   `json:"createdBy"`
		CreatedAt               int64         `json:"createdAt"`
		LastUpdatedBy           interface{}   `json:"lastUpdatedBy"`
		LastUpdatedAt           int64         `json:"lastUpdatedAt"`
		CompanyName             string        `json:"companyName"`
		NextGenEnabled          bool          `json:"nextGenEnabled"`
		AccountName             string        `json:"accountName"`
		WhitelistedDomains      []interface{} `json:"whitelistedDomains"`
		LicenseID               interface{}   `json:"licenseId"`
		DataRetentionDurationMs int           `json:"dataRetentionDurationMs"`
		LicenseInfo             struct {
			AccountType   string `json:"accountType"`
			AccountStatus string `json:"accountStatus"`
			ExpiryTime    int64  `json:"expiryTime"`
			LicenseUnits  int    `json:"licenseUnits"`
		} `json:"licenseInfo"`
		TrustLevel    int `json:"trustLevel"`
		CeLicenseInfo struct {
			LicenseType string `json:"licenseType"`
			ExpiryTime  int64  `json:"expiryTime"`
		} `json:"ceLicenseInfo"`
		AccountEvents           interface{} `json:"accountEvents"`
		SubdomainURL            string      `json:"subdomainUrl"`
		TwoFactorAdminEnforced  bool        `json:"twoFactorAdminEnforced"`
		ForImport               bool        `json:"forImport"`
		MigratedToClusterURL    interface{} `json:"migratedToClusterUrl"`
		DefaultExperience       string      `json:"defaultExperience"`
		CreatedFromNG           bool        `json:"createdFromNG"`
		AccountActivelyUsed     bool        `json:"accountActivelyUsed"`
		SmpAccount              bool        `json:"smpAccount"`
		SessionTimeOutInMinutes int         `json:"sessionTimeOutInMinutes"`
		PublicAccessEnabled     bool        `json:"publicAccessEnabled"`
		LocalEncryptionEnabled  bool        `json:"localEncryptionEnabled"`
		DelegateConfiguration   struct {
			DelegateVersions     []string    `json:"delegateVersions"`
			Action               interface{} `json:"action"`
			ValidUntil           int64       `json:"validUntil"`
			ValidTillNextRelease bool        `json:"validTillNextRelease"`
		} `json:"delegateConfiguration"`
		TechStacks             interface{} `json:"techStacks"`
		OauthEnabled           bool        `json:"oauthEnabled"`
		RingName               string      `json:"ringName"`
		AccountPreferences     interface{} `json:"accountPreferences"`
		CloudCostEnabled       bool        `json:"cloudCostEnabled"`
		CeAutoCollectK8SEvents bool        `json:"ceAutoCollectK8sEvents"`
		TrialSignupOptions     struct {
			ProductsSelected []string `json:"productsSelected"`
			AssistedOption   bool     `json:"assistedOption"`
		} `json:"trialSignupOptions"`
		ServiceGuardLimit           int         `json:"serviceGuardLimit"`
		ServiceAccountConfig        interface{} `json:"serviceAccountConfig"`
		GlobalDelegateAccount       bool        `json:"globalDelegateAccount"`
		ImmutableDelegateEnabled    bool        `json:"immutableDelegateEnabled"`
		OptionalDelegateTaskLimit   interface{} `json:"optionalDelegateTaskLimit"`
		ImportantDelegateTaskLimit  interface{} `json:"importantDelegateTaskLimit"`
		AuthenticationMechanism     string      `json:"authenticationMechanism"`
		ProductLed                  bool        `json:"productLed"`
		PovAccount                  bool        `json:"povAccount"`
		HarnessSupportAccessAllowed bool        `json:"harnessSupportAccessAllowed"`
		Defaults                    struct {
		} `json:"defaults"`
		Encryption                   interface{} `json:"encryption"`
		CrossGenerationAccessEnabled bool        `json:"crossGenerationAccessEnabled"`
	} `json:"resource"`
	ResponseMessages []interface{} `json:"responseMessages"`
}

type ACLRequest struct {
	Permissions []Permission `json:"permissions"`
}

type Permission struct {
	ResourceScope      ResourceScope `json:"resourceScope"`
	ResourceType       string        `json:"resourceType"`
	ResourceIdentifier string        `json:"resourceIdentifier"`
	Permission         string        `json:"permission"`
}

type ResourceScope struct {
	AccountIdentifier string `json:"accountIdentifier"`
	OrgIdentifier     string `json:"orgIdentifier"`
	ProjectIdentifier string `json:"projectIdentifier"`
}

type ACLPrincipal struct {
	PrincipalIdentifier string `json:"principalIdentifier"`
	PrincipalType       string `json:"principalType"`
}

type ACLResourceScope struct {
	AccountIdentifier string `json:"accountIdentifier"`
	OrgIdentifier     string `json:"orgIdentifier"`
	ProjectIdentifier string `json:"projectIdentifier"`
}

type ACLAccessControl struct {
	Permission         string           `json:"permission"`
	ResourceScope      ACLResourceScope `json:"resourceScope"`
	ResourceType       string           `json:"resourceType"`
	ResourceIdentifier string           `json:"resourceIdentifier"`
	Permitted          bool             `json:"permitted"`
}

type ACLResponse struct {
	Status        string      `json:"status"`
	Data          Data        `json:"data"`
	MetaData      interface{} `json:"metaData"`
	CorrelationID string      `json:"correlationId"`
}

type Data struct {
	Principal         ACLPrincipal       `json:"principal"`
	AccessControlList []ACLAccessControl `json:"accessControlList"`
}

// Client defines a log service client.
type Client interface {
	// Validate apikey of an account for auth.
	ValidateApiKey(ctx context.Context, accountID, routingId, apiKey string) error
	ValidateAccessforPipeline(ctx context.Context, cookies []*http.Cookie, request *ACLRequest) (bool, error)
}
