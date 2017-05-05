#!/bin/bash

bin=`which $0`
bin=`dirname ${bin}`

INST_DIR=`cd "$bin/.."; pwd`
MC="net.eric.bank.bod.BankServer"

echo $INST_DIR $MC

. $INST_DIR/bin/config-param.sh

start_server "$INST_DIR" "$MC" "boc" "10021"


