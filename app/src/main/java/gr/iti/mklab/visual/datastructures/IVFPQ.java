package gr.iti.mklab.visual.datastructures;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import com.aliasi.util.BoundedPriorityQueue;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DiskOrderedCursorConfig;
import com.sleepycat.je.ForwardCursor;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gr.iti.mklab.visual.aggregation.AbstractFeatureAggregator;
import gr.iti.mklab.visual.datastructures.PQ.TransformationType;
import gr.iti.mklab.visual.utilities.RandomPermutation;
import gr.iti.mklab.visual.utilities.RandomRotation;
import gr.iti.mklab.visual.utilities.Result;

/**
 * This class implements indexing and non-exhaustive approximate nearest neighbor search using the combination
 * of Product Quantization with an inverted file structure (IVFADC) as described in:<br>
 * 
 * <em>Jégou, H., Douze, M., & Schmid, C. (2011). Product quantization for nearest neighbor search. IEEE Transactions on Pattern Analysis and Machine Intelligence.</em>
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class IVFPQ extends AbstractSearchStructure {
	public String TAG = this.getClass().getName();
	/**
	 * BDB store for persistent storage of the ADC index.
	 */
	private Database iidToIvfpqDB;

	/**
	 * The number of sub-vectors.
	 */
	private int numSubVectors;

	/**
	 * The length of each subvector (= vectorLength/numSubVectors).
	 */
	private int subVectorLength;

	/**
	 * The number of centroids used to quantize each sub-vector. (Depending on this number we use a different
	 * type for storing the quantization code of each sub-vector. For k<=256=2^8 centroids we use a byte (8
	 * bits per subvector), for k>256 we use a short (16 bits per subvector).
	 * 
	 */
	private int numProductCentroids;

	/**
	 * The product-quantization codes for all vectors are stored in this list if the code can fit in the byte
	 * range.
	 */
	private TByteArrayList[] pqByteCodes;

	/**
	 * The product-quantization codes for all vector are stored in this list if the code cannot fit in the
	 * byte range.
	 */
	private TShortArrayList[] pqShortCodes;

	/**
	 * The inverted lists containing the internal ids of the vectors qunatized in each list.
	 */
	private TIntArrayList[] invertedLists;

	/**
	 * Number of centroids in the coarse quantizer.
	 */
	private int numCoarseCentroids;

	/**
	 * Number of lists to be visited during nn search.
	 */
	private int w;

	public void setW(int w) {
		this.w = w;
	}

	/**
	 * The coarse quantizer.<br>
	 * 
	 * A two dimensional array storing the coarse quantizer. The 1st dimension goes from
	 * 1...numCoarseCentroids and indexes the centroids of the coarse quantizer. The 2nd dimension goes from
	 * 1...vectorLength and indexes the dimensions of each centroid.
	 */
	private double[][] coarseQuantizer;

	/**
	 * The sub-quantizers of the product quantizer. They are needed for indexing and search using PQ.<br>
	 * 
	 * A three dimensional array storing the sub-quantizers of the product quantizer. The first dimension goes
	 * from 1..numSubquantizers and indexes the sub-quantizers. The second dimension goes from
	 * 1..numProductCentroids and indexes the centroids of each sub-quantizer of the product quantizer. The
	 * third dimension goes from 1...subVectorLength and indexes the components of each centroid.
	 */
	private double[][][] productQuantizer;

	/**
	 * The type of transformation to perform on the vectors prior to product quantization.
	 */
	private PQ.TransformationType transformation;

	/**
	 * This object is used for applying random permutation prior to product quantization.
	 */
	private RandomPermutation rp;

	/**
	 * This object is used for applying random rotation prior to product quantization.
	 */
	private RandomRotation rr;

	/**
	 * The seed used in random transformations. Should be the same as the one used at learning time.
	 */
	public final int seed = 1;

	/**
	 * Whether to use a disk ordered cursor or not. This setting changes how fast the index will be loaded in
	 * main memory.
	 */
	public final boolean useDiskOrderedCursor = false;

	/**
	 * Advanced constructor.
	 * 
	 * @param vectorLength
	 *            The dimensionality of the VLAD vectors being indexed
	 * @param maxNumVectors
	 *            The maximum allowable size (number of vectors) of the index
	 * @param readOnly
	 *            If true the persistent store will opened only for read access (allows multiple opens)
	 * @param BDBEnvHome
	 *            The BDB environment home directory
	 * @param numSubVectors
	 *            The number of subvectors
	 * @param numProductCentroids
	 *            The number of centroids used to quantize each sub-vector
	 * @param transformation
	 *            The type of transformation to perform on each vector
	 * @param numCoarseCentroids
	 *            The number of centroids of the coarse quantizer
	 * @param countSizeOnLoad
	 *            Whether the load counter will be initialized by the size of the persistent store
	 * @param loadCounter
	 *            The initial value of the load counter
	 * @param loadIndexInMemory
	 *            Whether to load the index in memory, we can avoid loading the index in memory when we only
	 *            want to perform indexing
	 * @param cacheSize
	 *            the size of the cache in Megabytes
	 * @throws Exception
	 */
	public IVFPQ(int vectorLength, int maxNumVectors, boolean readOnly, File BDBEnvHome, int numSubVectors,
			int numProductCentroids, TransformationType transformation, int numCoarseCentroids,
			boolean countSizeOnLoad, int loadCounter, boolean loadIndexInMemory, long cacheSize)
					throws Exception {
		super(vectorLength, maxNumVectors, readOnly, countSizeOnLoad, loadCounter, loadIndexInMemory,
				cacheSize);
		this.numSubVectors = numSubVectors;
		if (vectorLength % numSubVectors > 0) {
			throw new Exception("The given number of subvectors is not valid!");
		}
		this.subVectorLength = vectorLength / numSubVectors;
		this.numProductCentroids = numProductCentroids;
		this.transformation = transformation;
		this.numCoarseCentroids = numCoarseCentroids;
		w = (int) (numCoarseCentroids * 0.1); // by default set w to 10% of the lists

		if (transformation == TransformationType.RandomRotation) {
			this.rr = new RandomRotation(seed, vectorLength);
		} else if (transformation == TransformationType.RandomPermutation) {
			this.rp = new RandomPermutation(seed, vectorLength);
		}

		createOrOpenBDBEnvAndDbs(BDBEnvHome);

		// configuration of the persistent index
		DatabaseConfig dbConf = new DatabaseConfig();
		dbConf.setReadOnly(readOnly);
		dbConf.setTransactional(transactional);
		dbConf.setAllowCreate(true); // db will be created if it does not exist
		iidToIvfpqDB = dbEnv.openDatabase(null, "ivfadc", dbConf); // create/open the db using config

		if (loadIndexInMemory) {// load the existing persistent index in memory
			// create the memory objects with the appropriate initial size
			invertedLists = new TIntArrayList[numCoarseCentroids];

			if (numProductCentroids <= 256) {
				pqByteCodes = new TByteArrayList[numCoarseCentroids];
			} else {
				pqShortCodes = new TShortArrayList[numCoarseCentroids];
			}

			int initialListCapacity = (int) ((double) maxNumVectors / numCoarseCentroids);
			Log.i(TAG, "Calculated list size " + initialListCapacity);

			for (int i = 0; i < numCoarseCentroids; i++) {
				if (numProductCentroids <= 256) {
					// fixed initial size allows space efficiency measurements
					// pqByteCodes[i] = new TByteArrayList(initialListCapacity * numSubVectors);
					pqByteCodes[i] = new TByteArrayList();

				} else {
					// fixed initial size allows space efficiency measurements
					// pqShortCodes[i] = new TShortArrayList(initialListCapacity * numSubVectors);
					pqShortCodes[i] = new TShortArrayList();

				}
				// fixed initial size for each list, allows space efficiency measurements
				// invertedLists[i] = new TIntArrayList(initialListCapacity);
				invertedLists[i] = new TIntArrayList();
			}
			// load any existing persistent index in memory
			loadIndexInMemory();
		}
	}

	/**
	 * 
	 * @param vectorLength
	 *            The dimensionality of the VLAD vectors being indexed
	 * @param maxNumVectors
	 *            The maximum allowable size (number of vectors) of the index
	 * @param readOnly
	 *            If true the persistent store will opened only for read access (allows multiple opens)
	 * @param BDBEnvHome
	 *            The BDB environment home directory
	 * @param numSubVectors
	 *            The number of subvectors
	 * @param numProductCentroids
	 *            The number of centroids used to quantize each sub-vector
	 * @param transformation
	 *            The type of transformation to perform on each vector
	 * @param numCoarseCentroids
	 *            The number of centroids of the coarse quantizer
	 * @param cacheSize
	 *            the size of the cache in Megabytes
	 * @throws Exception
	 */
	public IVFPQ(int vectorLength, int maxNumVectors, boolean readOnly, File BDBEnvHome, int numSubVectors,
			int numProductCentroids, TransformationType transformation, int numCoarseCentroids,
			long cacheSize) throws Exception {
		this(vectorLength, maxNumVectors, readOnly, BDBEnvHome, numSubVectors, numProductCentroids,
				transformation, numCoarseCentroids, true, 0, true, cacheSize);
	}

	/**
	 * Load the product quantizer from the given file.
	 * 
	 * @param filename
	 *            Full path to the file containing the product quantizer
	 * @throws Exception
	 */
	public void loadProductQuantizer(String filename) throws Exception {
		productQuantizer = new double[numSubVectors][numProductCentroids][subVectorLength];
		BufferedReader in = new BufferedReader(new FileReader(new File(filename)));
		for (int i = 0; i < numSubVectors; i++) {
			for (int j = 0; j < numProductCentroids; j++) {
				String line = in.readLine();
				String[] centroidString = line.split(",");
				for (int k = 0; k < subVectorLength; k++) {
					productQuantizer[i][j][k] = Double.parseDouble(centroidString[k]);
				}
			}
		}
		in.close();
	}

	/**
	 * Load the coarse quantizer from the given file.
	 * 
	 * @param file pointing to the coarse quantizer
	 * @throws Exception
	 */
	public void loadCoarseQuantizer(File file) throws IOException {
		coarseQuantizer = new double[numCoarseCentroids][vectorLength];
		coarseQuantizer = AbstractFeatureAggregator.readQuantizer(file, numCoarseCentroids, vectorLength);
	}

	/**
	 * Append the IVFPQ index with the given vector.
	 * 
	 * @param vector
	 *            The vector to be indexed
	 * @throws Exception
	 */
	public void indexVectorInternal(double[] vector) throws Exception {
		if (vector.length != vectorLength) {
			throw new Exception("The dimensionality of the vector is wrong!");
		}

		// quantize to the closest centroid of the coarse quantizer and compute residual vector
		int nearestCoarseCentroidIndex = computeNearestCoarseIndex(vector);
		double[] residualVector = computeResidualVector(vector, nearestCoarseCentroidIndex);

		// apply a random transformation if needed
		if (transformation == TransformationType.RandomRotation) {
			residualVector = rr.rotate(residualVector);
		} else if (transformation == TransformationType.RandomPermutation) {
			residualVector = rp.permute(residualVector);
		}

		// transform the residual vector into a PQ code
		int[] pqCode = new int[numSubVectors];

		for (int i = 0; i < numSubVectors; i++) {
			// take the appropriate sub-vector
			int fromIdex = i * subVectorLength;
			int toIndex = fromIdex + subVectorLength;
			double[] subvector = Arrays.copyOfRange(residualVector, fromIdex, toIndex);
			// assign the sub-vector to the nearest centroid of the respective sub-quantizer
			pqCode[i] = computeNearestProductIndex(subvector, i);
		}

		if (loadIndexInMemory) { // append the ram-based index
			// add a new entry to the corresponding inverted list
			invertedLists[nearestCoarseCentroidIndex].add(loadCounter);
		}

		if (numProductCentroids <= 256) {
			byte[] pqByteCode = PQ.transformToByte(pqCode);
			if (loadIndexInMemory) { // append the ram-based index
				pqByteCodes[nearestCoarseCentroidIndex].add(pqByteCode);
			}
			appendPersistentIndex(nearestCoarseCentroidIndex, pqByteCode); // append the disk-based index
		} else {
			short[] pqShortCode = PQ.transformToShort(pqCode);
			if (loadIndexInMemory) { // append the ram-based index
				pqShortCodes[nearestCoarseCentroidIndex].add(pqShortCode); // append the ram-based index
			}
			appendPersistentIndex(nearestCoarseCentroidIndex, pqShortCode); // append the disk-based index
		}
	}

	public synchronized boolean indexPQCode(String id, int listId, byte[] code) throws Exception {
		if (numProductCentroids > 256) {
			throw new Exception(
					"Byte is not sufficient to enumerate the centroids of the product quantizer!");
		}
		// check if we can index more vectors
		if (loadCounter >= maxNumVectors) {
			Log.i(TAG, "Maximum index capacity reached, no more vectors can be indexed!");
			return false;
		}
		// check if name is already indexed
		if (isIndexed(id)) {
			Log.i(TAG, "Vector '" + id + "' already indexed!");
			return false;
		}
		// do the indexing
		// persist id to name and the reverse mapping
		createMapping(id);
		if (loadIndexInMemory) { // append the ram-based index
			invertedLists[listId].add(loadCounter);
			pqByteCodes[listId].add(code);
		}
		appendPersistentIndex(listId, code); // append the disk-based index

		loadCounter++; // increase the loadCounter
		if (loadCounter % 100 == 0) { // debug message
			Log.i(TAG, new Date() + " # indexed vectors: " + loadCounter);
		}
		return true;
	}

	protected BoundedPriorityQueue<Result> computeNearestNeighborsInternal(int k, double[] query)
			throws Exception {
		return computeKnnIVFADC(k, query);
	}

	protected BoundedPriorityQueue<Result> computeNearestNeighborsInternal(int k, int internalId)
			throws Exception {
		return computeKnnIVFSDC(k, internalId);
	}

	/**
	 * Computes and returns the k nearest neighbors of the query vector using the IVFADC approach.
	 * 
	 * @param k
	 *            The number of nearest neighbors to be returned
	 * @param qVector
	 *            The query vector
	 * @return
	 * @throws Exception
	 */
	private BoundedPriorityQueue<Result> computeKnnIVFADC(int k, double[] qVector) throws Exception {
		BoundedPriorityQueue<Result> nn = new BoundedPriorityQueue<Result>(new Result(), k);

		// find the w nearest coarse centroids
		int[] nearestCoarseCentroidIndices = computeNearestCoarseIndices(qVector, w);

		for (int i = 0; i < w; i++) { // for each assignment
			// quantize to the i-th closest centroid of the coarse quantizer and compute residual vector
			int nearestCoarseIndex = nearestCoarseCentroidIndices[i];
			double[] residualVectorQuery = computeResidualVector(qVector, nearestCoarseIndex);

			// apply a random transformation if needed
			if (transformation == TransformationType.RandomRotation) {
				residualVectorQuery = rr.rotate(residualVectorQuery);
			} else if (transformation == TransformationType.RandomPermutation) {
				residualVectorQuery = rp.permute(residualVectorQuery);
			}

			// compute lookup table
			double[][] lookUpTable = computeLookupADC(residualVectorQuery);

			for (int j = 0; j < invertedLists[nearestCoarseIndex].size(); j++) {
				int iid = invertedLists[nearestCoarseIndex].getQuick(j);
				double l2distance = 0;
				int codeStart = j * numSubVectors;
				if (numProductCentroids <= 256) {
					byte[] pqCode = pqByteCodes[nearestCoarseIndex].toArray(codeStart, numSubVectors);
					for (int m = 0; m < pqCode.length; m++) {
						// plus 128 because byte range is -128..127
						l2distance += lookUpTable[m][pqCode[m] + 128];
					}
				} else {
					short[] pqCode = pqShortCodes[nearestCoarseIndex].toArray(codeStart, numSubVectors);
					for (int m = 0; m < pqCode.length; m++) {
						l2distance += lookUpTable[m][pqCode[m]];
					}
				}
				nn.offer(new Result(iid, l2distance));
			}
		}

		return nn;
	}

	/**
	 * Utility methods that computes the distance between a query vector and the pq code associated with the
	 * given id using the IVFADC approach. <br>
	 * TODO: The computation of the lookUpTable is not needed in this case.
	 * 
	 * @param qVector
	 *            The query vector
	 * @param existingVecId
	 *            The id of an already indexed vector
	 * @return
	 * @throws Exception
	 */
	public double computeDistanceIVFADC(double[] qVector, String existingVecId) throws Exception {
		double distance = 0;
		// find the coarse centroid where the specified id is quantized as well as its pq code
		int existingCoarseCentroidIndex = getInvertedListId(existingVecId);

		// quantize the given vector to the centroid of the coarse quantizer where the existing vector is
		// quantized and compute the residual vector
		double[] residualVectorQuery = computeResidualVector(qVector, existingCoarseCentroidIndex);

		// apply a random transformation if needed
		if (transformation == TransformationType.RandomRotation) {
			residualVectorQuery = rr.rotate(residualVectorQuery);
		} else if (transformation == TransformationType.RandomPermutation) {
			residualVectorQuery = rp.permute(residualVectorQuery);
		}

		// compute lookup table
		double[][] lookUpTable = computeLookupADC(residualVectorQuery);

		if (numProductCentroids <= 256) {
			byte[] pqCode = getPQCodeByte(existingVecId);
			for (int m = 0; m < pqCode.length; m++) {
				// plus 128 because byte range is -128..127
				distance += lookUpTable[m][pqCode[m] + 128];
			}
		} else {
			short[] pqCode = getPQCodeShort(existingVecId);
			for (int m = 0; m < pqCode.length; m++) {
				distance += lookUpTable[m][pqCode[m]];
			}
		}

		return distance;
	}

	/**
	 * TODO: implement this method
	 * 
	 * @param k
	 *            The number of nearest neighbors to be returned
	 * @param iid
	 *            The internal id of the query vector (code actually)
	 * @return A bounded priority queue of Result objects, which contains the k nearest neighbors along with
	 *         their iids and distances from the query vector, ordered by lowest distance.
	 */
	private BoundedPriorityQueue<Result> computeKnnIVFSDC(int k, int iid) {
		return null;
	}

	/**
	 * Takes a (residual) query vector as input and returns a lookup table containing the distance between
	 * each sub-vector from each centroid of the corresponding sub-quantizer. The calculation of this look-up
	 * table requires numSubVectors*numProductCentroids*subVectorLength multiplications. After this
	 * calculation, the distance between the query and any vector in the database can be computed in constant
	 * time.
	 * 
	 * @param queryVector
	 *            The (residual) query vector
	 * @return A lookup table of size numSubVectors * numProductCentroids with the distance of each sub-vector
	 *         from the centroids of each sub-quantizer
	 */
	private double[][] computeLookupADC(double[] queryVector) {
		double[][] distances = new double[numSubVectors][numProductCentroids];

		for (int i = 0; i < numSubVectors; i++) {
			int subvectorStart = i * subVectorLength;
			for (int j = 0; j < numProductCentroids; j++) {
				for (int k = 0; k < subVectorLength; k++) {
					distances[i][j] += (queryVector[subvectorStart + k] - productQuantizer[i][j][k])
							* (queryVector[subvectorStart + k] - productQuantizer[i][j][k]);
				}
			}
		}
		return distances;
	}

	/**
	 * Returns the index of the coarse centroid which is closer to the given vector.
	 * 
	 * @param vector
	 *            The vector
	 * @return The index of the nearest coarse centroid
	 */
	private int computeNearestCoarseIndex(double[] vector) {
		int centroidIndex = -1;
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < numCoarseCentroids; i++) {
			double distance = 0;
			for (int j = 0; j < vectorLength; j++) {
				distance += (coarseQuantizer[i][j] - vector[j]) * (coarseQuantizer[i][j] - vector[j]);
				if (distance >= minDistance) {
					break;
				}
			}
			if (distance < minDistance) {
				minDistance = distance;
				centroidIndex = i;
			}
		}
		return centroidIndex;
	}

	/**
	 * Returns the indices of the k coarse centroids which are closer to the given vector.
	 * 
	 * @param vector
	 *            The vector
	 * @param k
	 *            The number of nearest centroids to return
	 * @return The indices of the k nearest coarse centroids
	 */
	protected int[] computeNearestCoarseIndices(double[] vector, int k) {
		BoundedPriorityQueue<Result> bpq = new BoundedPriorityQueue<Result>(new Result(), k);

		double lowest = Double.MAX_VALUE;
		for (int i = 0; i < numCoarseCentroids; i++) {
			boolean skip = false;
			double l2distance = 0;
			for (int j = 0; j < vectorLength; j++) {
				l2distance += (coarseQuantizer[i][j] - vector[j]) * (coarseQuantizer[i][j] - vector[j]);
				if (l2distance > lowest) {
					skip = true;
					break;
				}
			}
			if (!skip) {
				bpq.offer(new Result(i, l2distance));
				if (i >= k) {
					lowest = bpq.last().getDistance();
				}
			}
		}
		int[] nn = new int[k];
		for (int i = 0; i < k; i++) {
			nn[i] = bpq.poll().getId();
		}
		return nn;
	}

	/**
	 * Finds and returns the index of the centroid of the subquantizer with the given index which is closer to
	 * the given subvector.
	 * 
	 * @param subvector
	 *            The subvector
	 * @param subQuantizerIndex
	 *            The index of the the subquantizer
	 * @return The index of the nearest centroid
	 */
	private int computeNearestProductIndex(double[] subvector, int subQuantizerIndex) {
		int centroidIndex = -1;
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < numProductCentroids; i++) {
			double distance = 0;
			for (int j = 0; j < subVectorLength; j++) {
				distance += (productQuantizer[subQuantizerIndex][i][j] - subvector[j])
						* (productQuantizer[subQuantizerIndex][i][j] - subvector[j]);
				if (distance >= minDistance) {
					break;
				}
			}
			if (distance < minDistance) {
				minDistance = distance;
				centroidIndex = i;
			}
		}
		return centroidIndex;
	}

	/**
	 * Computes the residual vector.
	 * 
	 * @param vector
	 *            The original vector
	 * @param centroidIndex
	 *            The centroid of the coarse quantizer from which the original vector is subtracted
	 * @return The residual vector
	 */
	private double[] computeResidualVector(double[] vector, int centroidIndex) {
		double[] residualVector = new double[vectorLength];
		for (int i = 0; i < vectorLength; i++) {
			residualVector[i] = coarseQuantizer[centroidIndex][i] - vector[i];
		}
		return residualVector;
	}

	/**
	 * Utility method that calculates and prints the min, max and avg number of items per inverted list of the
	 * index.
	 */
	public void outputItemsPerList() {
		int max = 0;
		int min = Integer.MAX_VALUE;
		double sum = 0;
		for (int i = 0; i < numCoarseCentroids; i++) {
			// Log.i(TAG, "List " + (i + 1) + ": " + perListLoadCounter[i]);
			if (invertedLists[i].size() > max) {
				max = invertedLists[i].size();
			}
			if (invertedLists[i].size() < min) {
				min = invertedLists[i].size();
			}
			sum += invertedLists[i].size();
		}

		Log.i(TAG, "Maximum number of vectors: " + max);
		Log.i(TAG, "Minimum number of vectors: " + min);
		Log.i(TAG, "Average number of vectors: " + (sum / numCoarseCentroids));

	}

	/**
	 * Loads the persistent index in memory.
	 * 
	 * @throws Exception
	 */
	private void loadIndexInMemory() throws Exception {
		long start = System.currentTimeMillis();
		Log.i(TAG, "Loading persistent index in memory.");

		DatabaseEntry foundKey = new DatabaseEntry();
		DatabaseEntry foundData = new DatabaseEntry();

		ForwardCursor cursor = null;
		if (useDiskOrderedCursor) { // disk ordered cursor
			DiskOrderedCursorConfig docc = new DiskOrderedCursorConfig();
			cursor = iidToIvfpqDB.openCursor(docc);
		} else {
			cursor = iidToIvfpqDB.openCursor(null, null);
		}

		int counter = 0;
		while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS
				&& counter < maxNumVectors) {
			TupleInput input = TupleBinding.entryToInput(foundData);
			int listId = input.readInt();
			// The following code assumes that internal ids are consequtive (as they should be).
			// It is possible, however, tha an indexed with non-consequtive ids was constructed.
			// invertedLists[listId].add(counter); // update ram based index
			// The following code works for non-consequitve internal ids as well.
			int iid = IntegerBinding.entryToInt(foundKey);
			invertedLists[listId].add(iid); // update ram based index

			if (numProductCentroids <= 256) {
				byte[] code = new byte[numSubVectors];
				for (int i = 0; i < numSubVectors; i++) {
					code[i] = input.readByte();
				}
				pqByteCodes[listId].add(code); // update ram based index
			} else {
				short[] code = new short[numSubVectors];
				for (int i = 0; i < numSubVectors; i++) {
					code[i] = input.readShort();
				}
				pqShortCodes[listId].add(code); // update ram based index
			}
			counter++;
			if (counter % 1000 == 0) {
				Log.i(TAG, counter + " vectors loaded in memory!");
			}
		}
		cursor.close();
		long end = System.currentTimeMillis();
		Log.i(TAG, counter + " images loaded in " + (end - start) + " ms!");
	}

	/**
	 * This is a utility method that can be used to dump the contents of the iidToIvfpqDB to a txt file.<br>
	 * Currently, only the list id of each item is dumped.
	 * 
	 * @param dumpFilename
	 *            Full path to the file where the dump will be written.
	 * @throws Exception
	 */
	public void dumpIidToIvfpqDB(String dumpFilename) throws Exception {
		DatabaseEntry foundKey = new DatabaseEntry();
		DatabaseEntry foundData = new DatabaseEntry();

		ForwardCursor cursor = iidToIvfpqDB.openCursor(null, null);
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(dumpFilename)));
		while (cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			int iid = IntegerBinding.entryToInt(foundKey);
			TupleInput input = TupleBinding.entryToInput(foundData);
			int listId = input.readInt();
			out.write(iid + " " + listId + "\n");
		}
		cursor.close();
		out.close();
	}

	/**
	 * Appends the persistent index with the given (byte) code.
	 * 
	 * @param code
	 *            The code
	 */
	private void appendPersistentIndex(int listId, byte[] code) {
		// write id, listId and code
		TupleOutput output = new TupleOutput();
		output.writeInt(listId);
		for (int i = 0; i < numSubVectors; i++) {
			output.writeByte(code[i]);
		}
		DatabaseEntry data = new DatabaseEntry();
		TupleBinding.outputToEntry(output, data);
		DatabaseEntry key = new DatabaseEntry();
		IntegerBinding.intToEntry(loadCounter, key);
		iidToIvfpqDB.put(null, key, data);
	}

	/**
	 * Appends the persistent index with the given (short) code.
	 * 
	 * @param code
	 *            The code
	 */
	private void appendPersistentIndex(int listId, short[] code) {
		// write id, listId and code
		TupleOutput output = new TupleOutput();
		output.writeInt(listId);
		for (int i = 0; i < numSubVectors; i++) {
			output.writeShort(code[i]);
		}
		DatabaseEntry data = new DatabaseEntry();
		TupleBinding.outputToEntry(output, data);
		DatabaseEntry key = new DatabaseEntry();
		IntegerBinding.intToEntry(loadCounter, key);
		iidToIvfpqDB.put(null, key, data);
	}

	/**
	 * Returns the pq code of the image with the given id.
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public byte[] getPQCodeByte(String id) throws Exception {
		int iid = getInternalId(id);
		if (iid == -1) {
			throw new Exception("Id does not exist!");
		}
		if (numProductCentroids > 256) {
			throw new Exception("Call the short variant of the method!");
		}

		DatabaseEntry key = new DatabaseEntry();
		IntegerBinding.intToEntry(iid, key);
		DatabaseEntry data = new DatabaseEntry();
		if ((iidToIvfpqDB.get(null, key, data, null) == OperationStatus.SUCCESS)) {
			TupleInput input = TupleBinding.entryToInput(data);
			input.readInt(); // skip the list id
			byte[] code = new byte[numSubVectors];
			for (int i = 0; i < numSubVectors; i++) {
				code[i] = input.readByte();
			}
			return code;
		} else {
			throw new Exception("Id does not exist!");
		}
	}

	/**
	 * Returns the pq code of the image with the given id.
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public short[] getPQCodeShort(String id) throws Exception {
		int iid = getInternalId(id);
		if (iid == -1) {
			throw new Exception("Id does not exist!");
		}
		if (numProductCentroids <= 256) {
			throw new Exception("Call the short variant of the method!");
		}

		DatabaseEntry key = new DatabaseEntry();
		IntegerBinding.intToEntry(iid, key);
		DatabaseEntry data = new DatabaseEntry();
		if ((iidToIvfpqDB.get(null, key, data, null) == OperationStatus.SUCCESS)) {
			TupleInput input = TupleBinding.entryToInput(data);
			input.readInt(); // skip the list id
			short[] code = new short[numSubVectors];
			for (int i = 0; i < numSubVectors; i++) {
				code[i] = input.readShort();
			}
			return code;
		} else {
			throw new Exception("Id does not exist!");
		}
	}

	/**
	 * Returns the inverted list of the image with the given id.
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public int getInvertedListId(String id) throws Exception {
		int iid = getInternalId(id);
		if (iid == -1) {
			throw new Exception("Id does not exist!");
		}

		DatabaseEntry key = new DatabaseEntry();
		IntegerBinding.intToEntry(iid, key);
		DatabaseEntry data = new DatabaseEntry();
		if ((iidToIvfpqDB.get(null, key, data, null) == OperationStatus.SUCCESS)) {
			TupleInput input = TupleBinding.entryToInput(data);
			int listId = input.readInt();
			return listId;
		} else {
			throw new Exception("Id does not exist!");
		}
	}

	@Override
	public void outputIndexingTimesInternal() {
	}

	@Override
	public void closeInternal() {
		iidToIvfpqDB.close();
	}

}
