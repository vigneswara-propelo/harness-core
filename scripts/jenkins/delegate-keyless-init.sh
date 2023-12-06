#!/bin/bash
sudo curl -s -L -o /etc/yum.repos.d/google-cloud-sdk.repo https://harness.jfrog.io/artifactory/BuildsTools/yum-repos/google-cloud-sdk.repo
sudo microdnf install -y yum
sudo yum update -y
sudo yum install -y python3
sudo yum install -y python3-pip
sudo yum install -y python3-requests
sudo yum install -y google-cloud-cli --nodocs --skip-broken
sudo yum install -y mongodb-enterprise-4.2.18 mongodb-enterprise-server-4.2.18 mongodb-enterprise-mongos-4.2.18 mongodb-enterprise-tools-4.2.18 --nodocs --skip-broken
DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC
sudo yum install -y tzdata wget sudo openssl jq gnupg unzip --nodocs --skip-broken
