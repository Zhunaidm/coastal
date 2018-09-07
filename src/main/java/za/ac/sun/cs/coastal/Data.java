package za.ac.sun.cs.coastal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;

public class Data {

    private static Map<Tuple, String> tuples = new HashMap<Tuple, String>();
    private static Map<Tuple, bucket> buckets = new HashMap<Tuple, bucket>();
    private static Set<String> branches = new HashSet<String>();
    private static Map<Tuple, Integer> worstCaseBuckets = new HashMap<Tuple, Integer>();
    private static Map<Tuple, Integer> localBuckets;
    private static Map<ByteArrayWrapper, ArrayList<Tuple>> inputTuples = new HashMap<ByteArrayWrapper, ArrayList<Tuple>>();
    private static Map<ByteArrayWrapper, Integer> worstCaseScores = new HashMap<ByteArrayWrapper, Integer>();
    private static ArrayList<Byte[]> coastalInputs = new ArrayList<Byte[]>();
    private static String prevBranch = "Source";
    private static byte[] currentInput = null;
    private static boolean newTuple = false;
    private static boolean worstCaseMode = false;
    private static int branchNo = 0;
    private static int counter = 0;

    public static void resetAll() {
        tuples = null;
        tuples = new HashMap<Tuple, String>();
        branches = null;
        branches = new HashSet<String>();
        if (worstCaseMode) {
            worstCaseBuckets = null;
            worstCaseBuckets = new HashMap<Tuple, Integer>();
        } else {
            buckets = null;
            buckets = new HashMap<Tuple, bucket>();
        }
        prevBranch = "Source";
        newTuple = false;
        branchNo = 0;
    }

    public static void resetTuples() {
        prevBranch = "Source";
        newTuple = false;

    }

    public static void resetSource() {
        prevBranch = "Source";
    }

    public static void resetLocal() {
        localBuckets = null;
        localBuckets = new HashMap<Tuple, Integer>();
    }

    public static ArrayList<Tuple> getPossibleBranches(String branchName) {
        ArrayList<Tuple> branches = new ArrayList<Tuple>();
        for (Tuple tuple : tuples.keySet()) {
            if (tuple.getSrc().equals(branchName)) {
                branches.add(tuple);
            }
        }
        return branches;
    }

    public static ArrayList<Tuple> getTuples() {
        return new ArrayList<Tuple>(tuples.keySet());
    }

    public static void incCounter() {
        counter++;
    }

    public static int getCounter() {
        return counter;
    }

    public static int getSize() {
        return tuples.size();
    }

    public static int getNoBranches() {
        return branches.size();
    }

    public static byte[] getCurrentInput() {
        return currentInput;
    }

    public static void setCurrentInput(byte[] input) {
        currentInput = input;
    }

    public static void setWorstCaseMode(boolean result) {
        worstCaseMode = result;
    }

    public static int getNextBranchNo() {
        branchNo++;
        return branchNo;
    }

    public static void resetBranchNo() {
        branchNo = 0;
    }

    public static void addTuple(String src, String dest) {
        Tuple tuple = new Tuple(src, dest);

        if (!src.equals("Source")) {
            branches.add(src);
        }
        if (!dest.equals("Source")) {
            branches.add(dest);
        }

        if (!tuples.containsKey(tuple) && !src.equals(dest)) {
            tuples.put(tuple, "");
            if (worstCaseMode) {
                worstCaseBuckets.put(tuple, 1);
            } else {
                buckets.put(tuple, bucket.ONE);
            }
            localBuckets.put(tuple, 1);
            newTuple = true;
        } else if (!localBuckets.containsKey(tuple) && !src.equals(dest)) {
            localBuckets.put(tuple, 1);
        } else {
            incrementBucketCount(tuple);
            if (worstCaseMode) {
                int bucketCount = getBucketCount(tuple);
                if (bucketCount > worstCaseBuckets.get(tuple)) {
                    worstCaseBuckets.put(tuple, bucketCount);
                    newTuple = true;
                }

            } else {
                bucket type = getBucketValue(tuple);
                if (type.ordinal() > buckets.get(tuple).ordinal()) {
                    buckets.put(tuple, type);
                    newTuple = true;
                }
            }
        }
    }

