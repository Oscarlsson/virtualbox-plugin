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
    // TODO: Doesn't this set it offline always?
    // TODO: This should stop the VM?
    @Override
    public void onCompleted(Run run, TaskListener listener) {
	  Computer computer = run.getExecutor().getOwner();
	  try{
        listener.getLogger().println("job compleded as " + run.getResult().toString());
        listener.getLogger().println("Setting it temporarily offline");
        computer.setTemporarilyOffline(true,null);
		computer.cliDisconnect("Completed job"); //"security"

        } catch(Exception e)
		{
		  listener.getLogger().println("Failed to shutdown");
		}
	  listener.getLogger().println("Bye bye VM!");

	}


    // TODO: This should start the VM?
    @Override
    public void onStarted(Run run, TaskListener listener) {
        listener.getLogger().println("..Going to start a build..");
        VirtualBoxComputer computer = (VirtualBoxComputer) run.getExecutor().getOwner();
        VirtualBoxComputerLauncher launcher = (VirtualBoxComputerLauncher) computer.getLauncher();

        try{
            launcher.launchVm(computer, listener);
        } catch (Exception e) {

        }

    }

}
