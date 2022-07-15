package fr.formiko.formikolauncher;

import fr.formiko.usual.Folder;
import fr.formiko.usual.color;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
*{@summary Main class of the launcher package.}<br>
*Initialize some data &#38; launch the launcher.
*@author Hydrolien
*@lastEditedVersion 1.0
*/
public class Main {
  private static boolean launchGame;
  private static boolean rmGame;
  private static boolean rmData;
  private static String versionToDownload;

  /**
  *{@summary Initialize some data &#38; launch the launcher.}<br>
  *@lastEditedVersion 1.0
  */
  public static void main(String[] args) {
    color.iniColor();
    launchGame=true;
    if(args.length==1 && args[0] != null){ //for maven args
      args = args[0].split(" ");
    }
    Launcher l = new Launcher(launcherArgs(Arrays.asList(args)));

    if(rmData){
      Folder f = new Folder();
      fichier.deleteDirectory(f.getFolderGameJar());
    }
    if(rmGame){
      Folder f = new Folder();
      fichier.deleteDirectory(f.getFolderMain());
    }

    if(versionToDownload!=null){
      l.downloadGame(versionToDownload);
    }
    if(launchGame){
      l.launch();
    }
  }

  /**
  *{@summary Launch some args.}<br>
  *@return the args that Formiko.jar still need to have
  *@lastEditedVersion 1.0
  */
  private static List<String> launcherArgs(List<String> args){
    List<String> argsOut = new LinkedList<String>();
    for (String arg : args) {
      if(arg==null){continue;}
      String iniArg=arg;
      boolean withHyphen=false;
      arg=arg.replace("=",":");
      String t[] = arg.split(":");
      arg=t[0];
      String argOfTheArg="";
      if(t.length>1){
        argOfTheArg=t[1];
      }
      while(arg.startsWith("-")){
        withHyphen=true;
        arg=arg.substring(1,arg.length());
      }
      boolean passToFormiko=false;
      switch (arg) {
        case "version":{
          erreur.info("Launcher version: "+Launcher.getLauncherVersion());
          passToFormiko=true;
          break;
        }
        case "rmData":{
          rmData=true;
          break;
        }
        case "rmGame":{
          rmGame=true;
          break;
        }
        case "vtd":
        case "versionToDownload":{
          versionToDownload=argOfTheArg;
          break;
        }

        default:{
          passToFormiko=true;
          break;
        }
      }
      if(passToFormiko){
        argsOut.add(iniArg);
      }
    }
    return argsOut;
  }
}
