import numpy as np
from tabulate import tabulate
import numpy as np
import scipy.stats
import scipy as sp


def mean_confidence_interval(data, confidence=0.95):
    a = 1.0*np.array(data)
    n = len(a)
    m, se = np.mean(a), scipy.stats.sem(a)
    h = se * sp.stats.t._ppf((1+confidence)/2., n-1)
    return [m,h]

map2 = np.loadtxt(open("map2.csv","rb"),delimiter=",")
map1_tall = np.loadtxt(open("map1-tall.csv","rb"),delimiter=",")
map1_easy = np.loadtxt(open("map1-easy.csv","rb"),delimiter=",")
map1_wall = np.loadtxt(open("map1-wall.csv","rb"),delimiter=",")



maps = {}
maps["map2"] = map2
maps["map1-tall"] = map1_tall
maps["map1-easy"] = map1_easy
maps["map1-wall"] = map1_wall

print mean_confidence_interval(map2[:,0])

table = [(["map2"] + mean_confidence_interval(map2[:,0]) + mean_confidence_interval(map2[:,1])) ,
		 (["map1-tall"] + mean_confidence_interval(map1_tall[:,0]) + mean_confidence_interval(map1_tall[:,1])) ,
		 (["map1-easy"] + mean_confidence_interval(map1_easy[:,0]) + mean_confidence_interval(map1_easy[:,1])) ,
		 (["map1-wall"] + mean_confidence_interval(map1_wall[:,0]) + mean_confidence_interval(map1_wall[:,1])) ,
]
print tabulate(table, headers = ["Map Name", "Success Mean", "Success CI", "Ticks Mean", "Tick CI"], tablefmt="latex")



