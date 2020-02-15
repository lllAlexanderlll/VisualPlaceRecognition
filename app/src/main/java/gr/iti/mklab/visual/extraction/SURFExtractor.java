package gr.iti.mklab.visual.extraction;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;

import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.android.ConvertBitmap;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;

import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;

import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.ScalePoint;

import boofcv.struct.image.GrayF32;

import boofcv.struct.image.ImageGray;

/**
 * This class uses the BoofCV library for extracting SURF features.
 *
 * @author Eleftherios Spyromitros-Xioufis
 */
public class SURFExtractor extends AbstractFeatureExtractor {

	public String TAG = this.getClass().getName();

	/**
	 * Sets the value of {@link ConfigFastHessian#maxFeaturesPerScale}
	 */
	protected int maxFeaturesPerScale;
	/**
	 * Sets the value of {@link ConfigFastHessian#detectThreshold}
	 */
	protected int detectThreshold;

	/**
	 * Constructor using default "good" settings for the detector.
	 *
	 * @throws Exception
	 */
	public SURFExtractor() {
		this(-1, 1);
	}

	public SURFExtractor(int maxFeaturesPerScale, int minFeatureIntensity) {
		this.maxFeaturesPerScale = maxFeaturesPerScale;
		this.detectThreshold = minFeatureIntensity;
	}

	/**
	 * Detects key points inside the image and computes descriptions at those points.
	 */
	@Override
	public <II extends ImageGray<II>> double[][] extractFeaturesInternal(Bitmap bitmap, int width, int height) {

		//scale image
		if(bitmap.getWidth() != width|| bitmap.getHeight() != height){
			boolean bilinearFiltering = true;
			bitmap = Bitmap.createScaledBitmap(bitmap, width, height, bilinearFiltering);
		}

		//convert to grey scale BoofCV image
		GrayF32 image = ConvertBitmap.bitmapToGray(bitmap, (GrayF32) null, null);

		// define the feature detection algorithm
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 0, 5, true));
		FastHessianFeatureDetector<II> detector =
				new FastHessianFeatureDetector<>(extractor, -1, 2, 9, 4, 4, 6);

		Class<II> integralType = GIntegralImageOps.getIntegralType(GrayF32.class);
		// estimate orientation
		OrientationIntegral<II> orientation = FactoryOrientationAlgs.sliding_ii(null, integralType);

		DescribePointSurf<II> descriptor = FactoryDescribePointAlgs.<II>surfStability(null,integralType);

		// compute the integral image of 'image'
		II integralImage = GeneralizedImageOps.createSingleBand(integralType,image.width,image.height);
		GIntegralImageOps.transform(image, integralImage);

		// detect fast hessian features
		detector.detect(integralImage);
		// tell algorithms which image to process
		orientation.setImage(integralImage);
		descriptor.setImage(integralImage);

		List<ScalePoint> points = detector.getFoundPoints();

		List<BrightFeature> descriptions = new ArrayList<>();

		for( ScalePoint p : points ) {
			// estimate orientation
			orientation.setObjectRadius( p.scale* BoofDefaults.SURF_SCALE_TO_RADIUS);
			double angle = orientation.compute(p.x,p.y);

			// extract the SURF description for this region
			BrightFeature desc = descriptor.createDescription();
			descriptor.describe(p.x,p.y,angle,p.scale,desc);

			// save everything for processing later on
			descriptions.add(desc);
		}

		Log.i(TAG, "Found Features: "+points.size());
		Log.i(TAG, "First descriptor's first value: "+descriptions.get(0).value[0]);

		double[][] output = new double[points.size()][descriptions.get(0).size()];
		for (int i = 0; i < points.size(); i++) {
			output[i] = descriptions.get(i).getValue();
		}
		return output;
	}
}
