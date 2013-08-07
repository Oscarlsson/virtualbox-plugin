package hudson.plugins.virtualbox;

import java.util.ArrayList;
import java.util.List;
import org.virtualbox_4_2.*;


/**
 * @author Mihai Serban, Oscar Carlsson
 */
//TODO: Rearange the functions in better order.
public final class VirtualBoxControlV42 implements VirtualBoxControl {

    private final VirtualBoxManager virtualboxManager;
    private final IVirtualBox virtualBox;

    public VirtualBoxControlV42(String hostUrl, String userName, String password) {
        virtualboxManager = VirtualBoxManager.createInstance(null);
        virtualboxManager.connect(hostUrl, userName, password);
        virtualBox = virtualboxManager.getVBox();
    }

    public synchronized void disconnect() {
        try {
            virtualboxManager.disconnect();
        } catch (VBoxException e) {}
    }

    public synchronized boolean isConnected() {
        try {
            virtualBox.getVersion();
            return true;
        } catch (VBoxException e) {
            return false;
        }
    }

    /**
     * Get virtual machines installed on specified host.
     *
     * @param host VirtualBox host
     * @return list of virtual machines installed on specified host
     */
    public synchronized List<VirtualBoxMachine> getMachines(VirtualBoxCloud host, VirtualBoxLogger log) {
        List<VirtualBoxMachine> result = new ArrayList<VirtualBoxMachine>();
        for (IMachine machine : virtualBox.getMachines()) {
            String machineName = machine.getName();
            String machineId = machine.getId();
            result.add(new VirtualBoxMachine(host, machineName, machineId, null)); //TODO snapshot.id!

            if (machine.getSnapshotCount() > 0) {
                ISnapshot rootSnapshot = findRootSnapshot(machine);
                List<SnapshotData> snapshots = fillSnapshot(new ArrayList<SnapshotData>(), "", rootSnapshot);

                for (SnapshotData snapshot : snapshots){
                    result.add(new VirtualBoxMachine(host, machineName + "/" + snapshot.name, machineId, snapshot.id));
                }

            }
        }
        return result;
    }

    private class SnapshotData {
        String name;
        String id;
    }
    private static ISnapshot findRootSnapshot(IMachine machine){
        ISnapshot rootSnapshot = machine.getCurrentSnapshot();
        if (rootSnapshot.getParent() != null) {
            rootSnapshot = rootSnapshot.getParent();
        }
        return rootSnapshot;
    }

