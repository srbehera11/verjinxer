#!/usr/bin/env python
from __future__ import with_statement
import re


fname = "./src/verjinxer/util/HugeShortArray.java"
out = [("Byte","byte",1), ("Int","int",4), ("Long","long",8)]

pbegincut = re.compile("==begincut")
pendcut = re.compile("==endcut")
pshort = re.compile("short")
pShort = re.compile("Short")
pBuffer = re.compile('.asByteBuffer\(\)')
pSIZE = re.compile('Int.SIZE')
cut = 0

for info in out:
    Typename = info[0]
    typename = info[1]
    typesize = info[2]
    foname = pShort.sub(Typename, fname); # subst. 'Short' by oname in fname
    fout = file(foname, "w")
    with file(fname,"r") as fin:
        for line in fin:
            if cut>0:
                if pendcut.search(line): cut -= 1
                continue
            if pbegincut.search(line):
                cut += 1
                continue
            newline = pShort.sub(Typename, pshort.sub(typename, line))
            newline = pBuffer.sub('', newline);
            newline = pSIZE.sub('Integer.SIZE', newline);
            fout.write(newline)
    fout.close()

