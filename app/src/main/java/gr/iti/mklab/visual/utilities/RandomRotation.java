package gr.iti.mklab.visual.utilities;

import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;

/**
 * This class can be used for performing a random orthogonal transformation on a given vector.
 * 
 * @author Eleftherios Spyromitros-Xioufis
 * 
 */
public class RandomRotation {

	/**
	 * This is the random rotation matrix.
	 */
	private DMatrixRMaj randomMatrix;

	/**
	 * Constructor that initializes a random rotation matrix using the EJML library.
	 * 
	 * @param seed
	 *            The seed used for generating the random rotation matrix
	 * @param dim
	 *            The dimensionality of the vectors that we want to randomly rotate
	 */
	public RandomRotation(int seed, int dim) {
		Random rand = new Random(seed);
		// create a random rotation matrix
		randomMatrix = new DMatrixRMaj(dim, dim);
		randomMatrix = RandomMatrices_DDRM.orthogonal(dim, dim, rand);
	}

	/**
	 * Randomly rotates a vector using the random rotation matrix that was created in the constructor.
	 * 
	 * @param vector
	 *            The initial vector
	 * @return The randomly rotated vector
	 */
	public double[] rotate(double[] vector) {
		DMatrixRMaj transformed = new DMatrixRMaj(1, vector.length);
		DMatrixRMaj original = DMatrixRMaj.wrap(1, vector.length, vector);
		CommonOps_DDRM.mult(original, randomMatrix, transformed);
		return transformed.getData();
	}
}
