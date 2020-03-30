
# use intra-normailsation
# kmeans++


import numpy as np
import os
import argparse
from scipy import cluster
import matplotlib.pyplot as plt
import time
import datetime


def readTestCodebook(filepath):
    print()
    print("Test Codebook:")
    testCodebook = np.loadtxt(filepath, delimiter=",")
    print(testCodebook)
    print(f"test codebook dim: {testCodebook.shape}")
    return testCodebook


def checkCodebookCalculation(testCodebookPath, mycodebook):
    testCodebook = readTestCodebook(testCodebookPath)
    np.testing.assert_array_almost_equal(testCodebook, mycodebook)

def getExistingPath(path):
    fullpath = os.path.abspath(path)
    if(not os.path.exists(fullpath)):
        print(f"Path '{fullpath}' does not exist!")
        exit()
    return fullpath

# mvn exec:java -Dexec.mainClass=gr.iti.mklab.visual.quantization.CodebookLearning -Dexec.args="/home/alex/Documents/Git/multimedia-indexing/miniExample/features/features.csv 4 100 1 1 power+l2 true"
# /home/alex/Documents/Git/multimedia-indexing/miniExample/codebook/features.csv_codebook-64A-4C-100I-1S_power+l2.csv
if __name__ == "__main__":

    parser = argparse.ArgumentParser(
        description='Calculate a a file containing the training vector column mean, eigenvalues and eigenvectors by computing SVD with a given projection length (dim. reduction)')
    parser.add_argument("featuresFilePath", type=str, help="path to the file containing the features (Dump from linear index with first value: image name following values feature vector)")

    parser.add_argument("nCentroids", type=int, help="Number of centroids to calculate")
    parser.add_argument("--testCodebookPath", required=False, type=str, help="path to the file containing the codebook to test against")

    args = parser.parse_args()

    nCentroids = args.nCentroids
    featuresFilePath = getExistingPath(args.featuresFilePath)
    basePath = os.path.dirname(featuresFilePath)
    print("basepath", basePath)


    print(f"Calculation of codebook: {featuresFilePath} {nCentroids}")
    print(f"Start: {str(datetime.datetime.now()).split('.')[0]}")
    features = np.loadtxt(featuresFilePath, delimiter=",")
    print(features)
    print(f"features size: {features.shape}")

    #feature normalisation power+L2
    #power vector[i] = Math.signum(vector[i]) * Math.pow(Math.abs(vector[i]), 0.5);
    # features = np.array(range(4*4)).reshape((4,4))
    # features[0] *= -1
    powerFeatures = np.multiply(np.sign(features), np.power(np.abs(features), 0.5))
    normL2 = np.linalg.norm(powerFeatures, 2, axis=1)
    powerL2Features = powerFeatures / normL2[:,None]
    print("Features normalised. Starting kMeans...")
    print(f"Start: {str(datetime.datetime.now()).split('.')[0]}")
    
    centroids, label = cluster.vq.kmeans2(powerL2Features, nCentroids, minit='++', missing='warn', check_finite=True)
    print(f"Got centroids {centroids.shape}")
    
    # # Test --> is not expected to succeed, since kmeans initialisation is random
    # if args.testCodebookPath is not None:
    #     testCodebookPath = getExistingPath(args.testCodebookPath)
    #     checkCodebookCalculation(testCodebookPath, centroids)

    np.set_printoptions(suppress=True)
    savePath = os.path.join(basePath, f"codebook_{os.path.basename(featuresFilePath)[:-4]}_dim_{features.shape[1]}_centroids_{nCentroids}.csv")
    np.savetxt(savePath, centroids, delimiter=",", fmt="%.18f")
    print()
    print(f"Wrote pca file to: '{savePath}'")
    print(f"End: {str(datetime.datetime.now()).split('.')[0]}")
    print()