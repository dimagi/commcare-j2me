package org.javarosa.services.transport.senders;

import org.javarosa.core.services.Logger;
import org.javarosa.services.transport.TransportCache;
import org.javarosa.services.transport.TransportListener;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.impl.TransportMessageStatus;

import java.util.Vector;

public class TransporterSharingSender {

    private Vector messages;
    private TransportCache cache;
    private TransportListener listener;
    boolean halted = false;
    private int maxConsecutiveFails = -1;

    public TransporterSharingSender() {
    }

    public void init(Vector messages, TransportCache store, TransportListener listener, int maxConsecutiveFails) {
        this.messages = messages;
        this.cache = store;
        this.listener = listener;
        halted = false;
        this.maxConsecutiveFails =maxConsecutiveFails;
    }

    public void send() {
        System.out.println("Ready to send: "+this.messages.size()+" messages");

        int numSuccessful = 0;
        int consecutiveFailures = 0;
        for (int i = 0; i < this.messages.size(); i++) {
            if(halted) {return;}

            TransportMessage message = (TransportMessage) this.messages.elementAt(i);

            //If we're over our limit, jut go ahead and update the listener for the rest of the messages.
            if(maxConsecutiveFails != -1 && consecutiveFailures >= maxConsecutiveFails) {
                this.listener.onStatusChange(message);
                continue;
            }

            this.listener.onChange(message, "Preparing to send: " + message);
            message.send();

            if (message.isCacheable()) {
                // if the loop was executed merely because the tries have been
                // used up, then the message becomes cached, for sending
                // via the "Send Unsent" user function
                if (!message.isSuccess()) {
                    consecutiveFailures++;
                    Logger.log("send-all", "fail on " + (i + 1) + "/" + messages.size() + " " + message.getFailureReason());

                    message.setStatus(TransportMessageStatus.CACHED);
                    this.listener.onStatusChange(message);

                    try {
                        this.cache.updateMessage(message);
                    } catch (Exception e) {
                        Logger.exception("TransportSharingSender.send/failure", e);
                        e.printStackTrace();
                        // if this update fails, the SENDING status
                        // isn't permanent (the message doesn't fall through
                        // a gap) because we note the duration of the
                        // SenderThread
                        // and, should a message be found with the status SENDING
                        // and yet was created before (now-queuing thread
                        // duration)
                        // then it is considered to have the SENDING status
                    }
                } else {
                    numSuccessful++;
                    consecutiveFailures = 0;

                    // SUCCESS - remove from cache
                    this.listener.onStatusChange(message);
                    try {
                        this.cache.decache(message);
                    } catch (Exception e) {
                        Logger.exception("TransportSharingSender.send/success", e);
                        e.printStackTrace();

                    }
                }
            } else {
                Logger.log("sanity", "TransportSharingSender.send msg not cacheable");
            }
        }

        String logMsg = (numSuccessful == messages.size() ? "success" : numSuccessful + "/" + messages.size() + " successful");
        String logTag = "send-all-success";
        if(maxConsecutiveFails != -1 && consecutiveFailures >= maxConsecutiveFails) {
            //We stopped because we failed too many times in a row.
            logMsg = "Send cancelled after " + consecutiveFailures + " failures. " + logMsg;
            logTag = "send-all-fail";
        }
        Logger.log(logTag, logMsg);
    }

    public void halt() {
        halted = true;
    }

    public void uninit() {
        listener = null;
        messages.removeAllElements();
        System.out.println("Send all unsent completed. Message queue emptied");
    }

}
