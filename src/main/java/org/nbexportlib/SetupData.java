/*
 * SetupData.java
 *
 * Created on 16-dic-2013, 17.39.56
 *
 * Copyright (C) 2013 Informatica Medica s.r.l.
 *
 * Questo software è proprietà di Informatica Medica s.r.l.
 * Tutti gli usi non esplicitimante autorizzati sono da
 * considerarsi tutelati ai sensi di legge.
 *
 * Informatica Medica s.r.l.
 * Viale dei Tigli, 19
 * Casalnuovo di Napoli (NA)
 *
 * Creato il 16-dic-2013, 17.39.56
 */
package org.nbexportlib;

import com.sun.java.swing.plaf.motif.MotifLookAndFeel;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.*;
import java.util.Properties;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.commonlib5.gui.ErrorDialog;
import org.commonlib5.utils.*;

/**
 * Dati di setup.
 *
 * @author Nicola De Nisco
 */
public class SetupData
{
  public static final String APPNAME = "nbexportlib";

  /** directory di storage locale */
  public static File appDir;
  /** directory con file di libreria */
  public static File libDir;
  /** directory di esportazione */
  public static File exportDir;

  /** nome del look and feel da linea di comando */
  public static String lookfClassName;
  /** nome directory da usare come base durante l'export */
  public static String outputDirName = "/opt/shared-java-libs";
  /** flags */
  public static boolean nosplash, debug, simulazione, dryrun, gui;
  /** proprietà del file di versione */
  public static Properties resourceProperties = new Properties();
  /** proprietà persistenti su file separato non presenti in file di configurazione */
  public static PropertyManager pm = new PropertyManager();
  //
  //
  private static Log log = LogFactory.getLog(SetupData.class);

  public static void initialize(String[] args)
  {
    try
    {
      // imposta directory storage locale
      appDir = OsIdent.getAppDirectory(APPNAME);

      File alternateCmdFile = new File(OsIdent.getSystemTemp(), "nbexportlib.cmdline");
      if(alternateCmdFile.canRead())
      {
        String[] alternate = CommonFileUtils.grep(alternateCmdFile, "UTF-8", null);
        if(alternate != null && alternate.length > 0)
          args = ArrayOper.concat(args, alternate);
      }

      LongOptExt longopts[] = new LongOptExt[]
      {
        new LongOptExt("help", LongOpt.NO_ARGUMENT, null, 'h', "visualizza questo messaggio ed esce"),
        new LongOptExt("nosplash", LongOpt.NO_ARGUMENT, null, 'n', "disabilita splash screen iniziale"),
        new LongOptExt("simulate", LongOpt.NO_ARGUMENT, null, 'e', "simulazione presenza device hardware (debug)"),
        new LongOptExt("dry-run", LongOpt.NO_ARGUMENT, null, 'r', "non effettua salvataggio; solo scansione"),
        new LongOptExt("debug", LongOpt.NO_ARGUMENT, null, 'd', "abilita funzioni di debugging"),
        new LongOptExt("use-gui", LongOpt.NO_ARGUMENT, null, 'g', "abilita modalità gui"),
        new LongOptExt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o', "specifica la directory di output"),
        new LongOptExt("dir-name", LongOpt.REQUIRED_ARGUMENT, null, 'N', "il nome della dir che ricevera l'export"),
        new LongOptExt("lib", LongOpt.REQUIRED_ARGUMENT, null, 'l', "directory con i files di libreria"),
        new LongOptExt("laf", LongOpt.REQUIRED_ARGUMENT, null, 'f', "look and feel desiderato"),
      };

      InputStream strProperties = SetupData.class.getResourceAsStream(
         "/org/nbexportlib/setup.properties");
      if(strProperties != null)
      {
        resourceProperties.load(strProperties);
        strProperties.close();
      }

      try
      {
        File fprop = new File(SetupData.appDir, "private.properties");
        if(fprop.canRead())
          pm.load(fprop);
      }
      catch(Exception e)
      {
        log.error("Errore non fatale leggendo private.properties", e);
      }

      String optString = LongOptExt.getOptstring(longopts);
      Getopt g = new Getopt(APPNAME, args, optString, longopts);
      g.setOpterr(false); // We'll do our own error handling

      int c;
      while((c = g.getopt()) != -1)
      {
        switch(c)
        {
          case '?':
          case 'h':
            help(longopts);
            return;

          case 'n':
            nosplash = true;
            break;

          case 'd':
            debug = true;
            break;

          case 'e':
            simulazione = true;
            break;

          case 'f':
            lookfClassName = g.getOptarg();
            break;

          case 'r':
            dryrun = true;
            break;

          case 'g':
            gui = true;
            break;

          case 'o':
            exportDir = new File(g.getOptarg());
            break;

          case 'l':
            libDir = new File(g.getOptarg());
            break;

          case 'N':
            outputDirName = g.getOptarg();
            break;

          default:
            System.out.println("Opzione " + ((char) c) + " ignorata.");
        }
      }

      if(gui)
      {
        System.out.println("Spiacente, gui non ancora disponibile.");
        System.exit(0);
      }

      if(!debug)
      {
        // invio log su file se NON siamo in debug (in questo caso a console)
        String logFile = appDir.getAbsolutePath() + File.separator + APPNAME + ".log";
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure(new FileAppender(
           new PatternLayout("%d [%t] %-5p %c{1} - %m%n"),
           logFile, false));
      }

      // invia la command line alla log per eventuali tracce d'errore
      for(int i = 0; i < args.length; i++)
      {
        String arg = args[i];
        log.info(String.format("Argomento %d = %s", i, arg));
      }

      // se non specificata esplicitamente cerca di determinare la directory libreria
      if(libDir == null)
      {
        libDir = OsIdent.getAppDirectory("netbeans");
        if(!libDir.isDirectory())
        {
          System.out.println("Directory " + libDir.getAbsolutePath() + " non trovata. Occorre specificarla esplicitamente.");
          help(longopts);
        }

        File[] arFiles = libDir.listFiles((FileFilter) DirectoryFileFilter.INSTANCE);
        switch(arFiles.length)
        {
          case 0:
            System.out.println("Directory " + libDir.getAbsolutePath() + " vuota. Occorre specificarla esplicitamente.");
            help(longopts);
            break;

          case 1:
            libDir = new File(arFiles[0].getAbsolutePath() + "/config/org-netbeans-api-project-libraries/Libraries");
            break;

          default:
            System.out.println("Più versioni di Netbeans trovate in " + libDir.getAbsolutePath() + ". Occorre specificarla esplicitamente fra:");
            for(int i = 0; i < arFiles.length; i++)
            {
              File f = arFiles[i];
              System.out.println(f.getAbsolutePath() + "/config/org-netbeans-api-project-libraries/Libraries");
            }
            help(longopts);
            break;
        }
      }

      if(!libDir.isDirectory())
      {
        System.out.println("Directory " + libDir.getAbsolutePath() + " non trovata. Occorre specificarla esplicitamente.");
        help(longopts);
      }

      if(exportDir == null)
      {
        exportDir = new File("./export");
      }
    }
    catch(Exception ex)
    {
      manageExceptionAbort("Errore fatale nell'avvio applicazione.", ex);
    }
  }

