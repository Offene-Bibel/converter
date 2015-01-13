#!/bin/bash
set -e
cd ../tmp

resources="../resources"
results="../results"
rm -r offbileModule &> /dev/null || true

mkdir offbileModule
mkdir offbileModule/modules
mkdir offbileModule/modules/texts
mkdir offbileModule/modules/texts/ztext
mkdir offbileModule/modules/texts/ztext/offbile
mkdir offbileModule/mods.d

cp ${resources}/offbile.conf offbileModule/mods.d

#xmllint --noout --schema http://www.bibletechnologies.net/osisCore.2.1.1.xsd offeneBibelModule.osis
#~/Documents/sword/svn/sword/utilities/osis2mod offbileModule/modules/texts/rawtext/offbile/ offeneBibelLesefassungModule.osis -d 8
~/Documents/sword/svn/sword/utilities/osis2mod offbileModule/modules/texts/ztext/offbile/ ${results}/offeneBibelLesefassungModule.osis -z

cp -r offbileModule/* ~/.sword

rm -r offbistModule &> /dev/null || true

mkdir offbistModule
mkdir offbistModule/modules
mkdir offbistModule/modules/texts
mkdir offbistModule/modules/texts/ztext
mkdir offbistModule/modules/texts/ztext/offbist
mkdir offbistModule/mods.d

cp ${resources}/offbist.conf offbistModule/mods.d

#xmllint --noout --schema http://www.bibletechnologies.net/osisCore.2.1.1.xsd offeneBibelModule.osis
#~/Documents/sword/svn/sword/utilities/osis2mod offbistModule/modules/texts/rawtext/offbist/ offeneBibelStudienfassungModule.osis -d 8
~/Documents/sword/svn/sword/utilities/osis2mod offbistModule/modules/texts/ztext/offbist/ ${results}/offeneBibelStudienfassungModule.osis -z

cp -r offbistModule/* ~/.sword

rm OffBiModule.zip || true
zip -r OffBiModule.zip offbileModule offbistModule
