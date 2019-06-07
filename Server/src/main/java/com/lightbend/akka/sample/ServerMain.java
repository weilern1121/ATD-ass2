package com.lightbend.akka.sample;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.lightbend.akka.sample.Messages.Connect;
import com.lightbend.akka.sample.Messages.DisConnect;
import com.lightbend.akka.sample.Messages.GroupCreate;
import com.lightbend.akka.sample.Messages.SendTextMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerMain {
    public static String host;
    public static String port;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("MISSING HOST AND PORTS!");
            return;
        }
        host = args[0];
        port = args[1];
        Config conf = handleConfig();

        final ActorSystem system = ActorSystem.create("System", conf);

        try {
            ActorRef server = system.actorOf(Server.props(), "Server");
            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
        } catch (IOException ioe) {
        } finally {
            system.terminate();
        }
    }

    private static Config handleConfig() {
        try {
            String configPath = "src/main/resources/application.conf";
            new File(configPath);
            String config = "akka {\r\n" +
                    "loglevel = \"OFF\"\r\n" +
                    "  actor {\r\n" +
                    "  serialize-messages = on\r\n" +
                    "  serializers {\r\n" +
                    "        java = \"akka.serialization.JavaSerializer\"\r\n" +
                    "  }\r\n" +
                    "    provider = \"akka.remote.RemoteActorRefProvider\"\r\n" +
                    "    warn-about-java-serializer-usage = false\r\n" +
                    "  }\r\n" +
                    "  remote {\r\n" +
                    "    artery{\r\n" +
                    "        enabled = on\r\n" +
                    "        transport = tcp\r\n" +
                    "        canonical.hostname = \"" + host + "\"\r\n" +
                    "        canonical.port = " + port + "\r\n" +
                    "    }\r\n" +
                    "    enabled-transports = [\"akka.remote.netty.tcp\"]\r\n" +
                    "    netty.tcp {\r\n" +
                    "      hostname = \"127.0.0.1\"\r\n" +
                    "      port = 8082\r\n" +
                    "    }\r\n" +
                    "  }\r\n" +
                    "}\r\n";
            Path path = Paths.get(configPath);
            BufferedWriter writer = Files.newBufferedWriter(path);
            writer.write(config);
            writer.flush();
            writer.close();
            return ConfigFactory.load("application.conf");
        } catch (Exception e) {
            return null;
        }
    }
}
