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
package org.jenkinsci.plugins.qywechatwatcher;

import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;


public class WatcherNodeProperty extends NodeProperty<Node> {

    private final String onlineAddresses;
    private final String offlineAddresses;

    @DataBoundConstructor
    public WatcherNodeProperty(final String onlineAddresses, final String offlineAddresses) {
        this.onlineAddresses = onlineAddresses;
        this.offlineAddresses = offlineAddresses;
    }

    public String getOnlineAddresses() {
        return onlineAddresses;
    }

    public String getOfflineAddresses() {
        return offlineAddresses;
    }

    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {

        public static final String OFFLINE_ADDRESSES = "offlineAddresses";
        public static final String ONLINE_ADDRESSES = "onlineAddresses";

        @Override
        public boolean isApplicable(Class<? extends Node> nodeType) {

            return true;
        }

        @Override
        public NodeProperty<?> newInstance(final StaplerRequest req, final JSONObject formData) throws FormException {
            final String onlineAddresses = formData.getString(ONLINE_ADDRESSES);
            final String offlineAddresses = formData.getString(OFFLINE_ADDRESSES);

            assert onlineAddresses != null;
            assert offlineAddresses != null;

            if (onlineAddresses.isEmpty() && offlineAddresses.isEmpty()) return null;

            return new WatcherNodeProperty(onlineAddresses, offlineAddresses);
        }

        public FormValidation doCheckOnlineAddresses(@QueryParameter String value) {
            return MailWatcherMailer.validateMailAddresses(value);
        }

        public FormValidation doCheckOfflineAddresses(@QueryParameter String value) {
            return MailWatcherMailer.validateMailAddresses(value);
        }

        @Override
        public String getDisplayName() {
            return "Qywechat when Node online status changes";
        }
    }
}
