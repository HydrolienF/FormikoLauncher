package fr.formiko.formikolauncher;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Launcher {
  private Process pr;
  private String version = "2.24.26";

  public Launcher(String[] args){
    //TODO launcherArgs(args); (version=, no download of other version, etc)
  }

  public boolean downloadGame(String version){
    // if( version don't exist on github){return;}
    // TODO download jar file
    // TODO download data if needToDownload("data")
    // TODO download music if needToDownloadMusic("music");
    return true;
  }
  public boolean needToDownload(String partOfTheGame){
    // TODO this + same for music
    // Compare .formiko/data/version.json->partOfTheGame  .equals("https://github.com/HydrolienF/Formiko/releases/download/"+gameVersion+"/Formiko"+gameVersion+".zip->"Formiko"+gameVersion+"/version.json"->partOfTheGame)
    // if different download "https://github.com/HydrolienF/Formiko/releases/download/"+dataVersion+"/"+partOfTheGame+".zip"
    return true;
  }

  public void launchGame(){
    // set up the command and parameter
    String s2 = "";
    try {
      String[] cmd = new String[3];
      cmd[0] = getJavaCommand();
      cmd[1] = "-jar";
      cmd[2] = getJarPath();

      // create runtime to execute external command
      // TODO use a ProcessBuilder.
      pr = Runtime.getRuntime().exec(cmd);
      handleControlC();

      // retrieve output from command
      String line;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()))){
        while ((line = br.readLine()) != null){
          System.out.println(line);
        }
      }catch(Exception e) {
        throw e;
      }
    }catch (Exception e) {
      System.out.println("[ERROR] An error ocurre in launcher.");
      e.printStackTrace();
    }
  }
  public String getJarPath(){
    // TODO return path to game .jar depending of the OS.
    // TODO if version==null check what is the last version downloaded in .formiko/game/
    return "C:\\Users\\lili5\\AppData\\Roaming\\.formiko/game/"+version+"/Formiko.jar";
  }
  public String getJavaCommand(){
    // TODO return path to our java version depending of the OS.
    return "java";
  }

  //cf http://vkroz.github.io/posts/20170630-Java-interrupt-hook.html
  public void handleControlC(){
    Runtime.getRuntime().addShutdownHook(new Thread() {

      /** This handler will be called on Control-C pressed */
      @Override
      public void run() {
        System.out.println("Closing from launcher");
        if(pr!=null){
          pr.destroy();
        }
      }
    });
  }
}
