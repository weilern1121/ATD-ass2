package com.lightbend.akka.sample;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
            .match(ResponseToGroupInviteUser.class, this::ResponseToGroupInviteUser)
            .match(BasicGroupAdminAction.class, this::basicGroupAdminAction)
            .build();
  }

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
    if (group == null) {
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
    if (group == null) {
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
    if (users.containsKey(disConnect.userName)) {

      for (Map.Entry<String, ActorRef> entry : groups.entrySet()) {
        groupLeave(new GroupLeave(entry.getKey(), disConnect.userName), true);
      }
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
      ActorRef group =
              getContext().actorOf(Group.props(groupCreate.groupName, groupCreate.adminName), groupCreate.groupName);
      groups.put(groupCreate.groupName, group);
    }
    getSender().tell(response, getSelf());
  }


  private void groupLeave(GroupLeave groupLeave, boolean disconnect) {
    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
    Future<Object> answer;

    ActorRef group = groups.get(groupLeave.groupName);
    if (group == null) {
      if(!disconnect)
        getSender().tell(groupLeave.groupName + " does not exist!", getSelf());
      return;
    }
    answer = Patterns.ask(group, groupLeave, timeout);
    try {
      String response = (String) Await.result(answer, timeout.duration());
      if(response.equals("admin exit!")){
        groups.remove(groupLeave.groupName);
        getContext().stop(group);
      }
      if(!disconnect)
        getSender().tell("", getSelf());
    }
    catch (Exception e) {
      System.out.println("groupLeave ERROR: " + e);
    }
  }

  private void groupInviteUser(GroupInviteUser groupInviteUser) {
//    String response = "";
//    boolean defaultFLAG = true;
//    //check that <groupname> exist
//    if (!groups.containsKey(groupInviteUser.groupName))
//      response = "group does not exist!";
//    //heck that <targetUserName> exist
//    if (!users.containsKey(groupInviteUser.targetUserName))
//      response = "target does not exist!";
//
//    //check <sourceusername> not admin or co-admin ->send to Group!!
//    Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
//    Future<Object> groupResp = Patterns.ask(groups.get(groupInviteUser.groupName), groupInviteUser, timeout);
//    try {
//      String result = (String) Await.result(groupResp, timeout.duration());
//      switch (result){
//        case "not admin or co-admin!":
//          response = "not admin or co-admin!";
//          break;
//        case "not in group!":
//          response = "not in group!";
//          break;
//        case "target already in group!":
//          response="target already in group!";
//          break;
//        default: //this case is that all check in group were ok->send the request to the targetUser
//          users.get(groupInviteUser.targetUserName).tell(new ResponseToGroupInviteUser(
//                  groupInviteUser.groupName, groupInviteUser.sourceUserName,
//                  groupInviteUser.targetUserName, response,users.get(groupInviteUser.targetUserName)), getSelf());
//          defaultFLAG=false; //flag off for the .tell() outside the switch
//          break;
//      }
//
//    } catch (Exception e) {
//      System.out.println("Error in GroupLeave!");
//    }
//    // TODO -makesure that getSender() is the return-call user and not the group!
//    if(defaultFLAG) //if true- there was an error - sent it to the requested user
//      getSender().tell(response, getSelf());
  }

  private void ResponseToGroupInviteUser(ResponseToGroupInviteUser backFromUser) {
//    String response;
//    if (backFromUser.answer.equals("yes")) {
//      //if true-> notify group to add target, send confirmation to source and target
//      groups.get(backFromUser.groupName).tell(backFromUser, getSelf()); //backFromUser hold the target ActorRef
//      response = "welcome!";
//      users.get(backFromUser.targetUserName).tell(response, getSelf());
//      response = "done!";
//      users.get(backFromUser.sourceUserName).tell(response, getSelf());
//    }
//    else{
//      if (backFromUser.answer.equals("no")) {
//        response="declined invitation!";
//        users.get(backFromUser.sourceUserName).tell(response, getSelf());
//      }else {
//        //otherwise - send error to sourceUserName
//        response = "invite error!";
//        users.get(backFromUser.sourceUserName).tell(response, getSelf());
//      }
//    }
//  }
//}
  }
}