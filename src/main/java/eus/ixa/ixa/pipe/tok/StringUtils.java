package eus.ixa.ixa.pipe.tok;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Several string utils. The functions of this class
 * are to be used statically.
 *
 * @author ragerri
 * @version 2015-04-14
 */
public class StringUtils {

    /**
     * Pattern to remove double bars from disjunct regex.
     */
    public static Pattern doubleBar = Pattern.compile("\\|\\|");

    /**
     * Get an array of Strings and convert it into a string
     * separated by spaces.
     *
     * @param tokens the array
     * @return the string
     */
    public static String getStringFromTokens(final String[] tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final String tok : tokens) {
            sb.append(tok).append(" ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Get a List of Strings and convert it into a string
     * separated by spaces.
     *
     * @param tokens the list of tokens
     * @return the string
     */
    public static String getStringFromTokens(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final Token tok : tokens) {
            sb.append(tok.getTokenValue()).append(" ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String createDisjunctRegexFromList(final List<String> words) {
        final StringBuilder sb = new StringBuilder();
        for (final String word : words) {
            sb.append(word).append("|");
        }
        String regExp = sb.toString();
        regExp = doubleBar.matcher(regExp).replaceAll("\\|");
        regExp = regExp.replaceAll("\\.", "\\\\.");
        final String result = regExp.substring(1, regExp.length() - 1);
        return result;
    }
}
