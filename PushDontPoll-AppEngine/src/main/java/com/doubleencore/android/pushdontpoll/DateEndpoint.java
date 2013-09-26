package com.doubleencore.android.pushdontpoll;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonAutoDetect;
import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonProperty;

import java.io.IOException;
import java.util.Date;

@Api(name = "dateendpoint", namespace = @ApiNamespace(ownerDomain = "doubleencore.com", ownerName = "doubleencore.com", packagePath = "android.pushdontpoll"))
public class DateEndpoint {

    private static final String API_KEY = "";

    private static final DeviceInfoEndpoint endpoint = new DeviceInfoEndpoint();

    @JsonAutoDetect
    public static class CurrentDate {

        @JsonProperty
        private long date = new Date().getTime();

        public long getDate() {
            return date;
        }
    }

    @ApiMethod(name = "currentDate")
    public CurrentDate getDate() {
        return new CurrentDate();
    }

    /**
     * Sends the current time as a playload in the GCM message to up to 10 devices
     *
     * @return
     * @throws java.io.IOException
     */
    @ApiMethod(name = "sendUpdate")
    public void sendUpdate() throws IOException {
        Sender sender = new Sender(API_KEY);

        // Add the time to the payload data so it shows up in the intent as "time"
        String newTime = String.valueOf(new Date().getTime());
        Message message = new Message.Builder()
                .addData("new_time", newTime)
                .build();

        // ping a max of 10 registered devices
        CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null, 10);
        for (DeviceInfo deviceInfo : response.getItems()) {
            doSendViaGcm(message, sender, deviceInfo);
        }
    }

    /**
     * Sends a send to sync GCM message to up to 10 devices
     *
     * @return
     * @throws java.io.IOException
     */
    @ApiMethod(name = "sendTickle")
    public void sendTickle() throws IOException {
        Sender sender = new Sender(API_KEY);

        Message message = new Message.Builder()
                .collapseKey("syncTime")
                .build();

        // ping a max of 10 registered devices
        CollectionResponse<DeviceInfo> response = endpoint.listDeviceInfo(null, 10);
        for (DeviceInfo deviceInfo : response.getItems()) {
            doSendViaGcm(message, sender, deviceInfo);
        }
    }

    /**
     * Sends the message using the Sender object to the registered device.
     *
     * @param sender     the Sender object to be used for ping,
     * @param deviceInfo the registration id of the device.
     * @return Result the result of the ping.
     */
    private static Result doSendViaGcm(Message message, Sender sender,
                                       DeviceInfo deviceInfo) throws IOException {
        Result result = sender.send(message, deviceInfo.getDeviceRegistrationID(), 5);
        if (result.getMessageId() != null) {
            String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
                deviceInfo.setDeviceRegistrationID(canonicalRegId);
                endpoint.insertDeviceInfo(deviceInfo);
            }
        } else {
            String error = result.getErrorCodeName();
            if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                endpoint.removeDeviceInfo(deviceInfo.getDeviceRegistrationID());
            }
        }

        return result;
    }
}
