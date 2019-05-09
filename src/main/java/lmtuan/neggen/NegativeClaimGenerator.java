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
    	Token t1Token = tokens.get(t1);
    	Token prevToken = t1Token.previous();
    	Token nextToken = t1Token.next();
		String prevTokenStr = prevToken.originalText().toLowerCase();
		String nextTokenStr = nextToken.originalText().toLowerCase();
		String nextTokenTag = (t1 + 1 < posTags.size())? posTags.get(t1+1) : "";
		
    	String t1Lemma = t1Token.lemma().toLowerCase();
    	if (nextTokenStr.equalsIgnoreCase("n't")) {
			endInNT = true;
		}
    	
    	/*
    	 *  3) If T1 is followed or preceded by one of several negation strings 
    	 *  (e.g., "no", "not"), remove this negation and finish.
    	 */
    	Token negationToken = null;
		if (prevTokenStr.equals("not") || prevTokenStr.equals("no")) {
			negationToken = prevToken;
		} else if (nextTokenStr.equals("not") || nextTokenStr.equals("no")) {
			negationToken = nextToken;
    	}
    	if (negationToken != null) {
    		return claim.substring(0, negationToken.beginPosition() - negationToken.before().length()) 
    				+ claim.substring(negationToken.endPosition());
    	}

    	/*
    	 * 4) If T1 ends in "n't", remove this suffix and finish 
    	 * (so, e.g., "can't" would be transformed to "can").
    	 */
    	if (endInNT) {
    		// can't is tokenized to "ca" and "n't", so we have to add an n
    		String fix = tokens.get(t1).originalText().equalsIgnoreCase("ca")? "n" : "";
    		Token ntToken = tokens.get(t1 + 1);
    		return claim.substring(0, ntToken.beginPosition() - ntToken.before().length()) 
    				+ fix 
    				+ claim.substring(ntToken.endPosition());
    	}
    	
    	/*
    	 * 5) If T1 is a modal verb, is a form of the verb "to be" (e.g. "is" or "are"),
		 * or is a form of the verb "to do" or "to have" followed by another verb (V.. or IN),
    	 * or is followed by a gerund, then insert the word "not" after T1 and finish
    	 *
    	 * Note that this is slightly different than step 5 in the paper, 
    	 * as if T1 is followed by an adjective with negative prefix
    	 * (e.g. unworkable), we do not remove that prefix but instead just
    	 * insert not. Thus "unworkable" becomes "not unworkable" instead of 
    	 * "workable".
    	 */
    	if (t1Tag.equals("MD") 
    			|| t1Lemma.equals("be")
    			|| ((t1Lemma.equals("do") || t1Lemma.equals("have"))
    					&& (nextTokenTag.startsWith("V") || nextTokenTag.equals("IN")))
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
