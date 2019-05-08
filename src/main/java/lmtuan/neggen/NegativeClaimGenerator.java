package lmtuan.neggen;

import java.util.List;
import java.util.Scanner;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;

public class NegativeClaimGenerator {
    public static void main(String[] args) { 
//    	String claim = "The selection process can't be based on some arbitrary or irrelevant criterion.";
    	String claim = "He has failed us.";
    	Scanner sc = new Scanner(System.in);
    	while(true) {
    		String sen = sc.nextLine();
    		System.out.println(getNegativeClaim(sen));
    	}
    }
    
    /**
     * Generate the negative claim for a claim, using the algorithm described
     * in the paper https://aclweb.org/anthology/W15-0511
     * 
     * @param claim original claim
     * @return the negative claim, or null if algorithm fails
     */
    public static String getNegativeClaim(String claim) {
    	Sentence sen = new Sentence(claim);
    	List<Token> tokens = sen.tokens();
    	List<String> posTags = sen.posTags();
    	for (String s : posTags) {
    		System.out.println(s);
    	}
    	
    	int t1 = -1; // position of token T1
    	String t1Tag = null;
    	boolean endInNT = false;
    	
    	/*
    	 * 2) Find T1, which is the first verb that is modal, present tense, or end with "n't"
    	 * If can't find this verb, algorithm fails
    	 */
    	findT1:
    	for (int i = 0; i < posTags.size(); i++) {
    		String tag = posTags.get(i);
    		switch(tag) {
    		case "MD": // modal verb
    		case "VB": // verb, base form
    		case "VBZ": // verb, 3rd person singular present
    		case "VBP": // verb, non-3rd person singular present
    		case "VBD": // verb, past tense
    			t1 = i;
    			t1Tag = tag;
    			break findT1;
    		}
    		if (tokens.get(i).originalText().equalsIgnoreCase("n't")) {
    			t1 = i - 1;
    			t1Tag = posTags.get(i-1);
    			break findT1;
    		}
    	}
    	if (t1 == -1)
    		return null;
    	if (t1 < tokens.size() -1 && tokens.get(t1+1).originalText().equalsIgnoreCase("n't")) {
			endInNT = true;
		}
    	
    	/*
    	 *  3) If T1 is followed or preceded by one of several negation strings 
    	 *  (e.g., “no”, “not”’), remove this negation and finish.
    	 */
    	int negationToken = -1;
    	if (t1 >= 1) {
    		String prevToken = tokens.get(t1 - 1).originalText();
    		if (prevToken.equalsIgnoreCase("not") || prevToken.equalsIgnoreCase("no"))
    			negationToken = t1 - 1;
    	}
    	if (negationToken == -1 && t1 + 1 < tokens.size()) {
    		String nextToken = tokens.get(t1 + 1).originalText();
    		if (nextToken.equals("not") || nextToken.equals("no"))
    			negationToken = t1 + 1;
    	}
    	if (negationToken != -1) {
    		Token t = tokens.get(negationToken);
    		return claim.substring(0, t.beginPosition() - t.before().length()) + claim.substring(t.endPosition());
    	}

    	/*
    	 * 4) If T1 ends in "n't", remove this suffix and finish 
    	 * (so, e.g., "can't" would be transformed to "can").
    	 */
    	if (endInNT) {
    		// can't is tokenized to "ca" and "n't", so we have to add an n
    		String fix = tokens.get(t1).originalText().equalsIgnoreCase("ca")? "n" : "";
    		Token ntToken = tokens.get(t1 + 1);
    		return claim.substring(0, ntToken.beginPosition() - ntToken.before().length()) + fix + claim.substring(ntToken.endPosition());
    	}
    	
    	/*
    	 * 5) If T1 is a modal verb, is a form of the verb "to be" (e.g. "is" or "are"),
    	 * or is followed by a gerund, then insert the word "not" after T1 and finish
    	 *
    	 * Note that this is slightly different than step 5 in the paper, 
    	 * as if T1 is followed by an adjective with negative prefix
    	 * (e.g. unworkable), we do not remove that prefix but instead just
    	 * insert not. Thus "unworkable" becomes "not unworkable" instead of 
    	 * "workable".
    	 */
    	if (t1Tag.equals("MD") 
    			|| tokens.get(t1).lemma().equalsIgnoreCase("be")
    			|| (t1 < posTags.size() - 1 
    					&& posTags.get(t1 + 1).equals("VBG"))) {
    		StringBuilder sb = new StringBuilder(claim);
    		sb.insert(tokens.get(t1).endPosition(), " not");
    		return sb.toString();
    	}
    	
    	/*
    	 * Otherwise, insert the words "does not" or "do not" (according to 
    	 * plurality) before T1, and replace T1 with its lemmatized form.
    	 */
    	StringBuilder sb = new StringBuilder(claim);
    	Token t = tokens.get(t1);
    	sb.replace(t.beginPosition(), t.endPosition(), t.lemma());
    	String insertStr;
    	switch(t1Tag) {
		case "VB": // verb, base form
		case "VBP": // verb, non-3rd person singular present
			insertStr = "do not ";
			break;
		case "VBZ": // verb, 3rd person singular present
			insertStr = "does not ";
			break;
		case "VBD": // verb, past tense
			insertStr = "did not ";
			break;
		default:
			return "";
		}
    	sb.insert(t.beginPosition(), insertStr);
    	return sb.toString();
    }
}
