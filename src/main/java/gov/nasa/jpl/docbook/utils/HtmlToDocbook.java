/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.docbook.utils;

import gov.nasa.jpl.docbook.utils.HtmlManipulator;
import gov.nasa.jpl.docbook.model.DocumentElement;
import gov.nasa.jpl.docbook.model.From;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


public class HtmlToDocbook {

    /**
     * this was trying to use regex only to convert html tags to docbook tags,
     * should probably switch to just using jsoup
     */
    @SuppressWarnings("serial")
    public static final Map<String, String> html2docbookConvert = new HashMap<String, String>() {
                                                                    {
                                                                        put("<p>|<p [^>]*>", "<para>");
                                                                        put("</p>", "</para>");
                                                                        put("<ul>|<ul [^>]*>",
                                                                                "<itemizedlist spacing=\"compact\">");
                                                                        put("</ul>", "</itemizedlist>");
                                                                        put("<ol>|<ol [^>]*>",
                                                                                "<orderedlist spacing=\"compact\">");
                                                                        put("</ol>", "</orderedlist>");
                                                                        put("<li>|<li [^>]*>",
                                                                                "<listitem><para>");
                                                                        put("</li>", "</para></listitem>");
                                                                        put("<b>|<b [^>]*>|<em>|<em [^>]*>|<strong>|<strong [^>]*>",
                                                                                "<emphasis role=\"bold\">");
                                                                        put("<s>|<strike>|<s [^>]*>|<strike [^>]*>",
                                                                                "<emphasis role=\"strikethrough\">");
                                                                        put("<i>|<i [^>]*>", "<emphasis>");
                                                                        put("<u>|<u [^>]*>",
                                                                                "<emphasis role=\"underline\">");
                                                                        put("<span>|<span [^>]*>|</span>|<br>|<br/>|</br>|<br />",
                                                                                "");
                                                                        put("</b>|</i>|</u>|</strong>|</em>|</s>|</strike>",
                                                                                "</emphasis>");
                                                                        put("<font [^>]*>|</font>", "");
                                                                        put("<sup>|<sup [^>]*>",
                                                                                "<superscript>");
                                                                        put("<sub>|<sub [^>]*>",
                                                                                "<subscript>");
                                                                        put("</sup>", "</superscript>");
                                                                        put("</sub>", "</subscript>");
                                                                        put("<a href=\"(http[^\"]+)\">([^<]*)</a>",
                                                                                "<link xl:href=\"$1\">$2</link>");
                                                                        put("<a href=\"(file[^\"]+)\">([^<]*)</a>",
                                                                                "<link xl:href=\"$1\">$2</link>");
                                                                        put("<a href=\"(mailto[^\"]+)\">([^<]*)</a>",
                                                                                "<link xl:href=\"$1\">$2</link>");
                                                                        put("<a href=\"mdel://([^\"&^\\?]+)(\\?[^\"]*)?\">([^<]*)</a>",
                                                                                "<link linkend=\"$1\">$3</link>");
                                                                        put("<pre>|<pre [^>]*>", "<screen>");
                                                                        put("</pre>", "</screen>");
                                                                        put("<svg",
                                                                                "<mediaobject><imageobject><imagedata><svg");
                                                                        put("</svg>",
                                                                                "</svg></imagedata></imageobject></mediaobject>");
                                                                        put("&nbsp;", "&#160;");
                                                                        put("&sup2;",
                                                                                "<superscript>2</superscript>");
                                                                        put("&sup3;",
                                                                                "<superscript>3</superscript>");
                                                                    }
                                                                };

    /**
     * docbook ignores regular white space in table cells, this is to force
     * indentation in docbook, 1 indent is 4 spaces
     * 
     * @param name
     * @param depth
     * @return
     */
    public static String getIndented(String name, int depth) {
        String space = "";
        for (int i = 1; i < depth; i++)
            space += "&#xA0;&#xA0;&#xA0;&#xA0;";
        return space + name;
    }

    /**
     * given any object tries to return a string representation suitable for use
     * in docbook<br/>
     * 
     * @param s
     * @return
     */
    public static String fixString(Object s) {
        return fixString(s, true);
    }

    public static String fixString(Object s, boolean convertHtml) {
        // may want to look at
        // com.nomagic.magicdraw.uml.RepresentationTextCreator.getRepresentedText
        if (s instanceof String) {
            if (((String)s).contains("<html>")) {
                if (convertHtml)
                    return HtmlManipulator.replaceHtmlEntities(html2docbook((String)s));
            } else
                return HtmlManipulator.replaceHtmlEntities(((String)s)
                        .replaceAll("&(?![A-Za-z#0-9]+;)", "&amp;").replaceAll("<([>=\\s])", "&lt;$1")
                        .replaceAll("<<", "&lt;&lt;").replaceAll("<(?![^>]+>)", "&lt;"));
        } else if (s instanceof Integer) {
            return Integer.toString((Integer)s);
        } else if (s != null) {
            return fixString(s.toString());
        }
        return "";
    }
    
    
   
    /**
     * tries to make s into a docbook paragraph if not already one
     * 
     * @param s
     * @return
     */
    public static String addDocbook(String s) {
        String ss = html2docbook(s);
        if (ss.matches("(?s).*<para>.*")) // (s?) is the flag for DOTALL mode, .
                                          // doesn't match newlines by default
            // if (s.matches("(?s)\\s*<para>.*</para>\\s*"))
            return ss;
        return "<para>" + ss + "</para>";
    }

  

    /**
     * this is to help pdf transfrom be able to do wordwrap at non whitespace
     * chars, adds an invisible space to chars that should be able to break
     * probably should use some regex instead...
     * 
     * @param s
     * @return
     */
    public static String addInvisibleSpace(String s) {
        return s.replaceAll(";", ";&#x200B;").replaceAll("\\.", ".&#x200B;").replaceAll("\\(", "(&#x200B;")
                .replaceAll("\\)", ")&#x200B;").replaceAll(",", ",&#x200B;").replaceAll("/", "/&#x200B;")
                .replaceAll("_", "_&#x200B;").replaceAll("::", "::&#x200B;");
    }

    /**
     * if string contains html, converts it to docbook<br/>
     * also does some special processing if there's informal table elements,
     * removes width on tables so pdf transforms don't get cut off if width is
     * set too big should use jsoup processing to replace the regex map above
     * 
     * @param html
     * @return
     */
    public static String html2docbook(String html) {
        if (!html.contains("<html>"))
            return html;
        String s = null;
        Document d = Jsoup.parse(html);
        Elements tables = d.select("table.informal");
        if (!tables.isEmpty()) {
            tables.tagName("informaltable");
            tables.removeAttr("width");
        }
        tables = d.select("table");
        tables.removeAttr("width");

        Elements paragraphs = d.select("p");
        for (org.jsoup.nodes.Element e: paragraphs) {
            if (!e.hasText() || e.html().equals("&#160;") || e.html().equals("&nbsp;")) {
                e.remove();
            }
        }
        for (org.jsoup.nodes.Element e: d.select("span")) {
            if (e.hasAttr("style") && e.attr("style").startsWith("background-color")) {
                String style = e.attr("style");
                String color = style.substring(style.indexOf('#') + 1);
                e.tagName("phrase");
                e.removeAttr("style");
                e.attr("role", color);
            }
        }
        s = d.toString();
        int start = s.indexOf("<body>");
        int end = s.indexOf("</body>");
        if (start > -1 && end > -1)
            s = s.substring(start + 6, end);
        for (String key: html2docbookConvert.keySet()) {
            s = s.replaceAll(key, html2docbookConvert.get(key));
        }
        return s;
    }
}
