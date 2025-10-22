#! /bin/cash -f

SRD=$(dirname "${BASH_SOURCE[0]}")
pushd $SRD > /dev/null
SRD1=`pwd`
popd > /dev/null

java -jar $SRD1/limbarelay.jar -h llmserver.cs.brown.edu -p 11435







































