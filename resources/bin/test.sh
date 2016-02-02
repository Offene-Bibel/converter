#!/bin/bash

#cd to shellscript folder. Cargo-cult++
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
cd $DIR

cd ..
mkdir tmp
cd tmp
mkdir luther
mkdir luther/modules
mkdir luther/modules/texts
mkdir luther/modules/texts/rawtext
mkdir luther/modules/texts/rawtext/lutcust
mkdir luther/mods.d

cp lutcust.conf luther/mods.d

#xmllint --noout --schema http://www.bibletechnologies.net/osisCore.2.1.1.xsd offeneBibelModule.osis
osis2mod luther/modules/texts/rawtext/lutcust/ luther1912_osis.xml
