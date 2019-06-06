package com.lightbend.akka.sample;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.remote.RemoteScope;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class UserMain {

  private static ActorRef user;
  private static ActorSelection server;
  private static ActorSystem system;
  private static String userName;

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);
    boolean exit=false;
    String[] command;
    Config conf = ConfigFactory.load("user1.conf");;
    String port = "8082";
    if(args[0].equals("2")) {
      conf = ConfigFactory.load("user2.conf");
      port = "8084";
    }
    if(args[0].equals("3")) {
      conf = ConfigFactory.load("user3.conf");
      port = "8086";
    }

    system = ActorSystem.create("System", conf); // todo: need to change to remote SYSTEM!!!!!

    server = system.actorSelection("akka://System@127.0.0.1:8080/user/Server"); // todo: CHANGE PATH!!

    try {
      while (!exit) {
        command = scanner.nextLine().split(" ");
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
      System.out.println(e);
    }
    finally {
      system.terminate();
    }


    }
  private static void userCommand(String[] command){
    switch (command[1]){
      case "connect":
        connectToServer(new Connect(command[2]));
        break;
      case "disconnect":
        UserMain.user.tell(new DisConnect(command[2]), ActorRef.noSender());
        break;
      case "text":
        UserMain.user.tell(new SendTextMessage(command[2], command[3]), ActorRef.noSender());
        break;
      case "file":
        UserMain.user.tell(new Messages.SendFileMessage(command[2], command[3]), ActorRef.noSender());
        break;
    }
  }
  private static void groupCommand(String[] command){
    switch (command[1]){
      case "create":
        connectToServer(new Connect(command[2]));
        break;
      case "leave":
        UserMain.user.tell(new DisConnect(command[2]), ActorRef.noSender());
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
        UserMain.user = UserMain.system.actorOf(User.props(connect.userName),connect.userName); // todo: CHANGE PATH!!
        UserMain.userName = connect.userName;
        System.out.println(result);
      }
    }
    catch (Exception e){
      System.out.println("server is offline!");
    }
  }


}
