package com.lightbend.akka.sample;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class UserMain {

  public static String user_host;
  public static String user_port;
  public static String server_address;
  private static ActorRef user;
  private static ActorSelection server;
  private static ActorSystem system;
  private static String userName;
  private static boolean exit = false;
  public static boolean groupInviteFlag = false;
  public static String groupInviteName;


  private static void systemConnect(){
    int count = 0;
    handleConfig();
    Config conf = ConfigFactory.load("application.conf");
    conf.resolve();
    while(true) {
      try {
        system = ActorSystem.create("System", conf);
        user_port = Integer.toString((Integer)system.provider().getDefaultAddress().port().get());
        break;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        handleConfig();
        conf = ConfigFactory.load("application.conf");
        if (++count == 5)
          throw e;
      }
    }
  }
  public static void main(String[] args) {
    if(args.length < 2){
      System.out.println("MISSING ARGS!");
      return;
    }
    Scanner scanner = new Scanner(System.in);
    user_host = args[0];
    server_address = args[1];
    String[] command;
    try {
      systemConnect();
      server = system.actorSelection("akka://System@"+server_address+"/user/Server");
      System.out.println("Server address: " + server_address +"\n" + "User address: " + user_host+":"+ user_port);

      while (!exit) {
          command = scanner.nextLine().split(" ");
        if((userName == null) && (!command[0].equals("/user")) && (!command[1].equals("connect"))){
          System.out.println("NEED TO CONNECT FIRST!");
          continue;
        }
      if(groupInviteFlag) {
          if((!command[0].equals("yes")) && (!command[0].equals("no"))) {
              System.out.println("RESPONSE TO THE INVITE FIRST!");
              continue;
          }
          if(command[0].equals("yes"))
            server.tell(new ResponseToGroupInviteUser(groupInviteName, userName), ActorRef.noSender());
          groupInviteFlag = false;
          groupInviteName = null;
      }

        switch (command[0]){
          case "/user":
            userCommand(command);
            break;
          case "/group":
            groupCommand(command);
            break;
          case "exit":
            exit=true;
            break;
        }
      }
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
    finally {
      system.terminate();
    }


    }
  private static void userCommand(String[] command){
    switch (command[1]){
      case "connect":
        connectToServer(new Connect(command[2], user_host+":"+user_port));
        break;
      case "disconnect":
        UserMain.user.tell(new DisConnect(userName), ActorRef.noSender());
        userName=null;
        break;
      case "text":
        String message = String.join(" ", Arrays.asList(command).subList(3, command.length));
        UserMain.user.tell(new SendTextMessage(command[2], message), ActorRef.noSender());
        break;
      case "file":
        UserMain.user.tell(new Messages.SendFileMessage(command[2], fileToBytes(command[3])), ActorRef.noSender());
        break;
    }
  }
  private static void groupCommand(String[] command){
    switch (command[1]){
      case "create":
        UserMain.user.tell(new GroupCreate(command[2], userName), ActorRef.noSender());
        break;
      case "leave":
        UserMain.user.tell(new GroupLeave(command[2], userName), ActorRef.noSender());
        break;
      case "send":
        switch(command[2]){
          case "text":
            String message = String.join(" ", Arrays.asList(command).subList(4, command.length));
            UserMain.user.tell(new GroupMessage(new ReceiveTextMessage(userName, command[3], message)),
                    ActorRef.noSender());
            break;
          case "file":
            UserMain.user.tell(new GroupMessage(new ReceiveFileMessage(userName, command[3], fileToBytes(command[4]))),
                    ActorRef.noSender());
            break;
        }
        break;
      case "user":
        switch(command[2]){
          case "invite":
            UserMain.user.tell(new GroupInviteUser(command[3], userName, command[4]), ActorRef.noSender());
            break;
          case "remove":
            UserMain.user.tell(new GroupUserRemove(command[3], command[4]), ActorRef.noSender());
            break;
          case "mute":
            UserMain.user.tell(new GroupUserMute(command[3], command[4], Long.parseLong(command[5])), ActorRef.noSender());
            break;
          case "unmute":
            UserMain.user.tell(new GroupUserUnMute(command[3], command[4]), ActorRef.noSender());
            break;
        }
        break;
      case "coadmin":
          UserMain.user.tell(new GroupCoAdmin(command[3], command[4], userName,command[2]),ActorRef.noSender());
          break;
    }
  }

  private static byte[] fileToBytes(String path){
    try {
      File file = new File(path);
      byte[] bytesArray = new byte[(int) file.length()];
      FileInputStream fis = new FileInputStream(file); //todo: THIS SHOULD BE IN THE MAIN!!!!
      fis.read(bytesArray); //read file into bytes[]
      fis.close();
      return bytesArray;
    }
    catch (Exception e){
      System.out.println(path + " does not exist!");
      return null;
    }
  }

  private static void connectToServer(Connect connect){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    try{
      Future<Object> answer = Patterns.ask(server, connect, timeout);
      String result = (String) Await.result(answer, timeout.duration());
      if(result.equals(connect.userName + " is in use!")){
        System.out.println(result);
      }
      else{
        UserMain.user = UserMain.system.actorOf(User.props(connect.userName),connect.userName);
        UserMain.userName = connect.userName;
        System.out.println(result);
      }
    }
    catch (Exception e){
      System.out.println("server is offline!");
    }
  }
  private static void handleConfig(){
    try {
      String configPath = "src/main/resources/application.conf";
      String config = "akka {\r\n" +
              "loglevel = \"OFF\"\r\n" +
              "  actor {\r\n" +
              "  serialize-messages = on\r\n" +
              "  serializers {\r\n" +
              "        java = \"akka.serialization.JavaSerializer\"\r\n" +
              "  }\r\n" +
              "    provider = \"akka.remote.RemoteActorRefProvider\"\r\n" +
              "    warn-about-java-serializer-usage = false\r\n" +
              "  }\r\n" +
              "  remote {\r\n" +
              "    artery{\r\n" +
              "        enabled = on\r\n" +
              "        transport = tcp\r\n" +
              "        canonical.hostname = \"" + user_host + "\"\r\n" +
              "        canonical.port = 0\r\n" +
              "    }\r\n" +
              "    enabled-transports = [\"akka.remote.netty.tcp\"]\r\n" +
              "    netty.tcp {\r\n" +
              "      hostname = \"" + user_host + "\"\r\n" +
              "      port = 0\r\n" +
              "    }\r\n" +
              "  }\r\n" +
              "}\r\n";
        Path path = Paths.get(configPath);
        BufferedWriter writer = Files.newBufferedWriter(path);
        writer.write(config);
        writer.flush();
        writer.close();
    }
    catch (Exception e){
      System.out.println(e.getMessage());
    }
  }


}
