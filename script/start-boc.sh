#!/bin/bash

bin=`which $0`
bin=`dirname ${bin}`

INST_DIR=`cd "$bin/.."; pwd`
MC="net.eric.bank.bod.BankServer"

echo $INST_DIR $MC

<<<<<<< HEAD
. $INST_DIR/bin/config-param.sh
=======
. ./config-param.sh
>>>>>>> 635438f0c8a614c6782189a20279246c2709671a

start_server "$INST_DIR" "$MC" "boc" "10021"


