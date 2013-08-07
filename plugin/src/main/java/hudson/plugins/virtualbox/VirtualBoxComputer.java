package hudson.plugins.virtualbox;

import hudson.model.Slave;
import hudson.slaves.SlaveComputer;
import org.kohsuke.stapler.HttpResponse;

import java.io.IOException;

/**
 * @author Evgeny Mandrikov, Oscar Carlsson
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
        if (!hasBeenStarted)
            return super.isOffline();
        else
            return !hasBeenStarted;

    }

    public void setHasBeenStarted(boolean hasBeenStarted) {
        this.hasBeenStarted = hasBeenStarted;
    }

    @Override
    public HttpResponse doDoDelete() throws IOException {
        // TODO powerOff on delete


        return super.doDoDelete();
    }
}
