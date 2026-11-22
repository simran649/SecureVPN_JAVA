#!/bin/bash

cd "$(dirname "$0")" || exit 1

read -rsp "MySQL password: " SECURELINK_DB_PASSWORD
echo
export SECURELINK_DB_PASSWORD

read -rsp "Encryption secret: " SECURELINK_SECRET
echo
export SECURELINK_SECRET

mvn javafx:run
