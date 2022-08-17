package net.kozakcuu.wxapkg.media;

public class ResponsivePixelTransformer {
    private DeviceScreenProperties screenProperties;

    public ResponsivePixelTransformer(DeviceScreenProperties screenProperties) {
        this.screenProperties = screenProperties;
    }

    public DeviceScreenProperties getScreenProperties() {
        return screenProperties;
    }

    public void setScreenProperties(DeviceScreenProperties screenProperties) {
        this.screenProperties = screenProperties;
    }

    public double transform(double rpx) {
        if(rpx <= 0.0) return 0;
        double result = Math.floor(rpx / DeviceScreenProperties.BASE_DEVICE_WIDTH * screenProperties.getDeviceWidth());
        if (result <= 0) {
            if(screenProperties.getDevicePixelRatio() == 1 && !screenProperties.isForIOS()) return 1; else return 0.5;
        }
        return result;
    }
}
