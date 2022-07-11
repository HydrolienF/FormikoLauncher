package fr.formiko.formikolauncher;

import fr.formiko.usual.Folder;
import fr.formiko.usual.Version;
import fr.formiko.usual.Os;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Launcher {
  private Process pr;
  private Folder folder;
  private boolean userWantToDownloadNextVersion;
  private String args[];

  public Launcher(String[] args){
    //TODO args=launcherArgs(args); (version=, no download of other version, etc)
    // TODO it remove from args the args that launcher understand & leave the other one for formiko.
    folder = new Folder();
    Folder.setFolder(folder);
    userWantToDownloadNextVersion=false;
    this.args=args;
    if(this.args==null || this.args[0]==null){
      this.args=new String[0];
    }
  }

  private Folder getFolder(){return folder;}
  private String getVersion(){return Folder.getVersion();}
  public void setVersion(String version){Folder.setVersion(version);}

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

  // return true if we need to do launch() again.
  public boolean launchGame(){
    // set up the command and parameter
    String s2 = "";
    try {
      String[] cmd = new String[3+args.length];
      int k=0;
      cmd[k++] = getJavaCommand();
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
    switch (pr.exitValue()) {
      case 2: {
        userWantToDownloadNextVersion=true;
        return true;
      }
      default:
        return false;
    }
  }
  public String getJarPath(){
    return getFolder().getFolderGameJar()+getVersion()+"/Formiko.jar";
  }
  /**
  *{@summary Give path to execute java.}<br>
  *@lastEditedVersion 0.1
  *@return path to our java version depending of the OS
  */
  public String getJavaCommand(){
    if(Os.getOs().isWindows()){
      File f = new File(System.getenv("ProgramFiles")+"/Formiko/runtime/bin/java.exe");
      if(f.exists()){return f.toString();}
    }else if(Os.getOs().isLinux()){
      File f = new File("/opt/Formiko/runtime/bin/java");
      if(f.exists()){return f.toString();}
    }else if(Os.getOs().isMac()){
      File f = new File("/opt/Formiko/runtime/bin/java");
      if(f.exists()){return f.toString();}
    }
    return "java";
  }

  //cf http://vkroz.github.io/posts/20170630-Java-interrupt-hook.html
  private void handleControlC(){
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
