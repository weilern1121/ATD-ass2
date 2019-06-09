package com.lightbend.akka.sample;

public class Messages {

    static public class Connect implements java.io.Serializable{
        public final String userName;
        public final String address;

        public Connect(String userName, String address) {
            this.address = address;
            this.userName = userName;
        }
    }

    static public class DisConnect implements java.io.Serializable{
        public final String userName;

        public DisConnect(String userName) {
            this.userName = userName;
        }
    }

    static public class GetAddress implements java.io.Serializable{
        public final String userName;

        public GetAddress(String userName) {
            this.userName = userName;
        }
    }

    // -------------- Send Message

    static public class SendMessage implements java.io.Serializable{
        public final String sendTo;

        public SendMessage(String sendTo) {
            this.sendTo = sendTo;
        }
    }

    static public class SendTextMessage extends SendMessage implements java.io.Serializable{
        public final String message;

        public SendTextMessage(String sendTo, String message) {
            super(sendTo);
            this.message = message;
        }
    }

    static public class SendFileMessage extends SendMessage implements java.io.Serializable{
        public final byte[] file;

        public SendFileMessage(String sendTo, byte[] file) {
            super(sendTo);
            this.file = file;
        }
    }


// -------------- Receive Message

    static public class ReceiveMessage implements java.io.Serializable{
        public String sendFrom;
        public String userOrGroup;

        public ReceiveMessage(String sendFrom, String userOrGroup) {
            this.sendFrom = sendFrom;
            this.userOrGroup = userOrGroup;
        }
    }

    static public class ReceiveTextMessage extends ReceiveMessage implements java.io.Serializable{
        public final String message;

        public ReceiveTextMessage(String sendFrom, String userOrGroup, String message) {
            super(sendFrom, userOrGroup);
            this.message = message;
        }
    }

    static public class ReceiveFileMessage extends ReceiveMessage implements java.io.Serializable{
        public final byte[] file;

        public ReceiveFileMessage(String sendFrom, String userOrGroup, byte[] file) {
            super(sendFrom, userOrGroup);
            this.file = file;
        }
    }

    static public class GroupLeave implements java.io.Serializable{
        public final String groupName;
        public final String sourceUserName;

        public GroupLeave(String groupName, String sourceUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
        }
    }
    static public class ReceiveGroupInviteUser implements java.io.Serializable{
        public GroupInviteUser invite;

        public ReceiveGroupInviteUser(GroupInviteUser invite) {
            this.invite = invite;
        }
    }

    static public class GroupInviteUser implements java.io.Serializable{
        public final String groupName;
        public final String sourceUserName;
        public final String targetUserName;

        public GroupInviteUser(String groupName, String sourceUserName, String targetUserName) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
        }
    }


    static public class ResponseToGroupInviteUser implements java.io.Serializable{
        public final String groupName;
        public final String userName;

        public ResponseToGroupInviteUser(String groupName, String userName) {
            this.groupName = groupName;
            this.userName = userName;
        }
    }

    static public class GroupCreate implements java.io.Serializable{
        public final String groupName;
        public String adminName;

        public GroupCreate(String groupName, String adminName) {
            this.groupName = groupName;
            this.adminName = adminName;
        }
    }

    static public class GroupCoAdmin extends BasicGroupAdminAction implements java.io.Serializable{
        public final String requestType;
        public GroupCoAdmin(String groupName, String targetUserName, String source,String type) {
            super(groupName,targetUserName,source);
            this.requestType=type;
        }
    }



    static public class GroupMessage implements java.io.Serializable{
        public final ReceiveMessage message;

        public GroupMessage(ReceiveMessage message) {
            this.message = message;
        }
    }

    //----------------------------------------BasicGroupAdminAction

    static public class BasicGroupAdminAction implements java.io.Serializable{
        public final String groupName;
        public final String targetUserName;
        public String sourceUserName;

        public BasicGroupAdminAction(String groupName, String targetUserName, String sourceUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
            this.sourceUserName = sourceUserName;
        }
    }

    static public class GroupUserRemove extends BasicGroupAdminAction implements java.io.Serializable{
        public GroupUserRemove(String groupName, String targetUserName) {
            super(groupName, targetUserName, null);
        }
    }

    static public class GroupUserMute extends BasicGroupAdminAction implements java.io.Serializable{
        public long time;
        public GroupUserMute(String groupName, String targetUserName, long time) {
            super(groupName, targetUserName, null);
            this.time = time;
        }
    }

    static public class GroupUserUnMute extends BasicGroupAdminAction implements java.io.Serializable{
        public GroupUserUnMute(String groupName, String targetUserName) {
            super(groupName, targetUserName, null);
        }
    }

    //----------------------------------------BasicGroupAction END
}