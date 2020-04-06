# Visual Place Recognition
In this repo an implementation of visual place recognition system based on the content-based image retrieval VLAD-PQ framework, SURF feature extraction and PCA is presented.

## The App
The Android App is to install on a Segway Robotics Loomo Robot by using Android Studio.
For inference the environment in which place recognition shall be conducted is to capture (preferably with the HD camera of Loomo) and indexes, codebooks and a PCA projection matrix is to calculate.

## Image Capturing of Environment 
See https://github.com/lllAlexanderlll/CaptureDataset/tree/master

## Calculation of Linear and PQ indexes, Codebooks and PCA files
Linear and PQ indexes, Codebooks and PCA files can be calculated using the JAVA software of https://github.com/MKLab-ITI/multimedia-indexing.
A altered version of the referenced software is used in this app for inference on the robot.
[Licence notes](https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/app/src/main/java/gr/iti/mklab/visual/License.md) for https://github.com/MKLab-ITI/multimedia-indexing.
Alternatively e.g. if only 8 GB of memory are available the [python scripts](https://github.com/lllAlexanderlll/VisualPlaceRecognition/tree/master/app/src/main/python) may be used.

## Performing Tests
To perform tests on the robot the App needs to be compiled with a accordingly initialised [https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/app/src/main/java/com/tud/alexw/visualplacerecognition/framework/Config.java](configuration):
![Example configuration image](https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/images/config.png)

## Place Recognition
If tests are disabled (doRunTests=false), place recognition can be conducted by pressing the button "Capture" in the upper left corner of the app. This is only possible after the index was successfully loaded (may take 2 minutes).
In the MainActivity the head movements can be changed. The configuration entry "nQueriesForResult" must match to array size of head movements.

## Term Paper Reference
This implementation was created within the context of the term paper "Visual Place Recognition to Support Indoor Localisation"
