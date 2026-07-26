package com.tailg.lsposed.adblock;

import android.os.Bundle;

interface IConfigBridge {
    Bundle getSnapshot();
    boolean putBoolean(String key, boolean value);
    boolean putDistances(float unlockMeters, float lockMeters);
}
