package hudson.plugins.virtualbox;

import hudson.model.Slave;
import hudson.slaves.ComputerListener;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import java.util.concurrent.Future;

import hudson.util.Futures;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Evgeny Mandrikov
 */
public class VirtualBoxComputer extends SlaveComputer {
  private boolean hasBeenStarted;

  public VirtualBoxComputer(Slave slave) {
    super(slave);
    this.setHasBeenStarted(false);
  }

  @Override
  public VirtualBoxSlave getNode() {
    return (VirtualBoxSlave) super.getNode();
  }

  //Once it has been started it will override offlines
  @Override
  public boolean isOffline() {
    return !hasBeenStarted || super.isOffline();
  }

  public void setHasBeenStarted(boolean hasBeenStarted){
      this.hasBeenStarted = hasBeenStarted;
  }

  @Override
  public HttpResponse doDoDelete() throws IOException {
    //VirtualBoxSlave slave =  (VirtualBoxSlave) super.getNode();
    //a.delete_PowerOff();
    //VirtualBoxComputerLauncher launcher = (VirtualBoxComputerLauncher) this.getLauncher();
    //launcher.stopVm()
    //VirtualBoxMachine machine = VirtualBoxPlugin.getVirtualBoxMachine(slave.getHostName(), slave.getVirtualMachineName());
    // TODO powerOff on delete
    return super.doDoDelete();
  }
}
