#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the destinationPath where the symlink file was located
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
#SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

if [[ "$OSTYPE" == "linux-gnu" ]]; then
	UBUNTU_MENUPROXY=0 java -jar $SCRIPT_DIR/StorageCrypt.jar
elif [[ "$OSTYPE" == "darwin" ]]; then
	java -XstartOnFirstThread -jar $SCRIPT_DIR/StorageCrypt.jar
else
	java -jar $SCRIPT_DIR/StorageCrypt.jar
fi
