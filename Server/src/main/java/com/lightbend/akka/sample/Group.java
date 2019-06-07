package com.lightbend.akka.sample;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;

import static com.lightbend.akka.sample.Group.State.ADMIN;


public class Group extends AbstractActor {
    private String groupName;
    private final HashMap<String, GroupUser> groupUsers;
    private final MuteService muteService;
    private final ActorSelection server;

    public Group(String groupName, String adminName) {
        this.groupName = groupName;
        this.muteService = new MuteService();
        this.groupUsers = new HashMap<>();
        this.server = getContext().actorSelection("akka://System@"+ServerMain.host+":"+ServerMain.port+"/user/Server");
        ActorSelection adminRef = getActorByName(adminName);
        this.groupUsers.put(adminName, new GroupUser(ADMIN, adminRef));

        //-------------for tests
        ActorSelection userRef = getActorByName("t");
        this.groupUsers.put("t", new GroupUser(State.USER, userRef));
    }

    static public Props props(String groupName, String admin) {
        return Props.create(Group.class, () -> new Group(groupName, admin));
    }

    enum State {
        ADMIN,
        COADMIN,
        USER,
    }
    static public class GroupUser {
        public State state;
        public final ActorSelection actorRef;

        public GroupUser(State state, ActorSelection actorRef) {
            this.state = state;
            this.actorRef = actorRef;
        }

        public void setState(State state) {
            this.state = state;
        }
    }



    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GroupMessage.class, this::handleMessage)
                .match(GroupLeave.class, this::handleGroupLeave)
                .match(BasicGroupAdminAction.class, this::basicGroupAdminAction)
                .match(GroupInviteUser.class, this::handleGroupInviteUser)
//                .match(ResponseToGroupInviteUser.class, this::handleResponseToGroupInviteUser)
                .build();
    }
    //got to this func from server when checking validation of the groupInvite
    private void handleGroupInviteUser(GroupInviteUser invitation) {
//        //use flag to make sure that after first error this will be the output error
//        boolean ErrorFLAG = true;
//        String response = "";
//        //check source permission
//        if (groupUsers.get(invitation.sourceUserName).state == State.USER ||
//                groupUsers.get(invitation.sourceUserName).state == State.MUTED) {
//            response = "You are neither an admin nor a co-admin of " + invitation.groupName + "!";
//            ErrorFLAG = false;
//        }
//        //check if source is in group
//        if (ErrorFLAG && !groupUsers.containsKey(invitation.sourceUserName)) {
//            response = invitation.sourceUserName + "is not in " + invitation.groupName + "!";
//            ErrorFLAG = false;
//        }
//        //check if target is NOT in group
//        if (ErrorFLAG && groupUsers.containsKey(invitation.targetUserName)) {
//            response = invitation.targetUserName + "is already in " + invitation.groupName + "!";
//            ErrorFLAG = false;
//        }
//        if (ErrorFLAG) {//if true ->sent to target the request and future for answer
//            Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
//            AskTargetToGroupInviteUser targetRequest = new AskTargetToGroupInviteUser(invitation.groupName,
//                    invitation.sourceUserName, invitation.targetUserName, null);
//            scala.concurrent.Future<Object> answer = Patterns.ask(invitation.targetActorRef, targetRequest, timeout);
//            try {
//                response = (String) Await.result(answer, timeout.duration());
//                switch (response) {
//                    case "Yes":
//                    case "YES":
//                    case "yes":
//                        //if got here -add target to group
//                        groupUsers.put(invitation.targetUserName, new GroupUser(State.USER, invitation.targetActorRef));
//                        //send msg to target
//                        response = "Welcome to " + invitation.groupName + "!";
//                        invitation.targetActorRef.tell(response, getSelf());
//                        response = invitation.targetUserName + " added successfully to " + invitation.groupName + "!";
//                        break;
//                    case "no":
//                    case "NO":
//                    case "No":
//                        response = invitation.targetUserName + " declined to join to group " + invitation.groupName + "!";
//                        break;
//                    default:
//                        response = invitation.targetUserName + " did NOT added to group, his respond to request was: " + response;
//                        break;
//                }
//            } catch (Exception e) {
//                System.out.println(e);
//            }
//
//        }
//        getSender().tell(response, getSelf());
    }
    private String handleGroupCoAdmin(GroupCoAdmin CoadRequest, GroupUser targetUser) {
        String response = "";
        if (CoadRequest.requestType.equals("add")) {
            groupUsers.get(CoadRequest.targetUserName).setState(State.COADMIN);
            //TODO - <targetusername> will be be added to the <groupname> co-admin list ??
            //send message to target
            response = "You have been promoted to co-admin in " + CoadRequest.groupName + "!";
            targetUser.actorRef.tell(response, getSelf());
            response = "Succeed - promoted "+CoadRequest.sourceUserName+" to COADMIN!"; //set response to source
        }
        //if true - demote target to co-admin
        if (CoadRequest.requestType.equals("remove")) {
            groupUsers.get(CoadRequest.targetUserName).setState(State.USER);
            //TODO - <targetusername> will be be removed to the <groupname> co-admin list ??
            //send message to target
            response = "You have been demoted to user in " + CoadRequest.groupName + "!";
            targetUser.actorRef.tell(response, getSelf());
            response = "Succeed - demoted "+CoadRequest.targetUserName+" back to USER!";
        }
//        getSender().tell(response, getSelf());
        return response;
    }

