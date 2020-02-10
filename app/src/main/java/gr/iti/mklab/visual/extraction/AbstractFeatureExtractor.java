package gr.iti.mklab.visual.extraction;

import android.graphics.Bitmap;

import boofcv.struct.image.ImageGray;

/**
 * Abstract class for all feature extractors.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public abstract class AbstractFeatureExtractor {

	public static final int SURFLength = 64;
	public static final int SIFTLength = 128;
	public static final int CololSURFLength = 3 * SURFLength;

	/**
	 * The total feature extraction time.
	 */
	protected long totalExtractionTime;
	/**
	 * The total number of detected interest points.
	 */
	protected long totalNumberInterestPoints;

	/**
	 * Any normalizations of the features should be performed in the specific classes!
	 * 
	 * @param image
	 * @return
	 * @throws Exception
	 */
	public double[][] extractFeatures(Bitmap image, int width, int height) throws Exception {
		long start = System.currentTimeMillis();
		double[][] features = extractFeaturesInternal(image, width, height);
		totalNumberInterestPoints += features.length;
		totalExtractionTime += System.currentTimeMillis() - start;
		return features;
	}

	public abstract <II extends ImageGray<II>>  double[][] extractFeaturesInternal(Bitmap bitmap, int width, int height) throws Exception;

	public long getTotalExtractionTime() {
		return totalExtractionTime;
	}

	public long getTotalNumberInterestPoints() {
		return totalNumberInterestPoints;
	}

}
