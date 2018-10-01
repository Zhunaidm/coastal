
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

import org.apache.logging.log4j.Logger;

import org.apache.logging.log4j.LogManager;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;

import java.util.Properties;

import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.Thread;

import java.util.Random;
import java.util.Queue;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.Set;

import za.ac.sun.cs.coastal.ConfigurationBuilder;
import za.ac.sun.cs.coastal.reporting.ReporterManager;
import za.ac.sun.cs.coastal.strategy.JAFLStrategy;

import za.ac.sun.cs.coastal.COASTAL;
import za.ac.sun.cs.coastal.Configuration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Properties;

import java.nio.ByteBuffer;

import java.security.Permission;

public class JAFL {

    private static int ARITH_MAX = 35;
    private static int[] interesting_8 = { -128, -1, 0, 1, 16, 32, 64, 100, 127 };
    private static int[] interesting_16 = { -32768, -128, 128, 255, 256, 512, 1000, 1024, 4096, 32767 };
    private static int[] interesting_32 = { -2147483648, -100663046, -32769, 32768, 65535, 65536, 100663045,
            2147483647 };
    private static boolean abort = false;
    private static Class<?> cls;
    private static String className;
    private static String initialClassName;
    private static int noWorstInputs = 0;
    private static int paths = 0;
    private static int totalPaths = 0;
    private static String file = "";
    // private static PriorityQueue<Input> queue;
    private static Queue<Input> queue;
    // private static Comparator<Input> comparator = new InputComparator();
    private static double preTime;
    private static int runNumber = 0;
    private static int runs = 0;
    private static boolean worstCaseMode = false;
    private static boolean concolicMode = true;
    private static int currentOperation = 0;
    private static ByteSet crashingInputs = new ByteSet();
    private static Properties prop = null;

