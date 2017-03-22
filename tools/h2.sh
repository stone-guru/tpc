#!/bin/sh
H2_HOME=/opt/h2
java -cp "$H2_HOME/bin/h2-1.4.194.jar" org.h2.tools.Server -tcp -tcpPort 9100 \
  -baseDir  ~/workspace/tpc/db

