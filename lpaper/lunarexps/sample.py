import numpy as np
from matplotlib import cm
import matplotlib.pyplot as plt
from matplotlib.mlab import griddata
import numpy as np

ar = np.loadtxt(open("samplerun.csv","rb"),delimiter=",")
print len(ar)
fig = plt.figure()

sample = plt.plot(ar, color = "b" ,label = "Sample Run")

plt.xlabel('Game Tick')
plt.ylabel('Best CMA score')
#fig.set_size_inches(18.5,10.5)

plt.savefig("sample.pdf")

plt.hold(False)

i = 650

x = np.arange(i,len(ar),1)

print len(ar[i:]), len(x)

sample = plt.plot(x, ar[i:], color = "b" ,label = "Sample Run - Detail")
#fig.set_size_inches(18.5,10.5)

plt.xlabel('Game Tick')
plt.ylabel('Best CMA score')

plt.savefig("sample-detail.pdf")

