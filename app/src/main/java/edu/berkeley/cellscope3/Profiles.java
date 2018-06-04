package edu.berkeley.cellscope3;

import edu.berkeley.cellscope3.device.ble.BleProfile;

public final class Profiles {

    public static final BleProfile RBL_PROFILE =
            new BleProfile(
                    "713D0000-503E-4C75-BA94-3148F18D941E" /* serviceUuid */,
                    "00002902-0000-1000-8000-00805f9b34fb" /* clientConfig */,
                    "713D0003-503E-4C75-BA94-3148F18D941E" /* txUuid */,
                    "713D0002-503E-4C75-BA94-3148F18D941E" /* rxUuid */);
}
