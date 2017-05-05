#!/bin/sh
bin=`which $0`
bin=`dirname ${bin}`

INST_DIR=`cd "$bin/.."; pwd`
DB_DIR="$INST_DIR/database"

CLASS_PATH=~/Workspace/tpc/core/target/classes:~/Workspace/tpc/bank/target/classes
for f in `ls $INST_DIR/lib/*.jar`
do
    CLASS_PATH="$CLASS_PATH:$f"
done

java -cp $CLASS_PATH net.eric.bank.tool.DbTool BOC $DB_DIR
java -cp $CLASS_PATH net.eric.bank.tool.DbTool CCB $DB_DIR
java -cp $CLASS_PATH net.eric.bank.tool.DbTool ABC $DB_DIR
java -cp $CLASS_PATH net.eric.bank.tool.DbTool CBRC $DB_DIR

