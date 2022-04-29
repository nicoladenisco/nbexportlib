/*
 * Copyright (C) 2022 Nicola De Nisco
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nbexportlib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonlib5.io.StringArrayReader;
import org.commonlib5.utils.CommonFileUtils;
import org.commonlib5.utils.FileScanner;
import org.commonlib5.utils.StringOper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Legge librerie ed esporta.
 *
 * @author Nicola De Nisco
 */
public class Exporter
{
  private static Log log = LogFactory.getLog(Exporter.class);
  private Namespace ns = Namespace.getNamespace("", "http://www.netbeans.org/ns/library-declaration/3");

  void runExport()
     throws Exception
  {
    List<File> lsFiles = FileScanner.scan(SetupData.libDir, 0, "*.xml");
    System.out.printf("Trovati %d files XML da analizzare.\n", lsFiles.size());
    if(lsFiles.isEmpty())
      return;

    List<LibraryBean> arBeans = leggiListaLibrerie(lsFiles);

    exportLibrerie(arBeans);
  }

  private List<LibraryBean> leggiListaLibrerie(List<File> lsFiles)
  {
    ArrayList<LibraryBean> arBeans = new ArrayList<>(lsFiles.size());
    for(File fxml : lsFiles)
    {
      leggiFileXml(fxml, arBeans);
    }

    System.out.printf("Trovate %d librerie valide per l'export.\n", arBeans.size());
    return arBeans;
  }

