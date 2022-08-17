package net.kozakcuu.wxapkg.media;

public class DeviceScreenProperties {
    private double deviceWidth = 375;
    private double deviceHeight = 375;
    private double devicePixelRatio = 2;
    private boolean isForIOS = false;

    public final static double BASE_DEVICE_WIDTH = 750;

    public DeviceScreenProperties() {
    }

    public DeviceScreenProperties(double deviceWidth, double deviceHeight, double devicePixelRatio) {
        this.deviceWidth = deviceWidth;
        this.deviceHeight = deviceHeight;
        this.devicePixelRatio = devicePixelRatio;
    }

    public double getDeviceWidth() {
        return deviceWidth;
    }

    public void setDeviceWidth(double deviceWidth) {
        this.deviceWidth = deviceWidth;
    }

    public double getDeviceHeight() {
        return deviceHeight;
    }

    public void setDeviceHeight(double deviceHeight) {
        this.deviceHeight = deviceHeight;
    }

    public double getDevicePixelRatio() {
        return devicePixelRatio;
    }

    public void setDevicePixelRatio(double devicePixelRatio) {
        this.devicePixelRatio = devicePixelRatio;
    }

    public boolean isForIOS() {
        return isForIOS;
    }

    public void setForIOS(boolean forIOS) {
        isForIOS = forIOS;
    }
}
