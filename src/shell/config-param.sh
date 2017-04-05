

DB_DIR="$bin/../database"
DB_FILE="$DB_DIR/data_$BANK_CODE"
DB_URL="jdbc:h2:$DB_FILE"

LIB_DIR="$bin"/../lib

#CLASS_PATH=$LIB_DIR/eric-tpc-0.1.jar
CLASS_PATH=/home/bison/workspace/tpc/target/classes
for f in `ls $LIB_DIR/*.jar`
do
  CLASS_PATH="$CLASS_PATH:$f"
done

#CLASS_PATH=$CLASS_PATH:$LIB_DIR/guava-20.0.jar
#CLASS_PATH=$CLASS_PATH:$LIB_DIR/h2-1.4.194.jar
#CLASS_PATH=$CLASS_PATH:$LIB_DIR/log4j-1.2.17.jar
#CLASS_PATH=$CLASS_PATH:$LIB_DIR/mina-core-2.0.16.jar
#CLASS_PATH=$CLASS_PATH:$LIB_DIR/mybatis-3.4.2.jar
#CLASS_PATH=$CLASS_PATH:$LIB_DIR/slf4j-api-1.7.21.jar
#CLASS_PATH=$CLASS_PATH:$LIB_DIR/slf4j-log4j12-1.7.21.jar

