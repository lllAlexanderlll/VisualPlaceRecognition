package com.tud.alexw.visualplacerecognition.verification;


import android.graphics.Bitmap;

import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.fitting.DistanceFromModelResidual;
import boofcv.abst.geo.fitting.GenerateEpipolarMatrix;
import boofcv.abst.geo.fitting.ModelManagerEpipolarMatrix;
import boofcv.alg.geo.f.FundamentalResidualSampson;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.EnumFundamental;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;


public class FundamentalMatrix {

    public static DMatrixRMaj robustFundamental(List<AssociatedPair> matches ,
                                                List<AssociatedPair> inliers ) {

        // used to create and copy new instances of the fit model
        ModelManager<DMatrixRMaj> managerF = new ModelManagerEpipolarMatrix();
        // Select which linear algorithm is to be used.  Try playing with the number of remove ambiguity points
        Estimate1ofEpipolar estimateF = FactoryMultiView.computeFundamental_1(EnumFundamental.LINEAR_7, 2);
        // Wrapper so that this estimator can be used by the robust estimator
        GenerateEpipolarMatrix generateF = new GenerateEpipolarMatrix(estimateF);

        // How the error is measured
        DistanceFromModelResidual<DMatrixRMaj,AssociatedPair> errorMetric =
                new DistanceFromModelResidual<>(new FundamentalResidualSampson());

        // Use RANSAC to estimate the Fundamental matrix
        ModelMatcher<DMatrixRMaj,AssociatedPair> robustF =
                new Ransac<>(123123, managerF, generateF, errorMetric, 6000, 0.1);

        // Estimate the fundamental matrix while removing outliers
        if( !robustF.process(matches) )
            throw new IllegalArgumentException("Failed");

        // save the set of features that were used to compute the fundamental matrix
        inliers.addAll(robustF.getMatchSet());

        // Improve the estimate of the fundamental matrix using non-linear optimization
        DMatrixRMaj F = new DMatrixRMaj(3,3);
        ModelFitter<DMatrixRMaj,AssociatedPair> refine =
                FactoryMultiView.refineFundamental(1e-8, 400, EpipolarError.SAMPSON);
        if( !refine.fitModel(inliers, robustF.getModelParameters(), F) )
            throw new IllegalArgumentException("Failed");

        // Return the solution
        return F;
    }

    /**
     * Use the associate point feature example to create a list of {@link AssociatedPair} for use in computing the
     * fundamental matrix.
     */
    public static List<AssociatedPair> computeMatches( Bitmap left , Bitmap right ) {
        DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(
                new ConfigFastHessian(1, 2, 200, 1, 9, 4, 4), null,null, GrayF32.class);
//		DetectDescribePoint detDesc = FactoryDetectDescribe.sift(null,new ConfigSiftDetector(2,0,200,5),null,null);

        ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
        AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 1, true);

        AssociatePoints<GrayF32, BrightFeature> findMatches = new AssociatePoints<>(detDesc, associate, GrayF32.class);

        findMatches.associate(left,right);

        List<AssociatedPair> matches = new ArrayList<>();
        FastQueue<AssociatedIndex> matchIndexes = associate.getMatches();

        for( int i = 0; i < matchIndexes.size; i++ ) {
            AssociatedIndex a = matchIndexes.get(i);
            AssociatedPair p = new AssociatedPair(findMatches.pointsA.get(a.src) , findMatches.pointsB.get(a.dst));
            matches.add( p);
        }

        return matches;
    }

    public static int geometricVerification(Bitmap imageA, Bitmap imageB){
        List<AssociatedPair> matches = computeMatches(imageA,imageB);

        // Where the fundamental matrix is stored
        DMatrixRMaj F;
        // List of matches that matched the model
        List<AssociatedPair> inliers = new ArrayList<>();

        // estimate and print the results using a robust and simple estimator
        // The results should be difference since there are many false associations in the simple model
        // Also note that the fundamental matrix is only defined up to a scale factor.
        F = robustFundamental(matches, inliers);
        System.out.println("Robust");
        F.print();

        return inliers.size();
    }
}


