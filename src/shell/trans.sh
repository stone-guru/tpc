#!/bin/sh

bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

PORT=10024
CBRC_SERVER=127.0.0.1

. $bin/config-param.sh

java -cp $CLASS_PATH net.eric.tpc.terminal.Main -h $CBRC_SERVER -p $PORT "$@"

