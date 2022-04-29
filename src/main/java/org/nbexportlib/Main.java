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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

/**
 * Avvio applicazione.
 *
 * @author Nicola De Nisco
 */
public class Main
{
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {
    try
    {
      //String java = System.getProperty("java.version");
      //System.out.println("java=" + java);

      // su Mac OSX sposta il menu' in alto
      // su altri S.O. vengono semplicemente ignorate
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("com.apple.macos.useScreenMenuBar", "true");

      // configurazione per il logging a console (Log4j/apache commons)
      BasicConfigurator.configure(new ConsoleAppender(
         new PatternLayout("%d [%t] %-5p %c{1} - %m%n")));

      SetupData.initialize(args);

      Exporter exp = new Exporter();
      exp.runExport();

      SetupData.shutdown();
    }
    catch(Exception ex)
    {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
