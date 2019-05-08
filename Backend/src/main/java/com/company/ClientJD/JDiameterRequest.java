package com.company.ClientJD;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class JDiameterRequest {


    private static final Logger logger = LogManager.getLogger(JDiameterRequest.class);


    public String getBalance(String clientID){
        Session session = null;
        Request request = null;
        String balance = null;

        session = getSession();
        if(session != null){
            request = formDiameterRequest(session, clientID);
            balance = sendDiameterRequest(request, session);
        }

        return balance;
    }


    private Session getSession(){
        Session session = null;

        try {
            session = JDiameterConnectServer.connectionSession.getNewSession();
        } catch (InternalException e) {
            logger.error("Connection with diameter server was failed or Diameter-Server failed. Need to reboot!\n" + e.getMessage());
        }

        return session;
    }


    private Request formDiameterRequest(Session session, String clientID){
        Request request = null;

        request = session.createRequest(3000, ApplicationId.createByAuthAppId(33333),"exchange.example.org","127.0.0.1"); //формируем запрос
        request.getAvps().addAvp(Avp.USER_IDENTITY, clientID.getBytes()); //положили клиентский ID в Avp.USER_IDENTITY

        return request;
    }


    private String sendDiameterRequest(Request request, Session session){
        Avp avp = null;
        AvpSet avpSet = null;
        String answer = null;
        Future<Message> response = null;

        try {
            response = session.send(request); //посылаем запрос
            avpSet = response.get().getAvps(); //получаем все AVP ответа
            avp = avpSet.getAvp(Avp.CHECK_BALANCE_RESULT);

            if(avp != null){
                //if AVP with balance is not empty
                answer = avp.getUTF8String(); //получаем баланс

            }else{
                //if AVP with balance is empty
                avp = avpSet.getAvp(Avp.RESULT_CODE); //check errors

                if(avp != null){
                    if(avp.getInteger32() == ErrorCode.ERROR_TYPE_REQUEST){
                        logger.error("This client sends wrong type Diameter-requests.\n " + avpSet.getAvp(Avp.ERROR_MESSAGE).getUTF8String());
                    }

                    if(avp.getInteger32() == ErrorCode.RELOADING_APPS){
                        logger.warn("Some apps of server reloading... Wait!");
                    }
                }
            }
        } catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
            logger.error("Send diameter request failed\n" + e.getMessage());

        } catch (AvpDataException | InterruptedException | ExecutionException | NullPointerException e){
            logger.error("Balance was not received\n" + e.getMessage());

        }

        return answer;
    }


}
