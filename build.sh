#!/bin/bash
mvn package
mkdir install
cp -r resources/* install
cp -r target/lib install
cp target/Parser-0.0.1-SNAPSHOT.jar install/lib