    //For Mystery
    //examples.strings.MysteryFuzz test_mystery.txt
    //examples.strings.DB test.txt
    public static void main(String[] args) throws Exception {
        /* if (args.length == 3) {
            System.out.println(args[2]);
            if (args[2].equals("-b")) {
                worstCaseMode = false;
            } else if (args[2].equals("-w")) {
                worstCaseMode = true;
            }
        } */

        parseProperties(args[0]);
        className = initialClassName + "2";
        // Instrument Code

        Instrumenter.instrument(initialClassName, className);
        (new Thread(new FuzzUI())).start();

        //file = args[1];
        cls = Class.forName(className);
        // queue = new PriorityQueue<Input>(10, comparator);
        queue = new LinkedList<Input>();
        Path path = Paths.get(file);
        byte[] base = Files.readAllBytes(path);

        SystemExitControl.forbidSystemExitCall();
        Data.resetAll();
        execProgram(base);
        Data.addInitialList(base);
        int score = 0;
        if (worstCaseMode) {
            score = Data.getWorstCaseScore(base);
        } else {
            score = Data.getLocalBucketSize();
        }
        queue.add(new Input(base, false, score, false));
        BufferedReader br = new BufferedReader(new FileReader(".branches"));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            line = (line.split(":"))[1];
            line = line.trim();
            totalPaths += Integer.parseInt(line);
        }
        br.close();
        preTime = System.currentTimeMillis();
        while (!abort) {
            runNumber++;
            Data.resetTuples();
            Input input = queue.remove();
            byte[] basic = input.getData();
            System.out.println("Base: " + new String(basic));
            System.out.println("ASCII: " + input);
            queue.add(new Input(basic, true, input.getScore(), input.getCoastalEvaluated()));
            byte[] temp = Arrays.copyOf(basic, basic.length);
            if (!input.getEvaluated()) {
                // System.out.println("Performing Bit Flips...\n");
                currentOperation = 0;
                flipBits(temp);

                // System.out.println("Performing Byte Flips...\n");
                currentOperation = 1;
                flipBytes(temp);

                currentOperation = 2;
                arithInc(temp);

                currentOperation = 3;
                arithDec(temp);

                currentOperation = 4;
                replaceInteresting(temp);
            }
            currentOperation = 5;
            havoc(temp);

            if (runNumber % 5 == 0) {
                cullQueue();
            }            

            // Run Coastal
            if (concolicMode && runNumber != 0 && runNumber % 10 == 0) {
                // Create a temporary new queue
                ArrayList<Input> newInputs = new ArrayList<Input>();
                System.out.println("Startiing coastal...");
                for (Input qInput : queue) {
                    newInputs.add(new Input(qInput.getData(), qInput.getEvaluated(), qInput.getScore(), true));
                    if (qInput.getCoastalEvaluated()) {continue;}

                    byte[] fuzzInput = qInput.getData();
                    // Redirect coastal logging.
                    // PrintStream original = System.out;
                    /*System.setOut(new PrintStream(new OutputStream() {
                        public void write(int b) {
                            //DO NOTHING
                        }
                    }));*/
                    //runCoastal(fuzzInput);
                    runCoastal(Arrays.copyOf(fuzzInput, fuzzInput.length + 5));
                    //System.setOut(original);
                    System.out.println("COASTAL RAN SUCCESSFULLY");
                    System.out.println("Base Input: " + new String(fuzzInput));

                    System.out.println();
                    System.out.println("--------------------------------------------");
                    ArrayList<Byte[]> coastalInputs = JAFLStrategy.getCoastalInputs();

                    for (Byte[] cInput : coastalInputs) {
                        System.out.print("Coastal output: ");
                        byte[] word = new byte[cInput.length];
                        int i = 0;

                        for (Byte b : cInput) {
                            System.out.print(" " + b.byteValue());
                            word[i++] = b.byteValue();
                        }
                       // word = new byte[] {60, 0, 32, 104, 82, 69, 70, 61, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        System.out.println(" Word: " + new String(word));
                        
                        System.out.println();

                        // Execute program with the Coastal input
                        System.out.println("Executing input...");
                        execProgram(word);
                        System.out.println("Is New? :" + Data.getNew());
                        if (Data.getNew()) {
                            System.out.println("IT'S NEW");
                            int inputScore = Data.getLocalBucketSize();
                            newInputs.add(new Input(Arrays.copyOf(word, word.length), false, inputScore, false));
                            Data.resetTuples();
                            paths++;
                        }
                    }
                    System.out.println("--------------------------------------------");
                    Data.clearCoastalInputs();

                }
            
                queue = new LinkedList<Input>(newInputs);

            }

        }

    }    

    public static String getClassName() {
        return className;
    }

    public static int getMode() {
        return worstCaseMode ? 1 : 0;
    }

    public static double getExecutionTime() {
        return (System.currentTimeMillis() - preTime) / 1000;
    }

    public static int getCurrentOperation() {
        return currentOperation;
    }

    public static double getCoverage() {
        if (totalPaths == 0) {
            return 0;
        }
        return Math.round(((Data.getNoBranches() / (double) totalPaths) * 100) * 100.0) / 100.0;
    }

    public static int getNumberPaths() {
        return paths;
    }

    public static int getQueueSize() {
        if (queue == null) {
            return 0;
        }
        return queue.size();
    }

    public static int getNumberCrashes() {
        return crashingInputs.size();
    }

    public static int getNumberRuns() {
        return runs;
    }

    public static void parseProperties(String inputFile) {
        try {
            InputStream propFile = new FileInputStream(inputFile);
            prop = new Properties();

            prop.load(propFile);

            initialClassName = prop.getProperty("jafl.main");
            String[] classes = prop.getProperty("jafl.classes").split(",");
            file = prop.getProperty("jafl.test");

            propFile.close();


        } catch (Exception e) {
            System.out.println("Could not find specified properties file.");
        }
    }

    public static void execProgram(byte[] base) throws IOException {
        runs++;

        try {
            Method meth = cls.getMethod("main", String[].class);
            Data.resetTuples();
            Data.resetLocal();
            Data.setCurrentInput(base);
            FileOutputStream fos = new FileOutputStream(".temp");
            fos.write(base);
            fos.close();
            meth.invoke(null, (Object) (new String[] { ".temp" }));

        } catch (SystemExitControl.ExitTrappedException e) {
            if (!crashingInputs.containsByteArray(base)) {
                crashingInputs.add(Arrays.copyOf(base, base.length));
                saveResult(base, 1);
            }
            System.out.println("Preventing abort...");
            // abort = true;
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof SystemExitControl.ExitTrappedException) {
                if (!crashingInputs.containsByteArray(base)) {
                    crashingInputs.add(Arrays.copyOf(base, base.length));
                    saveResult(base, 1);
                }
                System.out.println("Preventing abort...");
                // abort = true;
            }

        } catch (Exception e) {}

        if (Data.getNew()) {
            if (worstCaseMode && Data.newMaxWorst(base)) {
                saveResult(base, 2);
                noWorstInputs++;
            } else if (!worstCaseMode) {
                saveResult(base, 0);
            }
        }

    }

    // Run Concolic execution trough Coastal
    private static void runCoastal(byte[] input) throws Exception {
        // Currently hardcoded to Deadbeef example.
        storeInputFile(input);
        final Logger log = LogManager.getLogger("COASTAL");
        // MysteryFuzz
        //props.setProperty("coastal.main", "examples.strings.MysteryFuzz");
        //props.setProperty("coastal.targets", "examples.strings");
        //props.setProperty("coastal.triggers", "examples.strings.MysteryFuzz.preserveSomeHtmlTagsAndRemoveWhitespaces(X: String)");
        //props.setProperty("coastal.delegates", "java.lang.String:za.ac.sun.cs.coastal.model.String");
        //DeadBeef
        //prop.setProperty("coastal.main", "examples.strings.DB");
        //prop.setProperty("coastal.targets", "examples.strings");
        //prop.setProperty("coastal.triggers", "examples.strings.DB.analyse(X: String)");
        
       //prop.setProperty("coastal.listeners", "za.ac.sun.cs.coastal.listener.control.StopController");
        //prop.setProperty("coastal.strategy", "za.ac.sun.cs.coastal.strategy.JAFLStrategy");
        //prop.setProperty("green.z3.path", "/home/zhunaid/z3/z3-4.7.1-x64-ubuntu-16.04/bin/z3");
        //props.setProperty("green.z3.path", "/Users/visserw/Documents/tools/z3-master/bin/z3");
        //prop.setProperty("coastal.echooutput", "false");

        final String version = "coastal-test";
        final ReporterManager reporterManager = new ReporterManager();
        ConfigurationBuilder cb = new ConfigurationBuilder(log, version, reporterManager);
        cb.readFromProperties(prop);
        cb.setArgs(".temp");
        Configuration config = cb.construct();
        new COASTAL(config).start();
    }

    private static void storeInputFile(byte[] input) throws Exception {
        FileOutputStream fos = new FileOutputStream(".temp");
        fos.write(input);
        fos.close();
    }

    public static void saveResult(byte[] result, int type) throws IOException {
        File inFile = new File(".temp");
        File outFile;

        switch (type) {
        case 0:
            // Save inputs with new tuples.
            outFile = new File("output/" + className + "/" + className + "_output" + paths + ".txt");
            outFile.mkdirs();
            Files.copy(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            break;
        case 1:
            // Save inputs which crash.
            outFile = new File("output/" + className + "/" + className + "_error" + crashingInputs.size() + ".txt");
            outFile.mkdirs();
            Files.copy(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            break;
        case 2:
            // Save Worst case inputs with higher score.
            outFile = new File("output/" + className + "/" + className + "_worst" + noWorstInputs + ".txt");
            outFile.mkdirs();
            Files.copy(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            break;
        default:
            break;
        }
    }

    public static void cullQueue() {
        ArrayList<Input> list = new ArrayList<Input>(queue);
        ArrayList<Tuple> tuples = Data.getTuples();
        Set<Input> newInputs = new HashSet<Input>();
        Set<Input> worstInputs = new HashSet<Input>();
        Set<Tuple> evaluatedTuples = new HashSet<Tuple>();
        boolean evaluated = false;
        boolean coastalEvaluated = false;
        int score = 0;
        int maxScore = 0;
        byte[] winningInput = null;

        for (Tuple tuple : tuples) {
            score = 0;
            if (!evaluatedTuples.contains(tuple)) {
                for (Input input : list) {
                    byte[] inputArr = input.getData();
                    ArrayList<Tuple> inputList = Data.getInputList(inputArr);
                    if (inputList == null && list.size() != 1) {
                        continue;
                    } else if (inputList == null && list.size() == 1) {
                        newInputs.add(input);
                        break;
                    }
                    if (worstCaseMode) {
                        if (inputList.contains(tuple) && (Data.getWorstCaseScore(inputArr) > score)) {
                            score = Data.getWorstCaseScore(inputArr);
                            winningInput = inputArr;
                            evaluated = input.getEvaluated();
                            coastalEvaluated = input.getCoastalEvaluated();
                            worstInputs.add(input);
                            if (maxScore < score) {
                                maxScore = score;
                            }
                        }
                    } else {
                        if (inputList.contains(tuple) && (inputList.size() > score)) {
                            score = inputList.size();
                            winningInput = inputArr;
                            evaluated = input.getEvaluated();
                            coastalEvaluated = input.getCoastalEvaluated();

                        }
                    }
                }
                evaluatedTuples.addAll(Data.getInputList(winningInput));
                newInputs.add(new Input(winningInput, evaluated, score, coastalEvaluated));
            }
        }
        if (worstCaseMode) {
            for (Input input : list) {
                if (!worstInputs.contains(input) && Data.getWorstCaseScore(input.getData()) > maxScore) {
                    worstInputs.add(input);
                    newInputs.add(
                            new Input(input.getData(), input.getEvaluated(), Data.getWorstCaseScore(input.getData()), input.getCoastalEvaluated()));
                }
            }
        }
        queue = new LinkedList<Input>(newInputs);

    }

    // Remove a byte from the byte array.
    public static byte[] removeByte(byte[] base, int index) {
        int count = 0;
        byte[] newBase = new byte[base.length - 1];
        for (int i = 0; i < base.length; i++) {
            if (i == index) {
                continue;
            }
            newBase[count++] = base[i];
        }

        return newBase;
    }

    // Add a byte from the byte array.
    public static byte[] addByte(byte[] base, byte temp, int index) throws IOException {
        int count = 0;
        byte[] newBase = new byte[base.length + 1];
        for (int i = 0; i < newBase.length; i++) {
            if (i == index) {
                newBase[i] = temp;
                continue;
            }
            newBase[i] = base[count++];
        }

        return newBase;
    }

    // Replace a byte from the byte array.
    public static byte[] replaceByte(byte[] base, byte temp, int index) throws IOException {
        base[index] = temp;
        return base;
    }

    public static void flipBits(byte[] base) throws Exception {
        // System.out.println("Single bit flip...");
        // 1 Walking bit.
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] ^ (1 << i));
            }
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] ^ (1 << i));
            }
        }
        // 2 Walking bits.
        // System.out.println("2 bit flips...");
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] ^ (1 << i));
                base[j] = (byte) (base[j] ^ (1 << (i + 1)));
            }
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] ^ (1 << i));
                base[j] = (byte) (base[j] ^ (1 << (i + 1)));
            }
        }
        // 4 Walking bits.
        // System.out.println("4 bit flips...");
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] ^ (1 << i));
                base[j] = (byte) (base[j] ^ (1 << (i + 1)));
                base[j] = (byte) (base[j] ^ (1 << (i + 2)));
                base[j] = (byte) (base[j] ^ (1 << (i + 3)));
            }
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] ^ (1 << i));
                base[j] = (byte) (base[j] ^ (1 << (i + 1)));
                base[j] = (byte) (base[j] ^ (1 << (i + 2)));
                base[j] = (byte) (base[j] ^ (1 << (i + 3)));
            }
        }

    }

    public static void flipBytes(byte[] base) throws Exception {
        // Walking byte.
        // System.out.println("Single byte flip...");
        for (int j = 0; j < base.length; j++) {
            base[j] = (byte) (base[j] ^ 0xFF);
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            base[j] = (byte) (base[j] ^ 0xFF);

        }
        // 2 Walking bytes.
        // System.out.println("2 byte flips...");
        if (base.length < 2) {
            return;
        }
        for (int j = 0; j < base.length - 1; j++) {
            base[j] = (byte) (base[j] ^ 0xFF);
            base[j + 1] = (byte) (base[j + 1] ^ 0xFF);
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            base[j] = (byte) (base[j] ^ 0xFF);
            base[j + 1] = (byte) (base[j + 1] ^ 0xFF);

        }

        // 4 Walking bytes.
        // System.out.println("4 byte flips...");
        if (base.length < 4) {
            return;
        }

        for (int j = 0; j < base.length - 3; j++) {
            base[j] = (byte) (base[j] ^ 0xFF);
            base[j + 1] = (byte) (base[j + 1] ^ 0xFF);
            base[j + 2] = (byte) (base[j + 2] ^ 0xFF);
            base[j + 3] = (byte) (base[j + 3] ^ 0xFF);
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            base[j] = (byte) (base[j] ^ 0xFF);
            base[j + 1] = (byte) (base[j + 1] ^ 0xFF);
            base[j + 2] = (byte) (base[j + 2] ^ 0xFF);
            base[j + 3] = (byte) (base[j + 3] ^ 0xFF);

        }

    }

    public static void arithInc(byte[] base) throws Exception {

        // 1 Byte increment
        for (int i = 1; i <= ARITH_MAX; i++) {
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] + i);

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }
                base[j] = (byte) (base[j] - i);
            }
        }
        // 2 Byte increment
        for (int i = 1; i <= ARITH_MAX; i++) {
            for (int j = 0; j < base.length - 1; j++) {
                base[j] = (byte) (base[j] + i);
                base[j + 1] = (byte) (base[j + 1] + i);

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }
                base[j] = (byte) (base[j] - i);
                base[j + 1] = (byte) (base[j + 1] - i);
            }
        }
        // 4 Byte increment
        for (int i = 1; i <= ARITH_MAX; i++) {
            for (int j = 0; j < base.length - 3; j++) {
                base[j] = (byte) (base[j] + i);
                base[j + 1] = (byte) (base[j + 1] + i);
                base[j + 2] = (byte) (base[j + 2] + i);
                base[j + 3] = (byte) (base[j + 3] + i);

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }
                base[j] = (byte) (base[j] - i);
                base[j + 1] = (byte) (base[j + 1] - i);
                base[j + 2] = (byte) (base[j + 2] - i);
                base[j + 3] = (byte) (base[j + 3] - i);
            }
        }

    }

    public static void arithDec(byte[] base) throws Exception {
        // 1 Byte decrement

        for (int i = 1; i <= ARITH_MAX; i++) {
            for (int j = 0; j < base.length; j++) {
                base[j] = (byte) (base[j] - i);
                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }
                base[j] = (byte) (base[j] + i);
            }
        }
        // 2 Byte decrement

        for (int i = 1; i <= ARITH_MAX; i++) {
            for (int j = 0; j < base.length - 1; j++) {
                base[j] = (byte) (base[j] - i);
                base[j + 1] = (byte) (base[j + 1] - i);

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }
                base[j] = (byte) (base[j] + i);
                base[j + 1] = (byte) (base[j + 1] + i);
            }
        }
        // 4 Byte decrement

        for (int i = 1; i <= ARITH_MAX; i++) {
            for (int j = 0; j < base.length - 3; j++) {
                base[j] = (byte) (base[j] - i);
                base[j + 1] = (byte) (base[j + 1] - i);
                base[j + 2] = (byte) (base[j + 2] - i);
                base[j + 3] = (byte) (base[j + 3] - i);

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }
                base[j] = (byte) (base[j] + i);
                base[j + 1] = (byte) (base[j + 1] + i);
                base[j + 2] = (byte) (base[j + 2] + i);
                base[j + 3] = (byte) (base[j + 3] + i);
            }
        }

    }

    public static void replaceInteresting(byte[] base) throws IOException {
        // Setting 1 byte integers
        for (int i = 0; i < interesting_8.length; i++) {
            for (int j = 0; j < base.length; j++) {
                byte currentVal = base[j];
                base[j] = (byte) interesting_8[i];
                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }

                base[j] = currentVal;
            }
        }

        // Setting 2 byte integers
        for (int i = 0; i < interesting_16.length; i++) {
            for (int j = 0; j < base.length - 1; j++) {
                byte currentVal1 = base[j];
                byte currentVal2 = base[j + 1];
                byte[] temp = ByteBuffer.allocate(4).putInt(interesting_16[i]).array();

                base[j] = temp[0];
                base[j + 1] = temp[1];

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }

                base[j] = temp[1];
                base[j + 1] = temp[0];

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }

                base[j] = currentVal1;
                base[j + 1] = currentVal2;

            }
        }
        // Setting 4 byte integers
        for (int i = 0; i < interesting_32.length; i++) {
            for (int j = 0; j < base.length - 3; j++) {
                byte currentVal1 = base[j];
                byte currentVal2 = base[j + 1];
                byte currentVal3 = base[j + 1];
                byte currentVal4 = base[j + 1];
                byte[] temp = ByteBuffer.allocate(4).putInt(interesting_32[i]).array();

                base[j] = temp[0];
                base[j + 1] = temp[1];
                base[j + 2] = temp[2];
                base[j + 3] = temp[3];

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }

                base[j] = temp[3];
                base[j + 1] = temp[2];
                base[j + 2] = temp[1];
                base[j + 3] = temp[0];

                execProgram(base);
                if (Data.getNew()) {
                    int score = Data.getLocalBucketSize();
                    queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                    Data.resetTuples();
                    paths++;
                }

                base[j] = currentVal1;
                base[j + 1] = currentVal2;
                base[j + 2] = currentVal3;
                base[j + 3] = currentVal4;

            }
        }

    }

    public static void havoc(byte[] base) throws IOException {
        Random rand = new Random();
        int byteNum, tmp;
        byte[] temp, backup = new byte[base.length];
        System.arraycopy(base, 0, backup, 0, base.length);
        int runs = rand.nextInt(984) + 16;
        int tweaks = rand.nextInt(32) + 1;

        for (int i = 0; i < runs; i++) {
            for (int j = 0; j < tweaks; j++) {
                int option = rand.nextInt(15);
                switch (option) {
                case 0:
                    // Flip a random bit somewhere.
                    byteNum = rand.nextInt(base.length);
                    base[byteNum] = (byte) (base[byteNum] ^ (1 << i));
                    break;
                case 1:
                    // Set random byte to an interesting value.
                    byteNum = rand.nextInt(base.length);
                    base[byteNum] = (byte) interesting_8[rand.nextInt(interesting_8.length)];
                    break;
                case 2:
                    // Set two bytes to interesting value.
                    if (base.length < 2) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length - 1);
                    temp = ByteBuffer.allocate(4).putInt(interesting_16[rand.nextInt(interesting_16.length)]).array();
                    if (rand.nextInt(2) == 0) {
                        base[byteNum] = temp[0];
                        base[byteNum + 1] = temp[1];
                    } else {
                        base[byteNum] = temp[1];
                        base[byteNum + 1] = temp[0];
                    }
                    break;
                case 3:
                    // Set four bytes to interesting value.
                    if (base.length < 4) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length - 3);
                    temp = ByteBuffer.allocate(4).putInt(interesting_16[rand.nextInt(interesting_32.length)]).array();
                    if (rand.nextInt(2) == 0) {
                        base[byteNum] = temp[0];
                        base[byteNum + 1] = temp[1];
                        base[byteNum + 2] = temp[2];
                        base[byteNum + 3] = temp[3];
                    } else {
                        base[byteNum] = temp[3];
                        base[byteNum + 1] = temp[2];
                        base[byteNum + 2] = temp[1];
                        base[byteNum + 3] = temp[0];
                    }
                    break;
                case 4:
                    // Randomly subtract from a byte.
                    byteNum = rand.nextInt(base.length);
                    base[byteNum] = (byte) (base[byteNum] - (rand.nextInt(ARITH_MAX) + 1));
                    break;
                case 5:
                    // Randomly subtract from two bytes.
                    if (base.length < 2) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length - 1);
                    tmp = rand.nextInt(ARITH_MAX) + 1;
                    base[byteNum] = (byte) (base[byteNum] - tmp);
                    base[byteNum + 1] = (byte) (base[byteNum + 1] - tmp);
                    break;
                case 6:
                    // Randomly subtract from four bytes.
                    if (base.length < 4) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length - 3);
                    tmp = rand.nextInt(ARITH_MAX) + 1;
                    base[byteNum] = (byte) (base[byteNum] - tmp);
                    base[byteNum + 1] = (byte) (base[byteNum + 1] - tmp);
                    base[byteNum + 2] = (byte) (base[byteNum + 2] - tmp);
                    base[byteNum + 2] = (byte) (base[byteNum + 3] - tmp);
                    break;
                case 7:
                    // Randomly add to byte.
                    byteNum = rand.nextInt(base.length);
                    base[byteNum] = (byte) (base[byteNum] - (rand.nextInt(ARITH_MAX) + 1));
                    break;
                case 8:
                    // Randomly add to two bytes.
                    if (base.length < 2) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length - 1);
                    tmp = rand.nextInt(ARITH_MAX) + 1;
                    base[byteNum] = (byte) (base[byteNum] + tmp);
                    base[byteNum + 1] = (byte) (base[byteNum + 1] + tmp);
                    break;
                case 9:
                    // Randomly add to four bytes.
                    if (base.length < 4) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length - 3);
                    tmp = rand.nextInt(ARITH_MAX) + 1;
                    base[byteNum] = (byte) (base[byteNum] - tmp);
                    base[byteNum + 1] = (byte) (base[byteNum + 1] + tmp);
                    base[byteNum + 2] = (byte) (base[byteNum + 2] + tmp);
                    base[byteNum + 2] = (byte) (base[byteNum + 3] + tmp);
                    break;
                case 10:
                    // Set a random byte to a random value.
                    byteNum = rand.nextInt(base.length);
                    tmp = rand.nextInt(255) + 1;
                    base[byteNum] = (byte) (base[byteNum] ^ tmp);
                    break;
                case 11:
                case 12:
                    // Delete bytes.
                    if (base.length < 2) {
                        continue;
                    }
                    byteNum = rand.nextInt(base.length);
                    base = removeByte(base, byteNum);
                    break;
                case 13:
                		base = CloningOrInserting(base);
                    // Clone or insert bytes.
                		/*
                    if (rand.nextInt(4) == 0) {
                        // Insert constant bytes.
                    	   System.out.println("Inserting");
                        byteNum = rand.nextInt(base.length);
                        base = addByte(base, (byte) (rand.nextInt(255) + 1), rand.nextInt(base.length + 1));
                        
                    } else {
                        // Clone Bytes.
                    	   System.out.println("Cloning");
                        byteNum = rand.nextInt(base.length);
                        base = addByte(base, base[byteNum], rand.nextInt(base.length + 1));

                    }
                    */
                    break;
                case 14:
                    // Overwrite bytes
                    // Random chunk or fixed bytes.
                    if (rand.nextInt(4) == 0) {
                        // Fixed bytes.
                        byteNum = rand.nextInt(base.length);
                        base = replaceByte(base, (byte) (rand.nextInt(255) + 1), byteNum);
                    } else {
                        // Random chunk.
                        byteNum = rand.nextInt(base.length);
                        tmp = rand.nextInt(base.length);
                        if (byteNum == tmp) {
                            continue;
                        }
                        base = replaceByte(base, base[tmp], byteNum);

                    }
                    break;
                default:
                    break;
                }
            }
            // System.out.println(base);
            execProgram(base);
            if (Data.getNew()) {
                int score = Data.getLocalBucketSize();
                queue.add(new Input(Arrays.copyOf(base, base.length), false, score, false));
                Data.resetTuples();
                paths++;
            }
            base = new byte[backup.length];
            System.arraycopy(backup, 0, base, 0, backup.length);
        }
    }
    
	private static byte[] CloningOrInserting(byte[] base) {
		// if things get too long lets stop this bus! This is super bad and needs to be configured
		if (base.length > 20) return base;
		//first figure out which one you want to do
		Random rand = new Random();
		boolean clone = (rand.nextInt(4)>0);
		int blockSize = rand.nextInt(base.length); // how much you want to clone or insert
		int blockStart = 0; // where do you start from
		if (clone) {
			blockStart = rand.nextInt(base.length-blockSize+1);
		}
		int newPos = rand.nextInt(base.length); // where are we putting the new stuff
		byte[] newBase = new byte[base.length + blockSize];
		System.arraycopy(base, 0, newBase, 0, newPos);
		if (clone) {
			System.arraycopy(base, blockStart, newBase, newPos, blockSize);
		} else {
			byte data = (byte)(rand.nextInt(256) -128);
			Arrays.fill(newBase,newPos,newPos+blockSize,data);
		}
		System.arraycopy(base, newPos, newBase, newPos+blockSize, base.length-newPos);
		//System.out.println("Cloning? "  + clone + (new String(newBase)));
		return newBase;
	}

}