  public static void manageExceptionAbort(String msg, Throwable ex)
  {
    if(gui)
      ErrorDialog.showAbort(msg, ex);
    else
    {
      System.err.println(msg);
      ex.printStackTrace();
    }
  }

  public static void manageExceptionError(String msg, Throwable ex)
  {
    if(gui)
      ErrorDialog.showError(msg, ex);
    else
    {
      System.err.println(msg);
      ex.printStackTrace();
    }
  }

  public static void shutdown()
  {
    savePrivateProp();
  }

  public static void savePrivateProp()
  {
    try
    {
      File fprop = new File(SetupData.appDir, "private.properties");
      pm.save(fprop);
    }
    catch(Exception e)
    {
      log.error("Errore non fatale scrivendo private.properties", e);
    }
  }

  public static void help(LongOptExt longopts[])
  {
    String appVersion = resourceProperties.getProperty("Application.version", "1.0");

    System.out.printf(
       "%s - ver. %s\n"
       + "Esportazione librerie Netbeans.\n"
       + "modo d'uso:\n"
       + "  %s [-h] [-c file.conf]\n", APPNAME, appVersion, APPNAME);

    for(LongOptExt l : longopts)
    {
      System.out.println(l.getHelpMsg());
    }

    System.out.println();
    System.exit(0);
  }

  public static void setLookAndFeel()
  {
    try
    {
      if(OsIdent.checkOStype() != OsIdent.OS_MACOSX)
      {
        String s = StringOper.okStrNull(lookfClassName);

        if(s == null)
          setNimbusTheme();
        else if(s.equalsIgnoreCase("motif"))
          setMotifTheme();
        else if(s.equalsIgnoreCase("nimrod"))
          setNimrodTheme();
        else if(s.equalsIgnoreCase("nimbus"))
          setNimbusTheme();
        else if(s.equalsIgnoreCase("system"))
          setSystemTheme();
        else // usa look and feel specificato sulla linea di comando
          UIManager.setLookAndFeel(s);
      }
    }
    catch(Exception e)
    {
      log.error("", e);
    }
  }

  public static void setNullTheme()
     throws Exception
  {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
  }

  public static void setNimrodTheme()
     throws Exception
  {
    UIManager.setLookAndFeel("com.nilo.plaf.nimrod.NimRODLookAndFeel");
  }

  public static void setMotifTheme()
     throws Exception
  {
    UIManager.setLookAndFeel(new MotifLookAndFeel());
  }

  public static void setSystemTheme()
     throws Exception
  {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
  }

  public static void setNimbusTheme()
     throws Exception
  {
    LookAndFeelInfo[] installedLookAndFeels = javax.swing.UIManager.getInstalledLookAndFeels();

    for(javax.swing.UIManager.LookAndFeelInfo info : installedLookAndFeels)
    {
      if("Nimbus".equalsIgnoreCase(info.getName()))
      {
        UIManager.setLookAndFeel(info.getClassName());
        break;
      }
    }
  }
}
