import numpy as np
import os
import argparse
from scipy import linalg
import datetime

def readTestPCA(filepath):
    print()
    print("Test PCA file:")
    with open(filepath) as f:
        line = f.readline()
        means = np.array(line.split(' '), dtype=np.float32)
        print(f"row 0: the first line of the file contains the training sample means per component:  #columns: {means.shape}")

        line = f.readline()
        eigValDSC = np.array(line.split(' '), dtype=np.float32)
        print(f"row 1: the second line of the file contains the eigenvalues in descending order:  #columns: {len(eigValDSC)}")

    print(f"rest: the next lines of the file contain the eigenvectors in descending eigenvalue order:")        
    pca = np.loadtxt(filepath, delimiter=" ", skiprows=2)
    print(f"test pca dim: {pca.shape}")
    return means, eigValDSC, pca

def checkPCACalculation(basePath, newPCA, meanTrainVec, eigValDSC):
    testPCAFilePath = os.path.join(basePath, "pca/pca_16_3_60ms.txt")
    testMeanTrainingVec, testEigValDSC, testPCA = readTestPCA(testPCAFilePath)
    
    np.testing.assert_array_almost_equal(np.abs(testPCA), np.abs(newPCA))
    np.testing.assert_array_almost_equal(testMeanTrainingVec, meanTrainVec)
    np.testing.assert_array_almost_equal(testEigValDSC, eigValDSC)

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description='Calculate a a file containing the training vector column mean, eigenvalues and eigenvectors by computing SVD with a given projection length (dim. reduction)')
    parser.add_argument("featuresFilePath", type=str, help="path to the file containing the features (Dump from linear index with first value: image name following values feature vector)")
    #parser.add_argument("projectionLength", type=int, help="Number of dimentions to reduce the vector to")
    args = parser.parse_args()
    
    fullpath = os.path.abspath(args.featuresFilePath)
    if(not os.path.exists(fullpath)):
        print(f"Path '{fullpath}' does not exist!")
        exit()
    
    #projectionLength = args.projectionLength
    featuresFilePath = fullpath
    basePath = os.path.dirname(featuresFilePath)
    print("basepath", basePath)

    print(f"{str(datetime.datetime.now()).split('.')[0]} Loading indexed vectors...")
    with open(featuresFilePath) as f:
        nCols = len(f.readline().split(','))
    features = np.loadtxt(featuresFilePath, delimiter=",", usecols=range(1,nCols), dtype=np.float64)
    print(f"{str(datetime.datetime.now()).split('.')[0]} Data loaded: {features.shape}")

    centeredFeatures = (features - np.mean(features, axis=0, dtype=np.float64)).astype(np.float32)
    #np.lin.svd forces type np.float64 --> leads to 8.5 GB (32768x32768) matrix (with 32768 dim. features in index)

    print(f"{str(datetime.datetime.now()).split('.')[0]} Starting PCA calculation...")
    u, s, vh = linalg.svd(centeredFeatures, full_matrices=True)
    print(f"{str(datetime.datetime.now()).split('.')[0]} PCA calculation finished.")
    
    for dimPCA in [24,48,96]:
        newPCA = vh[:dimPCA,:]    
        print(f"projectionLength: {newPCA.shape}")

        meanTrainVec = np.mean(features, axis=0)
        print(f"mean training vec shape: {meanTrainVec.shape}")

        eigValDSC = s[:dimPCA]
        print(f"eig val shape: {eigValDSC.shape}")

        # checkPCACalculation(basePath, newPCA, meanTrainVec, eigValDSC)

        np.set_printoptions(suppress=True)
        savePath = os.path.join(basePath, f"pca_{features.shape[1]}_to_{dimPCA}.txt")
        with open(savePath, 'w') as f:
            np.savetxt(f, meanTrainVec.reshape(1, meanTrainVec.shape[0]), delimiter=" ", fmt="%.18f")
            np.savetxt(f, eigValDSC.reshape(1, eigValDSC.shape[0]), delimiter=" ", fmt="%.18f")
            np.savetxt(f, newPCA, delimiter=" ", fmt="%f")
        print()
        print(f"Wrote pca file to: '{savePath}'")
        print()
