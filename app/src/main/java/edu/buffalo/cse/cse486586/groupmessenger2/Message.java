package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by tushar on 3/16/16.
 */
public class Message implements Serializable {
    private String message;
    private int initialPriority;
    private int proposedPriority;
    private int agreedPriority;
    private MessageType messageType;
    private int avdId;
    private boolean deliverable;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getInitialPriority() {
        return initialPriority;
    }

    public void setInitialPriority(int initialPriority) {
        this.initialPriority = initialPriority;
    }


    public int getProposedPriority() {
        return proposedPriority;
    }

    public void setProposedPriority(int proposedPriority) {
        this.proposedPriority = proposedPriority;
    }

    public int getAvdId() {
        return avdId;
    }

    public void setAvdId(int avdId) {
        this.avdId = avdId;
    }

    public int getAgreedPriority() {
        return agreedPriority;
    }

    public void setAgreedPriority(int agreedPriority) {
        this.agreedPriority = agreedPriority;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", initialPriority=" + initialPriority +
                ", proposedPriority=" + proposedPriority +
                ", agreedPriority=" + agreedPriority +
                ", messageType=" + messageType +
                ", avdId=" + avdId +
                ", deliverable=" + deliverable +
                '}';
    }

    public Message() {
    }

    public Message(String message, int initialPriority, MessageType messageType, int avdId) {
        this.message = message;
        this.initialPriority = initialPriority;
        this.messageType = messageType;
        this.avdId = avdId;
        this.proposedPriority=-1;
        this.agreedPriority=-1;
        deliverable = false;
    }

    public Message(Message message) {
        this.message = message.getMessage();
        this.initialPriority = message.getInitialPriority();
        this.messageType = message.getMessageType();
        this.avdId = message.getAvdId();
        this.proposedPriority=message.getProposedPriority();
        this.agreedPriority=message.getAgreedPriority();
        this.deliverable =message.isDeliverable();
    }


}
