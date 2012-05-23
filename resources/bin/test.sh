#!/bin/bash
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
