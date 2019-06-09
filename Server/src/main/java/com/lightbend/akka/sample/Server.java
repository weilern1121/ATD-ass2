package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;


public class Server extends AbstractActor {
  private final HashMap<String, ActorRef> groups;
  private final HashMap<String, String> users;

  static public Props props() {
    return Props.create(Server.class, () -> new Server());
  }


  public Server() {
    this.groups = new HashMap<>();
    this.users = new HashMap<>();
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(Connect.class, this::connect)
            .match(GetAddress.class, this::getAddress)
            .match(DisConnect.class, this::disConnect)
            .match(GroupCreate.class, this::groupCreate)
            .match(GroupMessage.class, this::groupMessage)
            .match(GroupLeave.class, (GroupLeave groupLeave) -> groupLeave(groupLeave, false))
            .match(GroupInviteUser.class, this::groupInviteUser)
            .match(ResponseToGroupInviteUser.class, this::responseToGroupInviteUser)
            .match(BasicGroupAdminAction.class, this::basicGroupAdminAction)
            .build();
  }

  //used via group/user to get the actorSelect
  private void getAddress(GetAddress getAddress) {
    String userAdress = users.get(getAddress.userName);
    if(userAdress == null)
      getSender().tell("", getSelf());
    else
      getSender().tell(userAdress, getSelf());
  }

  private void basicGroupAdminAction(BasicGroupAdminAction basicGroupAdminAction) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;

    ActorRef group = groups.get(basicGroupAdminAction.groupName);
    if (group == null) {//validation check
      getSender().tell(basicGroupAdminAction.groupName + " does not exist!", getSelf());
      return;
    }
    answer = Patterns.ask(group, basicGroupAdminAction, timeout);
    try {
      String response = (String) Await.result(answer, timeout.duration());
      getSender().tell(response, getSelf());
    } catch (Exception e) {
      System.out.println("basicGroupAdminAction ERROR: " + e);
    }
  }

  private void groupMessage(GroupMessage groupMessage) {
    String response = "";
    ActorRef group = groups.get(groupMessage.message.userOrGroup);
    if (group == null) {//validation check
      response = groupMessage.message.userOrGroup + " does not exist!";
    }
    else {
      Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
      Future<Object> answer = Patterns.ask(group, groupMessage, timeout);
      try {
        response = (String) Await.result(answer, timeout.duration());
      } catch (Exception e) {
        System.out.println(e);
      }
    }
    getSender().tell(response, getSelf());
  }

  private void connect(Connect connect) {
    String response;
    if (users.containsKey(connect.userName)) {
      response = connect.userName + " is in use!";
    } else {
      response = connect.userName + " has connected successfully";
      users.put(connect.userName, connect.address);
    }
    getSender().tell(response, getSelf());
  }

  private void disConnect(DisConnect disConnect) {
    String response;
    if (users.containsKey(disConnect.userName)) {//validation check
      //itarate over the groups list and remove groups that disConnect.userName==group.ADMIN
      groups.entrySet().removeIf(e -> groupLeave(new GroupLeave(e.getKey(), disConnect.userName), true));
      //finally - remove user from user list
      users.remove(disConnect.userName);
      response = disConnect.userName + " has been disconnected successfully!";
    }
    else {
      response = disConnect.userName + " failed to disconnect";
    }
    getSender().tell(response, getSelf());
  }

  private void groupCreate(GroupCreate groupCreate) {
    String response;
    if (groups.containsKey(groupCreate.groupName)) {
      response = groupCreate.groupName + " already exists!";
    } else {
      response = groupCreate.groupName + " created successfully!";
      //create the actorRef for this new group
      ActorRef group =
              getContext().actorOf(Group.props(groupCreate.groupName, groupCreate.adminName), groupCreate.groupName);
      groups.put(groupCreate.groupName, group);
    }
    getSender().tell(response, getSelf());
  }


  private boolean groupLeave(GroupLeave groupLeave, boolean disconnect) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;
    boolean adminExit=false;
    ActorRef group = groups.get(groupLeave.groupName);
    if (group == null) {
      if(!disconnect)
        getSender().tell(groupLeave.groupName + " does not exist!", getSelf());
      return false;
    }
    answer = Patterns.ask(group, groupLeave, timeout);
    try {
      String response = (String) Await.result(answer, timeout.duration());
      if(response.equals("admin exit!")){
        if(!disconnect) //if true - should remove group from this func. else -remove via iteration in disconnect
          groups.remove(groupLeave.groupName);
        getContext().stop(group);//close actor
        adminExit = true;
      }
      if(!disconnect) //if from disconnect - should not send message to target
        getSender().tell(response, getSelf());
      return adminExit;
    }
    catch (Exception e) {
      System.out.println("groupLeave ERROR: " + e);
      return false;
    }
  }

  private void groupInviteUser(GroupInviteUser groupInviteUser) {
    String response;
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;
    String userAddress = users.get(groupInviteUser.targetUserName);
    if(userAddress == null) {//validation check
      getSender().tell(groupInviteUser.targetUserName + " does not exist!", getSelf());
      return;
    }
    ActorRef group = groups.get(groupInviteUser.groupName);
    if (group == null) {//validation check
      getSender().tell(groupInviteUser.groupName + " does not exist!", getSelf());
      return;
    }
    answer = Patterns.ask(group, groupInviteUser, timeout);
    try {
      response = (String) Await.result(answer, timeout.duration());
      if(response.length() == 0){ //if true - all check were passed ->send message to targetUser
        ActorSelection actorRef = getContext().
                actorSelection("akka://System@"+userAddress+"/user/"+groupInviteUser.targetUserName);
        actorRef.tell(new ReceiveGroupInviteUser(groupInviteUser), getSelf());
      }
      getSender().tell(response, getSelf());
    }
    catch (Exception e) {
      System.out.println("groupInviteUser ERROR: " + e);
    }
  }

  private void responseToGroupInviteUser(ResponseToGroupInviteUser responseToGroupInviteUser) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;

    ActorRef group = groups.get(responseToGroupInviteUser.groupName);
    if (group == null) {//validation check
      getSender().tell(responseToGroupInviteUser.groupName + " does not exist!", getSelf());
      return;
    }
    group.tell(responseToGroupInviteUser, getSelf());
  }
}