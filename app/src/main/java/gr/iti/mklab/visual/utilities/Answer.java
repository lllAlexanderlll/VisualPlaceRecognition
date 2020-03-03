package gr.iti.mklab.visual.utilities;

import com.tud.alexw.visualplacerecognition.result.ImageAnnotation;

/**
 * Objects of this class represent the response of an index structure to a query.
 *
 * @author Eleftherios Spyromitros-Xioufis
 */
public class Answer {

    private String TAG = "Answer";
    /**
     * Time taken to search the index (ms).
     */
    private long indexSearchTime;
    /**
     * Time taken for name look-up (ms).
     */
    private long nameLookupTime;

    /**
     * The ids of the results ordered by increasing distance.
     */
    private String[] ids;

    /**
     * The distances of the results in ascending order.
     */
    private double[] distances;

    public String[] getIds() {
        return ids;
    }

    public double[] getDistances() {
        return distances;
    }

    public ImageAnnotation[] imageAnnotations;

    /**
     * Constructor
     *
     * @param ids
     * @param distances
     * @param nameLookupTime
     * @param indexSearchTime
     */
    public Answer(String[] ids, double[] distances, long nameLookupTime, long indexSearchTime) {
        this.ids = ids;
        this.distances = distances;
        this.nameLookupTime = nameLookupTime;
        this.indexSearchTime = indexSearchTime;
        this.imageAnnotations = new ImageAnnotation[distances.length];
    }

    public long getIndexSearchTime() {
        return indexSearchTime/1000000;
    }

    public long getNameLookupTime() {
        return nameLookupTime/1000000;
    }

    public void calculateAnnotations(){
        for(int i = 0; i< ids.length; i++){
            imageAnnotations[i] = ImageAnnotation.decodeFilename(ids[i]);
        }
    }

    public ImageAnnotation[] getImageAnnotations(){
        return imageAnnotations;
    }


    @Override
    public String toString() {
        if (ids.length == 0) {
            return "Noting was retrieved";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            stringBuilder.append(String.format("%d. %s %f", i, ids[i], distances[i])).append("\n");
        }
        stringBuilder
                .append("Index search time: ").append(getIndexSearchTime()).append("ms \n")
                .append("Name lookup time: ").append(getNameLookupTime()).append("ms \n");
        return stringBuilder.toString();
    }
}
