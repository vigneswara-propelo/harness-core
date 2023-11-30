# bootstrap

![Version: 0.10.5](https://img.shields.io/badge/Version-0.10.5-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.16.0](https://img.shields.io/badge/AppVersion-1.16.0-informational?style=flat-square)

A Helm chart for Kubernetes

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| file://./database | database | 0.10.x |
| file://./networking | networking | 0.9.x |
| file://./rbac | rbac | 0.9.x |
| file://./secrets | harness-secrets | 0.10.x |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| global.database.clickhouse.enabled | bool | `false` |  |
| global.database.clickhouse.secrets.kubernetesSecrets[0].keys.CLICKHOUSE_PASSWORD | string | `""` |  |
| global.database.clickhouse.secrets.kubernetesSecrets[0].keys.CLICKHOUSE_USERNAME | string | `""` |  |
| global.database.clickhouse.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.database.clickhouse.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.CLICKHOUSE_PASSWORD.name | string | `""` |  |
| global.database.clickhouse.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.CLICKHOUSE_PASSWORD.property | string | `""` |  |
| global.database.clickhouse.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.CLICKHOUSE_USERNAME.name | string | `""` |  |
| global.database.clickhouse.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.CLICKHOUSE_USERNAME.property | string | `""` |  |
| global.database.clickhouse.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.database.clickhouse.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.database.minio.installed | bool | `true` |  |
| global.database.minio.secrets.kubernetesSecrets[0].keys.S3_PASSWORD | string | `""` |  |
| global.database.minio.secrets.kubernetesSecrets[0].keys.S3_USER | string | `""` |  |
| global.database.minio.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.database.minio.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.S3_PASSWORD.name | string | `""` |  |
| global.database.minio.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.S3_PASSWORD.property | string | `""` |  |
| global.database.minio.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.S3_USER.name | string | `""` |  |
| global.database.minio.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.S3_USER.property | string | `""` |  |
| global.database.minio.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.database.minio.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.database.mongo.extraArgs | string | `""` |  |
| global.database.mongo.hosts | list | `[]` | provide default values if mongo.installed is set to false |
| global.database.mongo.installed | bool | `true` |  |
| global.database.mongo.passwordKey | string | `""` |  |
| global.database.mongo.protocol | string | `"mongodb"` |  |
| global.database.mongo.secretName | string | `""` |  |
| global.database.mongo.secrets.kubernetesSecrets[0].keys.MONGO_PASSWORD | string | `""` |  |
| global.database.mongo.secrets.kubernetesSecrets[0].keys.MONGO_REPLICA_SET_KEY | string | `""` |  |
| global.database.mongo.secrets.kubernetesSecrets[0].keys.MONGO_USER | string | `""` |  |
| global.database.mongo.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_PASSWORD.name | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_PASSWORD.property | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_REPLICA_SET_KEY.name | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_REPLICA_SET_KEY.property | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_USER.name | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_USER.property | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.database.mongo.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.database.mongo.userKey | string | `""` |  |
| global.database.postgres.installed | bool | `true` |  |
| global.database.postgres.secrets.kubernetesSecrets[0].keys.POSTGRES_PASSWORD | string | `""` |  |
| global.database.postgres.secrets.kubernetesSecrets[0].keys.POSTGRES_USER | string | `""` |  |
| global.database.postgres.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.database.postgres.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.POSTGRES_PASSWORD.name | string | `""` |  |
| global.database.postgres.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.POSTGRES_PASSWORD.property | string | `""` |  |
| global.database.postgres.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.POSTGRES_USER.name | string | `""` |  |
| global.database.postgres.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.POSTGRES_USER.property | string | `""` |  |
| global.database.postgres.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.database.postgres.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.database.redis.extraArgs | string | `""` |  |
| global.database.redis.hosts | list | `[]` | provide default values if redis.installed is set to false |
| global.database.redis.installed | bool | `true` |  |
| global.database.redis.passwordKey | string | `""` |  |
| global.database.redis.protocol | string | `""` |  |
| global.database.redis.secretName | string | `""` |  |
| global.database.redis.secrets.kubernetesSecrets[0].keys.REDIS_PASSWORD | string | `""` |  |
| global.database.redis.secrets.kubernetesSecrets[0].keys.REDIS_USERNAME | string | `""` |  |
| global.database.redis.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.database.redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_PASSWORD.name | string | `""` |  |
| global.database.redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_PASSWORD.property | string | `""` |  |
| global.database.redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_USERNAME.name | string | `""` |  |
| global.database.redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_USERNAME.property | string | `""` |  |
| global.database.redis.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.database.redis.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.database.redis.userKey | string | `""` |  |
| global.database.timescaledb.certKey | string | `""` |  |
| global.database.timescaledb.certName | string | `""` |  |
| global.database.timescaledb.extraArgs | string | `""` |  |
| global.database.timescaledb.hosts | list | `[]` |  |
| global.database.timescaledb.installed | bool | `true` |  |
| global.database.timescaledb.passwordKey | string | `""` |  |
| global.database.timescaledb.protocol | string | `""` |  |
| global.database.timescaledb.secretName | string | `""` | TimescaleDB secrets |
| global.database.timescaledb.secrets.kubernetesSecrets[0].keys.TIMESCALEDB_ADMIN_PASSWORD | string | `""` |  |
| global.database.timescaledb.secrets.kubernetesSecrets[0].keys.TIMESCALEDB_PASSWORD | string | `""` |  |
| global.database.timescaledb.secrets.kubernetesSecrets[0].keys.TIMESCALEDB_REPLICATION_PASSWORD | string | `""` |  |
| global.database.timescaledb.secrets.kubernetesSecrets[0].keys.TIMESCALEDB_SSL_ROOT_CERT | string | `""` |  |
| global.database.timescaledb.secrets.kubernetesSecrets[0].keys.TIMESCALEDB_USERNAME | string | `""` |  |
| global.database.timescaledb.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_ADMIN_PASSWORD.name | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_ADMIN_PASSWORD.property | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_PASSWORD.name | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_PASSWORD.property | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_REPLICATION_PASSWORD.name | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_REPLICATION_PASSWORD.property | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_SSL_ROOT_CERT.name | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_SSL_ROOT_CERT.property | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_USERNAME.name | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.TIMESCALEDB_USERNAME.property | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.database.timescaledb.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.database.timescaledb.sslEnabled | bool | `false` | Enable TimescaleDB SSL |
| global.database.timescaledb.userKey | string | `""` |  |
| global.ingress.className | string | `"harness"` |  |
| global.ingress.enabled | bool | `false` |  |
| global.ingress.hosts[0] | string | `"myhost.example.com"` |  |
| global.ingress.loadBalancerEnabled | bool | `false` |  |
| global.ingress.loadBalancerIP | string | `"0.0.0.0"` |  |
| global.ingress.tls.enabled | bool | `true` |  |
| global.ingress.tls.secretName | string | `"harness-cert"` |  |
| global.istio | object | `{"enabled":false,"gateway":{"create":true,"name":"","namespace":"","port":443,"protocol":"HTTPS","selector":{"istio":"ingressgateway"}},"hosts":["*"],"istioGatewayServiceUrl":"","strict":false,"tls":{"credentialName":null,"minProtocolVersion":"TLSV1_2","mode":"SIMPLE"},"virtualService":{"gateways":[""],"hosts":null}}` | Istio Ingress Settings |
| global.istio.gateway.create | bool | `true` | Enable to create istio-system gateway |
| global.istio.gateway.name | string | `""` | override the name of gateway |
| global.istio.gateway.namespace | string | `""` | override the name of namespace to deploy gateway |
| global.istio.gateway.selector | object | `{"istio":"ingressgateway"}` | adds a gateway selector |
| global.istio.hosts | list | `["*"]` | add global.istio.istioGatewayServiceUrl in hosts if global.istio.istioGatewayServiceUrl is not empty. |
| global.istio.istioGatewayServiceUrl | string | `""` | set to istio gateway's k8s service FQDN for internal use case. eg "internal-istio-gateway.istio-system.svc.cluster.local" If not set, internal request routing would happen via global.loadbalancerUrl |
| global.istio.virtualService.hosts | string | `nil` | add global.istio.istioGatewayServiceUrl in hosts if global.istio.istioGatewayServiceUrl is not empty. |
| global.license.cg | string | `""` |  |
| global.license.ng | string | `""` |  |
| global.license.secrets.kubernetesSecrets[0].keys.CG_LICENSE | string | `""` |  |
| global.license.secrets.kubernetesSecrets[0].keys.NG_LICENSE | string | `""` |  |
| global.license.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.license.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.CG_LICENSE.name | string | `""` |  |
| global.license.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.CG_LICENSE.property | string | `""` |  |
| global.license.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.NG_LICENSE.name | string | `""` |  |
| global.license.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.NG_LICENSE.property | string | `""` |  |
| global.license.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.license.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| global.smtpCreateSecret.SMTP_HOST | string | `""` |  |
| global.smtpCreateSecret.SMTP_PASSWORD | string | `""` |  |
| global.smtpCreateSecret.SMTP_PORT | string | `""` |  |
| global.smtpCreateSecret.SMTP_USERNAME | string | `""` |  |
| global.smtpCreateSecret.SMTP_USE_SSL | string | `""` |  |
| global.smtpCreateSecret.enabled | bool | `false` |  |
| global.smtpCreateSecret.secrets.kubernetesSecrets[0].keys.SMTP_HOST | string | `""` |  |
| global.smtpCreateSecret.secrets.kubernetesSecrets[0].keys.SMTP_PASSWORD | string | `""` |  |
| global.smtpCreateSecret.secrets.kubernetesSecrets[0].keys.SMTP_PORT | string | `""` |  |
| global.smtpCreateSecret.secrets.kubernetesSecrets[0].keys.SMTP_USERNAME | string | `""` |  |
| global.smtpCreateSecret.secrets.kubernetesSecrets[0].keys.SMTP_USE_SSL | string | `""` |  |
| global.smtpCreateSecret.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_HOST.name | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_HOST.property | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_PASSWORD.name | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_PASSWORD.property | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_PORT.name | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_PORT.property | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_USERNAME.name | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_USERNAME.property | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_USE_SSL.name | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.SMTP_USE_SSL.property | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| global.smtpCreateSecret.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| harness-secrets.enabled | bool | `true` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
