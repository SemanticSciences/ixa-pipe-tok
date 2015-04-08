/*
 * Copyright 2015 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eus.ixa.ixa.pipe.tok;

import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.ALPHA_APOS_ALPHA;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.ALPHA_APOS_NOALPHA;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.ASCII_HEX;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.DASH;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.DIGIT_COMMA_NODIGIT;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.DOTMULTI_DOT;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.DOTMULTI_DOT_ANY;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.LINK;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.MULTI_DOTS;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.MULTI_SPACE;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.NOALPHA_APOS_NOALPHA;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.NOALPHA_DIGIT_APOS_ALPHA;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.NODIGIT_COMMA_DIGIT;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.NODIGIT_COMMA_NODIGIT;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.QEXC;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.SPECIALS;
import static eus.ixa.ixa.pipe.tok.NonPrefixBreaker.YEAR_APOS;

import java.io.InputStream;
import java.util.regex.Matcher;


public class RuleBasedTokenizer implements Tokenizer {

  NonPrefixBreaker nonBreaker;

  public RuleBasedTokenizer(InputStream nonBreakingFile, String lang) {
    nonBreaker = new NonPrefixBreaker(nonBreakingFile);
  }

  public String[] tokenize(String line, String lang) {
    String[] tokens = tokDetector(line, lang);
    return tokens;
  }

  private String[] tokDetector(String line, String lang) {

    // remove extra spaces and ASCII stuff
    line = " " + line + " ";
    line = MULTI_SPACE.matcher(line).replaceAll(" ");
    line = ASCII_HEX.matcher(line).replaceAll("");
    line = Normalizer.convertNonCanonicalStrings(line);
    line = Normalizer.normalizeQuotes(line, lang);
    // separate question and exclamation marks
    line = QEXC.matcher(line).replaceAll(" $1 ");
    // separate dash if before an upper case character 
    //line = DASH_LU.matcher(line).replaceAll("$1 $2");
    // separate dash if before or after space
    line = DASH.matcher(line).replaceAll(" $1 ");
    // separate out other special characters [^\p{Alnum}s.'`,-?!]
    line = SPECIALS.matcher(line).replaceAll(" $1 ");

    // do not separate multidots
    line = generateMultidots(line);

    // separate "," except if within numbers (1,200)
    line = NODIGIT_COMMA_NODIGIT.matcher(line).replaceAll("$1 , $2");
    // separate pre and post digit
    line = DIGIT_COMMA_NODIGIT.matcher(line).replaceAll("$1 , $2");
    line = NODIGIT_COMMA_DIGIT.matcher(line).replaceAll("$1 , $2");
    

    // contractions it's, l'agila
    line = treatContractions(line, lang);
    // non prefix breaker
    line = nonBreaker.TokenizerNonBreaker(line);

    // clean up extra spaces
    line = line.replaceAll("\\s+", " ");
    line = line.trim();

    // restore multidots
    line = restoreMultidots(line);

    // urls 
    line = detokenizeURLs(line);

    // create final array of tokens
    //System.out.println(line);
    String[] tokens = line.split(" ");

    // ensure final line break
    // if (!line.endsWith("\n")) { line = line + "\n"; }
    return tokens;
  }

  /**
   * This function normalizes multi-period expressions (...) to make
   * tokenization easier.
   * 
   * @param line
   * @return string
   */
  private String generateMultidots(String line) {

    line = MULTI_DOTS.matcher(line).replaceAll(" DOTMULTI$1 ");
    Matcher dotMultiDot = DOTMULTI_DOT.matcher(line);

    while (dotMultiDot.find()) {
      line = DOTMULTI_DOT_ANY.matcher(line).replaceAll("DOTDOTMULTI $1");
      line = dotMultiDot.replaceAll("DOTDOTMULTI");
      // reset the matcher otherwise the while will stop after one run
      dotMultiDot.reset(line);
    }
    return line;
  }

  /**
   * Restores the normalized multidots to its original state and it tokenizes
   * them.
   * 
   * @param line
   * @return the tokenized multidots
   */
  private String restoreMultidots(String line) {

    while (line.contains("DOTDOTMULTI")) {
      line = line.replaceAll("DOTDOTMULTI", "DOTMULTI.");
    }
    line = line.replaceAll("DOTMULTI", ".");
    return line;
  }

  private String treatContractions(String line, String lang) {

    if (lang.equalsIgnoreCase("en")) {
      line = NOALPHA_APOS_NOALPHA.matcher(line).replaceAll("$1 ' $2");
      line = NOALPHA_DIGIT_APOS_ALPHA.matcher(line).replaceAll("$1 ' $2");
      line = ALPHA_APOS_NOALPHA.matcher(line).replaceAll("$1 ' $2");
      line = ALPHA_APOS_ALPHA.matcher(line).replaceAll("$1 '$2");
      line = YEAR_APOS.matcher(line).replaceAll("$1 ' $2");
    } else if (lang.equalsIgnoreCase("fr") || lang.equalsIgnoreCase("gl") || lang.equalsIgnoreCase("it")) {
      line = NOALPHA_APOS_NOALPHA.matcher(line).replaceAll("$1 ' $2");
      line = NOALPHA_DIGIT_APOS_ALPHA.matcher(line).replaceAll("$1 ' $2");
      line = ALPHA_APOS_NOALPHA.matcher(line).replaceAll("$1 ' $2");
      line = ALPHA_APOS_ALPHA.matcher(line).replaceAll("$1' $2");
    }
    return line;
  }

  private String detokenizeURLs(String line) {
    Matcher link = LINK.matcher(line);
    StringBuffer sb = new StringBuffer();
    while (link.find()) {
      link.appendReplacement(sb, link.group().replaceAll("\\s", ""));
    }
    link.appendTail(sb);
    line = sb.toString();
    return line;
  }

}
