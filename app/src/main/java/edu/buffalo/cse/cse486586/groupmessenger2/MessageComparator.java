package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by tushar on 3/17/16.
 */
public class MessageComparator implements Comparator<Message>{
    @Override
    public int compare(Message m1, Message m2){
        int m1Val=m1.getAgreedPriority()+m1.getAvdId();
        int m2Val=m2.getAgreedPriority()+m2.getAvdId();
        if(m1Val< m1Val){
            return -1;
        }else if(m1Val> m1Val){
            return 1;
        }else {
            return 0;
        }

    }
}
