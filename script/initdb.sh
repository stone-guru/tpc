#!/bin/sh
bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`


. $bin/config-param.sh

DB_DIR="$bin/../database"
echo $CLASS_PATH

java -cp $CLASS_PATH net.eric.tpc.tool.DbTool BOC $DB_DIR
java -cp $CLASS_PATH net.eric.tpc.tool.DbTool CCB $DB_DIR
java -cp $CLASS_PATH net.eric.tpc.tool.DbTool ABC $DB_DIR
java -cp $CLASS_PATH net.eric.tpc.tool.DbTool CBRC $DB_DIR

