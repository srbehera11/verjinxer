# some experiments to align color-space sequences (coseqs?)

def transpose(M):
    """return transpose of a matrix (list of lists)"""
    return [[M[j][i] for j in range(len(M[0]))] for i in range(len(M))]


MAXQUAL = 80
INDELSCORE = -MAXQUAL

def minqual(qList):
    """return minimum of a list of quality values (or MAXQUAL)"""
    return min(min(qList),MAXQUAL)

def colorpair(cList):
    """return an integer 0..15 representing a pair of color values"""
    return 4*cList[0]+cList[1]

def computeMatchcost():
    M = [[-2]*16 for i in range(16)] # all mismatch
    for i in range(16): M[i][i] = 2  # perfect matches
    # TODO: almost perfect matches
    return M

def qualign(read, genome, readqual=None):
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
    matchcost = computeMatchcost()
    RL = len(read)
    GL = len(genome)
    # initialize RL x GL edit matrix, stored in column-major order
    # row/column 0 conveniently corresponds to ignored nucleotide
    matrix = [[None]*RL for i in range(GL)]
    matrix[0] = [INDELCOST*i for i in range(RL)]
    ## TODO: matrix[1]
    matrix[1] = [genome[1]==c forINDELCOST*i for i in range(RL)]
    for j in range(2,GL):
        # compute matrix[j]
        gpair = colorpair(genome[j-1:j])
        matrix[j][0]=0
        matrix[j][1]=0 # TODO!
        for i in range(2,RL):
            q = minqual(readqual[i-1:i])
            rpair = colorpair(read[i-1:i])
            mcost = matchcost[gpair][rpair]*q
            scoreD = matrix[j-2][i-2] + mcost
            
