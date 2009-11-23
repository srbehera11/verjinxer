#!/usr/bin/env python


ERROR = 0
UNMODIFIED = 1
CT = 2
CT_CC = 4
GA = 8
GA_CC = 16

def matchLength(s, t):
	state1 = UNMODIFIED
	state2 = CT
	state3 = GA
	assert len(s) == len(t)
	i = 0
	for (x,y) in zip(s, t):
		xy = x + y
		if state1 == UNMODIFIED:
			if x != y:
				state1 = ERROR

		if state2 == CT:
			if xy == 'CC':
				state2 = CT_CC
			elif xy in ['AA', 'TT', 'GG', 'CT']:
				pass
			else:
				state2 = ERROR
		elif state2 == CT_CC:
			if xy == 'GG':
				state2 = CT
			else:
				state2 = ERROR

		if state3 == GA:
			if xy in ['AA', 'TT', 'GA']:
				pass
			elif xy == 'CC':
				state3 = GA_CC
			else:
				state3 = ERROR
		elif state3 == GA_CC:
			if xy == 'CC':
				pass
			elif xy in ['AA', 'TT', 'GA', 'GG']:
				state3 = GA
			else:
				state3 = ERROR
		if state1 == ERROR and state2 == ERROR and state3 == ERROR:
			return (i, state1, state2, state3)
		i += 1
	return (i, state1, state2, state3)


testpairs = [
	('CGA', 'TGA'),
	('CGA', 'CGA'),
	('CGCG', 'CGTG'),
	('CG', 'CA'),
	('CGCGC', 'CACGC'),
	('ACGT', 'ACGT'),
	('ACGT', 'ATGT'),
	('CCGT', 'CCAT'),
	('ACCCGTTA', 'ATTTGTTA'),
	('CGC', 'TGC'),

	# partial matches
	('CGA', 'CGT'),
	('CGCGCG', 'CGTGCA'),
	('ATGCCA', 'ATGTCT'),
	]

for (s, t) in testpairs:
	print (s, t), matchLength(s, t)
