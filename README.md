
## ATD-ass2- Actor model- akka

Name: Tomer Kulla
ID: 308235944
Name: Noam Weiler
ID: 203570619 </br>

## ABOUT: <br />

	Text-based messages application.
	Support all the commands as described at: 
	https://www.cs.bgu.ac.il/~majeek/atd/192/assignments/2/


## ACTORS: <br />
* User - fields:<br />
   - Username <br />
   - Server ActorRef (refrence to the main app server) <br />
	
* Server - fields:<br />
   - Users map, holds each username with his full address. <br />
   - Groups map, holds each groupname with her ActorRef. <br />
	
* Group - fields: <br />
   - GroupName <br />
   - groupUsers map, holds each group user's username with his ActoRef(Actor Selection in this case) and his
   - state(ADMIN,COADMIN or USER). <br />
   - Server ActorRef (refrence to the main app server) <br />
   - MuteService, a singel threaded app that controls and holds the info regrading muted group members. <br /> <br />

## ACTORS DESIGN: <br />

* The main app server has a singlton Server Actor.
* Each User, once he has conncted to the server, has his own unique User Actor.
* Users Actors can connect directly with each other (e.g when sending text/file messages),
	  and also to the server (to do group actions etc).
* Groups are created by the Server Actor, and they are the server's childrens (not the system's),
	  groups receive messages only from the Server Actor.


## MESSAGES: <br />

* Each command has described in the assignment page has her unique Object Message.
* The messages are passed between the actors and affect thier behave as described in the assignment.
* Each Message contains her relevence fields so she can pass all her relevence data.
* Message Objects:<br />
   - Connect
   - DisConnect
   - Get Address (not from the assignment page, it's to get the user's address from the server)
   - Send Message
   - GroupCreate
   - GroupLeave
   - GroupInviteUser
   - GroupCOAdmin
   - GroupUserRemove
   - GroupUserMute
   - GroupUserUnMute 	
	

## HOW TO RUN: <br />

* To Start Server:<br />
	inside Server folder-
	run:
 1. mvn compile
 2. mvn clean install
 3. mvn exec:exec -Dhost=<server_host> -Dport=<server_port>
 EXAMPLE: mvn exec:exec -Dhost=127.0.0.1 -Dport=8080.

* To Start User:<br />
	inside User folder-
	run:
1. mvn compile
2. mvn clean install
3. mvn exec:exec mvn exec:exec -Duser_host=<user_host> -Dserver_address=<server_full_adress> 
EXAMPLE: mvn exec:exec -Duser_host=127.0.0.1 -Dserver_address=127.0.0.1:8080


