
function start_server()
{
    INST_DIR="$1"
    MAIN_CLASS="$2"
    BANK_CODE="$3"
    PORT="$4"

    CLASS_PATH=/home/bison/workspace/tpc/core/target/classes:/home/bison/workspace/tpc/bank/target/classes
    for f in `ls $INST_DIR/lib/*.jar`
    do
	CLASS_PATH="$CLASS_PATH:$f"
    done

    DB_DIR=$INST_DIR/database
    DB_FILE=$DB_DIR/data_$BANK_CODE
    DB_URL="jdbc:h2:$DB_FILE"

    java -cp $CLASS_PATH $MAIN_CLASS $BANK_CODE $PORT $DB_URL
}
