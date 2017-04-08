#!/bin/sh
bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

PORT=10023
BANK_CODE=cbrc

. $bin/config-param.sh

java -cp $CLASS_PATH net.eric.tpc.regulator.RegulatorServer $BANK_CODE $PORT $DB_URL

