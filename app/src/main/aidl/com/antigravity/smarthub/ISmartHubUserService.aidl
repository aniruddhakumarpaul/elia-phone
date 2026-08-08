// ISmartHubUserService.aidl
package com.antigravity.smarthub;

interface ISmartHubUserService {
    int executeShellCommand(in String command);
    String readSetting(in String table, in String key);
    void destroy();
}
