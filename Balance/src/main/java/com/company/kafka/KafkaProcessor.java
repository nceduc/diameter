package com.company.kafka;


import com.company.balance.Balance;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static com.company.balance.Balance.*;

public class KafkaProcessor {

    private static final Logger logger = LogManager.getLogger(KafkaProcessor.class);

    public void start(){
        String clientID = null;
        String balance = null;
        boolean isClientNotFound = false;
        Balance balanceCassandra = new Balance(); //создали экземпляр и установили connection
        KafkaConsumer<String, String> consumer = new KafkaListener().getConsumer(); //получаем подписчика (слушателя)


        while (true) { //читаем топик requestBalance
            ConsumerRecords<String, String> records = consumer.poll(1000);
            if(records.count() > 1000){
                for (ConsumerRecord<String, String> ignored : records){
                    //если записей в кафке слишком много
                    System.out.println(records.count());
                    System.out.println("Only read...");
                    logger.info("Only read:"+records.count()+" records");
                }
            }else{
                for (ConsumerRecord<String, String> record : records){
                    System.out.println(records.count());
                    System.out.println("Read record");

                    try {
                        clientID = record.value(); // получаем клиентский ID
                        balance = balanceCassandra.getBalance(clientID); //получаем баланс

                        if(balance != null){
                            isClientNotFound = false;
                        }else if(isCassandraRunning()){
                            isClientNotFound = true;
                        }else{
                            connection = null;
                        }


                        KafkaRequest.writeRecordKafka(clientID, balance, isClientNotFound); //запись в кафку

                    }catch (NullPointerException e){
                        logger.error("Balance or clientID was not got\n" + e.getMessage());
                    }
                }
            }
        }
    }
}
