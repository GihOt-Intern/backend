package com.server.game.config;

// import java.lang.reflect.Method;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// import com.server.game.handler.MoveHandler;
// import com.server.game.message.receive.DistanceReceive;
import com.server.game.ws.messageMapping.MessageDispatcher;

@Configuration
public class DispatcherConfig {

    @Bean
    public MessageDispatcher messageDispatcher() {
        MessageDispatcher dispatcher = new MessageDispatcher();

        // try {
        //     MoveHandler moveHandler = new MoveHandler();
        //     Method handleDistanceMethod = MoveHandler.class.getMethod("handleDistance", DistanceReceive.class);
        //     dispatcher.register(DistanceReceive.class, moveHandler, handleDistanceMethod);


        // } catch (NoSuchMethodException e) {
        //     System.out.println("Error registering handler method: " + e.getMessage());
        // } catch (Exception e) {
        //     System.out.println("Error during dispatcher initialization: " + e.getMessage());
        // }



        return dispatcher;
    }
}
