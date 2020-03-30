# Visual Place Recognition
In this repo an implementation of visual place recognition system based on the content-based image retrieval VLAD-PQ framework, SURF feature extraction and PCA is presented.

## The App
The Android App is to install on a Segway Robotics Loomo Robot by using Android Studio.
For inference the environemnt in which place recognition shall be conducted is to capture (preferably with the HD camera of Loomo) and indexes, codebooks and a PCA projection matrix is to calculate.

## Image Capturing of Environment 

## Calculation of Linear and PQ indexes, Codebooks and PCA files
Linear and PQ indexes, Codebooks and PCA files can be calculated using the JAVA software of https://github.com/MKLab-ITI/multimedia-indexing.
A altered version of the referenced software is used in this app for inference on the robot.
[https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/app/src/main/java/gr/iti/mklab/visual/License.md](Lincence notes) for https://github.com/MKLab-ITI/multimedia-indexing
Alternatively e.g. if only 8 GB of memory are available the [https://github.com/lllAlexanderlll/VisualPlaceRecognition/tree/master/app/src/main/python](python scripts) may be used.

## Performing Tests
To perform tests on the robot the App needs to be compiled with a accordingly initialised [https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/app/src/main/java/com/tud/alexw/visualplacerecognition/framework/Config.java](configuration):
![Example configuration image](https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/images/config.png)
