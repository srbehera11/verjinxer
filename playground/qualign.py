# Experiments to align color-space sequences (coseqs?)
#
# nucleotides are
#    A=0, C=1, G=2, T=3, X=4                            # capital letters
# colors are
#    blue=b=0, green=g=1, yellow=y=2, red=r=3, xxx=x=4  # small letters

import itertools
import random

##########################################################################
_DEBUG=False

DNA  = "ACGTX"
DNAx = DNA[:-1]
_DNACodeTable = {'A':0, 'C':1, 'G':2, 'T':3, 'U':3 }
for c in "RYKMSWBDHVNX": _DNACodeTable[c]=4
def DNACode(nuc):      return _DNACodeTable[nuc]
def DNAPairCode(pair): return len(DNA)*DNACode(pair[0]) + DNACode(pair[1])

COLOR = "bgyrx"
COLORx = COLOR[:-1]
NCOLORS = len(COLOR)
NCOLORSx = len(COLORx)
_COLORCodeTable = {'b':0, 'g':1, 'y':2, 'r':3, 'x':4, 0:0, 1:1, 2:2, 3:3, 4:4 }
def ColorCode(col): return _COLORCodeTable[col]
def ColorPairCode(cpair):
    """return an integer representing a pair of color values"""
    if _DEBUG: print(cpair)
    return len(COLOR)*ColorCode(cpair[0]) + ColorCode(cpair[1])
NCOLPAIRS = len(COLOR)*len(COLOR)

_DNAPairColor = ['b', 'g', 'y', 'r', 'x',
                 'g', 'b', 'r', 'y', 'x',
                 'y', 'r', 'b', 'g', 'x',
                 'r', 'y', 'g', 'b', 'x',
                 'x', 'x', 'x', 'x', 'x']
def DNAPairColor(pair):  return _DNAPairColor[DNAPairCode(pair)]

##########################################################################

# compute good colorpair subsitiutions
def _goodColorPairSubstitutions():
    goodsub = [ [] for i in range(NCOLPAIRS) ]
    for seq in itertools.product(DNAx,DNAx,DNAx):
        cp = ColorPairCode((DNAPairColor(seq[0:2]),DNAPairColor(seq[1:3])))
        subs = [[seq[0], c, seq[2]] for c in DNAx if c!=seq[1]]
        for sub in subs:
            dp = ColorPairCode((DNAPairColor(sub[0:2]),DNAPairColor(sub[1:3])))
            goodsub[cp].append(dp)
    return [frozenset(gs) for gs in goodsub]

# compute good (colorpair, color) indels
def _goodColorPairIndels():
    goodindel = [ [] for i in range(NCOLPAIRS) ]
    for seq in itertools.product(DNAx,DNAx,DNAx):
        cp = ColorPairCode((DNAPairColor(seq[0:2]),DNAPairColor(seq[1:3])))
        indel = [seq[0],seq[2]]
        d = ColorCode(DNAPairColor(indel))
        goodindel[cp].append(d)
    return [frozenset(gi) for gi in goodindel]

    
def scorematrices(match=1, mismatch=-1, indel=-1, goodmismatch=1, goodindel=0):
    """returns a tuple (M1,D1,M2,D2) of score matrices for
    1-matches, 1-indels, 2-matches, 2-indels
    """
    M1 = [[2*mismatch]*NCOLORS for i in range(NCOLORS)]    # bad 1-mismatches
    for i in range(NCOLORSx): M1[i][i] = match             # 1-matches
    M2 = [[mismatch]*NCOLPAIRS for i in range(NCOLPAIRS)]  # bad 2-mismatches
    for p in itertools.product(COLORx,COLORx):
        i = ColorPairCode(p)
        M2[i][i] = match                                   # 2-matches
    for (i,s) in enumerate(_goodColorPairSubstitutions()):
        for j in s: M2[i][j] = goodmismatch                # good 2-mismatches
        
    D1 = 2*indel                                           # 1-indels
    D2 = [[indel]*NCOLORS for i in range(NCOLPAIRS)]       # bad 2-indels
    for (i,s) in enumerate(_goodColorPairIndels()):
        for j in s: D2[i][j] = goodindel                   # good 2-indels
        
    minscore = min(mismatch,2*mismatch,2*indel,indel)
    return (M1,D1,M2,D2, minscore)

##########################################################################        
    
_MAXQUAL = 80
_MINQUAL = 20
def minqual(qList):
    """return minimum of a list of quality values (or MAXQUAL)"""
    return max(min(min(qList),_MAXQUAL),_MINQUAL)

def transpose(M):
    """return transpose of a matrix (list of lists)"""
    return [[M[j][i] for j in range(len(M))] for i in range(len(M[0]))]



