#!/bin/sh
bin=`which $0`
bin=`dirname ${bin}`
bin=`cd $bin; pwd`

LIB_DIR="$bin"/../../deploy/lib
TPC_DIR="/home/bison/workspace/tpc"
CLASS_PATH="$TPC_DIR/core/target/classes:$TPC_DIR/core/target/test-classes"
for f in `ls $LIB_DIR/*.jar`
do
  CLASS_PATH="$CLASS_PATH:$f"
done

java -cp $CLASS_PATH net.eric.tpc.proto.ProtoTotalTestServer

