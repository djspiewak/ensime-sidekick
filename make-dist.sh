#!/bin/bash

CWD=`pwd`

mkdir /tmp/ensime-sidekick
cp target/scala-2.9.1/ensimesidekick_2.9.1-0.1.jar /tmp/ensime-sidekick/EnsimeSidekick.jar
cp $SCALA_HOME/lib/scala-library.jar /tmp/ensime-sidekick

cd /tmp/ensime-sidekick
tar cf ensime-sidekick.tar *

cd $CWD
mv /tmp/ensime-sidekick/ensime-sidekick.tar target
rm -r /tmp/ensime-sidekick
