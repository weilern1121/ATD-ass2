package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import com.lightbend.akka.sample.Messages.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class User extends AbstractActor {
  private String userName;
  private final ActorSelection server;

  static public Props props(String userName) {
    return Props.create(User.class, () -> new User(userName));
  }

  public User(String userName) {

    this.userName = userName;
    this.server = getContext().actorSelection("akka://System@127.0.0.1:8080/user/Server");
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(ReceiveMessage.class, this::receiveMessage)
        .match(SendMessage.class, this::sendMessage)
        .match(GroupCreate.class, this::groupCreate)
        .match(Connect.class, this::connect)
        .match(DisConnect.class, this::disConnect)
        .match(GroupLeave.class, this::groupLeave)
        .match(GroupInviteUser.class,this::groupInviteUser)
        .match(BasicGroupAdminAction.class, this::basicGroupAdminAction)
//        .match(ResponseToGroupInviteUser.class, this::responseToGroupInviteUser)
        .match(GroupMessage.class, this::groupMessage)
        .build();
  }

  private void responseToGroupInviteUser(AskTargetToGroupInviteUser responseToGroupInviteUser){
    System.out.println("You have been invited to "+ responseToGroupInviteUser.groupName+", Accept? [Yes]/[No]");
    String response = new Scanner(System.in).nextLine(); //yes/no answer input
    getSender().tell(response, getSelf()); //send answer to server
  }

  private void basicGroupAdminAction(BasicGroupAdminAction basicGroupAction){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;
    basicGroupAction.sourceUserName = this.userName;

    answer = Patterns.ask(server, basicGroupAction, timeout);
    try {
      String result = (String) Await.result(answer, timeout.duration());
      System.out.println(result);
    }
    catch (Exception e) {
      System.out.println("server is offline! try again later!");
    }
  }

  private void groupMessage(GroupMessage groupMessage){
      Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
      Future<Object> answer = Patterns.ask(server, groupMessage, timeout);
      try{
          String result = (String) Await.result(answer, timeout.duration());
          System.out.println(result);
      }
      catch (Exception e){
          System.out.println("server is offline! try again later!");
      }
  }
  private void groupInviteUser(GroupInviteUser groupInviteUser) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, groupInviteUser, timeout);
    try {
      String result = (String) Await.result(answer, timeout.duration());
      System.out.println(result);
    } catch (Exception e) {
      System.out.println("server is offline! try again later!");
    }

  }

  private void groupLeave(GroupLeave groupLeave) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, groupLeave, timeout);
    try {
      String result = (String) Await.result(answer, timeout.duration());
      if (result.equals("not found!")) {
        System.out.println(this.userName + " is not in " + groupLeave.groupName+"!");
      } else { //TODO - not sure if need to print or get this by the group broadCast
        if(result.equals("coadmin exit!"))
          System.out.println(this.userName + " is removed from co-admin list in " + groupLeave.groupName+"!");
        else
          System.out.println(this.userName+" left group: "+groupLeave.sourceUserName);
      }
    } catch (Exception e) {
      System.out.println("server is offline! try again later!");
    }


  }
  private void connect(Connect connect) {
      Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
      Future<Object> answer = Patterns.ask(server, connect, timeout);
      try{
        String result = (String) Await.result(answer, timeout.duration());
        if(result.equals(connect.userName + " is in use!")){
          System.out.println(result);
        }
        else{
          this.userName = connect.userName;
          System.out.println(result);
        }
      }
      catch (Exception e){
        System.out.println("server is offline!");
      }
    }

    private void disConnect(DisConnect disConnect) {
        Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
        Future<Object> answer = Patterns.ask(server, disConnect, timeout);
        try{
          String result = (String) Await.result(answer, timeout.duration());
          if((disConnect.userName.equals(this.userName)) && result.equals(disConnect.userName + " has been disconnected successfully!")){
            System.out.println(result);
          }
          else{
            System.out.println(disConnect.userName + " failed to disconnect");
          }
        }
        catch (Exception e){
          System.out.println("server is offline! try again later!");
        }
  }

  private void groupCreate(GroupCreate groupCreate){
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer = Patterns.ask(server, groupCreate, timeout);
    try{
      String result = (String) Await.result(answer, timeout.duration());
      System.out.println(result);
      }
    catch (Exception e){
      System.out.println("server is offline! try again later!");
    }
  }

  private void sendMessage(SendMessage message) {
    String[] splitUserport =  message.sendTo.split(":");
    ActorSelection sendTo = getContext().actorSelection("akka://System@127.0.0.1:"+splitUserport[1]+"/user/" + splitUserport[0]); // todo: CHANGE PATH!!
    if (message instanceof SendFileMessage) {
      this.sendFileMessage(sendTo, (SendFileMessage) message);
    }
    else{
      sendTextMessage(sendTo, (SendTextMessage) message);
    }
  }

  private void sendFileMessage(ActorSelection sendTo, SendFileMessage message) {
    try {
      File file = new File(message.path);
      byte[] bytesArray = new byte[(int) file.length()];
      FileInputStream fis = new FileInputStream(file); //todo: THIS SHOULD BE IN THE MAIN!!!!
      fis.read(bytesArray); //read file into bytes[]
      fis.close();
      sendTo.tell(new ReceiveFileMessage(this.userName,"user", bytesArray), getSelf());
    }
    catch (Exception e){
      System.out.println(message.path + " does not exist!");
    }
  }

  private void sendTextMessage(ActorSelection sendTo, SendTextMessage message) {
    try{
      sendTo.tell(new ReceiveTextMessage(this.userName,"user", message.message), getSelf());
    }
    catch (Exception e){
      System.out.println(message.sendTo + " does not exist!");
    }
  }

  private void receiveMessage(ReceiveMessage message) {
    Date date = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    if (message instanceof ReceiveFileMessage) {
      this.receiveFileMessage(formatter.format(date), (ReceiveFileMessage) message);
    }
    else{
      receiveTextMessage(formatter.format(date), (ReceiveTextMessage) message);
    }
  }


  private void receiveFileMessage(String date, ReceiveFileMessage message) {
    try {

      File file = new File("files/");
      OutputStream os = new FileOutputStream(file);
      os.write(message.file);
      System.out.printf("[%s][%s][%s] File received: /files\n", date, message.userOrGroup, message.sendFrom);
      os.close();
    }
    catch (Exception e){
      System.out.println("failed to convert file");
    }
  }

  private void receiveTextMessage(String date, ReceiveTextMessage message) {
    System.out.printf("[%s][%s][%s] %s\n", date, message.userOrGroup, message.sendFrom, message.message);
  }

  //-------------------------------------------------------------------------------

}
