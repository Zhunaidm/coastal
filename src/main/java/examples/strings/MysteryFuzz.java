package examples.strings;

import java.util.Scanner;
import java.io.File;

public class MysteryFuzz {

	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner(new File(args[0]));
		String word = sc.nextLine();
		sc.close();
		preserveSomeHtmlTagsAndRemoveWhitespaces(word);
	}

	static int indexOf(String str, String c, int start) {
		if (start < 0)
			start = 0;
		if (c.length() > str.length())
			return -1;
		for (int i = start; i < str.length(); i++) {
			boolean flag = true;
			for (int j = 0; j < c.length() && (i+j) < str.length(); j++) {
			   if (str.charAt(i+j) != c.charAt(j)) {
				  flag = false;
				  break;
			   }
			}
			if (flag)
				return i;
		}
		return -1;
	}
	
	public static String preserveSomeHtmlTagsAndRemoveWhitespaces(String body) {
		if (body == null) {
			return body;
		}
		int len = body.length();
		int i = 0;
		int old = i - 1;
		while (i < len) {
			// assert i >= old: "Infinite loop";
			if (i <= old) {
				// for (int idx = 0; idx < body.length(); idx++) {
				// Debug.printPC("Current PC: ");
				// }
				// throw new RuntimeException("Infinite loop");
				//System.out.println("BUG");
				//return "EXCEPTION: Infinite loop";
				System.exit(0);
			}
			old = i;
			if (body.charAt(i) == '<') {
				if (i + 14 < len && (body.charAt(i + 8) == '\"') && (body.charAt(i + 7) == '=')
						&& (body.charAt(i + 6) == 'f' || body.charAt(i + 6) == 'F')
						&& (body.charAt(i + 5) == 'e' || body.charAt(i + 5) == 'E')
						&& (body.charAt(i + 4) == 'r' || body.charAt(i + 4) == 'R')
						&& (body.charAt(i + 3) == 'h' || body.charAt(i + 3) == 'H') && (body.charAt(i + 2) == ' ')
						&& (body.charAt(i + 1) == 'a' || body.charAt(i + 1) == 'A')) {
					int idx = i + 9;
					int idx2 = indexOf(body,"'", idx);//body.indexOf("'", idx);
					int idxStart = indexOf(body, ">", idx2); //body.indexOf(">", idx2);
					int idxEnd = indexOf(body,"</a>", idxStart); //body.indexOf("</a>", idxStart);
					if (idxEnd == -1) {
						idxEnd = indexOf(body, "</A>", idxStart); //body.indexOf("</A>", idxStart);
					}
					
					i = idxEnd + 4;
					continue;
				}
			}
			i++;
		}
		return body;
	}

}
