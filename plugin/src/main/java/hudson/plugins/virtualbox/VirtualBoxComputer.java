package hudson.plugins.virtualbox;

import com.sun.xml.ws.commons.virtualbox_3_1.IProgress;
import com.sun.xml.ws.commons.virtualbox_3_1.ISession;
import hudson.model.Slave;
import hudson.slaves.ComputerListener;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.concurrent.Future;

import hudson.util.Futures;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.virtualbox_4_2.IMachine;
import org.virtualbox_4_2.IProcess;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

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
