# Software changing notes according to Apache License 2.0
The code for the VLAD-PQ framework implementation was taken from https://github.com/MKLab-ITI/multimedia-indexing
The modified code can be found here: [app/src/main/java/gr/iti/mklab/visual](https://github.com/lllAlexanderlll/VisualPlaceRecognition/tree/master/app/src/main/java/gr/iti/mklab/visual)

## Paper reference
E. Spyromitros-Xioufis, S. Papadopoulos, I. Y. Kompatsiaris, G. Tsoumakas and I. Vlahavas, "A Comprehensive Study Over VLAD and Product Quantization in Large-Scale Image Retrieval," in IEEE Transactions on Multimedia, vol. 16, no. 6, pp. 1713-1728, Oct. 2014.
doi: 10.1109/TMM.2014.2329648
keywords: {content-based retrieval;feature extraction;image representation;image retrieval;indexing;quantisation (signal);VLAD;product quantization;content-based large-scale image retrieval;indexing scheme;representation quality;local features extraction;vector of locally aggregated descriptors;Vectors;Visualization;Feature extraction;Vocabulary;Accuracy;Image color analysis;Image retrieval;Image classification;image retrieval;indexing},
URL: http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6847226&isnumber=6898894

## Status Changes:
All code taken from the public repository was converted by [app/src/convert_to_ejml31.py](https://github.com/lllAlexanderlll/VisualPlaceRecognition/blob/master/app/src/convert_to_ejml31.py) to EJML version 31:
utilities
* utilities/RandomRotation.java
* utilities/AnswerWithGeolocation.java
* utilities/MetaDataEntity.java
* utilities/FeatureIO.java
* utilities/Normalization.java
* utilities/RandomPermutation.java
* utilities/Answer.java
* utilities/Result.java

datastructures
* datastructures/AbstractSearchStructure.java
* datastructures/IVFPQ.java
* datastructures/Linear.java
* datastructures/PQ.java

dimreduction
* dimreduction/PCA.java

extraction
* extraction/AbstractFeatureExtractor.java
* extraction/SURFExtractor.java

aggregation
* aggregation/VladAggregatorMultipleVocabularies.java
* aggregation/AbstractFeatureAggregator.java
* aggregation/BowAggregator.java
* aggregation/VladAggregator.java


Also the dependency to java.awt.* was removed

java.awt.image.BufferedImage replaced by android.graphics.Bitmap
```
The BoofCV version 0.27 and the boofcv-android package is used:
+--- org.boofcv:boofcv-android:0.27
|    +--- org.georegression:georegression:0.13
|    |    \--- org.ddogleg:ddogleg:0.11
|    |         +--- org.ejml:ejml-core:0.31
|    |         +--- org.ejml:ejml-fdense:0.31
|    |         |    \--- org.ejml:ejml-core:0.31
|    |         +--- org.ejml:ejml-ddense:0.31
|    |         |    \--- org.ejml:ejml-core:0.31
|    |         \--- org.ejml:ejml-simple:0.31
|    |              +--- org.ejml:ejml-core:0.31
|    |              +--- org.ejml:ejml-fdense:0.31 (*)
|    |              \--- org.ejml:ejml-ddense:0.31 (*)
|    +--- org.boofcv:boofcv-ip:0.27
|    |    \--- org.georegression:georegression:0.13 (*)
|    +--- org.boofcv:boofcv-feature:0.27
|    |    +--- org.georegression:georegression:0.13 (*)
|    |    \--- org.boofcv:boofcv-ip:0.27 (*)
|    +--- org.boofcv:boofcv-calibration:0.27
|    |    +--- org.georegression:georegression:0.13 (*)
|    |    +--- org.boofcv:boofcv-ip:0.27 (*)
|    |    +--- org.boofcv:boofcv-feature:0.27 (*)
|    |    \--- org.boofcv:boofcv-geo:0.27
|    |         +--- org.georegression:georegression:0.13 (*)
|    |         +--- org.boofcv:boofcv-ip:0.27 (*)
|    |         \--- org.boofcv:boofcv-feature:0.27 (*)
|    \--- org.boofcv:boofcv-geo:0.27 (*)
```
Since the feature extractor and descriptor interfaces and packages changed in 0.27,
their code is adapted.
