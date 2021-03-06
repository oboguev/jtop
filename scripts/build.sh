#!/bin/bash

SCRIPTDIR=`dirname "${BASH_SOURCE[0]}"`
SCRIPTDIR=`realpath -s $SCRIPTDIR`
cd "$SCRIPTDIR"
cd ..

function onexit
{
    echo "*** BUILD FAILED ***" 1>&2
    exit 1
}

echo "Building ..."
mvn --quiet clean compile assembly:single || onexit $?
mkdir -p dist
cp target/jtop-*.jar dist/jtop.jar
cp scripts/jtop dist/jtop
cp scripts/jtop.groups dist/jtop.groups
dos2unix dist/jtop 
dos2unix dist/jtop.groups
