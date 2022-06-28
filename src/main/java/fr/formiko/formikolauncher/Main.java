package fr.formiko.formikolauncher;

public class Main {
  public static void main(String[] args) {
    Launcher l = new Launcher(args);
    // if there isn't any folder in .formiko/game{
    //   l.downloadGame(getLastVersion());
    // }else if (can ping github && there is a new version aviable) {
    //   if(ask for download){
    //     l.downloadGame(getLastVersion());
    //   }
    // }
    l.launchGame();
  }
}
