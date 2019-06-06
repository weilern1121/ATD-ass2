package com.lightbend.akka.sample;

import akka.actor.ActorRef;

public class Messages {

    static public class Connect implements java.io.Serializable{
        public final String userName;

        public Connect(String userName) {
            this.userName = userName;
        }
    }

    static public class DisConnect {
        public final String userName;

        public DisConnect(String userName) {
            this.userName = userName;
        }
    }

    // -------------- Send Message

    static public class SendMessage {
        public final String sendTo;

        public SendMessage(String sendTo) {
            this.sendTo = sendTo;
        }
    }

    static public class SendTextMessage extends SendMessage {
        public final String message;

        public SendTextMessage(String sendTo, String message) {
            super(sendTo);
            this.message = message;
        }
    }

    static public class SendFileMessage extends SendMessage {
        public final String path;

        public SendFileMessage(String sendTo, String path) {
            super(sendTo);
            this.path = path;
        }
    }


// -------------- Receive Message

    static public class ReceiveMessage {
        public String sendFrom;
        public String userOrGroup;

        public ReceiveMessage(String sendFrom, String userOrGroup) {
            this.sendFrom = sendFrom;
            this.userOrGroup = userOrGroup;
        }
    }

    static public class ReceiveTextMessage extends ReceiveMessage {
        public final String message;

        public ReceiveTextMessage(String sendFrom, String userOrGroup, String message) {
            super(sendFrom, userOrGroup);
            this.message = message;
        }
    }

    static public class ReceiveFileMessage extends ReceiveMessage {
        public final byte[] file;

        public ReceiveFileMessage(String sendFrom, String userOrGroup, byte[] file) {
            super(sendFrom, userOrGroup);
            this.file = file;
        }
    }

    static public class GroupLeave {
        public final String groupName;
        public final String sourceUserName;

        public GroupLeave(String groupName, String sourceUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
        }
    }

    static public class GroupInviteUser {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;
        public ActorRef targetActorRef;

        public GroupInviteUser(String groupName, String sourceUserName, String targetUserName, ActorRef ref) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.targetActorRef = ref;
        }
        public void setActorRef(ActorRef ref){
            this.targetActorRef=ref;
        }
    }

    static public class AskTargetToGroupInviteUser extends GroupInviteUser {
        public AskTargetToGroupInviteUser(String groupName, String sourceUserName, String targetUserName, ActorRef ref) {
            super(groupName, sourceUserName, targetUserName, ref);
        }
    }


    static public class ResponseToGroupInviteUser {
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;
        public final String answer;
        public final ActorRef targetActorRef;

        public ResponseToGroupInviteUser(String groupName, String sourceUserName, String targetUserName, String ans, ActorRef actorref) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.answer = ans;
            this.targetActorRef = actorref;
        }
    }

    static public class GroupCreate {
        public final String groupName;
        public String adminName;

        public GroupCreate(String groupName, String adminName) {
            this.groupName = groupName;
            this.adminName = adminName;
        }
    }

    static public class GroupCoAdmin extends BasicGroupAdminAction{
        public final String requestType;
        public GroupCoAdmin(String groupName, String targetUserName, String source,String type) {
            super(groupName,targetUserName,source);
            this.requestType=type;
        }
    }



    static public class GroupMessage{
        public final ReceiveMessage message;

        public GroupMessage(ReceiveMessage message) {
            this.message = message;
        }
    }

    //----------------------------------------BasicGroupAdminAction

    static public class BasicGroupAdminAction{
        public final String groupName;
        public final String targetUserName;
        public String sourceUserName;

        public BasicGroupAdminAction(String groupName, String targetUserName, String sourceUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
            this.sourceUserName = sourceUserName;
        }
    }

    static public class GroupUserRemove extends BasicGroupAdminAction{
        public GroupUserRemove(String groupName, String targetUserName) {
            super(groupName, targetUserName, null);
        }
    }

    static public class GroupUserMute extends BasicGroupAdminAction{
        public long time;
        public GroupUserMute(String groupName, String targetUserName, long time) {
            super(groupName, targetUserName, null);
            this.time = time;
        }
    }

    static public class GroupUserUnMute extends BasicGroupAdminAction{
        public GroupUserUnMute(String groupName, String targetUserName) {
            super(groupName, targetUserName, null);
        }
    }

    //----------------------------------------BasicGroupAction END
}