  private void leggiFileXml(File fxml, List<LibraryBean> arBeans)
  {
    try
    {
      log.info("Leggo " + fxml.getAbsolutePath());
      List<String> allLines = Files.readAllLines(fxml.toPath(), Charset.forName("UTF-8"));

      if(allLines.stream().anyMatch((s) -> s.contains("<!DOCTYPE")))
      {
        // vecchia versione
        List<String> purgedLines = allLines.stream()
           .filter((s) -> !s.contains("<!DOCTYPE"))
           .collect(Collectors.toList());

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringArrayReader(purgedLines.iterator(), "\n"));

        LibraryBean b = parseLibrary(doc, new LibraryBean());

        if(b.isValid())
          arBeans.add(b);
      }
      else
      {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(fxml);

        LibraryBean b = parseLibrary3(doc, new LibraryBean());

        if(b.isValid())
          arBeans.add(b);
      }
    }
    catch(JDOMException e)
    {
      log.error("** scartato: " + e.getMessage());
    }
    catch(IOException e)
    {
      log.error("** scartato: " + e.getMessage());
    }
  }

  private LibraryBean parseLibrary(Document doc, LibraryBean b)
  {
    Element r = doc.getRootElement();

    b.name = r.getChildTextTrim("name");
    b.type = r.getChildTextTrim("type");
    b.displayName = r.getChildTextTrim("display-name");

    List<Element> lsVols = r.getChildren("volume");

    for(Element e : lsVols)
    {
      String type = e.getChildTextTrim("type");
      if(type == null)
        continue;

      List<Element> lsResources = e.getChildren("resource");

      switch(type)
      {
        case "src":
          parseResources(b.src, lsResources);
          break;

        case "classpath":
          parseResources(b.classpath, lsResources);
          break;

        case "javadoc":
          parseResources(b.javadoc, lsResources);
          break;

        case "maven-pom":
          parseResources(b.mavenPom, lsResources);
          break;
      }
    }

    return b;
  }

  private LibraryBean parseLibrary3(Document doc, LibraryBean b)
  {
    Element r = doc.getRootElement();

    b.name = r.getChildTextTrim("name", ns);
    b.type = r.getChildTextTrim("type", ns);
    b.displayName = r.getChildTextTrim("display-name", ns);

    List<Element> lsVols = r.getChildren("volume", ns);

    for(Element e : lsVols)
    {
      String type = e.getChildTextTrim("type", ns);
      if(type == null)
        continue;

      List<Element> lsResources = e.getChildren("resource", ns);

      switch(type)
      {
        case "src":
          parseResources(b.src, lsResources);
          break;

        case "classpath":
          parseResources(b.classpath, lsResources);
          break;

        case "javadoc":
          parseResources(b.javadoc, lsResources);
          break;

        case "maven-pom":
          parseResources(b.mavenPom, lsResources);
          break;
      }
    }

    return b;
  }

  Pattern pjar = Pattern.compile("jar:file:(.+)!/+");

  private void parseResources(List<String> ls, List<Element> lsResources)
  {
    // <resource>jar:file:/home/nicola/cvsjupiter/apache-commons/commons-csv-1.4/commons-csv-1.4.jar!/</resource>
    // <resource>jar:file:/home/nicola/cvsjupiter/apache-commons/commons-csv-1.4/commons-csv-1.4.jar!//</resource>

    String res, tosave;

    for(Element e : lsResources)
    {
      res = e.getTextTrim();
      Matcher m = pjar.matcher(res);

      if(m.find())
        ls.add(m.group(1));
    }
  }

  private void exportLibrerie(List<LibraryBean> arBeans)
     throws Exception
  {
    File dirJars = new File(SetupData.exportDir, "librerie");
    if(!SetupData.dryrun)
      makeDir(dirJars);

    for(LibraryBean b : arBeans)
    {
      File outxml = new File(SetupData.exportDir, b.name + ".xml");
      exportLibreria(b, outxml, dirJars);
    }
  }

  private void exportLibreria(LibraryBean b, File outxml, File dirJars)
     throws Exception
  {
    if(!SetupData.dryrun)
    {
      File dirJarsLib = new File(dirJars, StringOper.CvtFILEstring(b.name));
      makeDir(dirJarsLib);

      copyJars(b.classpath, dirJarsLib, SetupData.exportDir);
      copyJars(b.src, dirJarsLib, SetupData.exportDir);
      copyJars(b.javadoc, dirJarsLib, SetupData.exportDir);
    }

    Element root = new Element("library", ns);
    root.setAttribute("version", "3.0");
    root.addContent(new Element("name", ns).setText(b.name));
    root.addContent(new Element("type", ns).setText(b.type));
    root.addContent(new Element("display-name", ns).setText(StringOper.okStr(b.displayName, b.name)));

    if(!b.classpath.isEmpty())
      root.addContent(getXmlPart("classpath", b.classpath));
    if(!b.src.isEmpty())
      root.addContent(getXmlPart("src", b.src));
    if(!b.javadoc.isEmpty())
      root.addContent(getXmlPart("javadoc", b.javadoc));

    root.addContent(new Element("properties", ns));
    Document docOutput = new Document(root);

    // output del documento
    XMLOutputter xout = new XMLOutputter();
    xout.setFormat(Format.getPrettyFormat());

    if(!SetupData.dryrun)
    {
      try (OutputStreamWriter out = new FileWriter(outxml))
      {
        xout.output(docOutput, out);
      }
    }
    else
      xout.output(docOutput, System.out);
  }

  private void copyJars(ArrayList<String> originJars, File dirJarsLib, File dirParent)
     throws Exception
  {
    ArrayList<String> newpaths = new ArrayList<>(originJars.size());

    for(String s : originJars)
    {
      File origin = new File(s);
      if(origin.canRead())
      {
        File destin = new File(dirJarsLib, origin.getName());
        CommonFileUtils.copyFile(origin, destin);

        String relativePath = CommonFileUtils.getRelativePath(dirParent, destin);
        newpaths.add(SetupData.outputDirName + "/" + relativePath);
      }
    }

    originJars.clear();
    originJars.addAll(newpaths);
  }

  private void makeDir(File dir)
     throws IOException
  {
    if(!dir.exists())
      if(!dir.mkdirs())
        throw new IOException("Impossibile creare directory " + dir.getAbsolutePath());
  }

  private Element getXmlPart(String type, List<String> classpath)
     throws Exception
  {
    // <resource>jar:file:/home/nicola/cvsjupiter/apache-commons/commons-csv-1.4/commons-csv-1.4.jar!/</resource>

    Element volume = new Element("volume", ns);
    volume.addContent(new Element("type", ns).setText(type));

    for(String s : classpath)
      volume.addContent(new Element("resource", ns).setText("jar:file:" + s + "!/"));

    return volume;
  }
}
