package converter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {

        File testFile = new File("/Users/elliot/Documents/Development/hyperSkill/JSON - XML " +
                "converter/task/src/converter/test.txt");
        StringBuilder dataString = new StringBuilder();

        try (Scanner fileScanner = new Scanner(testFile)) {
            while (fileScanner.hasNext()) {
                dataString.append(fileScanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        new XmlParser(dataString.toString());
    }
}

class XmlParser {
    String xmlData;

    public XmlParser(String xmlData) {
        this.xmlData = cleanXML(xmlData);
        parse(this.xmlData, null);
    }

    private String cleanXML(String xmlData) {
        String cleanXML;
        cleanXML = xmlData.replaceAll("(?<=>)\\s+?(?=<)", ""); //Removes space(s) between tags
        cleanXML = cleanXML.replaceAll("\\s+?(?=/>)", ""); //Removes the space(s) before />
        return cleanXML;
    }

    public void parse(String xmlData, Deque<String> parents) {

        //matches all the characters between <> or <  />
        Pattern wholeTagPattern = Pattern.compile("<(\\w+).*?(/>|>.*?</\\1>)");
        Matcher wholeTagMatcher = wholeTagPattern.matcher(xmlData);

        if (wholeTagMatcher.find()) {
            int endPosition;
            Deque<String> parentsStack;

            do {
                parentsStack = parents == null ? new ArrayDeque<>() : parents;
                String wholeTag = wholeTagMatcher.group();
                //the end position of the match. To see if it checked the whole string
                endPosition = wholeTagMatcher.end();

                //matches the opening tag (and the key in group 1)
                Pattern tagPattern = Pattern.compile("(?<=<)(\\w+).*?/?(?=>)");
                Matcher tagMatcher = tagPattern.matcher(wholeTag);

                String key = null;
                String tag = null;
                if (tagMatcher.find()) {
                     key = tagMatcher.group(1);
                     tag = tagMatcher.group();
                }
                
                XmlTag xmlTag = new XmlTag(tag, key, wholeTag);
                xmlTag.setParents(parentsStack);
                System.out.println(xmlTag.toString());

                if (xmlTag.HasChild()) { //if there are subchilden inside this tag, recursively parse them.
                    //Matches the text inside <tag>text</tag>
                    Pattern textInsideMatchedTagPattern = Pattern.compile("(?<=<" + tag + ">).*?(?=</" + key + ")");
                    Matcher textInsideMatchedTagMatcher = textInsideMatchedTagPattern.matcher(wholeTag);

                    if (textInsideMatchedTagMatcher.find()) {
                        parentsStack.offer(key);
                        parse(textInsideMatchedTagMatcher.group(), parentsStack);
                        parentsStack.pollLast();
                    }
                }
                wholeTagMatcher.find();
            } while (endPosition < xmlData.length());
        }
    }
}


class XmlTag {

    private final String key;
    private String value;
    private boolean hasChild;
    private final boolean hasAttributes;
    private final String XmlString;
    private final String tag; //the complete tag
    private LinkedHashMap<String, String> attributes;
    private Deque<String> parents;

    public XmlTag(String tag, String key, String XmlString) {
        this.tag = tag;
        this.hasAttributes = hasAttributes();
        this.key = key;
        this.XmlString = XmlString;
        parseTag();
    }

    private boolean hasAttributes() {
        //matches the pattern key attribute1 = "value1" .. attributeN = "valueN" or "value1" .. attributeN = "valueN" /
        Pattern pattern = Pattern.compile("\\w*?=\\s*?\".*?\"\\s*?/?");
        Matcher matcher = pattern.matcher(this.tag);

        return matcher.find();
    }

    private void parseTag() {

        this.hasChild = hasChild();

        if (this.hasChild) {
            this.value = null;
        } else {
            //Now see if tag is self closing
            if (this.XmlString.matches("<.*?/>")) {
                this.value = null;
            } else {
                //matches value within a tag
                Pattern tagPattern = Pattern.compile("(?<=<" + this.tag + ">).*?(?=</" + this.key + ">)");
                Matcher tagMatcher = tagPattern.matcher(this.XmlString);

                if (tagMatcher.find()) {
                    this.value = tagMatcher.group();
                } else {
                    this.value = "";
                }
            }
        }

        if(this.hasAttributes()) {

            Pattern attributePattern = Pattern.compile("\\w*\\s*?=\\s*?\"\\w*?\""); //matches attribute = "value"
            Matcher attributeMatcher = attributePattern.matcher(this.tag);

            this.attributes = new LinkedHashMap<>();

            //loop through to find all attributes and add them to the LinkedHashMap
            while (attributeMatcher.find()) {

                //matches a key followed by =
                Pattern attributeKeyPattern = Pattern.compile("\\w*(?=\\s*?=\\s*?)");
                Matcher attributeKeyMatcher = attributeKeyPattern.matcher(attributeMatcher.group());

                attributeKeyMatcher.find();
                String attributeKey = attributeKeyMatcher.group();

                //matches a word enclosed in ""
                Pattern attributeValuePattern = Pattern.compile("(?<=\")\\w*(?=\")");
                Matcher attributeValueMatcher = attributeValuePattern.matcher(attributeMatcher.group());

                attributeValueMatcher.find();
                String attributeValue = attributeValueMatcher.group();

                attributes.put(attributeKey, attributeValue);
            }
        }
    }

    private boolean hasChild() {
        //matches the text in <tag>text</tag> or <tag/> or <tag></tag> whichever comes first.
        Pattern tagPattern = Pattern.compile("((?<=<" + this.tag + ">).+?(?=</" + this.key + ">)|(?<=<)" + this.tag +
                "(?=\\s*?/\\s*?>)|(?<=<)" + this.tag + "(?=></" + this.key + ">))");
        Matcher tagMatcher = tagPattern.matcher(this.XmlString);

        if (tagMatcher.find()) {
            Pattern pattern = Pattern.compile("<.*?>"); //matches a tag
            Matcher matcher = pattern.matcher(tagMatcher.group());
            return matcher.find(); //if it finds a tag inside the provided tag
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder elementString = new StringBuilder();

        elementString.append("Element:\n");
        elementString.append("path = ");

        for (String parent : this.parents) {
            elementString.append(parent);
            elementString.append(", ");
        }
        elementString.append(this.key).append("\n");

        if (!this.hasChild) {
            elementString.append("value = ");
            if (this.value != null) {
                elementString.append("\"")
                        .append(this.value)
                        .append("\"\n");
            } else {
                elementString.append("null\n");
            }
        }

        if (this.hasAttributes) {
            elementString.append("attributes:\n");

            for (String key : attributes.keySet()) {
                elementString.append(key)
                        .append(" = \"")
                        .append(this.attributes.get(key))
                        .append("\"\n");
            }
        }
        return elementString.toString();
    }

    public boolean HasChild() {
        return hasChild;
    }

    public void setParents(Deque<String> parents) {
        this.parents = parents;
    }
}