#!/bin/sh

bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

LIB_DIR="$bin"/../lib
DB_DIR="$bin"/../database

java -cp $LIB_DIR/h2-1.4.194.jar org.h2.tools.Console -tcp -tcpPort 9100 -web -baseDir $DB_DIR