class SystemExitControl {
    @SuppressWarnings("serial")
    public static class ExitTrappedException extends SecurityException {
    }

    public static void forbidSystemExitCall() {
        final SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                if (permission.getName().contains("exitVM.100")) {

                } else if (permission.getName().contains("exitVM")) {
                    throw new ExitTrappedException();
                }
            }
        };
        System.setSecurityManager(securityManager);
    }

    public static void enableSystemExitCall() {
        System.setSecurityManager(null);
    }
}

class Input {
    private byte[] data;
    private boolean evaluated;
    private int score;
    private boolean coastalEvaluated;

    public Input(byte[] data, boolean evaluated, int score, boolean coastalEvaluated) {
        this.data = data;
        this.evaluated = evaluated;
        this.score = score;
        this.coastalEvaluated = coastalEvaluated;
    }

    public byte[] getData() {
        return this.data;
    }

    public boolean getEvaluated() {
        return this.evaluated;
    }

    public int getScore() {
        return this.score;
    }

    public boolean getCoastalEvaluated() {
        return this.coastalEvaluated;
    }

    public void setCoastalEvaluated(boolean evaluated) {
        this.coastalEvaluated = evaluated;
    }

    public String toString() {
        String out = "[";
        for (int i = 0; i < data.length; i++) {
            out += data[i] + ",";
        }
        out += "] = " + score;
        return out;
    }
}

class InputComparator implements Comparator<Input> {
    @Override
    public int compare(Input x, Input y) {

        if (x.getScore() > y.getScore()) {
            return -1;
        }
        if (x.getScore() < y.getScore()) {
            return 1;
        }
        return 0;
    }
}

class Tuple {
    private String src;
    private String dest;

    public Tuple(String src, String dest) {
        this.src = src;
        this.dest = dest;
    }

    public String getSrc() {
        return this.src;
    }

    public String getDest() {
        return this.dest;
    }

    @Override
    public int hashCode() {
        return src.hashCode() + dest.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        Tuple tuple = (Tuple) obj;
        if (this.src.equals(tuple.getSrc()) && this.dest.equals(tuple.getDest())) {
            return true;
        } else {
            return false;
        }

    }
}

class ByteSet extends HashSet<byte[]> {
    public boolean containsByteArray(byte[] input) {
        for (byte[] base : this) {
            if (Arrays.equals(base, input)) {
                return true;
            }
        }
        return false;
    }
}
