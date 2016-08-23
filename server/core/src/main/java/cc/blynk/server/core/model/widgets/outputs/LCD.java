package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.utils.LimitedQueue;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Command.SYNC;
import static cc.blynk.utils.ByteBufUtil.makeStringMessage;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LCD extends MultiPinWidget implements FrequencyWidget {

    //configured property via server.properties
    private static final int POOL_SIZE = ParseUtil.parseInt(System.getProperty("lcd.strings.pool.size", "6"));
    private transient final List<String> lastCommands = new LimitedQueue<>(POOL_SIZE);
    public boolean advancedMode;
    public String textFormatLine1;
    public String textFormatLine2;
    //todo remove after migration.
    public boolean textLight;
    public boolean textLightOn;
    private int frequency;
    private transient Map<String, Long> lastRequestTS = new HashMap<>();

    private static void sendSyncOnActivate(Pin pin, int dashId, Channel appChannel) {
        if (pin.notEmpty()) {
            String body = dashId + BODY_SEPARATOR_STRING + pin.makeHardwareBody();
            appChannel.write(makeStringMessage(SYNC, 1111, body), appChannel.voidPromise());
        }
    }

    @Override
    public boolean updateIfSame(byte pinIn, PinType type, String value) {
        boolean isSame = false;
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, type)) {
                    pin.value = value;
                    isSame = true;
                }
            }
            if (advancedMode && isSame) {
                lastCommands.add(value);
            }
        }
        return isSame;
    }

    @Override
    public void sendSyncOnActivate(Channel appChannel, int dashId) {
        if (pins == null) {
            return;
        }
        if (advancedMode) {
            for (String command : lastCommands) {
                pins[0].value = command;
                sendSyncOnActivate(pins[0], dashId, appChannel);
            }
        } else {
            for (Pin pin : pins) {
                sendSyncOnActivate(pin, dashId, appChannel);
            }
        }
    }

    @Override
    public boolean isSplitMode() {
        return !advancedMode;
    }

    @Override
    public final int getFrequency() {
        return frequency;
    }

    @Override
    public final long getLastRequestTS(String body) {
        return lastRequestTS.getOrDefault(body, 0L);
    }

    @Override
    public final void setLastRequestTS(String body, long now) {
        this.lastRequestTS.put(body, now);
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public int getPrice() {
        return 400;
    }
}