//    private void handleResponseToGroupInviteUser (ResponseToGroupInviteUser invitationConfirm){
//        groupUsers.put(invitationConfirm.targetUserName,
//                new GroupUser(State.USER,invitationConfirm.targetActorRef));
//    }


    private void basicGroupAdminAction(BasicGroupAdminAction basicGroupAdminAction){
        GroupUser targetUser = groupUsers.get(basicGroupAdminAction.targetUserName);
        GroupUser sourceUser = groupUsers.get(basicGroupAdminAction.sourceUserName);
        String message="";
        if(targetUser == null)
            message = basicGroupAdminAction.targetUserName + " does not exist!";
        else if ((sourceUser==null) || ((sourceUser.state != State.COADMIN) && (sourceUser.state != ADMIN)))
            message = "You are neither an admin nor a co-admin of " + groupName + "!";
        else {
            if (basicGroupAdminAction instanceof GroupUserRemove)
                message = groupUserRemove((GroupUserRemove) basicGroupAdminAction, targetUser);
            if (basicGroupAdminAction instanceof GroupUserMute)
                message = groupUserMute((GroupUserMute) basicGroupAdminAction, targetUser);
            if (basicGroupAdminAction instanceof GroupUserUnMute)
                message = groupUserUnMute((GroupUserUnMute) basicGroupAdminAction, targetUser);
            if (basicGroupAdminAction instanceof GroupCoAdmin)
                message = handleGroupCoAdmin((GroupCoAdmin) basicGroupAdminAction, targetUser);
        }
        getSender().tell(message, getSelf());
    }

    private String groupUserMute(GroupUserMute groupUserMute, GroupUser targetUser) {
        muteService.mute(groupUserMute, targetUser.actorRef);
        return "";
    }

    private String groupUserUnMute(GroupUserUnMute groupUserUnMute, GroupUser targetUser) {
        if(! muteService.isMute(groupUserUnMute.targetUserName))
            return groupUserUnMute.targetUserName+" is not muted!";
        muteService.unMute(groupUserUnMute, targetUser.actorRef);
        return "";
    }

    private String groupUserRemove(GroupUserRemove groupUserRemove, GroupUser targetUser) {
        ReceiveTextMessage messagToTarget = new ReceiveTextMessage(groupUserRemove.sourceUserName, groupName,
                "You have been removed from " + groupName + " by " + groupUserRemove.targetUserName + "!");
        targetUser.actorRef.tell(messagToTarget, getSelf());
        groupUsers.remove(groupUserRemove.targetUserName);
        return "";
    }

    private void handleMessage(GroupMessage groupMessage) {
        ReceiveMessage message = groupMessage.message;
        String sender = message.sendFrom;
        GroupUser user = groupUsers.get(sender);
        if (user == null) {
            getSender().tell(String.format("You are not part of %s!", groupName), getSelf());
            return;
        }
        else if (muteService.isMute(sender)){
            getSender().tell(String.format("You are Muted in %s!", groupName), getSelf());
            return;
        }
        getSender().tell("", getSelf());
        sendToAll(message);
    }

    private void sendToAll(ReceiveMessage message) {
        GroupUser user;
        for (Map.Entry<String, GroupUser> entry : groupUsers.entrySet()) {
            user = entry.getValue();
            user.actorRef.tell(message, getSelf());
        }
    }

    private void handleGroupLeave(GroupLeave groupLeaveReaquest){
        String response = "";
        //check that user is in group
        GroupUser user = groupUsers.get(groupLeaveReaquest.sourceUserName);
        if(user == null) {
            getSender().tell(String.format("You are not part of %s!", groupName), getSelf());
            return;
        }
        sendToAll(new ReceiveTextMessage(groupLeaveReaquest.sourceUserName, this.groupName,
                groupLeaveReaquest.sourceUserName + " has left " + groupLeaveReaquest.groupName + "!"));
        if(user.state == ADMIN){
            sendToAll(new ReceiveTextMessage(groupLeaveReaquest.sourceUserName, this.groupName,
                    groupLeaveReaquest.groupName + " admin has closed " + groupLeaveReaquest.groupName + "!"));
            response = "admin exit!";
        }
        groupUsers.remove(groupLeaveReaquest.sourceUserName);//remove from user list
        getSender().tell(response, getSelf());
    }

    private ActorSelection getActorByName(String userName){
        Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
        Future<Object> answer = Patterns.ask(server, new GetAddress(userName), timeout);
        try{
            String userAddress = (String) Await.result(answer, timeout.duration());
            if(userAddress.length() > 0)
                return getContext().actorSelection("akka://System@"+userAddress+"/user/"+userName);
            else {
                System.out.println(userName + "is not connected");
                return null;
            }
        }
        catch (Exception e){
            System.out.println("server is offline! try again later!");
            return null;
        }
    }
}
