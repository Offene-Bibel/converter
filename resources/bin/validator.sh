#!/bin/bash

#cd to shellscript folder
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done

java -cp $( dirname "$SOURCE" )/../lib/Parser-0.0.1-SNAPSHOT.jar offeneBibel.validator.Validator "$@"