    public static void incrementBucketCount(Tuple tuple) {
        localBuckets.put(tuple, localBuckets.get(tuple) + 1);
    }

    public static int getLocalBucketSize() {
        return localBuckets.size();
    }

    public static bucket getBucketValue(Tuple tuple) {
        int count = localBuckets.get(tuple);
        bucket type = null;

        if (count == 1) {
            type = bucket.ONE;
        } else if (count == 2) {
            type = bucket.TWO;
        } else if (count == 3) {
            type = bucket.THREE;
        } else if (count >= 4 && count <= 7) {
            type = bucket.FOUR;
        } else if (count >= 8 && count <= 15) {
            type = bucket.FIVE;
        } else if (count >= 16 && count <= 31) {
            type = bucket.SIX;
        } else if (count >= 32 && count <= 127) {
            type = bucket.SEVEN;
        } else if (count >= 128) {
            type = bucket.EIGHT;
        }

        return type;
    }

    public static int getBucketCount(Tuple tuple) {
        return localBuckets.get(tuple);
    }

    public static boolean containsTuple(String src, String dest) {
        return tuples.containsKey(new Tuple(src, dest));
    }

    public static String getPrevious() {
        return prevBranch;
    }

    public static void setPrevious(String branch) {
        prevBranch = branch;
    }

    public static boolean getNew() {
        if (newTuple) {
            Set<Tuple> set = localBuckets.keySet();
            ArrayList<Tuple> tuples = new ArrayList<Tuple>(set);
            inputTuples.put(new ByteArrayWrapper(Arrays.copyOf(currentInput, currentInput.length)), tuples);

            if (worstCaseMode) {
                ArrayList<Integer> bucketValues = new ArrayList<Integer>(localBuckets.values());
                ArrayList<Tuple> bucketTuples = new ArrayList<Tuple>(localBuckets.keySet());
                int score = 0;
                // for (Integer value : bucketValues) {
                System.out.println("INPUT: " + new String(currentInput));
                for (int i = 0; i < bucketValues.size(); i++) {
                    System.out.println("Src: " + bucketTuples.get(i).getSrc() + " Dest: "
                            + bucketTuples.get(i).getDest() + " Score: " + bucketValues.get(i));
                    score += bucketValues.get(i);
                }
                // }
                worstCaseScores.put(new ByteArrayWrapper(Arrays.copyOf(currentInput, currentInput.length)), score);
            }
        }
        return newTuple;
    }

    public static boolean newMaxWorst(byte[] input) {
        int max = 0;

        for (int score : worstCaseScores.values()) {
            if (score > max) {
                max = score;
            }
        }

        if (worstCaseScores.get(new ByteArrayWrapper(input)) == max) {
            return true;
        }

        return false;
    }

    public static void addInitialList(byte[] input) {
        if (worstCaseMode) {
            ArrayList<Integer> bucketValues = new ArrayList<Integer>(localBuckets.values());
            int score = 0;
            for (Integer value : bucketValues) {
                score += value;
            }
            worstCaseScores.put(new ByteArrayWrapper(Arrays.copyOf(currentInput, currentInput.length)), score);
        } else {
            Set<Tuple> set = localBuckets.keySet();
            ArrayList<Tuple> tuples = new ArrayList<Tuple>(set);
            inputTuples.put(new ByteArrayWrapper(Arrays.copyOf(input, input.length)), tuples);
        }
    }

    public static ArrayList<Tuple> getInputList(byte[] input) {
        return inputTuples.get(new ByteArrayWrapper(input));
    }

    public static int getWorstCaseScore(byte[] input) {
        return worstCaseScores.get(new ByteArrayWrapper(input));
    }

    public static void addCoastalInput(Byte[] input) {
        coastalInputs.add(input);
    }

    public static ArrayList<Byte[]> getCoastalInputs() {
        return coastalInputs;
    }

    public static void clearCoastalInputs() {
        coastalInputs.clear();
    }

    public enum bucket {
        ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT;
    }

}

class ByteArrayWrapper {
    private final byte[] data;

    public ByteArrayWrapper(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper)) {
            return false;
        }
        return Arrays.equals(data, ((ByteArrayWrapper) other).data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
