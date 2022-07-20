package fr.formiko.formikolauncher;

import fr.formiko.usual.Folder;
import fr.formiko.usual.Os;
import fr.formiko.usual.Progression;
import fr.formiko.usual.ReadFile;
import fr.formiko.usual.Version;
import fr.formiko.usual.ecrireUnFichier;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;
import fr.formiko.usual.structures.listes.GString;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
*{@summary Launcher interface.}<br>
*All launcher aviable action are functions in this class.
*@author Hydrolien
*@lastEditedVersion 1.0
*/
public class Launcher {
  private Process pr;
  private Folder folder;
  private boolean userWantToDownloadNextVersion;
  private List<String> args;
  /** Getter have lazy initialization. */
  private Progression progression;

  /**
  *{@summary Main constructor with the command line args.}<br>
  *@params args the args to use or transfer to Formiko.jar.
  *@lastEditedVersion 0.1
  */
  public Launcher(List<String> args){
    folder = new Folder();
    Folder.setFolder(folder);
    userWantToDownloadNextVersion=false;
    this.args=args;
  }

  private Folder getFolder(){return folder;}
  private String getVersion(){return Folder.getVersion();}
  public void setVersion(String version){Folder.setVersion(version);}
  /**
  *{@summary Getter with lazy initialization.}<br>
  *@lastEditedVersion 1.0
  */
  public Progression getProgression() {
    if(progression==null){progression=new ProgressionCLI();}
    return progression;
  }
	public void setProgression(Progression progression) {this.progression=progression;}


