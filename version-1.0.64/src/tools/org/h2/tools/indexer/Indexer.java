/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.tools.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.h2.util.IOUtils;
import org.h2.util.StringUtils;

/**
 * The indexer creates the fulltext index of the HTML documentation.
 * It is used for the built-in HTML javascript search.
 */
public class Indexer {

    ArrayList pages = new ArrayList();
    HashMap words = new HashMap();
    HashSet noIndex = new HashSet();
    ArrayList wordList;
    int totalAllWeights;
    PrintWriter output;

    Page page;
    boolean title;
    boolean heading;

    private static final int MIN_WORD_SIZE = 3;
    private static final int MAX_RELATIONS = 20;

    public static void main(String[] args) throws Exception {
        new Indexer().run(args);
    }

    void run(String[] args) throws Exception {
        System.out.println(getClass().getName());
        String dir = "docs";
        String destDir = "docs/html";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-dir")) {
                dir = args[++i];
            } else if (args[i].equals("-destDir")) {
                destDir = args[++i];
            }
        }
        File file = new File(dir);
        System.out.println("indexing " + file.getCanonicalPath());
        setNoIndex(new String[] { "index.html", "html/header.html", "html/search.html", "html/frame.html",
                "javadoc/index.html", "javadoc/classes.html", "javadoc/allclasses-frame.html",
                "javadoc/allclasses-noframe.html", "javadoc/constant-values.html", "javadoc/overview-frame.html",
                "javadoc/overview-summary.html", "javadoc/serialized-form.html" });
        output = new PrintWriter(new FileWriter(destDir + "/index.js"));
        readPages("", file, 0);
        output.println("var pages=new Array();");
        output.println("var ref=new Array();");
        output.println("function Page(title, file) { this.title=title; this.file=file; }");
        output.println("function load() {");
        sortWords();
        removeOverflowRelations();
        sortPages();
        listPages();
        listWords();
        output.println("}");
        output.close();
    }

    private void setNoIndex(String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            noIndex.add(strings[i]);
        }
    }

    void sortWords() {
        wordList = new ArrayList(words.values());
        // TODO support ignored keywords (to shrink the index)
        // String ignored = "";
        // for(int i=0; i<wordList.size(); i++) {
        // Word word = (Word) wordList.get(i);
        // if(word.pages.size() >= pages.size()/4) {
        // wordList.remove(i);
        // if(ignored.length()==0) {
        // ignored += ",";
        // }
        // ignored += word.name;
        // i--;
        // }
        // }
        // output.println("var ignored = '" + convertUTF(ignored) + "'");
        // TODO support A, B, C,... class links in the index file and use them
        // for combined AND searches
        Collections.sort(wordList, new Comparator() {
            public int compare(Object o0, Object o1) {
                Word w0 = (Word) o0;
                Word w1 = (Word) o1;
                return w0.name.compareToIgnoreCase(w1.name);
            }
        });
    }

    void removeOverflowRelations() {
        for (int i = 0; i < wordList.size(); i++) {
            Word word = (Word) wordList.get(i);
            ArrayList weights = word.getSortedWeights();
            int max = MAX_RELATIONS;
            if (weights.size() > max) {
                while (max < weights.size()) {
                    Weight weight = (Weight) weights.get(max);
                    if (weight.value < Weight.HEADER) {
                        break;
                    }
                    max++;
                }
            }
            while (max < weights.size()) {
                Weight weight = (Weight) weights.get(max);
                weights.remove(max);
                weight.page.relations--;
            }
        }
    }

    void sortPages() {
        Collections.sort(pages, new Comparator() {
            public int compare(Object o0, Object o1) {
                Page p0 = (Page) o0;
                Page p1 = (Page) o1;
                return p0.relations == p1.relations ? 0 : p0.relations < p1.relations ? 1 : -1;
            }
        });
        for (int i = 0; i < pages.size(); i++) {
            Page page = (Page) pages.get(i);
            page.id = i;
        }
    }

    void listPages() {
        for (int i = 0; i < pages.size(); i++) {
            Page page = (Page) pages.get(i);
            output.println("pages[" + page.id + "]=new Page('" + convertUTF(page.title) + "', '" + page.fileName
                    + "');");
        }
    }

    void readPages(String dir, File file, int level) throws Exception {
        String name = file.getName();
        String fileName = dir.length() > 0 ? dir + "/" + name : level > 0 ? name : "";
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            for (int i = 0; i < list.length; i++) {
                readPages(fileName, list[i], level + 1);
            }
            return;
        }
        String lower = StringUtils.toLowerEnglish(name);
        if (!lower.endsWith(".html") && !lower.endsWith(".htm")) {
            return;
        }
        if (lower.indexOf("_ja.") >= 0) {
            return;
        }
        if (!noIndex.contains(fileName)) {
            page = new Page(pages.size(), fileName);
            pages.add(page);
            readPage(file);
        }
    }

    void listWords() {
        output.println("// words: " + wordList.size());
        StringBuffer buff = new StringBuffer();
        String first = "";
        int firstLen = 1;
        int totalRelations = 0;
        for (int i = 0; i < wordList.size(); i++) {
            Word word = (Word) wordList.get(i);
            ArrayList weights = word.getSortedWeights();
            String lower = StringUtils.toLowerEnglish(word.name);
            if (!first.equals(lower.substring(0, firstLen))) {
                if (buff.length() > 0) {
                    output.println("ref['" + convertUTF(first) + "']='" + buff.toString() + "';");
                    buff = new StringBuffer();
                }
                first = lower.substring(0, firstLen);
            }
            if (buff.length() > 0) {
                buff.append(';');
            }
            buff.append(convertUTF(word.name));
            buff.append('=');
            String weightString = "r";
            totalRelations += weights.size();
            for (int j = 0; j < weights.size(); j++) {
                Weight weight = (Weight) weights.get(j);
                Page page = weight.page;
                if (j > 0) {
                    buff.append(",");
                }
                String ws;
                if (weight.value >= Weight.TITLE) {
                    ws = "t";
                } else if (weight.value >= Weight.HEADER) {
                    ws = "h";
                } else {
                    ws = "r";
                }
                if (ws != weightString) {
                    weightString = ws;
                    buff.append(ws);
                }
                buff.append(page.id);
                // TODO compress weight
                // buff.append(",");
                // buff.append(weight.value);
            }
        }
        // TODO optimization: could support "a name=" and go to _first_
        // occurrence, or scan page and mark
        output.println("ref['" + convertUTF(first) + "']='" + buff.toString() + "';");
        output.println("// totalRelations: " + totalRelations);
    }

    private void readPage(File file) throws Exception {
        byte[] data = IOUtils.readBytesAndClose(new FileInputStream(file), 0);
        String text = new String(data, "UTF-8");
        StringTokenizer t = new StringTokenizer(text, "<> \r\n", true);
        boolean inTag = false;
        title = false;
        heading = false;
        while (t.hasMoreTokens()) {
            String token = t.nextToken();
            if (token.length() == 1) {
                char c = token.charAt(0);
                switch (c) {
                case '<': {
                    if (inTag) {
                        process("???");
                    }
                    inTag = true;
                    if (!t.hasMoreTokens()) {
                        break;
                    }
                    token = t.nextToken();
                    if (token.startsWith("/")) {
                        title = false;
                        heading = false;
                    } else if (token.equalsIgnoreCase("title")) {
                        title = true;
                    } else if (token.length() == 2 && Character.toLowerCase(token.charAt(0)) == 'h'
                            && Character.isDigit(token.charAt(1))) {
                        heading = true;
                    }
                    // TODO maybe skip script tags?
                    break;
                }
                case '>': {
                    if (!inTag) {
                        process("???");
                    }
                    inTag = false;
                    break;
                }
                case '\r':
                case '\n':
                case ' ':
                    break;
                default:
                    if (!inTag) {
                        process(token);
                    }
                }
            } else {
                if (!inTag) {
                    process(token);
                }
            }
        }

        if (page.title == null || page.title.trim().length() == 0) {
            System.out.println("Error: not title found in " + file.getName());
            page.title = file.getName();
        }
        page.title = page.title.trim();
    }

    void process(String text) {
        text = HtmlConverter.convertHtmlToString(text);
        if (title) {
            if (page.title == null) {
                page.title = text;
            } else {
                page.title = page.title + " " + text;
            }
        }
        int weight;
        if (title) {
            weight = Weight.TITLE;
        } else if (heading) {
            weight = Weight.HEADER;
        } else {
            weight = Weight.PARAGRAPH;
        }
        // this list of constants needs to be the same in search.js
        StringTokenizer t = new StringTokenizer(text, " \t\r\n\"'.,:;!&/\\?%@`[]{}()+-=<>|*^~#$" + (char) 160, // nbsp
                false);
        while (t.hasMoreTokens()) {
            String token = t.nextToken();
            if (token.length() < MIN_WORD_SIZE) {
                continue;
            }
            if (Character.isDigit(token.charAt(0))) {
                continue;
            }
            String lower = StringUtils.toLowerEnglish(token);
            Word word = (Word) words.get(lower);
            if (word == null) {
                word = new Word(token);
                words.put(lower, word);
            } else if (!word.name.equals(token)) {
                word.name = token.compareTo(word.name) > 0 ? token : word.name;
            }
            page.totalWeight += weight;
            totalAllWeights += weight;
            word.addPage(page, weight);
        }
    }

    String convertUTF(String s) {
        s = StringUtils.quoteJavaString(s);
        s = s.substring(1, s.length() - 1);
        return s;
    }

}
