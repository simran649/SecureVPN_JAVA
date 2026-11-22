#!/bin/bash

cd "$(dirname "$0")" || exit 1

read -rsp "MySQL password: " SECURELINK_DB_PASSWORD
echo
export SECURELINK_DB_PASSWORD

mvn exec:java -Dexec.mainClass=VPNServer
