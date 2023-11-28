#!/bin/bash
############################
# Module Dependency Check
# See BT-3430, BT-6045
############################
# DEBUG="true"
checkresult="false"
set +x

function update_jira_label() {
  if [ "$1" == "null" ]; then
    echo "PR Title missing Jira case - Exiting"
    exit 1
  fi

  if [ "$2" == "null" ]; then
    echo "Jira Label missing - Exiting"
    exit 1
  fi

  response=$(curl -X PUT https://harness.atlassian.net/rest/api/2/issue/$1 --write-out '%{http_code}' --output /dev/null --silent --user ${JIRA_USERNAME}:${JIRA_PASSWORD} -H "Content-Type: application/json" -d '{
    "update": {
      "labels" : [
        { "add": "'"$2"'" }
      ] } }'
  )

  if  [[ "$response" -eq 200 ]] ; then
    echo "Successfully added label $2 to $1"
  elif  [[ "$response" -ne 204 ]] ; then
    echo "Failed to add label to $1 - Response code: $response"
  elif [[ "$response" -eq 404 ]] ; then
    echo "404 response indicates that user $JIRA_USERNAME does not have permissions to update labels on $1"
  fi
}

function check_for_module() {
  checkresult="false"
  if [ "$1" == "null" ]; then
    echo "Missing PR Number to check"
    exit 1
  fi

  if [ "$3" == "null" ]; then
    echo "Missing label to apply"
    exit 1
  fi

  if [ "$4" == "null" ]; then
    echo "Missing module"
    exit 1
  fi

  ##############################
  # Read PR diff into temp file
  ##############################
  resp=$(curl -L -s "https://patch-diff.githubusercontent.com/raw/harness/harness-core/pull/$1.diff" --write-out '%{http_code}' --output pr.diff)
  if [ $? -eq 0 ];
  then
    if [ $resp -ne 200 ];
    then
        echo "Could not download ${ghprbPullId}.diff from github"
        exit 1
    fi
  else
    echo "curl command malformed."
    exit 1
  fi

  # Extract changed files
  cat pr.diff | grep "^diff " | sed 's/diff --git a\///' | sed 's/ b\/.*$//' > $tmpdiff

  if [ "$DEBUG" == "true" ]; then cat $tmpdiff; fi

  # Compare each line in tmpdif to the cleaned outputs from bazel
  awk 'NR==FNR {a[$0]; next} $1 in a' $tmpdiff $tmpdeps>$tmpawk

  anyfound=$(cat $tmpawk | wc -l)
  if [ $anyfound != "0" ];
  then
    echo "**PR $1 HAS $4 changes**"
    cat $tmpawk
    checkresult="true"
    if [ "$2" == "null" ]; then
      echo "No Jira key provided - skipping Jira update"
    else
      update_jira_label $2 "$3"
    fi
  else
    echo "PR $1 does NOT have $4 changes"
    checkresult="false"
  fi
}

function runPRTests() {
  positive=("45911" "45770" "45426" "44756" "46180" "46768" "45409" "44781")
  negative=("46381" "46327" "46271" "45585")

  echo "***** Running Positive Tests *****"
  for apos in ${positive[@]}; do
    check_for_module $apos "null" "Affects_delegate" "260-delegate"
    if [ "$checkresult" != "true" ]; then
      echo "Test Failed - $apos should have found delegate change!"
      exit 1
    fi
  done
  echo "***** Running negative Tests *****"
  for aneg in ${negative[@]}; do
    check_for_module $aneg "null" "Affects_delegate" "260-delegate"
    if [ "$checkresult" != "false" ]; then
      echo "Test Failed - $aneg should NOT have found delegate change!"
      exit 1
    fi
  done
}
set +e
######################
# Read Jira Ticket
######################
echo "ghprbPullTitle=$ghprbPullTitle"
echo "ghprbPullId=$ghprbPullId"
COMMIT_CONTENT="\[feat]|\[fix]|\[techdebt]|\[hotfixpreqa]|feat|fix|techdebt|hotfixpreqa"
SHDIR=`dirname "$0"`
echo "SHDIR: $SHDIR"
PROJECTS=$(<$SHDIR/jira-projects.txt)
echo "PROJECTS: $PROJECTS"

PR_MESSAGE=`echo "${ghprbPullTitle}" | grep -iE "^(${COMMIT_CONTENT}[\ ]*):[\ ]*\[(${PROJECTS})-[0-9]+][:\ ]*"`
echo "PR_MESSAGE=$PR_MESSAGE"
KEY=$(echo "${ghprbPullTitle}" | grep -o -iE '('$PROJECTS')-[0-9]+')
echo "KEY=$KEY"
if [ "$KEY" == "" ]; then
  echo "PR Title doesn't have valid Jira Key - exiting"
  exit 1
fi
######################
# Setup Bazel
######################
export GOOGLE_CREDENTIALS_FILE=/tmp/gcp_creds.json
scripts/bazel/generate_credentials.sh
cat test-util.bzl
cp scripts/jenkins/module-dependency-check-bazelignore.txt .bazelignore
echo "************* .bazelignore files *************"
cat .bazelignore

######################
# Clean up temp files
# on exit
######################
tmpawk=$(mktemp -q /tmp/awktemp.XXXXXX)
tmpdeps=$(mktemp -q /tmp/depstemp.XXXXXX)
tmpdiff=$(mktemp -q /tmp/diff.XXXXXX)
trap 'rm -f -- "$tmpawk"' EXIT
trap 'rm -f -- "$tmpdeps"' EXIT
trap 'rm -f -- "$tmpdiff"' EXIT

######################
# Do work...
######################
if [ "$runtests" != "true" ]; then
  #################################
  # Read modules from file.
  # File format should be:
  # modulename jiraLabel
  # e.g.
  # 260-delegate Affects_delegate
  # access-control Affects_access_control
  # ... etc
  #################################
  configs="scripts/jenkins/module-dependency-check-modules.txt"
  while IFS= read -r line
  do
    read -r -a pieces <<< "$line"
    module="${pieces[0]}"
    label="${pieces[1]}"
    echo "Querying Bazel for $module:module / Jira label $label"
    bazel query "deps(//$module:module)" | grep -v  ^@ | sed 's/^.*:module$//' | sed 's/$\/\///' | sed 's/\/\///' | sed 's/:/\//' | sed '/^$/d'> $tmpdeps
    if [ "$DEBUG" == "true" ]; then tail -n100 $tmpdeps; fi
    check_for_module ${ghprbPullId} ${KEY} ${label} ${module}
  done < "$configs"
else
  #################################
  # Test logic changes - the tests
  # Should run without issues
  #################################
  module="260-delegate"
  bazel query "deps(//$module:module)" | grep -v  ^@ | sed 's/^.*:module$//' | sed 's/$\/\///' | sed 's/\/\///' | sed 's/:/\//' | sed '/^$/d'> $tmpdeps
  runPRTests
fi
