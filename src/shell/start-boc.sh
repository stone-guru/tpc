#!/bin/sh
bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

PORT=10021
BANK_CODE=boc

. $bin/config-param.sh

java -cp $CLASS_PATH net.eric.tpc.bank.BankServer $BANK_CODE $PORT $DB_URL

