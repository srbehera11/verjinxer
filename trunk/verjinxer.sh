#!/bin/sh
# 32-bit machines:
java -ea -Xms1900M -Xmx1900M -jar ${HOME}/dev/verjinxer/dist/VerJInxer.jar ${*}
