package kielakjr.api_gateway;

public class App {
  public static void main(String[] args) throws Exception {
    int port = 8080;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }

    new DiscardServer(port).run();

  }
}
