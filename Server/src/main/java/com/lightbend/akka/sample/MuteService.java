package com.lightbend.akka.sample;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.lightbend.akka.sample.Messages;
import java.util.LinkedList;
import java.util.concurrent.*;

public class MuteService{
    public LinkedList<String> mutedUsers;
    ScheduledExecutorService schd;

    public MuteService() {
        mutedUsers = new LinkedList<>();
        schd = Executors.newSingleThreadScheduledExecutor();
    }

    public void mute(Messages.GroupUserMute muteMessage, ActorSelection targetActorRef) {
        Runnable mute = () -> {
            mutedUsers.add(muteMessage.targetUserName);
            targetActorRef.tell(new Messages.ReceiveTextMessage(muteMessage.sourceUserName, muteMessage.groupName,
                            "You have been muted for "+muteMessage.time+" in "+muteMessage.groupName+" by "+muteMessage.sourceUserName+"!"),
                    ActorRef.noSender());
        };
        Runnable unMute = () -> {
            if(mutedUsers.remove(muteMessage.targetUserName))
                targetActorRef.tell(new Messages.ReceiveTextMessage(muteMessage.sourceUserName, muteMessage.groupName,
                        "You have been unmuted! Muting time is up!"),ActorRef.noSender());
        };

        schd.schedule(mute,0, TimeUnit.SECONDS);
        schd.schedule(unMute, muteMessage.time, TimeUnit.SECONDS);
    }

    public void unMute(Messages.GroupUserUnMute unMuteMessage, ActorSelection targetActorRef) {
        Runnable unMuteTask = () -> {
            if(mutedUsers.remove(unMuteMessage.targetUserName))
                targetActorRef.tell(new Messages.ReceiveTextMessage(unMuteMessage.sourceUserName, unMuteMessage.groupName,
                        "You have been unmuted by "+unMuteMessage.sourceUserName+"!"),ActorRef.noSender());
        };
        schd.schedule(unMuteTask,0, TimeUnit.SECONDS);
    }

    public Boolean isMute(String user) {
        try {
            Callable<Boolean> check = () -> {return mutedUsers.contains(user);};
            Future<Boolean> answer = schd.schedule(check,0,TimeUnit.SECONDS);
            return answer.get();
        } catch (Exception e) {
            return false;
        }
    }

    public void shutDown(){
        schd.shutdown();
    }

}