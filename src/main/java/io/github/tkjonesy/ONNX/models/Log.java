package io.github.tkjonesy.ONNX.models;

import io.github.tkjonesy.ONNX.enums.LogEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.Component;
import java.awt.Graphics;

@Getter
@AllArgsConstructor
public class Log {
    private LogEnum logType;
    private String message;
    private String timeStamp;

    public Log(LogEnum logType, String message) {
        this.logType = logType;
        this.message = message;
        this.timeStamp = getCurrentTimestamp();
    }

    private String getCurrentTimestamp(){
        return "["+java.time.LocalTime.now()+"]";
    }

    public Component getCuteLog(){
        return new Component() {
            @Override
            public void paint(Graphics g) {
                g.setColor(logType.getColor());
                g.drawString(timeStamp + " " + message, 0, 0);
            }
        };
    }
}
