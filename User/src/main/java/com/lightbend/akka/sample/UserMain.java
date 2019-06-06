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
  public static String server_host;
  public static String server_port;
  private static ActorRef user;
  private static ActorSelection server;
  private static ActorSystem system;
  private static String userName;
  private static boolean exit = false;



  public static void main(String[] args) {
    if(args.length < 4){
      System.out.println("MISSING HOST AND PORTS!");
      return;
    }
    Scanner scanner = new Scanner(System.in);
    user_host = args[0];
    user_port = args[1];
    server_host = args[2];
    server_port = args[3];
    Config conf = handleConfig();
    String[] command;


    system = ActorSystem.create("System", conf);
    server = system.actorSelection("akka://System@"+server_host+":"+server_port+"/user/Server");

    try {
      while (!exit) {
        command = scanner.nextLine().split(" ");
        if((userName == null) && (!command[0].equals("/user")) && (!command[1].equals("connect"))){
          System.out.println("NEED TO CONNECT FIRST!");
          continue;
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
        break;
      case "text":
        String message = String.join(" ", Arrays.asList(command).subList(3, command.length));
        UserMain.user.tell(new SendTextMessage(command[2], message), ActorRef.noSender());
        break;
      case "file":
        UserMain.user.tell(new Messages.SendFileMessage(command[2], command[3]), ActorRef.noSender());
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
            UserMain.user.tell(new GroupMessage(new ReceiveTextMessage(userName, command[3], command[4])),
                    ActorRef.noSender());
            break;
          case "file":
            //          UserMain.user.tell(new GroupMessage(new ReceiveFileMessage(userName, command[3], command[4])),
            //                  ActorRef.noSender());
            break;
        }
        break;
      case "user":
        switch(command[2]){
          case "invite":
//            UserMain.user.tell(new GroupInviteUser(command[3], userName, command[4]), ActorRef.noSender());
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
  private static void connectToServer(Connect connect){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, connect, timeout);
    try{
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
  private static Config handleConfig(){
    try {
//      String configPath = "src/main/resources/" + user_host + ":" + user_port + ".conf";
      String configPath = "src/main/resources/application.conf";

      new File(configPath);
      String config = "akka {\r\n" +
              "#loglevel = \"OFF\"\r\n" +
              "  actor {\r\n" +
              "  serialize-messages = on\r\n" +
              "  serializers {\r\n" +
              "        java = \"akka.serialization.JavaSerializer\"\r\n" +
              "  }\r\n" +
              "    provider = \"akka.remote.RemoteActorRefProvider\"\r\n" +
              "    akka.actor.warn-about-java-serializer-usage = false\r\n" +
              "  }\r\n" +
              "  remote {\r\n" +
              "    artery{\r\n" +
              "        enabled = on\r\n" +
              "        transport = tcp\r\n" +
              "        canonical.hostname = \"" + user_host + "\"\r\n" +
              "        canonical.port = " + user_port + "\r\n" +
              "    }\r\n" +
              "    enabled-transports = [\"akka.remote.netty.tcp\"]\r\n" +
              "    netty.tcp {\r\n" +
              "      hostname = \"127.0.0.1\"\r\n" +
              "      port = 8082\r\n" +
              "    }\r\n" +
              "  }\r\n" +
              "}\r\n";
      Path path = Paths.get(configPath);
      BufferedWriter writer = Files.newBufferedWriter(path);
      writer.write(config);
      writer.close();
//      Thread.sleep(4000);
//      return ConfigFactory.load(user_host + ":" + user_port + ".conf");
      return ConfigFactory.load("application.conf");
    }
    catch (Exception e){
      return null;
    }
  }


}
