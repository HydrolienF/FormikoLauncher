package fr.formiko.formikolauncher;

import fr.formiko.usual.Folder;
import fr.formiko.usual.Os;
import fr.formiko.usual.ReadFile;
import fr.formiko.usual.Version;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
*{@summary Launcher interface.}<br>
*All launcher aviable action are functions in this class.
*@author Hydrolien
*@lastEditedVersion 0.1
*/
public class Launcher {
  private Process pr;
  private Folder folder;
  private boolean userWantToDownloadNextVersion;
  private String args[];

  /**
  *{@summary Main constructor with the command line args.}<br>
  *@params args the args to use or transfer to Formiko.jar.
  *@lastEditedVersion 0.1
  */
  public Launcher(String[] args){
    //TODO args=launcherArgs(args); (version=, no download of other version, etc)
    // TODO it remove from args the args that launcher understand & leave the other one for formiko.
    folder = new Folder();
    Folder.setFolder(folder);
    userWantToDownloadNextVersion=false;
    this.args=args;
    if(this.args==null || this.args.length==0 || this.args[0]==null){
      this.args=new String[0];
    }
  }

  private Folder getFolder(){return folder;}
  private String getVersion(){return Folder.getVersion();}
  public void setVersion(String version){Folder.setVersion(version);}

  /**
  *{@summary Main function that will download game if needed then launch game.}<br>
  *@lastEditedVersion 0.1
  */
  public void launch(){
    if(needToDownloadGame()){
      if(!downloadGame(getFolder().getLastStableVersion())){
        erreur.erreur("Download & unzip new game version fail. Be sur to be connected to the internet");
      }
    }else{
      setVersion(Folder.getLastDownloadedGameVersion());
    }
    erreur.info("launch game");
    if(launchGame()){
      launch();
    }
  }

  /**
  *{@summary Return true if we need to download the game.}<br>
  *@lastEditedVersion 0.1
  */
  public boolean needToDownloadGame(){
    File gameJarFolder = new File(getFolder().getFolderGameJar());
    if(!gameJarFolder.exists() || gameJarFolder.listFiles().length==0){
      erreur.info("Need to download jar because there is no current game version");
      return true;
    }
    String lastStableVersion = getFolder().getLastStableVersion();
    if(lastStableVersion.equals(Folder.DEFAULT_NULL_VERSION)){
      erreur.alerte("Unable to fetch new game version");
      return false; //no wifi or version unaviable.
    }else if(!getFolder().haveLastVersion()){
      if(userWantToDownloadNextVersion){return true;}
      // boolean userChoice;
      // // TODO ask user if he want to get it.
      // userChoice=true; //TOREMOVE
      // return userChoice;
      erreur.info("A new version is aviable ("+lastStableVersion+")");
      return false;
    }else{
      erreur.info("You have the last stable game version");
      return false;
    }
  }

  /**
  *{@summary download the game at given version.}<br>
  *@param version the version to download game at
  *@return true if it work
  *@lastEditedVersion 0.1
  */
  public boolean downloadGame(String version){
    erreur.info("download Formiko"+version);
    boolean itWork=getFolder().downloadAndUnzip(
        "https://github.com/HydrolienF/Formiko/releases/download/"+version+"/Formiko"+version+".zip",
        getFolder().getFolderGameJar(), true);
    if(!itWork){return itWork;}
    File temp = new File(getFolder().getFolderGameJar()+"Formiko"+version+"/");
    if(!temp.renameTo(new File(getFolder().getFolderGameJar()+version+"/"))){
      erreur.erreur("Fail to rename folder");
      fichier.deleteDirectory(temp);
    }
    if(itWork){
      setVersion(version);
    }
    return itWork;
  }

  /**
  *{@summary launch the game with selected version.}<br>
  *Version can be set before call launchGame() with setVersion(String version).
  *@return true if we need to do launch() again
  *@lastEditedVersion 0.1
  */
  public boolean launchGame(){
    // set up the command and parameter
    String s2 = "";
    try {
      String[] cmd = new String[4+args.length];
      int k=0;
      cmd[k++] = getJavaCommand();
      cmd[k++] = getJVMConfig();
      cmd[k++] = "-jar";
      cmd[k++] = getJarPath();
      for (String arg : args) {
        cmd[k++]=arg;
      }

      System.out.println("commande launch: ");//@a
      for (String s : cmd) {
        System.out.print(s+" ");//@a
      }
      System.out.println();//@a
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
    erreur.info("wait for the end of the Process");
    try {
      pr.waitFor(10, TimeUnit.SECONDS);
    }catch (InterruptedException e) {
      erreur.erreur("Process have been interrupted");
    }
    switch (pr.exitValue()) {
      case 2: {
        userWantToDownloadNextVersion=true;
        return true;
      }
      default:
      erreur.info("exit code "+pr.exitValue());
        return false;
    }
  }
  /**
  *{@summary Give path to Formiko.jar.}<br>
  *@return path to Formiko.jar depending of the OS
  *@lastEditedVersion 0.1
  */
  public String getJarPath(){
    return getFolder().getFolderGameJar()+getVersion()+"/Formiko.jar";
  }
  /**
  *{@summary Give path to execute java.}<br>
  *@return path to our java version depending of the OS
  *@lastEditedVersion 1.0
  */
  public String getJavaCommand(){
    if(Os.getOs().isWindows()){
      File f = new File(getPathToLauncherFiles()+"runtime/bin/java.exe");
      if(f.exists()){return f.toString();}
    }else if(Os.getOs().isLinux()){
      File f = new File(getPathToLauncherFiles()+"runtime/bin/java");
      if(f.exists()){return "/."+f.toString();}
    }else if(Os.getOs().isMac()){
      // File f = new File("/opt/Formiko/runtime/bin/java");
      // if(f.exists()){return f.toString();}
    }
    return "java";
  }
  /**
  *{@summary Give args to execute java.}<br>
  *Args are download as ressources & can be find in .../app/jvm.config
  *@return args for the JVM
  *@lastEditedVersion 1.0
  */
  public String getJVMConfig(){
    if(Os.getOs().isWindows()){
      File f = new File(getPathToLauncherFiles()+"app/jvm.config");
      if(f.exists()){return ReadFile.readFile(f);}
    }else if(Os.getOs().isLinux()){
      File f = new File(getPathToLauncherFiles()+"app/jvm.config");
      if(f.exists()){return ReadFile.readFile(f);}
    }else if(Os.getOs().isMac()){
      // File f = new File("/opt/Formiko/runtime/bin/java");
      // if(f.exists()){return f.toString();}
    }
    return "";
  }
  /**
  *{@summary Give path to launcher files.}<br>
  *@return path launcher files depending of the OS
  *@lastEditedVersion 1.0
  */
  public String getPathToLauncherFiles(){
    if(Os.getOs().isWindows()){
      return System.getenv("ProgramFiles")+"/Formiko/";
    }else if(Os.getOs().isLinux()){
      return "/opt/formiko/lib/";
    }else if(Os.getOs().isMac()){

    }
    return "";
  }

  /**
  *{@summary This handler will be called on Control-C pressed.
  *cf http://vkroz.github.io/posts/20170630-Java-interrupt-hook.html
  *@lastEditedVersion 0.1
  */
  private void handleControlC(){
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      /**
      *{@summary This handler will be called on Control-C pressed.}
      *@lastEditedVersion 0.1
      */
      public void run() {
        System.out.println("Closing from launcher");
        if(pr!=null){
          pr.destroy();
        }
      }
    });
  }
}
