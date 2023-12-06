#!/bin/bash
DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC
curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-455.0.0-linux-x86_64.tar.gz
tar -xf google-cloud-cli-455.0.0-linux-x86_64.tar.gz
rm google-cloud-cli-455.0.0-linux-x86_64.tar.gz
./google-cloud-sdk/install.sh --quiet
# Fix Bash
echo "alias ls='ls --color=auto'">>.bashrc
echo "alias grep='grep --color=auto'">>.bashrc
echo "alias ll='ls -alF'">>.bashrc
echo "alias la='ls -A'">>.bashrc
echo "alias l='ls -CF'">>.bashrc
echo "export PYTHON_BIN_PATH=/opt/harness-delegate/google-cloud-sdk/platform/bundledpythonunix/bin">>.bashrc
echo 'export PATH=$PATH:/opt/harness-delegate/google-cloud-sdk/bin:/opt/harness-delegate/google-cloud-sdk/platform/bundledpythonunix/bin'>>.bashrc

