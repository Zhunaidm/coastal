package examples.strings;

import java.io.*;
import java.util.*;

public class DB {

    public static int analyse(String word) {
        if (word.charAt(0) == 'd') {
            if (word.charAt(1) == 'e') {
                if (word.charAt(2) == 'a') {
                    if (word.charAt(3) == 'd') {
                        if (word.charAt(4) == 'b') {
                            if (word.charAt(5) == 'e') {
                                if (word.charAt(6) == 'e') {
                                    if (word.charAt(7) == 'f') {
                                        return 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (word.charAt(0) == 'f') {
            if (word.charAt(1) == 'u') {
                if (word.charAt(2) == 'n') {
                    if (word.charAt(3) == 'd') {
                        if (word.charAt(4) == 'a') {
                            if (word.charAt(5) == 'z') {
                                if (word.charAt(6) == 'e') {
                                    if (word.charAt(7) == 'd') {
                                        return 2;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(new File(args[0]));
        String word = sc.next();
        sc.close();
        System.out.println("Initial Word: " + word);
        int r = analyse(word);
    }
}