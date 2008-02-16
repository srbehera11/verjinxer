#!/bin/sh
# 64-bit machines:
java -server -d64 -ea -Xms8G -Xmx8G -jar ${HOME}/dev/verjinxer/dist/VerJInxer.jar ${*}
