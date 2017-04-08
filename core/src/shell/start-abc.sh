#!/bin/sh
bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

PORT=10024
BANK_CODE=abc

. $bin/config-param.sh

java -cp $CLASS_PATH net.eric.tpc.coor.CoorServer $BANK_CODE $PORT $DB_URL