    private List<SnapshotData> fillSnapshot(List<SnapshotData> snapshotList, String snapshotPath, ISnapshot snapshot){
        if (snapshot != null){
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.name = snapshot.getName(); //snapshotPath + "/" + snapshot.getName();
            snapshotData.id = snapshot.getId();
            snapshotList.add(snapshotData);

            if (snapshot.getChildren() != null){
                for (ISnapshot child : snapshot.getChildren()){

                    snapshotList = fillSnapshot(snapshotList, snapshotData.name, child);
                }
            }
        }
        return snapshotList;
    }
    /**
     * Starts specified VirtualBox virtual machine.
     *
     * @param vbMachine virtual machine to start
     * @param type      session type (can be headless, vrdp, gui, sdl)
     * @param log
     * @return result code
     */
    public synchronized long startVm(VirtualBoxMachine vbMachine, String type, VirtualBoxLogger log) {
        String machineName = vbMachine.getName();
        String machineId = vbMachine.getMachineId();
        String snapshotId = vbMachine.getSnapshotId();

        IMachine machine = virtualBox.findMachine(machineId);
        if (null == machine) {
            log.logFatalError("Cannot find node: " + machineName);
            return -1;
        }

        ISnapshot snapshot = null;
        if (snapshotId != null) {
            snapshot = machine.findSnapshot(snapshotId);
        }

        // states diagram: https://www.virtualbox.org/sdkref/_virtual_box_8idl.html#80b08f71210afe16038e904a656ed9eb
        MachineState state = machine.getState();
        ISession session;
        IProgress progress;

        // wait for transient states to finish
        while (state.value() >= MachineState.FirstTransient.value() && state.value() <= MachineState.LastTransient.value()) {
            log.logInfo("node " + machineName + " in state " + state.toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            state = machine.getState();
        }

        if (MachineState.Running == state) {
            log.logInfo("node " + machineName + " in state " + state.toString());

            if (snapshotId != null) {
                log.logInfo("node " + machineName + " already started and snapshot could not be restored");
                return -1;
            }
            log.logInfo("node " + machineName + " started");
            return 0;
        }

        if (MachineState.Stuck == state || MachineState.Paused == state) {
            log.logInfo("starting node " + machineName + " from state " + state.toString());
            try {
                session = getSession(machine);
            } catch (Exception e) {
                log.logFatalError("node " + machineName + " openMachineSession: " + e.getMessage());
                return -1;
            }

            progress = null;
            if (MachineState.Stuck == state) {
                progress = session.getConsole().powerDown();
            } else if (MachineState.Paused == state) {
                session.getConsole().resume();
            }

            long result = 0; // success
            if (null != progress) {
                progress.waitForCompletion(-1);
                result = progress.getResultCode();
            }

            releaseSession(session, machine);
            if (0 != result) {
                log.logFatalError("node " + machineName + " error: " + getVBProcessError(progress));
                return -1;
            }

            if (MachineState.Stuck != state) {
                log.logInfo("node " + machineName + " started");
                return 0;
            }
            // continue from PoweredOff state
            state = machine.getState(); // update state
        }

        log.logInfo("starting node " + machineName + " from state " + state.toString());

        // powerUp from Saved, Aborted or PoweredOff states
        String env = "";   //TODO: why env?
        if (snapshot != null){
            session = getSession(machine);
            progress = session.getConsole().restoreSnapshot(snapshot);
            progress.waitForCompletion(-1);
            session.unlockMachine();
        }
        session = getSession(null);
        progress = machine.launchVMProcess(session, type, env);

        progress.waitForCompletion(-1);
        long result = progress.getResultCode();
        releaseSession(session, machine);

        if (0 != result) {
            log.logFatalError("node " + machineName + " error: " + getVBProcessError(progress));
        } else {
            log.logInfo("node " + machineName + " started");
        }

        return result;
    }

//    //TODO: Testing purpose
//    public synchronized void powerDownVm(VirtualBoxMachine vbMachine, VirtualBoxLogger log){
//        String machineId = vbMachine.getMachineId();
//        IMachine machine = virtualBox.findMachine(machineId);
//
//        MachineState state = machine.getState();
//        ISession session;
//        IProgress progress;
//        session = getSession(machine);
//        progress = session.getConsole().powerDown();
//        progress.waitForCompletion(-1);
//
//    }

    /**
     * Stops specified VirtualBox virtual machine.
     *
     * @param vbMachine virtual machine to stop
     * @param log
     * @return result code
     */
    public synchronized long stopVm(VirtualBoxMachine vbMachine, String stopMode, VirtualBoxLogger log) {
        String machineName = vbMachine.getMachineName();
        String machineId = vbMachine.getMachineId();
        String snapshotId = vbMachine.getSnapshotId();

        IMachine machine = virtualBox.findMachine(machineId);

        if (null == machine) {
            log.logFatalError("Cannot find node: " + machineName);
            return -1;
        }

        ISnapshot snapshot = null;
        if (snapshotId != null) {
            snapshot = machine.findSnapshot(snapshotId);
        }

        // states diagram: https://www.virtualbox.org/sdkref/_virtual_box_8idl.html#80b08f71210afe16038e904a656ed9eb
        MachineState state = machine.getState();
        ISession session;
        IProgress progress;

        // wait for transient states to finish
        while (state.value() >= MachineState.FirstTransient.value() && state.value() <= MachineState.LastTransient.value()) {
            log.logInfo("node " + machineName + " in state " + state.toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            state = machine.getState();
        }

        log.logInfo("stopping node " + machineName + " from state " + state.toString());

        if (MachineState.Aborted == state || MachineState.PoweredOff == state
                || MachineState.Saved == state) {
            log.logInfo("node " + machineName + " stopped");
            return 0;
        }

        try {
            session = getSession(machine);
        } catch (Exception e) {
            log.logFatalError("node " + machineName + " openMachineSession: " + e.getMessage());
            return -1;
        }

        if (MachineState.Stuck == state || "powerdown".equals(stopMode)) {                 //TODO: Hardcoded stopMode! There are a enum of them.
            // for Stuck state call powerDown and go to PoweredOff state
            progress = session.getConsole().powerDown();
        }else if (snapshot != null) {
            progress = session.getConsole().powerDown();
            progress.waitForCompletion(-1);

            session = getSession(machine);
            progress = session.getConsole().restoreSnapshot(snapshot);
            progress.waitForCompletion(-1);
            session.unlockMachine();
        } else {
            // Running or Paused
            progress = session.getConsole().saveState();
        }

        progress.waitForCompletion(-1);
        long result = progress.getResultCode();

        releaseSession(session, machine);

        if (0 != result) {
            log.logFatalError("node " + machineName + " error: " + getVBProcessError(progress));
        } else {
            log.logInfo("node " + machineName + " stopped");
        }

        return result;
    }

    /**
     * MAC Address of specified virtual machine.
     *
     * @param vbMachine virtual machine
     * @return MAC Address of specified virtual machine
     */
    public synchronized String getMacAddress(VirtualBoxMachine vbMachine, VirtualBoxLogger log) {
        IMachine machine = virtualBox.findMachine(vbMachine.getName());
        String macAddress = machine.getNetworkAdapter(0L).getMACAddress();
        return macAddress;
    }

    private String getVBProcessError(IProgress progress) {
        if (0 == progress.getResultCode()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("");
        IVirtualBoxErrorInfo errInfo = progress.getErrorInfo();
        while (null != errInfo) {
            sb.append(errInfo.getText());
            sb.append("\n");
            errInfo = errInfo.getNext();
        }
        return sb.toString();
    }

    private boolean isTransientState(SessionState state) {
        return SessionState.Spawning == state || SessionState.Unlocking == state;
    }

    //TODO: Dry
    private ISession getSession(IMachine machine) {
        ISession session = virtualboxManager.getSessionObject();
        if (null != machine) {
            machine.lockMachine(session, LockType.Shared);
            while (isTransientState(machine.getSessionState())) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
        }

        while (isTransientState(session.getState())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }

        return session;
    }

    //TODO : DRY
    private void releaseSession(ISession session, IMachine machine) {
        while (isTransientState(machine.getSessionState()) || isTransientState(session.getState())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }

        try {
            session.unlockMachine();
        } catch (VBoxException e) {}

        while (isTransientState(machine.getSessionState()) || isTransientState(session.getState())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
    }
}
