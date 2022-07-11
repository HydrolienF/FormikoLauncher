package fr.formiko.formikolauncher;

import fr.formiko.usual.color;

public class Main {
  public static void main(String[] args) {
    color.iniColor();
    Launcher l = new Launcher(args);

    l.launch();
    // l.downloadGame("2.26.24");
    // l.launchGame();
  }
}
