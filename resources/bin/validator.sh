#!/bin/bash
cd ..
java -cp lib/Parser-0.0.1-SNAPSHOT.jar offeneBibel.validator.Validator $@
