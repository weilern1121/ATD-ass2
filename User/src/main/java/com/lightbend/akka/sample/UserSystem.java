package com.lightbend.akka.sample;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

class UserSystem
{
    // static variable single_instance of type Singleton
    private static UserSystem single_instance = null;

    // variable of type String
    public ActorSystem system = null;

    // private constructor restricted to this class itself
    private UserSystem()
    {

    }

    // static method to create instance of Singleton class
    public static UserSystem getInstance()
    {
        if (single_instance == null)
            single_instance = new UserSystem();

        return single_instance;
    }
}
