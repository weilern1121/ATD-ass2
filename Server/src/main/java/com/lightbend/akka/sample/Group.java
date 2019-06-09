package com.lightbend.akka.sample;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.lightbend.akka.sample.Messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import java.util.HashMap;
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
        this.groupUsers.put(adminName, new GroupUser(ADMIN, adminRef));//insert group admin during the group creation
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
                .match(ResponseToGroupInviteUser.class, this::responseToGroupInviteUser)
                .build();
    }

    //got to this func from server when checking validation of the groupInvite
    private void handleGroupInviteUser(GroupInviteUser invitation) {
        GroupUser targetUser = groupUsers.get(invitation.targetUserName);
        GroupUser sourceUser = groupUsers.get(invitation.sourceUserName);
        String message="";
        if(targetUser != null)
            message = invitation.targetUserName + " is already group!";
        else if ((sourceUser==null) || ((sourceUser.state != State.COADMIN) && (sourceUser.state != ADMIN)))
            message = "You are neither an admin nor a co-admin of " + groupName + "!";
        else
            message = "";
        getSender().tell(message, getSelf());
    }
    //handles both add&remove Coad , functionality options are in the field requestType
    private String handleGroupCoAdmin(GroupCoAdmin CoadRequest, GroupUser targetUser) {
        String response = "";
        boolean alreadyFLAG=true;
        if (CoadRequest.requestType.equals("add")) {
            if(groupUsers.get(CoadRequest.targetUserName).state == State.COADMIN)
                alreadyFLAG=false;
            groupUsers.get(CoadRequest.targetUserName).setState(State.COADMIN);
            //send message to target
            if(alreadyFLAG) {
                response = "You have been promoted to co-admin in " + CoadRequest.groupName + "!";
                targetUser.actorRef.tell(response, getSelf());
                response = "Succeed - promoted " + CoadRequest.sourceUserName + " to COADMIN!"; //set response to source
            }
            else{
                response = CoadRequest.sourceUserName + " is already COADMIN!"; //set response to source
            }
        }
        //if true - demote target to co-admin
        if (CoadRequest.requestType.equals("remove")) {
            if(groupUsers.get(CoadRequest.targetUserName).state != State.COADMIN)
                alreadyFLAG=false;
            groupUsers.get(CoadRequest.targetUserName).setState(State.USER);

            if(alreadyFLAG) {
                //send message to target
                response = "You have been demoted to user in " + CoadRequest.groupName + "!";
                targetUser.actorRef.tell(response, getSelf());
                response = "Succeed - demoted " + CoadRequest.targetUserName + " back to USER!";
            }
            else{
                response = CoadRequest.targetUserName + " is not COADMIN!";
            }
        }
        return response;
    }

    //got here after user accept groupInvite
    private void responseToGroupInviteUser (ResponseToGroupInviteUser responseToGroupInviteUser){
        String userName = responseToGroupInviteUser.userName;
        ActorSelection userRef = getActorByName(userName);
        this.groupUsers.put(userName, new GroupUser(State.USER, userRef));
        userRef.tell("Welcome to " + groupName +"!", getSelf());
    }


    //general checks for several extending funcs, after checks call the specific handler
    private void basicGroupAdminAction(BasicGroupAdminAction basicGroupAdminAction){
        GroupUser targetUser = groupUsers.get(basicGroupAdminAction.targetUserName);
        GroupUser sourceUser = groupUsers.get(basicGroupAdminAction.sourceUserName);
        String message="";
        if(targetUser == null)
            message = basicGroupAdminAction.targetUserName + " does not exist!";
        else if ((sourceUser==null) || ((sourceUser.state != State.COADMIN) && (sourceUser.state != ADMIN)))
            message = "You are neither an admin nor a co-admin of " + groupName + "!";
        else {//got here- check ok -> send to specific handler
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
        if (user == null) {//validation check
            getSender().tell(String.format("You are not part of %s!", groupName), getSelf());
            return;
        }
        else if (muteService.isMute(sender)){//mute validation check
            getSender().tell(String.format("You are Muted in %s!", groupName), getSelf());
            return;
        }
        getSender().tell("", getSelf());
        sendToAll(message);
    }

    //broadcast func in group
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
        if(user == null) {//validation check
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