  class ProgressionCLI implements Progression {
    @Override
    public void iniLauncher(){
      fichier.setProgression(this);
    }
    @Override
    public void setDownloadingMessage(String message){
      System.out.println("Dowload: "+message);
    }
    @Override
    public void setDownloadingValue(int value){
      System.out.println(value+"% done");
    }
  }
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
  *{@summary Download the game at given version.}<br>
  *If needed it will download JRE
  *@param version the version to download game at
  *@return true if it work
  *@lastEditedVersion 1.0
  */
  public boolean downloadGame(String version){
    erreur.info("download Formiko"+version);
    getProgression().iniLauncher();
    File fi = new File(getFolder().getFolderGameJar()+version+"/");
    fi.mkdirs();
    boolean itWork=fichier.downloadAndUnzip(
        "https://github.com/HydrolienF/Formiko/releases/download/"+version+"/Formiko"+version+".zip",
        // getFolder().getFolderGameJar());
        getFolder().getFolderGameJar()+version+"/",
        "Formiko"+version+"/");
    if(!itWork){return itWork;}
    if(itWork){
      setVersion(version);
      getProgression().setDownloadingValue(20);
    }
    String wantedVersionJRE=ReadFile.readFile(getFolder().getFolderGameJar()+version+"/JREVersion.md").split("\n")[0];
    if(!canUseLauncherJRE(wantedVersionJRE) && !canUseDownloadedJRE(wantedVersionJRE)){
      itWork=downloadJRE(wantedVersionJRE);
    }
    return itWork;
  }
  public boolean canUseLauncherJRE(String wantedVersionJRE){
    String currentVersionJRE=null;
    File f=new File(getPathToLauncherFiles()+"app/JREVersion.md");
    if(f.exists()){
      currentVersionJRE=ReadFile.readFile(f).split("\n")[0];
    }
    if(currentVersionJRE==null || !currentVersionJRE.equals(wantedVersionJRE)){
      return false;
    }
    return true;
  }
  public boolean canUseDownloadedJRE(String wantedVersionJRE){
    String currentVersionJRE=null;
    File f=new File(getFolder().getFolderGameJar()+"JRE/JREVersion.md");
    if(f.exists()){
      currentVersionJRE=ReadFile.readFile(f).split("\n")[0];
    }
    if(currentVersionJRE==null || !currentVersionJRE.equals(wantedVersionJRE)){
      return false;
    }
    return true;
  }
  /**
  *{@summary download the JRE at given version.}<br>
  *@param versionJRE the version to download JRE at
  *@return true if it work
  *@lastEditedVersion 1.0
  */
  public boolean downloadJRE(String versionJRE){
    // if(getJavaCommand().equals("java")){ //no JRE downloaded
    String osName=null;
    if(Os.getOs().isWindows()){osName="Windows";}
    if(Os.getOs().isLinux()){osName="Linux";}
    if(Os.getOs().isMac()){osName="Mac";}
    boolean itWork=fichier.downloadAndUnzip(
        "https://github.com/HydrolienF/JRE/releases/download/"+versionJRE+"/jlink.zip/",
        getFolder().getFolderGameJar()+"JRE/",
        "j"+osName+"/");
    if(itWork){
      GString gs = new GString();
      gs.add(versionJRE);
      ecrireUnFichier.ecrireUnFichier(gs,getFolder().getFolderGameJar()+"JRE/JREVersion.md");
      erreur.info("downloaded JRE "+versionJRE);
      getProgression().setDownloadingValue(70);
    }else{
      erreur.erreur("fail to download JRE "+versionJRE);
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
    String jvmConfig = getJVMConfig();
    String javaArgs [] = null;
    if(jvmConfig!=null){
      javaArgs = jvmConfig.split("\n")[0].split(" ");
    }
    if(javaArgs==null || javaArgs.length==0){
      javaArgs=new String[0];
    }
    args.add("-launchFromLauncher");
    try {
      String[] cmd = new String[3+args.size()+javaArgs.length];
      int k=0;
      cmd[k++] = getJavaCommand();
      for (String arg : javaArgs) {
        cmd[k++]=arg;
      }
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
      ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmd));
          // .inheritIO();
      File parentLog = new File(Folder.getFolder().getFolderTemporary());
      parentLog.mkdirs();
      if(Main.logToFile && parentLog.exists()){
        File fout = new File(Folder.getFolder().getFolderTemporary()+"log.txt");
        try {
          pb.redirectOutput(Redirect.appendTo(fout));
        }catch (Exception e) {
          erreur.alerte("Fail to redirectOutput to log file.");
          pb.redirectOutput(Redirect.INHERIT);
        }
      }else{
        pb.redirectOutput(Redirect.INHERIT);
      }
      pr=pb.start();

      handleControlC();

    }catch (Exception e) {
      System.out.println("[ERROR] An error ocurre in launcher.");
      e.printStackTrace();
    }
    erreur.info("wait for the end of the Process");
    try {
      pr.waitFor();
    }catch (InterruptedException e) {
      erreur.erreur("Process have been interrupted");
    }
    switch (pr.exitValue()) {
      case 2: {
        userWantToDownloadNextVersion=true;
        return true;
      }
      default:{
        erreur.info("exit code "+pr.exitValue());
        return false;
      }
    }
  }
  /**
  *{@summary Give path to Formiko.jar.}<br>
  *@return path to Formiko.jar depending of the Os
  *@lastEditedVersion 0.1
  */
  public String getJarPath(){
    return getFolder().getFolderGameJar()+getVersion()+"/Formiko.jar";
  }
  /**
  *{@summary Give path to execute java.}<br>
  *@return path to our java version depending of the Os
  *@lastEditedVersion 1.0
  */
  public String getJavaCommand(){
    String pathToJava=getPathToLauncherFiles()+"runtime/bin/java";
    String wantedVersionJRE=ReadFile.readFile(getFolder().getFolderGameJar()+getVersion()+"/JREVersion.md").split("\n")[0];
    String javaCmd=null;
    if(canUseLauncherJRE(wantedVersionJRE)){
      if(Os.getOs().isWindows()){
        File f = new File(pathToJava+".exe");
        if(f.exists()){javaCmd=f.toString();}
      }else if(Os.getOs().isLinux()){
        File f = new File(pathToJava);
        if(f.exists()){javaCmd=f.toString();}
      }else if(Os.getOs().isMac()){
        File f = new File(pathToJava);
        if(f.exists()){javaCmd=f.toString();}
      }
      if(javaCmd!=null && Files.isExecutable(Paths.get(javaCmd))){
        return javaCmd;
      }else{
        erreur.alerte("Can't execute "+javaCmd);
      }
    }
    pathToJava=getFolder().getFolderGameJar()+"JRE/bin/java";
    if(Os.getOs().isWindows()){
      File f = new File(pathToJava+".exe");
      if(f.exists()){javaCmd=f.toString();}
    }else if(Os.getOs().isLinux()){
      File f = new File(pathToJava);
      if(f.exists()){javaCmd=f.toString();}
    }else if(Os.getOs().isMac()){
      File f = new File(pathToJava);
      if(f.exists()){javaCmd=f.toString();}
    }
    if(javaCmd!=null && Files.isExecutable(Paths.get(javaCmd))){
      return javaCmd;
    }else{
      erreur.alerte("Can't execute "+javaCmd);
    }
    return "java";
  }
  /**
  *{@summary Give args to execute java.}<br>
  *Args are download as ressources & can be find in .../app/jvm.config
  *@return args for the JVM
  *@lastEditedVersion 1.0
  */
  public static String getJVMConfig(){
    File f = new File(getPathToLauncherFiles()+"app/jvm.config");
    if(f.exists()){return ReadFile.readFile(f);}
    return null;
  }
  /**
  *{@summary Return launcher version.}<br>
  *Launcher Version is in .../app/version.md
  *@return launcher version depending of the Os
  *@lastEditedVersion 1.0
  */
  public static String getLauncherVersion(){
    File f = new File(getPathToLauncherFiles()+"app/version.md");
    if(f.exists()){return ReadFile.readFile(f).split("\n")[0];}
    f = new File("version.md");
    if(f.exists()){return ReadFile.readFile(f).split("\n")[0];}
    return null;
  }
  /**
  *{@summary Give path to launcher files.}<br>
  *@return path launcher files depending of the Os
  *@lastEditedVersion 1.0
  */
  public static String getPathToLauncherFiles(){
    if(Os.getOs().isWindows()){
      return System.getenv("ProgramFiles")+"/Formiko/";
    }else if(Os.getOs().isLinux()){
      return "/opt/formiko/lib/";
    }else if(Os.getOs().isMac()){
      // TODO
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
