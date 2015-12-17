package smtchahal.regextester;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFind {
    private final Matcher matcher;
    private static final String LOG_TAG = "RegexFind";

    public RegexFind(String subject, String patternText, int flags) {
        Pattern pattern = Pattern.compile(patternText, flags);
        matcher = pattern.matcher(subject);
    }

    /**
     * Returns the number of matches found.
     * @return The number of matches found.
     */
    public int matchesCount() {
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        matcher.reset();

        return count;
    }

    /**
     * Checks whether any matches were found.
     * @return {@code true} if number of matches found is &gt; 0, {@code false} otherwise.
     */
    public boolean matchFound() {
        return this.matchesCount() > 0;
    }

    /**
     * Returns a two-dimensional integer array containing the indices where the matches are found.
     * @return a two-dimensional integer array of size nx2, where n is
     * the number of matches if {@link #matchFound()}; throws RuntimeException otherwise.
     * @throws RuntimeException if {@link #matchFound()} returns false.
     */
    public int[][] getIndices() {

        if (!matchFound()) {
            throw new RuntimeException("No match found");
        }
        int count = matchesCount();

        int matchesIndices[][] = new int[count][2];

        for (int i = 0; matcher.find(); i++) {
            matchesIndices[i][0] = matcher.start();
            matchesIndices[i][1] = matcher.end();
        }

        return matchesIndices;
    }
}
