package hudson.plugins.virtualbox;

/* CJ: Extensions for RunListener*/

import hudson.Extension;
import hudson.model.listeners.RunListener;
import hudson.slaves.*;
import hudson.model.Node;

import java.io.Serializable;
import java.util.logging.Logger;

import hudson.model.*;


/* CJ: Start normal import*/
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;

import org.kohsuke.stapler.DataBoundConstructor;

// http://jenkins.361315.n4.nabble.com/Take-a-slave-off-line-if-a-job-fails-td377500.html

/**
 * @author Christian John, Oscar Carlsson
 */
@Extension
public class VirtualBoxListener extends RunListener<Run> implements Serializable {
    private static final Logger LOG = Logger.getLogger(VirtualBoxComputerLauncher.class.getName());


    public VirtualBoxListener() {
        super(Run.class);
    }


    /* onCompleted EP: RunListener*/
    @Override
    public void onCompleted(Run run, TaskListener listener) {
        listener.getLogger().println("job compleded as " + run.getResult().toString());
        Computer computer = run.getExecutor().getOwner();

        if (computer instanceof VirtualBoxComputer) {
            VirtualBoxComputer virtualboxcomputer = (VirtualBoxComputer) computer;

            //VirtualBoxComputerLauncher launcher = (VirtualBoxComputerLauncher) computer.getLauncher();
            VirtualBoxSlave slave = ((VirtualBoxComputer) virtualboxcomputer).getNode();

            String hostname           = slave.getHostName();
            String virtualmachinename = slave.getVirtualMachineName();
            VirtualBoxMachine machine = VirtualBoxPlugin.getVirtualBoxMachine(hostname, virtualmachinename);

            listener.getLogger().println("job compleded as " + run.getResult().toString());
            VirtualBoxUtils.stopVm(machine, slave.getVirtualMachineStopMode(), new VirtualBoxTaskListenerLog(listener, "[VirtualBox] "));
        }else{
            super.onCompleted(run, listener);
        }

    }


    @Override
    public void onStarted(Run run, TaskListener listener) {
        listener.getLogger().println("..Going to start a build..");
        Computer computer = run.getExecutor().getOwner();

        if (computer instanceof VirtualBoxComputer) {
            VirtualBoxComputer virtualboxcomputer = (VirtualBoxComputer) computer;
            VirtualBoxComputerLauncher launcher = (VirtualBoxComputerLauncher) virtualboxcomputer.getLauncher();

            try {
                launcher.launchVm(virtualboxcomputer, listener);
            } catch (Exception e) {

            }

        }else{
            super.onStarted(run,listener);
        }


        }

    }
