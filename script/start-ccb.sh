#!/bin/sh

bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

PORT=10022
BANK_CODE=ccb

. $bin/config-param.sh

java -cp $CLASS_PATH net.eric.tpc.bank.BankServer $BANK_CODE $PORT $DB_URL

