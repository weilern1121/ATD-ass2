package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.lightbend.akka.sample.Messages.Connect;
import com.lightbend.akka.sample.Messages.DisConnect;
import com.lightbend.akka.sample.Messages.GroupCreate;
import com.lightbend.akka.sample.Messages.SendTextMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;

public class ServerMain {
  public static void main(String[] args) {
    Config conf = ConfigFactory.load("application.conf");
    final ActorSystem system = ActorSystem.create("System",conf);
//    ActorRefProvider check = system.provider();
    try {
      ActorRef server = system.actorOf(Server.props(), "Server");

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ioe) {
    } finally {
      system.terminate();
    }
  }
}