def qualign(read, genome, scores=None, readquality=None, savememory=True):
    """cmopute quality-weighted alignment distance (edit distance)
    of a read against (a part of) a genome.
    Both read and genome must be given in color space format.
    Example: T3210, G230001223210, ...
    (Each element is 0,1,2,3; first nucleotide must be given but is ignored.)
    Quality values range from 0 (bad) to 99? (good).
    If quality values are not given, a constant of 1 is assumed.
    It is assumed that all parameters support the [] operator.
    See source code for details. WORK IN PROGRESS.
    """
    RL = len(read)
    GL = len(genome)
    if scores==None: scores = scorematrices()
    (M1,D1,M2,D2, minscore) = scores
    if readquality==None: readquality = [1]*RL
    
    # pre-compute read pair codes and quality values for read singletons and pairs
    rpair = [NCOLPAIRS-1,NCOLPAIRS-1] + [ColorPairCode(read[i-1:i+1]) for i in range(2,RL)]
    q2    = [0,0] + [minqual(readquality[i-1:i+1]) for i in range(2,RL)]
    q1    = [0] + [minqual(readquality[i:i+1]) for i in range(1,RL)]
    
    # initialize RL x GL edit matrix, stored in column-major order
    # row/column 0 conveniently corresponds to explicit nucleotide
    if savememory:
        GL3 = GL//3 + 1 # better safe than sorry: +1
        matrix = [[None]*RL,[None]*RL,[None]*RL]*GL3 #repeat same 3 columns
    else:
        matrix = [[None]*RL for i in range(GL)]
    matrix[0] = [0]*RL # TODO -- but works for the moment
    matrix[1] = [0]*RL # TODO -- but works for the moment
    best  = -2*(RL+1)*abs(minscore) # best score so far: -infinity
    bestj = None                    # j-position of best score
    for j in range(2,GL):
        g     = ColorCode(genome[j])
        gpair = ColorPairCode(genome[j-1:j+1])
        matrix[j][0]=0
        matrix[j][1]=0 # TODO -- but works for the moment
        for i in range(2,RL):
            r     = ColorCode(read[i])
            scorem1 = matrix[j-1][i-1] + M1[g][r]*q1[i]
            scorem2 = matrix[j-2][i-2] + M2[gpair][rpair[i]]*q2[i]
            scorel1 = matrix[j-1][i]   + D1*q1[i]
            scoreu1 = matrix[j][i-1]   + D1*q1[i]
            scorel2 = matrix[j-2][i-1] + D2[gpair][r]*q1[i]
            scoreu2 = matrix[j-1][i-2] + D2[rpair[i]][g]*q2[i]
            matrix[j][i] = max(scorem1,scorem2,scorel1,scorel2,scoreu1,scoreu2)
        if matrix[j][RL-1] > best:
            best  = matrix[j][RL-1]
            bestj = j
    return (best, bestj, matrix)

##########################################################################        

def test1():
    (M1,D1,M2,D2) = scorematrices()
    for m in M1: print(m)
    for m in M2: print(m)
    print(D1)
    for d in D2: print(d)


genome = "G1221311123033200201111122113213301100000300201101001000000022000003320221030111231120300111132222130100100010000110022010023011101230230101"
read1 = "G133011000003002011010010000000220000"
read2 = "G1330110000030020110010000000220000"

def test2():
    smat = scorematrices()
    r = [None] + list(map(int,read2[1:]))
    g = [None] + list(map(int,genome[1:]))
    (opt,optj) = qualign(r,r, smat)
    (score,endj) = qualign(r,g, smat)
    print(score, endj, "| opt:", opt, optj, "| readlen:",len(r))


def test3(Lread=35, Lgenome=100, err=1, iterations=1000):
    smat = scorematrices()

    for tt in range(iterations):
        read = [9,0] + [random.randint(0,3) for i in range(Lread)]
        g = read[2:]
        for k in range(err):
            p = random.randint(0,len(g)-1)
            choice = random.randint(0,2)
            if choice==0:
                g[p] = random.randint(0,3)
            elif choice==1:
                del g[p]
            else:
                g.insert(p,random.randint(0,3))
        Lg = len(g)
        Lsuff = random.randint(1,Lgenome-Lg-1)
        Lpref = Lgenome-Lg-Lsuff
        genome = [9,0] + [random.randint(0,3) for i in range(Lpref)] + g + [random.randint(0,3) for i in range(Lsuff)]
        qual = [random.randint(_MINQUAL,_MAXQUAL) for i in range(len(genome))]

        #print("".join(map(str,read)))
        #print("".join(map(str,genome)))
        (opt,optj, mat) = qualign(read, read, smat, qual)
        (score,endj, mat) = qualign(read, genome, smat, qual)
        delta = (opt-score)/_MAXQUAL
        print(score,opt, endj,optj, delta)
        
                                                
##########################################################################        

def lookAtQualityValues(fname):
    with open(fname) as f:
        for line in f:
            if line[0] in "#>": continue
            for q in line.split(): print(q)

def lookAtQualityValuesbyPosition(fname):
    with open(fname) as f:
        for line in f:
            if line[0] in "#>": continue
            for q in enumerate(line.split()):
                print(q, end=" ")

##########################################################################        
    
if __name__=="__main__":
    random.seed(17)
    #test3()
    lookAtQualityValues("/home2/bio/nb/ncrna/data/s0329_20090331_552to561_613to614_2_552_561_F3_QV.qual")
    
