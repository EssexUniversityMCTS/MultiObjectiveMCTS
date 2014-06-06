import numpy as np
import scipy as sp
import scipy.stats as st
import pandas as pd
import itertools


def mean_confidence_interval(data, confidence=0.95):
	a = 1.0*np.array(data)
	n = len(a)
	m, se = np.mean(a), scipy.stats.sem(a)
	h = se * st.t.ppf((1+confidence)/2., n-1)
	return m, m-h, m+h



def ttest_ind(m0, m1,se_0, se_1, n,  axis=0, equal_var=True):
 	#print type(m0)
	m0 = np.array(m0,dtype = "f")
	m1 = np.array(m1,dtype = "f")
	se_0 = np.array(se_0,dtype = "f")
	se_1 = np.array(se_1,dtype = "f")
	v1 = se_0*np.sqrt(n)
	v2 = se_1*np.sqrt(n)
	v1 = v1*v1
	v2 = v2*v2
	n1 = n
	n2 = n

	#print v1, v2

	if (equal_var):
		df = n1 + n2 - 2
		svar = ((n1 - 1) * v1 + (n2 - 1) * v2) / float(df)
		denom = np.sqrt(svar * (1.0 / n1 + 1.0 / n2))
	else:
		vn1 = v1 / n1
		vn2 = v2 / n2
		df = ((vn1 + vn2)**2) / ((vn1**2) / (n1 - 1) + (vn2**2) / (n2 - 1))

        # If df is undefined, variances are zero (assumes n1 > 0 & n2 > 0).
        # Hence it doesn't matter what df is as long as it's not NaN.
        df = np.where(np.isnan(df), 1, df)
        denom = np.sqrt(vn1 + vn2)

	d = m0 - m1
	##print "====================="
	#print d, denom
	t = np.divide(d, denom)
	##print m0,m1, se_0, se_1
	##print "--->t", t, df
	t, prob = st.stats._ttest_finish(df, t)
	##print t, prob
	##print "===================="
	return t, prob

def mean_confidence_interval_nodata(m, se, n, confidence = 0.95):
	z = st.t.ppf((1+confidence)/2., n-1)
	h = se * z
	#print st.t._ppf((1+confidence)/2., n-1)
	#print (-z,z)
	#print st.norm.interval(0.95, loc=0, scale=1)
	return m, h

def printtestline(prefix, comb, mean, se, tt):
	#print tt
 	print "%s , %s, %.5f , %.5f , %.5f , %.5f , %.10f\t" % (prefix, comb,mean[0], se[0], mean[1],se[1],tt[1])

if __name__=="__main__":
	filename = "./results.csv"
	df = pd.DataFrame.from_csv(filename, index_col = 0)

	mmap = 0
	new_index = list(df.index)
	for i in range(0,df.shape[0]):
		if(i%4 ==0):
			mmap+=1
		new_index[i] = "Map " + str(mmap) + " " + df.index[i]
		#print i,mmap

	df.index = new_index
	for mmap in range(0,df.shape[0], 4):
	#for mmap in range(0,4,4):
		#print mmap, mmap+4
		imp_indexes = [df.index[i] for i in range(mmap,mmap+4)]
		combs =  list(itertools.combinations(imp_indexes,2))
		
		for comb in combs:
			mean_time = [0]*2; se_time = [0]*2;mean_fuel= [0]*2; se_fuel= [0]*2; mean_damage = [0]*2; se_damage = [0]*2;
			for i in range(0,2):
				mean_time[i], se_time[i], mean_fuel[i], se_fuel[i], mean_damage[i], se_damage[i] = df.loc[comb[i]]

			tt_time = ttest_ind(mean_time[0], mean_time[1],se_time[0], se_time[1], 30.0, axis=0, equal_var=False)
			tt_fuel = ttest_ind(mean_fuel[0], mean_fuel[1],se_fuel[0], se_fuel[1], 30.0, axis=0, equal_var=False)
			tt_damage = ttest_ind(mean_damage[0], mean_damage[1],se_damage[0], se_damage[1], 30.0, axis=0, equal_var=False)
			#if(tt_time[1] > 0.05):
			printtestline("time", comb, mean_time,se_time,tt_time)
			#if(tt_fuel[1] > 0.05):
			#printtestline("fuel", comb, mean_fuel,se_fuel,tt_fuel)
			#if(tt_damage[1] > 0.05):
			#printtestline("damage", comb, mean_damage,se_damage,tt_damage)
		print "=================="
		#print("%s %.2f" % 1, tt[1])
		#print comb, tt
	#print imp_indexes
	#print df.loc[imp_indexes[0]]
	exit()

	for i, row in enumerate(df.values):
		name = df.index[i]
		mean_time, se_time = row[0], row[1] 
		mean_fuel, se_fuel = row[2], row[3]
		mean_damage, se_damage = row[4], row[5]
		time = mean_confidence_interval_nodata(mean_time,se_time,30 )
		fuel =  mean_confidence_interval_nodata(mean_fuel,se_fuel,30 )
		damage = mean_confidence_interval_nodata(mean_damage,se_damage,30)
		print name, time, fuel, damage
