/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.scriptwatcher;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.slaves.OfflineCause;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.DescribableList;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


@Extension
public class WatcherComputerListener extends ComputerListener {
    private static final Logger LOGGER = Logger.getLogger(WatcherComputerListener.class.getName());

    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        LOGGER.info("public void preLaunch(Computer c, TaskListener taskListener)");
        LOGGER.info("computer: " + c);
    }

    @Override
    public void onLaunchFailure(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        LOGGER.info("public void onLaunchFailure(Computer c, TaskListener taskListener)");
        LOGGER.info("computer: " + c);
    }

    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        LOGGER.info("public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) ");
        LOGGER.info("computer: " + c);

    }


    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        LOGGER.info("public void onOnline(Computer c, TaskListener listener)");
        LOGGER.info("computer: " + c);
    }


    @Override
    public void onOffline(@Nonnull Computer c, @CheckForNull OfflineCause cause) {
        LOGGER.info("public void onOffline(@Nonnull Computer c, @CheckForNull OfflineCause cause)");
        LOGGER.info("computer: " + c);
        LOGGER.info("cause: " + cause);
        String script = "env";

        final Node slave = c.getNode();
        final Node master = Jenkins.get();

        if (slave == null)
            return;

        if (slave == master) {
            LOGGER.info("ignore master node");
            return;
        }

        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = (slave instanceof Jenkins) ? ((Jenkins) slave).getGlobalNodeProperties() : slave.getNodeProperties();
        WatcherNodeProperty watcherNodeProperty = properties.get(WatcherNodeProperty.class);
        LOGGER.info("watcherNodeProperty = " + watcherNodeProperty);
        if(watcherNodeProperty==null){
            LOGGER.info("ignore watcherNodeProperty");
            return;
        }
        FilePath root = master.getRootPath();

        LOGGER.info("Master given path is " + root.getRemote());

        final LogTaskListener listener = new LogTaskListener(LOGGER, Level.INFO);

        EnvVars enviroment = new EnvVars();


        Shell shell = new Shell(script);
        FilePath scriptFile = null;
        try {
            Launcher launcher = root.createLauncher(listener);
            scriptFile = shell.createScriptFile(root);
            launcher.launch().cmds(shell.buildCommandLine(scriptFile)).pwd(root).envs(enviroment).stdout(listener).join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (scriptFile != null)
                    scriptFile.delete();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private static WatcherNodeProperty getWatcherNodeProperty(final Computer computer) {
        final Node node = computer.getNode();
        if (node == null) return null;
        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = (node instanceof Jenkins) ? ((Jenkins) node).getGlobalNodeProperties() : node.getNodeProperties();
        return properties.get(WatcherNodeProperty.class);
    }

    @Override
    public void onTemporarilyOnline(Computer c) {
        LOGGER.info("public void onTemporarilyOnline(Computer c)");
        LOGGER.info("computer: " + c);
    }

    @Override
    public void onTemporarilyOffline(Computer c, OfflineCause cause) {
        LOGGER.info("public void onTemporarilyOffline(Computer c, OfflineCause cause)");
        LOGGER.info("computer: " + c);
        LOGGER.info("cause: " + cause);
    }

    @Override
    public void onConfigurationChange() {
        LOGGER.info(" public void onConfigurationChange()");
    }
}
