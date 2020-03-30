import os
import numpy as np
import pandas as pd
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('path', type=str, help='folder to check')
parser.add_argument('xTurn', type=str, help='degree to add')
args = parser.parse_args()

if not (args.xTurn and args.path):
	print("Invalid number of arguments. Exit.")
	exit()
	
if not args.xTurn.isdigit():
	print("Second argument must be a number. Exit.")
	exit()

directory = args.path

grid = np.zeros((20,20), dtype=int)
print(f'{directory}:')
for filename in os.listdir(directory):
	extention = filename[-4:]
	filename_splitted = filename[:-4].split("_")
	if(filename_splitted[-4].isdigit()):
		coords = filename_splitted[-4:]
	else:
		coords = filename_splitted[-3:]
        
	if len(coords) == 3:
		filename_splitted.append("0")
		coords = filename_splitted[-4:]
	
	timeAndLabel = filename_splitted[:-4]
	
	x,y,yaw,pitch = [int(a) for a in coords]
	
	yaw = (yaw*-1 + int(args.xTurn)) % 360
	x = 2 - x
	#y = abs(y)
	
	print(("{:5}" * 4 + " --> " + "{:5}" * len(coords)).format(*coords, *(x,y,yaw,pitch)))
	coords = [str(a) for a in [x,y,yaw,pitch]]
	os.rename(os.path.join(directory, filename), os.path.join(directory, "_".join(timeAndLabel + coords) + extention))
	#grid[x, y] += 1
	
#print(pd.DataFrame(grid))
	

