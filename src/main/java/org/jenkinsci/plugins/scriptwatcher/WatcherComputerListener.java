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
import hudson.slaves.*;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.DescribableList;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


@Extension
public class WatcherComputerListener extends ComputerListener {
    private static final Logger LOGGER = Logger.getLogger(WatcherComputerListener.class.getName());

    private static final String MARKER = "#:#:#";
    private static final String CAUSE_VAR = "CAUSE";
    private static final String CRLF = "\r\n";

    public WatcherComputerListener() {
        LOGGER.info("构造方法: public WatcherComputerListener()");
    }

    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        LOGGER.info("public void preLaunch(Computer c, TaskListener taskListener)");
        LOGGER.info("computer: " + c);
        LOGGER.info("taskListener: " + taskListener);

        taskListener.getLogger().println("preLaunch: taskListener: " + taskListener);

        if (c instanceof SlaveComputer) {
            TaskListener slaveListener = ((SlaveComputer) c).getListener();
            LOGGER.info("slaveListener: " + slaveListener);
            slaveListener.getLogger().println("preLaunch: slaveListener: " + slaveListener);
        }
    }

    @Override
    public void onLaunchFailure(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        LOGGER.info("public void onLaunchFailure(Computer c, TaskListener taskListener)");
        LOGGER.info("computer: " + c);
        LOGGER.info("taskListener: " + taskListener);
        taskListener.getLogger().println("onLaunchFailure: taskListener: " + taskListener);

        if (c instanceof SlaveComputer) {
            TaskListener slaveListener = ((SlaveComputer) c).getListener();
            LOGGER.info("slaveListener: " + slaveListener);
            slaveListener.getLogger().println("onLaunchFailure: slaveListener: " + slaveListener);
        }
    }

    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener taskListener) throws IOException, InterruptedException {
        LOGGER.info("public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) ");
        LOGGER.info("computer: " + c);
        LOGGER.info("taskListener: " + taskListener);
        LOGGER.info("FilePath: " + root);

        taskListener.getLogger().println("preOnline: taskListener: " + taskListener);

        if (c instanceof SlaveComputer) {
            TaskListener slaveListener = ((SlaveComputer) c).getListener();
            LOGGER.info("slaveListener: " + slaveListener);
            slaveListener.getLogger().println("preOnline: slaveListener: " + slaveListener);
        }
    }


    @Override
    public void onOnline(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        LOGGER.info("public void onOnline(Computer c, TaskListener listener)");
        LOGGER.info("computer: " + c);
        LOGGER.info("taskListener: " + taskListener);
        taskListener.getLogger().println("onOnline: taskListener: " + taskListener);

        if (c instanceof SlaveComputer) {
            TaskListener slaveListener = ((SlaveComputer) c).getListener();
            LOGGER.info("slaveListener: " + slaveListener);
            slaveListener.getLogger().println("onOnline: slaveListener: " + slaveListener);
        }
    }


    @Override
    public void onOffline(@Nonnull Computer c, @CheckForNull OfflineCause cause) {
        LOGGER.info("public void onOffline(Computer c, OfflineCause cause)");
        LOGGER.info("computer: " + c);
        LOGGER.info("cause: " + cause.toString());


        final Node slave = c.getNode();
        final Node master = Jenkins.get();

        if (slave == null) {
            LOGGER.info("slave node is null");
            return;
        }
        if (slave == master) {
            LOGGER.info("ignore master node");
            return;
        }
        LOGGER.info("slaveListener: " + ((SlaveComputer) c).getListener());
        TaskListener slaveListener = ((SlaveComputer) c).getListener();
        slaveListener.getLogger().println("onOffline: slaveListener:  " + slaveListener);

        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = (slave instanceof Jenkins) ? ((Jenkins) slave).getGlobalNodeProperties() : slave.getNodeProperties();
        WatcherNodeProperty watcherNodeProperty = properties.get(WatcherNodeProperty.class);
        LOGGER.info("watcherNodeProperty = " + watcherNodeProperty);
        if (watcherNodeProperty == null) {
            LOGGER.info("ignore watcherNodeProperty");
            return;
        }
        String script = watcherNodeProperty.getScripts();
        FilePath root = master.getRootPath();


        LOGGER.info("Master given path is " + root.getRemote());


        EnvVars enviroment = new EnvVars();


        FilePath scriptFile = null;
        try {
            final FilePath batchFile = createScriptFile(root, script); //ws.createTextTempFile("jenkins", ".bat", makeScript(), true);
            final String[] cmd = buildCommandLine(batchFile);

            Launcher launcher = root.createLauncher(slaveListener);

            launcher.launch().cmds(cmd).pwd(root).envs(enviroment).stdout(slaveListener).join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (scriptFile != null){
                    scriptFile.delete();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private String[] buildCommandLine(FilePath batchFile) {
        String[] cmd;
        if (isUnix()) {
            cmd = new String[]{"bash", batchFile.getRemote()};
        } else {
            cmd = new String[]{"cmd", "/c", "call", batchFile.getRemote()};
        }
        return cmd;
    }

    private FilePath createScriptFile(FilePath dir, String script) throws IOException, InterruptedException {
        return dir.createTextTempFile("jenkins", getFileExtension(), getContents(script), false);
    }

    private boolean isUnix() {
        return File.pathSeparatorChar == ':';
    }

    private String getFileExtension() {
        if (isUnix()) {
            return ".sh";
        } else {
            return ".bat";
        }
    }

    private String getContents(String script) {
        String contents = ""
                + "@set " + CAUSE_VAR + "=" + CRLF
                + "@echo off" + CRLF
                + "call :TheActualScript" + CRLF
                + "@echo off" + CRLF
                + "echo " + MARKER + CAUSE_VAR + MARKER + "%" + CAUSE_VAR + "%" + MARKER + CRLF
                + "goto :EOF" + CRLF
                + ":TheActualScript" + CRLF
                + script + CRLF;
        if (isUnix()) {
            return script;
        } else {
            return contents;
        }
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
