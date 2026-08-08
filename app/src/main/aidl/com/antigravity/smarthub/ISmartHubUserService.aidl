// ISmartHubUserService.aidl
package com.antigravity.smarthub;

interface ISmartHubUserService {
    int setRefreshRateMode(int mode);
    int setStandbyBucket(in String packageName, in String bucket);
    int setAppOpsBackground(in String packageName, in String mode);
    String readSetting(in String table, in String key);
    int readStandbyBucket(in String packageName);
    String readAppOpsBackground(in String packageName);
    void destroy();
}
